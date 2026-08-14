@echo off
:: Batch script to elevate and optimize Realtek NIC & Windows Power for Wake-on-LAN
title Optimize Realtek NIC for Wake-on-LAN
echo ========================================================
echo   Configuring Realtek Network Card for 100%% Wake-on-LAN
echo ========================================================
echo.
powershell -NoProfile -Command "Start-Process powershell -Verb RunAs -ArgumentList '-NoProfile -Command `\"Set-NetAdapterAdvancedProperty -Name ''Ethernet'' -DisplayName ''Green Ethernet'' -DisplayValue ''Disabled'' -ErrorAction SilentlyContinue; Set-NetAdapterAdvancedProperty -Name ''Ethernet'' -DisplayName ''Energy-Efficient Ethernet'' -DisplayValue ''Disabled'' -ErrorAction SilentlyContinue; Set-NetAdapterAdvancedProperty -Name ''Ethernet'' -DisplayName ''Power Saving Mode'' -DisplayValue ''Disabled'' -ErrorAction SilentlyContinue; Set-NetAdapterAdvancedProperty -Name ''Ethernet'' -DisplayName ''Shutdown Wake-On-Lan'' -DisplayValue ''Enabled'' -ErrorAction SilentlyContinue; Set-NetAdapterAdvancedProperty -Name ''Ethernet'' -DisplayName ''Wake on Magic Packet'' -DisplayValue ''Enabled'' -ErrorAction SilentlyContinue; Set-NetAdapterAdvancedProperty -Name ''Ethernet'' -DisplayName ''Wake on magic packet when system is in the S0ix power state'' -DisplayValue ''Enabled'' -ErrorAction SilentlyContinue; powercfg /h off; Write-Host '''' ; Write-Host ''[SUCCESS] Realtek Wake-on-LAN settings optimized and Fast Startup hibernation disabled!'' -ForegroundColor Green; Start-Sleep -Seconds 4`\"'"
