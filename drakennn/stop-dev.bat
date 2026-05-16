@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0stop-dev.ps1"
endlocal
