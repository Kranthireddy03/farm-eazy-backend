@echo off
REM FarmEazy Spring Boot Server Launcher
REM Run this batch file to start the server

cd /d "C:\Users\krant\FarmEazy\backend"
echo Starting FarmEazy Application on port 8080...
echo.
java -jar target\farmeazy-backend-1.0.0.jar
pause
