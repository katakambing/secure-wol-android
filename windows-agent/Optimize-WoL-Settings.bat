@echo off
:: Batch script to optimize Realtek Ethernet adapter for Wake-on-LAN
title Optimize Realtek NIC for Wake-on-LAN
echo ========================================================
echo   Configuring Realtek Network Card for Wake-on-LAN
echo ========================================================
echo.
echo Disabling Green Ethernet and Energy Efficient Ethernet...
powershell -Command "Start-Process powershell -Verb RunAs -ArgumentList '-NoProfile -Command `\"Set-NetAdapterAdvancedProperty -Name ''Ethernet'' -DisplayName ''Green Ethernet'' -DisplayValue ''Disabled'' -ErrorAction SilentlyContinue; Set-NetAdapterAdvancedProperty -Name ''Ethernet'' -DisplayName ''Energy-Efficient Ethernet'' -DisplayValue ''Disabled'' -ErrorAction SilentlyContinue; Write-Host ''[SUCCESS] Realtek Wake-on-LAN settings optimized!'' -ForegroundColor Green; Start-Sleep -Seconds 3`\"'"
echo Done.
pause
