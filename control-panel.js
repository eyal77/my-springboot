const http = require('http');
const { exec } = require('child_process');
const net = require('net');
const path = require('path');

const PORT = 8090;
let logBuffer = '=== Service Control Panel Initialized ===\n';

// Helper to append messages to the log buffer
function appendLog(message) {
    const timestamp = new Date().toLocaleTimeString();
    logBuffer += `[${timestamp}] ${message}\n`;
    // Cap log buffer size at 50,000 characters to prevent memory leaks
    if (logBuffer.length > 50000) {
        logBuffer = logBuffer.substring(logBuffer.length - 30000);
    }
}

// TCP Port checker
function checkPort(port) {
    return new Promise((resolve) => {
        const socket = new net.Socket();
        socket.setTimeout(800);
        
        const cleanUp = () => {
            socket.destroy();
        };

        socket.on('connect', () => {
            cleanUp();
            resolve(true);
        });

        socket.on('error', () => {
            cleanUp();
            resolve(false);
        });

        socket.on('timeout', () => {
            cleanUp();
            resolve(false);
        });

        socket.connect(port, '127.0.0.1');
    });
}

// Execute batch script and capture output
function runScript(scriptName) {
    appendLog(`Triggering ${scriptName}...`);
    const proc = exec(scriptName, { cwd: __dirname });

    proc.stdout.on('data', (data) => {
        logBuffer += data.toString();
    });

    proc.stderr.on('data', (data) => {
        logBuffer += `[ERROR] ${data.toString()}`;
    });

    proc.on('close', (code) => {
        appendLog(`${scriptName} finished with exit code ${code}`);
    });
}

// HTTP Request Handler
const server = http.createServer(async (req, res) => {
    // API endpoints
    if (req.url === '/api/status' && req.method === 'GET') {
        const zkOnline = await checkPort(2181);
        const bootOnline = await checkPort(8081);
        const nodeOnline = await checkPort(8080);
        
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            zookeeper: { name: 'ZooKeeper Server', port: 2181, online: zkOnline },
            backend: { name: 'Spring Boot Backend', port: 8081, online: bootOnline },
            frontend: { name: 'Node.js Frontend', port: 8080, online: nodeOnline }
        }));
        return;
    }

    if (req.url === '/api/start' && req.method === 'POST') {
        runScript('start-all.bat');
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Start-All script triggered.' }));
        return;
    }

    if (req.url === '/api/stop' && req.method === 'POST') {
        runScript('stop-all.bat');
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Stop-All script triggered.' }));
        return;
    }

    if (req.url === '/api/logs' && req.method === 'GET') {
        res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end(logBuffer);
        return;
    }

    if (req.url === '/api/clear-logs' && req.method === 'POST') {
        logBuffer = '=== Logs Cleared ===\n';
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Logs cleared.' }));
        return;
    }

    // Serve Static Dashboard Page
    if (req.url === '/' || req.url === '/index.html') {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(getHtmlContent());
        return;
    }

    // Fallback for 404
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('404 Not Found');
});

