@echo off
call gradlew.bat :lwjgl3:launch
if errorlevel 1 pause
