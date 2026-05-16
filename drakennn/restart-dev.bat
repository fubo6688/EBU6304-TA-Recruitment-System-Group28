@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0restart-dev.ps1"
endlocal
