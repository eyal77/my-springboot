package com.example.eyal.rest.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.nodes.PersistentEphemeralNode;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class ZooKeeperRegistrationService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ZooKeeperRegistrationService.class);

    @Value("${zookeeper.connectionString:localhost:2181}")
    private String connectionString;

    @Value("${zookeeper.serviceName:backend-service}")
    private String serviceName;

    @Value("${server.port:8081}")
    private int port;

    @Value("${zookeeper.enabled:true}")
    private boolean enabled;

    private CuratorFramework client;
    private PersistentEphemeralNode persistentNode;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!enabled) {
            log.info("ZooKeeper registration is disabled.");
            return;
        }

        log.info("Starting ZooKeeper registration service. Connecting to {}", connectionString);
        try {
            // Set up curator client with retries (will retry automatically in background)
            client = CuratorFrameworkFactory.builder()
                    .connectString(connectionString)
                    .retryPolicy(new ExponentialBackoffRetry(1000, 29))
                    .build();
            client.start();

            String servicePath = "/services/" + serviceName;
            String host = "localhost";
            String metadata = String.format("{\"host\":\"%s\",\"port\":%d,\"uri\":\"http://%s:%d\"}", host, port, host, port);

            String instanceNodeName = "instance-" + host + "-" + port;
            String nodePath = servicePath + "/" + instanceNodeName;

            // Use PersistentEphemeralNode to automatically register when Zookeeper comes up,
            // and recreate the node automatically on session reconnection.
            persistentNode = new PersistentEphemeralNode(
                    client,
                    PersistentEphemeralNode.Mode.EPHEMERAL,
                    nodePath,
                    metadata.getBytes()
            );
            persistentNode.start();
            log.info("Successfully initialized PersistentEphemeralNode registration at path: {} with metadata: {}", nodePath, metadata);

        } catch (Exception e) {
            log.error("Failed to initialize ZooKeeper registration. Service will run without registration.", e);
            if (persistentNode != null) {
                try {
                    persistentNode.close();
                } catch (Exception pe) {
                    // Ignore
                }
                persistentNode = null;
            }
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ce) {
                    // Ignore
                }
                client = null;
            }
        }
    }

    @Component
    public static class ShutdownListener implements ApplicationListener<ContextClosedEvent> {
        private final ZooKeeperRegistrationService registrationService;

        public ShutdownListener(ZooKeeperRegistrationService registrationService) {
            this.registrationService = registrationService;
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            registrationService.shutdown();
        }
    }

    public synchronized void shutdown() {
        log.info("Shutting down ZooKeeper registration service.");
        if (persistentNode != null) {
            try {
                persistentNode.close();
                log.info("Persistent node closed.");
            } catch (Exception e) {
                log.error("Error closing persistent node", e);
            } finally {
                persistentNode = null;
            }
        }
        if (client != null) {
            try {
                client.close();
                log.info("ZooKeeper client closed.");
            } catch (Exception e) {
                log.error("Error closing ZooKeeper client", e);
            } finally {
                client = null;
            }
        }
    }
}
