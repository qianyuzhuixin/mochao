@echo off
cd /d %~dp0
echo Starting MoChao Scraper Service...
echo.
echo Port: 3001
echo Health: http://127.0.0.1:3001/health
echo Platforms: http://127.0.0.1:3001/platforms
echo.
node server.js
pause
