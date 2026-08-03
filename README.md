# System Monitor & Access Management API

A secure, high-performance Java Spring Boot application providing system telemetry (hostname, local system date/time, disk space monitoring, and full local network configuration) paired with a responsive, glassmorphic Single Page Application (SPA) dashboard UI and custom token-based role authentication.

---

## Features

- **System Diagnostics**: REST endpoint querying local machine host details, disk storage partition metrics, and network interface settings.
- **Network Telemetry**: Includes local IP, subnet mask, default gateway, and external IP.
- **Multipart Upload Builder**: Endpoint `/api/multipart` for handling single and multiple file uploads (under unique or repeated parameter keys), complete with a dynamic visual builder displaying the equivalent `curl` command.
- **Encrypted Local Storage**: Restarts do not lose data. Users and tokens are securely serialized and saved in local files (`users.dat` and `tokens.dat`) using a 128-bit AES block cipher.
- **Role-Based Access Control (RBAC)**:
  - `ROOT`: Master account. Can manage user accounts (CRUD) and API keys. Pre-seeded with username `root` and password `Project!!!111` (cannot be deleted). Cannot modify or delete itself.
  - `ADMIN`: Administrator role. Can generate and revoke access tokens for generic users. Cannot view, modify, or interact with higher roles (like `ROOT`).
  - `USER`: Customer/client role. Can query the diagnostic endpoints using a generated token key.
- **Custom Token Security Filter**: Lightweight interceptor validating bearer tokens via standard headers (`Authorization: Bearer <token>`) or query strings (`?token=<token>`).
- **Interactive SPA Console**: Beautiful responsive interface built with glassmorphic cards, modern typography (Outfit & Plus Jakarta Sans), neon status states, and dynamic view tabs for user administration, key management, and API testing.
- **API Documentation**: Automated Swagger/OpenAPI interactive portal.

---

## Project Structure

```text
src/
├── main/
│   ├── java/com/example/eyal/rest/
│   │   ├── config/
│   │   │   └── OpenApiConfig.java          # Swagger Security Scheme Configuration
│   │   ├── controller/
│   │   │   ├── AuthController.java         # User Authentication Login API
│   │   │   ├── AdminController.java        # User & Token RBAC Management API
│   │   │   ├── SystemController.java       # Hostname & Storage diagnostics API
│   │   │   ├── MultipartController.java    # Handles file uploads via multipart
│   │   │   └── ViewController.java         # Redirects /admin /admin/ to dashboard
│   │   ├── dto/
│   │   │   ├── DiskInfo.java               # Record for individual drive metrics
│   │   │   ├── NetworkInfo.java            # Record for networking interface details
│   │   │   └── SystemInfoResponse.java     # Combined system diagnostic payload
│   │   ├── model/
│   │   │   ├── User.java                   # User entity
│   │   │   ├── Token.java                  # Access/Session token details
│   │   │   └── UserRole.java               # ROOT, ADMIN, USER privilege enum
│   │   ├── repository/
│   │   │   ├── UserRepository.java         # Encrypted filesystem persistent user repo
│   │   │   └── TokenRepository.java        # Encrypted filesystem persistent token repo
│   │   ├── security/
│   │   │   ├── EncryptionUtils.java        # AES 128-bit Encryption Utilities
│   │   │   ├── SecurityContext.java        # Request thread context holder
│   │   │   └── SecurityFilter.java         # Token validation filter
│   │   ├── service/
│   │   │   └── SystemService.java          # Disk metrics, networking, & environment utilities
│   │   └── RestApplication.java            # Main Spring Boot entry-point
│   └── resources/
│       └── static/
│           └── index.html                  # Responsive Web Dashboard Interface
└── test/
    └── java/com/example/eyal/rest/
        └── controller/
            ├── SystemControllerTest.java   # Controller DTO structure unit test
            ├── AdminControllerTest.java    # Standalone MockMvc security integration test
            └── MultipartControllerTest.java # Multipart upload validation test
```

---

## Getting Started

### Prerequisites
- **Java Development Kit (JDK)**: Version 21.0 or newer
- **Maven**: (Wrapper scripts `mvnw` and `mvnw.cmd` are included in the repository)

### Running Automated Tests
Run clean test compilation via Maven Wrapper:
```bash
# Windows
.\mvnw.cmd clean test

# Linux/macOS
./mvnw clean test
```

### Running the Application
Launch the Spring Boot server:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```
The server starts on port **`8080`**.

### Control Scripts (Windows)
For convenience, scripts are provided to manage the server:
- `start-server.bat`: Starts the Spring Boot application in the background.
- `stop-server.bat`: Safely stops any running Java process listening on port `8080`.

---

## API Endpoints

| Endpoint | Method | Required Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/auth/login` | `POST` | Public | Authenticates user; returns token, role, and username. |
| `/api/system-info` | `GET` | `ROOT`, `ADMIN`, `USER` | Gets hostname, system date, time, network configuration, and disk partition stats. |
| `/api/multipart` | `POST` | `ROOT`, `ADMIN`, `USER` | Uploads one or more files (under unique or repeated parameter keys). |
| `/api/admin/users` | `GET` | `ROOT` | Lists all registered user accounts. |
| `/api/admin/users` | `POST` | `ROOT` | Registers a new user (`ROOT`, `ADMIN`, or `USER`). |
| `/api/admin/users/{username}` | `DELETE` | `ROOT` | Deletes a user account (fails if target is `root` or requestor is ADMIN). |
| `/api/admin/tokens` | `GET` | `ROOT`, `ADMIN` | Lists all active authentication tokens. |
| `/api/admin/tokens` | `POST` | `ROOT`, `ADMIN` | Generates a new API token for a user (expires after max 24 hours). |
| `/api/admin/tokens/{token}` | `DELETE` | `ROOT`, `ADMIN` | Revokes and deletes an active access token. |
| `/admin`, `/admin/` | `GET` | Public | Redirects browser request to the main dashboard interface. |

---

## Interactive Documentation & Portals

1. **Dashboard Interface**: [http://localhost:8080/](http://localhost:8080/)
   - Log in using credentials: username **`root`** and password **`Project!!!111`**.
   - Manage administrators, view active system keys, copy tokens, or run diagnostics.
2. **Interactive Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) (redirected automatically from `/swagger-ui.html`)
   - Explore endpoints interactively. Use the **Authorize** lock button in the top right to authenticate using your bearer token.
3. **Raw OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
