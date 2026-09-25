@echo off
call gradlew.bat :lwjgl3:launchEditor
if errorlevel 1 pause