// HTML Page Content (Dashboard with CSS & JS embedded)
function getHtmlContent() {
    return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Service Control Dashboard</title>
    <!-- Google Fonts: Outfit & Fira Code -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;500&family=Outfit:wght@300;400;600;800&display=swap" rel="stylesheet">
    
    <style>
        :root {
            --bg-gradient: radial-gradient(circle at top right, #1a103c, #07070f 60%);
            --glass-bg: rgba(255, 255, 255, 0.03);
            --glass-border: rgba(255, 255, 255, 0.07);
            --glass-hover-bg: rgba(255, 255, 255, 0.06);
            --text-primary: #f3f3f6;
            --text-secondary: #9ea0ab;
            --neon-green: #00ffaa;
            --neon-green-glow: rgba(0, 255, 170, 0.4);
            --neon-red: #ff3b69;
            --neon-red-glow: rgba(255, 59, 105, 0.4);
            --cyber-blue: #00d2ff;
            --cyber-blue-glow: rgba(0, 210, 255, 0.4);
            --cyber-purple: #9000ff;
            --cyber-purple-glow: rgba(144, 0, 255, 0.4);
            --terminal-bg: rgba(7, 7, 15, 0.85);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Outfit', sans-serif;
            background: var(--bg-gradient);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: flex-start;
            padding: 2.5rem 1.5rem;
            overflow-x: hidden;
        }

        /* Glassmorphic Background Shapes */
        .decor-blob {
            position: absolute;
            border-radius: 50%;
            filter: blur(100px);
            z-index: -1;
            opacity: 0.25;
        }
        .blob-1 {
            width: 400px;
            height: 400px;
            background: var(--cyber-blue);
            top: -100px;
            right: -100px;
        }
        .blob-2 {
            width: 500px;
            height: 500px;
            background: var(--cyber-purple);
            bottom: -150px;
            left: -150px;
        }

        /* Container */
        .container {
            width: 100%;
            max-width: 1000px;
            display: flex;
            flex-direction: column;
            gap: 2rem;
            position: relative;
        }

        /* Header section */
        header {
            text-align: center;
            margin-bottom: 0.5rem;
        }

        h1 {
            font-size: 2.5rem;
            font-weight: 800;
            background: linear-gradient(135deg, #ffffff, #8f8aff);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            letter-spacing: -0.5px;
            margin-bottom: 0.5rem;
        }

        .subtitle {
            color: var(--text-secondary);
            font-size: 1.05rem;
            font-weight: 300;
        }

        /* Status Grid */
        .status-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 1.5rem;
        }

        .service-card {
            background: var(--glass-bg);
            border: 1px solid var(--glass-border);
            border-radius: 16px;
            padding: 1.75rem;
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            display: flex;
            flex-direction: column;
            gap: 1.25rem;
            transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
            position: relative;
            overflow: hidden;
        }

        .service-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: linear-gradient(135deg, rgba(255,255,255,0.03), transparent);
            pointer-events: none;
        }

        .service-card:hover {
            transform: translateY(-4px);
            border-color: rgba(255, 255, 255, 0.15);
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
        }

        .card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .service-info {
            display: flex;
            align-items: center;
            gap: 0.85rem;
        }

        .service-icon {
            width: 44px;
            height: 44px;
            border-radius: 10px;
            background: rgba(255, 255, 255, 0.05);
            display: flex;
            align-items: center;
            justify-content: center;
            color: var(--text-primary);
        }

        .service-icon svg {
            width: 24px;
            height: 24px;
        }

        .service-title {
            font-size: 1.15rem;
            font-weight: 600;
            color: var(--text-primary);
        }

        .port-badge {
            font-family: 'Fira Code', monospace;
            font-size: 0.8rem;
            background: rgba(255, 255, 255, 0.06);
            padding: 0.25rem 0.6rem;
            border-radius: 6px;
            border: 1px solid rgba(255, 255, 255, 0.08);
            color: var(--text-secondary);
        }

        .card-body {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-top: 0.5rem;
        }

        .status-wrapper {
            display: flex;
            align-items: center;
            gap: 0.75rem;
        }

        .status-dot {
            width: 12px;
            height: 12px;
            border-radius: 50%;
            transition: all 0.3s ease;
        }

        .status-dot.online {
            background-color: var(--neon-green);
            box-shadow: 0 0 12px var(--neon-green-glow);
            animation: pulse-green 2s infinite alternate;
        }

        .status-dot.offline {
            background-color: var(--neon-red);
            box-shadow: 0 0 12px var(--neon-red-glow);
            animation: pulse-red 2s infinite alternate;
        }

        .status-text {
            font-size: 1.1rem;
            font-weight: 600;
            letter-spacing: 0.5px;
            text-transform: uppercase;
        }

        .status-text.online {
            color: var(--neon-green);
            text-shadow: 0 0 8px rgba(0, 255, 170, 0.2);
        }

        .status-text.offline {
            color: var(--neon-red);
            text-shadow: 0 0 8px rgba(255, 59, 105, 0.2);
        }

        /* Action Buttons Area */
        .actions-panel {
            background: var(--glass-bg);
            border: 1px solid var(--glass-border);
            border-radius: 16px;
            padding: 1.75rem;
            backdrop-filter: blur(16px);
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 1.5rem;
            flex-wrap: wrap;
        }

        .btn {
            font-family: 'Outfit', sans-serif;
            font-size: 1.1rem;
            font-weight: 600;
            padding: 0.9rem 2rem;
            border-radius: 12px;
            border: none;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 0.75rem;
            transition: all 0.25s ease;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
            color: #ffffff;
            position: relative;
            overflow: hidden;
        }

        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(0, 0, 0, 0.35);
        }

        .btn:active {
            transform: translateY(0);
        }

        .btn-start {
            background: linear-gradient(135deg, #00b4db, #0083b0);
            border: 1px solid rgba(0, 210, 255, 0.2);
        }

        .btn-start:hover {
            background: linear-gradient(135deg, #00d2ff, #0083b0);
            box-shadow: 0 8px 25px var(--cyber-blue-glow);
        }

        .btn-stop {
            background: linear-gradient(135deg, #ed213a, #93291e);
            border: 1px solid rgba(255, 59, 105, 0.2);
        }

        .btn-stop:hover {
            background: linear-gradient(135deg, #ff3b69, #93291e);
            box-shadow: 0 8px 25px var(--neon-red-glow);
        }

        .btn svg {
            width: 20px;
            height: 20px;
            fill: currentColor;
        }

        /* Spin Animation */
        .spinner {
            animation: rotate 1.5s linear infinite;
            display: none;
        }

        @keyframes rotate {
            100% {
                transform: rotate(360deg);
            }
        }

        .btn.loading .spinner {
            display: inline-block;
        }

        .btn.loading .btn-icon {
            display: none;
        }

        /* Terminal Window */
        .terminal-window {
            background: var(--terminal-bg);
            border: 1px solid var(--glass-border);
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 15px 35px rgba(0,0,0,0.5);
            display: flex;
            flex-direction: column;
            height: 380px;
            backdrop-filter: blur(12px);
        }

        .terminal-header {
            background: rgba(255, 255, 255, 0.04);
            padding: 0.75rem 1rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-bottom: 1px solid rgba(255,255,255,0.05);
        }

        .terminal-dots {
            display: flex;
            gap: 6px;
        }

        .dot {
            width: 12px;
            height: 12px;
            border-radius: 50%;
        }

        .dot-red { background-color: #ff5f56; }
        .dot-yellow { background-color: #ffbd2e; }
        .dot-green { background-color: #27c93f; }

        .terminal-title {
            font-size: 0.85rem;
            color: var(--text-secondary);
            font-family: 'Fira Code', monospace;
            font-weight: 500;
        }

        .btn-clear-logs {
            background: transparent;
            border: 1px solid rgba(255, 255, 255, 0.1);
            color: var(--text-secondary);
            font-family: 'Outfit', sans-serif;
            font-size: 0.8rem;
            padding: 0.25rem 0.6rem;
            border-radius: 6px;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .btn-clear-logs:hover {
            background: rgba(255, 255, 255, 0.05);
            color: var(--text-primary);
            border-color: rgba(255,255,255,0.2);
        }

        .terminal-body {
            flex-grow: 1;
            padding: 1.25rem;
            overflow-y: auto;
            font-family: 'Fira Code', monospace;
            font-size: 0.9rem;
            line-height: 1.5;
            color: #d1d2d6;
            white-space: pre-wrap;
            scroll-behavior: smooth;
        }

        /* Custom Scrollbar */
        ::-webkit-scrollbar {
            width: 8px;
            height: 8px;
        }
        ::-webkit-scrollbar-track {
            background: rgba(0, 0, 0, 0.1);
        }
        ::-webkit-scrollbar-thumb {
            background: rgba(255, 255, 255, 0.1);
            border-radius: 4px;
        }
        ::-webkit-scrollbar-thumb:hover {
            background: rgba(255, 255, 255, 0.2);
        }

        /* Pulsing Keyframe Animations */
        @keyframes pulse-green {
            0% {
                box-shadow: 0 0 4px var(--neon-green-glow);
            }
            100% {
                box-shadow: 0 0 16px var(--neon-green-glow);
            }
        }

        @keyframes pulse-red {
            0% {
                box-shadow: 0 0 4px var(--neon-red-glow);
            }
            100% {
                box-shadow: 0 0 16px var(--neon-red-glow);
            }
        }

        /* Responsive Breakpoints */
        @media (max-width: 600px) {
            h1 {
                font-size: 2rem;
            }
            body {
                padding: 1.5rem 1rem;
            }
            .actions-panel {
                flex-direction: column;
                align-items: stretch;
            }
            .btn {
                justify-content: center;
            }
        }
    </style>
</head>
<body>

    <div class="decor-blob blob-1"></div>
    <div class="decor-blob blob-2"></div>

    <div class="container">
        
        <header>
            <h1>Service Control Dashboard</h1>
            <div class="subtitle">Orchestrate and monitor system components in real-time</div>
        </header>

        <!-- Service Status Grid -->
        <section class="status-grid">
            <!-- ZooKeeper Card -->
            <div class="service-card" id="card-zookeeper">
                <div class="card-header">
                    <div class="service-info">
                        <div class="service-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <ellipse cx="12" cy="5" rx="9" ry="3"></ellipse>
                                <path d="M3 5V19A9 3 0 0 0 21 19V5"></path>
                                <path d="M3 12A9 3 0 0 0 21 12"></path>
                            </svg>
                        </div>
                        <span class="service-title">ZooKeeper</span>
                    </div>
                    <span class="port-badge">Port 2181</span>
                </div>
                <div class="card-body">
                    <div class="status-wrapper">
                        <div id="dot-zookeeper" class="status-dot offline"></div>
                        <span id="text-zookeeper" class="status-text offline">Checking...</span>
                    </div>
                </div>
            </div>

            <!-- Spring Boot Card -->
            <div class="service-card" id="card-backend">
                <div class="card-header">
                    <div class="service-info">
                        <div class="service-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
                                <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
                                <line x1="6" y1="6" x2="6.01" y2="6"></line>
                                <line x1="6" y1="18" x2="6.01" y2="18"></line>
                            </svg>
                        </div>
                        <span class="service-title">Spring Backend</span>
                    </div>
                    <span class="port-badge">Port 8081</span>
                </div>
                <div class="card-body">
                    <div class="status-wrapper">
                        <div id="dot-backend" class="status-dot offline"></div>
                        <span id="text-backend" class="status-text offline">Checking...</span>
                    </div>
                </div>
            </div>

            <!-- Frontend Card -->
            <div class="service-card" id="card-frontend">
                <div class="card-header">
                    <div class="service-info">
                        <div class="service-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                <line x1="3" y1="9" x2="21" y2="9"></line>
                                <line x1="9" y1="21" x2="9" y2="9"></line>
                            </svg>
                        </div>
                        <span class="service-title">Node Frontend</span>
                    </div>
                    <span class="port-badge">Port 8080</span>
                </div>
                <div class="card-body">
                    <div class="status-wrapper">
                        <div id="dot-frontend" class="status-dot offline"></div>
                        <span id="text-frontend" class="status-text offline">Checking...</span>
                    </div>
                </div>
            </div>
        </section>

        <!-- Command Panel -->
        <section class="actions-panel">
            <button id="btn-start" class="btn btn-start" onclick="triggerServiceAction('start')">
                <svg class="spinner" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" stroke-dasharray="32" style="opacity:0.3;"></circle>
                    <path d="M12 2C6.47715 2 2 6.47715 2 12C2 13.5997 2.37562 15.1116 3.0434 16.4527" stroke="currentColor" stroke-width="4" stroke-linecap="round"></path>
                </svg>
                <svg class="btn-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                    <path d="M8 5v14l11-7z"/>
                </svg>
                <span>Start All Services</span>
            </button>

            <button id="btn-stop" class="btn btn-stop" onclick="triggerServiceAction('stop')">
                <svg class="spinner" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" stroke-dasharray="32" style="opacity:0.3;"></circle>
                    <path d="M12 2C6.47715 2 2 6.47715 2 12C2 13.5997 2.37562 15.1116 3.0434 16.4527" stroke="currentColor" stroke-width="4" stroke-linecap="round"></path>
                </svg>
                <svg class="btn-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                    <path d="M6 6h12v12H6z"/>
                </svg>
                <span>Stop All Services</span>
            </button>
        </section>

        <!-- Live Terminal Log Stream -->
        <section class="terminal-window">
            <div class="terminal-header">
                <div class="terminal-dots">
                    <div class="dot dot-red"></div>
                    <div class="dot dot-yellow"></div>
                    <div class="dot dot-green"></div>
                </div>
                <span class="terminal-title">live_orchestrator_log.sh</span>
                <button class="btn-clear-logs" onclick="clearLogs()">Clear Log</button>
            </div>
            <div id="terminal-body" class="terminal-body">=== Control Panel Dashboard Loaded ===</div>
        </section>

    </div>

    <script>
        const terminalBody = document.getElementById('terminal-body');
        let currentLogs = '';

        function setServiceStatus(serviceKey, isOnline) {
            const dot = document.getElementById(\`dot-\${serviceKey}\`);
            const text = document.getElementById(\`text-\${serviceKey}\`);
            
            if (isOnline) {
                dot.className = 'status-dot online';
                text.className = 'status-text online';
                text.innerText = 'Online';
            } else {
                dot.className = 'status-dot offline';
                text.className = 'status-text offline';
                text.innerText = 'Offline';
            }
        }

        async function fetchStatus() {
            try {
                const response = await fetch('/api/status');
                if (!response.ok) throw new Error('Status request failed');
                const data = await response.json();
                
                setServiceStatus('zookeeper', data.zookeeper.online);
                setServiceStatus('backend', data.backend.online);
                setServiceStatus('frontend', data.frontend.online);
            } catch (err) {
                console.error('Error fetching services status:', err);
                setServiceStatus('zookeeper', false);
                setServiceStatus('backend', false);
                setServiceStatus('frontend', false);
            }
        }

        async function fetchLogs() {
            try {
                const response = await fetch('/api/logs');
                if (!response.ok) throw new Error('Logs request failed');
                const text = await response.text();
                
                if (text !== currentLogs) {
                    currentLogs = text;
                    const isAtBottom = terminalBody.scrollHeight - terminalBody.clientHeight <= terminalBody.scrollTop + 40;
                    
                    terminalBody.innerText = currentLogs;
                    
                    if (isAtBottom) {
                        terminalBody.scrollTop = terminalBody.scrollHeight;
                    }
                }
            } catch (err) {
                console.error('Error fetching logs:', err);
            }
        }

        async function triggerServiceAction(action) {
            const btn = document.getElementById(\`btn-\${action}\`);
            btn.classList.add('loading');
            btn.disabled = true;

            try {
                const response = await fetch(\`/api/\${action}\`, { method: 'POST' });
                if (!response.ok) throw new Error('Action failed');
                
                setTimeout(fetchLogs, 500);
            } catch (err) {
                console.error(\`Failed to \${action} services:\`, err);
                terminalBody.innerText += \`\\n[PANEL ERROR] Failed to send \${action} command to server.\\n\`;
                terminalBody.scrollTop = terminalBody.scrollHeight;
            } finally {
                setTimeout(() => {
                    btn.classList.remove('loading');
                    btn.disabled = false;
                }, 2000);
            }
        }

        async function clearLogs() {
            try {
                const response = await fetch('/api/clear-logs', { method: 'POST' });
                if (response.ok) {
                    currentLogs = '';
                    terminalBody.innerText = '=== Logs Cleared ===';
                }
            } catch (err) {
                console.error('Failed to clear logs:', err);
            }
        }

        fetchStatus();
        fetchLogs();
        setInterval(fetchStatus, 1500);
        setInterval(fetchLogs, 1500);
    </script>
</body>
</html>`;
}

// Start server listening
server.listen(PORT, () => {
    console.log(`Service Control Panel Server running at http://localhost:${PORT}`);
    appendLog(`Service Control Panel Server running at http://localhost:${PORT}`);
});
