@echo off
rem Start frontend (run from cmd or double-click)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-frontend.ps1"
if errorlevel 1 (
  echo.
  echo Frontend startup failed. Check the error above, then press any key to close.
  pause >nul
)
