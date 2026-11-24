@echo off
rem Start MinIO (object storage, API 9000 / console 9001)
rem Double-click to start. Add -Check to only verify environment (no start).
rem Example: start-minio.bat -Check
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-minio.ps1" %*
if errorlevel 1 (
  echo.
  echo MinIO startup failed. See the error above, then press any key to close.
  pause >nul
)
