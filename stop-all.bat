@echo off
rem Stop all services (backend 8088 / frontend 5173, 4173 / MinIO 9000, 9001)
rem Only list who holds the ports, do not kill: stop-all.bat -Check
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1" %*
