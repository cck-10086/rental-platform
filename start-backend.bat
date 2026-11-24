@echo off
rem Start backend (Spring Boot, port 8088)
rem Double-click to start. Add -Check to only verify environment (no start).
rem Example: start-backend.bat -Check
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-backend.ps1" %*
if errorlevel 1 (
  echo.
  echo Backend startup failed. See the error above, then press any key to close.
  pause >nul
)
