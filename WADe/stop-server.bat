@echo off
echo Stopping WADe Server gracefully...
curl -X POST http://localhost:8081/actuator/shutdown
echo.
pause