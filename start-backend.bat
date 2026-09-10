@echo off
rem Start backend (run from cmd or double-click)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-backend.ps1"
if errorlevel 1 (
  echo.
  echo Backend startup failed. Check the error above, then press any key to close.
  pause >nul
)
