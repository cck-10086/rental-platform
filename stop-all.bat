@echo off
rem Stop all services (run from cmd or double-click)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1"
