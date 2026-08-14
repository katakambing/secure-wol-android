@echo off
title Secure WOL - Windows Companion Agent
echo Starting Secure WOL Companion Agent...
python "%~dp0SecureWolAgent.py"
if %errorlevel% neq 0 (
    echo.
    echo Python not found. Trying with py launcher...
    py "%~dp0SecureWolAgent.py"
)
pause
