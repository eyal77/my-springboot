@echo off
echo Starting Spring Boot Server...
start /B "" mvnw.cmd spring-boot:run
echo Server starting in background. Access http://localhost:8080/
