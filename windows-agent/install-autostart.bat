@echo off
set "SCRIPT_DIR=%~dp0"
set "STARTUP_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup"
set "SHORTCUT_PATH=%STARTUP_DIR%\SecureWolAgent.vbs"

echo Setting up Secure WOL Companion Agent to start automatically on Windows boot...

(
echo Set WshShell = CreateObject^("WScript.Shell"^)
echo WshShell.Run "python ""%SCRIPT_DIR%SecureWolAgent.py""", 0, False
) > "%SHORTCUT_PATH%"

if exist "%SHORTCUT_PATH%" (
    echo.
    echo =======================================================
    echo  [SUCCESS] Auto-start installed successfully!
    echo  The agent will now run silently in the background
    echo  every time your Windows PC starts.
    echo =======================================================
) else (
    echo [ERROR] Failed to install auto-start shortcut.
)
pause
