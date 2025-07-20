@echo off
setlocal

REM Set path to settings.json (adjust if needed)
set SETTINGS_FILE=%USERPROFILE%\Documents\My Games\AdventureRPG\settings.json

REM Check if settings.json exists
if not exist "%SETTINGS_FILE%" (
    echo Settings file not found: %SETTINGS_FILE%
    goto runWithConsole
)

REM Look for debug setting in settings.json (simple check for "debug": true)
findstr /i /c:"\"debug\": true" "%SETTINGS_FILE%" >nul
if %errorlevel% equ 0 (
    REM debug is true
    echo Debug mode detected, launching with console...
    goto runWithConsole
) else (
    REM debug is false or missing
    echo Debug mode not detected, launching without console...
    goto runWithoutConsole
)

:runWithConsole
REM Run gradlew normally (console window stays)
call gradlew.bat run
goto end

:runWithoutConsole
REM Run gradlew with javaw (no console)
REM To do this, we need to run the javaw command manually instead of gradlew because gradlew runs java which pops console.

REM Check if gradlew.bat exists
if not exist "gradlew.bat" (
    echo gradlew.bat not found, cannot launch without console.
    goto end
)

REM Option 1: Run your jar with javaw directly (replace this path)
REM You must have a runnable jar or your app packaged separately
set JAR_PATH=path\to\your\app.jar

REM If you don't have a runnable jar, you must build it first or create a separate javaw launcher

if exist "%JAR_PATH%" (
    start "" javaw -jar "%JAR_PATH%"
) else (
    echo Runnable jar not found at %JAR_PATH%
    echo Please build your project or create a javaw launcher.
)

goto end

:end
pause
