@echo off
echo Stopping Node.js Frontend on port 8080...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /F /PID %%a
    echo Stopped process %%a
)

echo Stopping Spring Boot Backend on port 8081...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8081 ^| findstr LISTENING') do (
    taskkill /F /PID %%a
    echo Stopped process %%a
)

echo Stopping ZooKeeper on port 2181...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :2181 ^| findstr LISTENING') do (
    taskkill /F /PID %%a
    echo Stopped process %%a
)

echo All services stopped.
