# ZooKeeper & Microservices Architecture Guide

This document explains the architecture of your newly migrated microservices system and how Apache ZooKeeper coordinates the communication between the services.

---

## 1. Architectural Components

Our system is split into three decoupled components, each with a distinct responsibility:

1. **ZooKeeper (`localhost:2181`)**: The **Service Registry & Coordinator**. It tracks which backend services are alive and where they are running.
2. **Java Spring Boot Service (`localhost:8081`)**: The **Backend REST API**. It performs the heavy lifting (telemetry, security, filesystem storage) and registers its presence in ZooKeeper.
3. **Node.js Express Service (`localhost:8080`)**: The **Frontend Server & API Gateway**. It serves the static dashboard files to the browser, reads backend addresses from ZooKeeper, and proxies API calls to the correct backend instance.

---

## 2. Component Diagram

The diagram below shows how the components interact. Notice that the browser never communicates with the backend directly; all API traffic is routed through the Frontend Server acting as a reverse proxy.

```mermaid
graph TD
    classDef zookeeper fill:#2b7fb9,stroke:#1e5e8a,stroke-width:2px,color:#fff;
    classDef frontend fill:#2ecc71,stroke:#27ae60,stroke-width:2px,color:#fff;
    classDef backend fill:#e67e22,stroke:#d35400,stroke-width:2px,color:#fff;
    classDef client fill:#9b59b6,stroke:#8e44ad,stroke-width:2px,color:#fff;

    Client["🌐 Browser Client"]:::client
    
    subgraph FrontendService ["Frontend Service (Port 8080)"]
        Express["Express Web Server"]:::frontend
        Proxy["Proxy Middleware"]:::frontend
    end
    
    subgraph BackendService ["Backend Service (Port 8081)"]
        SpringBoot["Spring Boot API"]:::backend
        RegService["Spring Cloud ZooKeeper Discovery"]:::backend
    end
    
    subgraph ZooKeeperRegistry ["ZooKeeper Cluster (Port 2181)"]
        ZNode["/services/backend-service/{instance-id}<br>(Ephemeral Node)"]:::zookeeper
    end

    Client -->|1. Request UI & Assets| Express
    Client -->|4. Request /api/system-info| Proxy
    Proxy -->|5. Forward API request| SpringBoot
    
    RegService -->|2. Register instance details| ZNode
    Express -->|3. Discover & Watch backend instances| ZNode
```

---

## 3. Detailed Sequence Flow

The interaction sequence consists of two distinct phases: **Startup** and **Runtime Request**.

```mermaid
sequenceDiagram
    autonumber
    participant ZK as ZooKeeper (2181)
    participant Backend as Spring Boot Backend (8081)
    participant Frontend as Express Frontend (8080)
    participant Browser as Browser Client
    
    Note over ZK,Frontend: Phase 1: Startup & Discovery
    Backend->>ZK: 1. Establish connection & create ephemeral node
    Note over ZK: Node created:<br>/services/backend-service/{instance-id}<br>Data: {"name":"backend-service","address":"localhost","port":8081,...}
    
    Frontend->>ZK: 2. Connect & watch children of /services/backend-service
    ZK-->>Frontend: 3. Return active instance list: ["{instance-id}"]
    Note over Frontend: Reads address + port from the node and caches backend URL:<br>http://localhost:8081
    
    Note over Browser,Backend: Phase 2: Runtime Request Routing
    Browser->>Frontend: 4. Request HTML/CSS/JS dashboard
    Frontend-->>Browser: 5. Load SPA Dashboard UI
    
    Browser->>Frontend: 6. Request GET /api/system-info
    Note over Frontend: Reads cached backend URL (http://localhost:8081)
    Frontend->>Backend: 7. Proxy request to: GET http://localhost:8081/api/system-info
    Backend-->>Frontend: 8. Return system diagnostics payload
    Frontend-->>Browser: 9. Forward diagnostics JSON to browser
```

---

## 4. Key ZooKeeper Concepts Used

### A. Service Registration (Ephemeral Nodes)
The Spring Boot backend registers itself using **Spring Cloud ZooKeeper Discovery** (`spring-cloud-starter-zookeeper-discovery`) - no hand-written registration code. On startup it creates an **Ephemeral** Znode at `/services/${spring.application.name}/<instance-id>` (here `/services/backend-service/...`) and removes it on graceful shutdown.
* **Data format:** the node holds a standard [Curator Service Discovery](https://curator.apache.org/docs/service-discovery/) `ServiceInstance` JSON (`name`, `id`, `address`, `port`, `payload`, ...), so any Curator/Spring Cloud client can read it. The Node.js frontend reads `address` and `port` from it.
* **Configuration** (`application.properties`): `spring.cloud.zookeeper.connect-string`, `spring.cloud.zookeeper.discovery.instance-host`. Disable with `spring.cloud.zookeeper.enabled=false` (used by the tests).
* **What is an Ephemeral Node?** Unlike persistent nodes, ephemeral nodes only exist as long as the client session that created them is active.
* **Why use it?** If the backend server crashes, gets killed, or experiences a network partition, its TCP session with ZooKeeper will expire. ZooKeeper will automatically delete the ephemeral node once the session timeout (~60s by default) expires. 

### B. Service Discovery (Watchers)
Our Node.js Express frontend server doesn't just read the backend address once. It places a **Watcher** (a listener event) on the parent folder `/services/backend-service`.
* **Dynamic Updates:** If ZooKeeper deletes the backend node (due to a backend crash), ZooKeeper fires a `NODE_CHILDREN_CHANGED` event to the Node.js watcher.
* **Immediate Failover:** The Node.js server immediately updates its internal `backendUrl` variable to `null`. Any incoming browser API requests are intercepted and returned as `503 Service Unavailable` instead of hanging or timing out.
* **Auto-recovery:** Once the backend restarts and re-registers, ZooKeeper alerts the Node.js server again, which resolves the backend URL, restoring normal routing seamlessly.
