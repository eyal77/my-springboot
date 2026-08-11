const express = require('express');
const zookeeper = require('node-zookeeper-client');
const { createProxyMiddleware } = require('http-proxy-middleware');
const path = require('path');

const PORT = process.env.PORT || 8080;
const ZOOKEEPER_CONNECT = process.env.ZOOKEEPER_CONNECT || 'localhost:2181';

const app = express();

// Global variable to store the discovered backend service URL
let backendUrl = null;

// Connect to ZooKeeper
const zkClient = zookeeper.createClient(ZOOKEEPER_CONNECT);

function discoverBackend() {
  zkClient.getChildren(
    '/services/backend-service',
    (event) => {
      console.log('ZooKeeper Watcher: Node children changed. Event:', event);
      discoverBackend(); // Re-register the watcher
    },
    (error, children, stats) => {
      if (error) {
        console.error('Failed to retrieve children of /services/backend-service:', error);
        return;
      }

      if (children.length === 0) {
        console.warn('ZooKeeper Registry: No active backend service instances found.');
        backendUrl = null;
        return;
      }

      // Read the data of the first instance found
      const firstInstancePath = `/services/backend-service/${children[0]}`;
      zkClient.getData(
        firstInstancePath,
        (error, data, stat) => {
          if (error) {
            console.error(`Failed to read data of ${firstInstancePath}:`, error);
            return;
          }

          try {
            const metadata = JSON.parse(data.toString('utf8'));
            backendUrl = metadata.uri;
            console.log(`ZooKeeper Discovery: Active backend instance resolved at -> ${backendUrl}`);
          } catch (e) {
            backendUrl = data.toString('utf8').trim();
            console.log(`ZooKeeper Discovery: Active backend instance resolved (raw string) -> ${backendUrl}`);
          }
        }
      );
    }
  );
}

zkClient.once('connected', () => {
  console.log('Successfully connected to ZooKeeper at:', ZOOKEEPER_CONNECT);
  // Ensure the parent services directory path exists, then run discovery
  zkClient.mkdirp('/services/backend-service', (err) => {
    if (err) {
      console.error('Failed to create /services/backend-service base directory:', err);
    }
    discoverBackend();
  });
});

zkClient.connect();

// Setup proxy for backend API and Swagger UI documentation
const backendProxy = createProxyMiddleware({
  target: 'http://localhost:8081', // Fallback target
  pathFilter: ['/api', '/swagger-ui', '/v3/api-docs'],
  router: () => {
    return backendUrl;
  },
  changeOrigin: true,
  ws: true,
  on: {
    proxyReq: (proxyReq, req, res) => {
      if (!backendUrl) {
        console.error('Request received, but backend service is not registered in ZooKeeper!');
        res.status(503).json({
          status: 503,
          error: 'Service Unavailable',
          message: 'No backend instances registered in ZooKeeper.'
        });
      }
    },
    error: (err, req, res) => {
      console.error('Proxy connection error:', err.message);
      res.status(502).json({
        status: 502,
        error: 'Bad Gateway',
        message: 'Could not connect to the backend service instance.'
      });
    }
  }
});

// Route API and Swagger requests through proxy (globally registered to preserve path prefixes)
app.use(backendProxy);

// Serve static UI assets
app.use(express.static(path.join(__dirname, 'public')));

// Fallback to index.html for UI client router
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start the server
app.listen(PORT, () => {
  console.log(`Frontend service running on http://localhost:${PORT}`);
});
