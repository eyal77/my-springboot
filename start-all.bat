@echo off
echo Starting ZooKeeper...
if exist zookeeper-server\apache-zookeeper-3.9.2-bin\bin\zkServer.cmd (
    start /B "" "zookeeper-server\apache-zookeeper-3.9.2-bin\bin\zkServer.cmd"
) else (
    echo ZooKeeper is not found! Please run the powershell setup script: .\setup-zookeeper.ps1
    exit /b 1
)
echo Waiting 5 seconds for ZooKeeper port binding...
timeout /t 5 >nul

echo Starting Spring Boot Backend (Port 8081)...
start /B "" mvnw.cmd spring-boot:run

echo Starting Node.js Frontend (Port 8080)...
cd frontend
start /B "" npm start
cd ..

echo Services started.
echo Access the application dashboard at: http://localhost:8080
echo Access the API Swagger documentation at: http://localhost:8080/swagger-ui/index.html
