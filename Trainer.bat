@echo off
call gradlew.bat :lwjgl3:trainer -q --console=plain
if errorlevel 1 pause
