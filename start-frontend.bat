@echo off
rem Start frontend (Vue 3 + Vite, port 5173)
rem Double-click to start. Add -Check to only verify environment (no start).
rem Example: start-frontend.bat -Check
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-frontend.ps1" %*
if errorlevel 1 (
  echo.
  echo Frontend startup failed. See the error above, then press any key to close.
  pause >nul
)
