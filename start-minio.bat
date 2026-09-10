@echo off
rem Start MinIO (run from cmd or double-click)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-minio.ps1"
if errorlevel 1 (
  echo.
  echo MinIO startup failed. Check the error above, then press any key to close.
  pause >nul
)
