@echo off
setlocal

rem Converts every sound under assets\sounds that is not Ogg Vorbis (.wav, .flac, .mp3, .aif, .aiff) into an .ogg
rem beside it under the same name, then deletes the original, so the game finds only the one format it loads.
rem Needs nothing installed: when ffmpeg is not on this PC, a portable copy is downloaded once with PowerShell into
rem %LOCALAPPDATA%\AdventureRPG and reused on later runs. The game itself never uses ffmpeg.

set "SOUNDS=%~dp0..\assets\sounds"
set "TOOLS=%LOCALAPPDATA%\AdventureRPG"
set "FFMPEG_URL=https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-lgpl.zip"
set /a CONVERTED=0
set /a FAILED=0

if not exist "%SOUNDS%\" (
    echo Sound folder not found: "%SOUNDS%"
    goto :fail
)

call :find
if not defined FFMPEG (
    echo ffmpeg was not found. Downloading a portable copy, about 175 MB, once...
    call :download
    call :find
)
if not defined FFMPEG (
    echo ffmpeg could not be downloaded. Check the internet connection and run this again.
    goto :fail
)

for /r "%SOUNDS%" %%F in (*.wav *.flac *.mp3 *.aif *.aiff) do call :convert "%%F"

echo.
echo Converted %CONVERTED% sounds to .ogg, %FAILED% failed.
if %FAILED% gtr 0 goto :fail

pause
exit /b 0

:find
set "FFMPEG="
for /f "delims=" %%P in ('where ffmpeg 2^>nul') do if not defined FFMPEG set "FFMPEG=%%P"
if not defined FFMPEG if exist "%TOOLS%\ffmpeg.exe" set "FFMPEG=%TOOLS%\ffmpeg.exe"
exit /b 0

:download
if not exist "%TOOLS%\" mkdir "%TOOLS%"

set "PS=$ErrorActionPreference = 'Stop'; $ProgressPreference = 'SilentlyContinue';"
set "PS=%PS% [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;"
set "PS=%PS% $zip = Join-Path $env:TOOLS 'ffmpeg.zip'; $unpacked = Join-Path $env:TOOLS 'ffmpeg';"
set "PS=%PS% Invoke-WebRequest -Uri $env:FFMPEG_URL -OutFile $zip;"
set "PS=%PS% Expand-Archive -LiteralPath $zip -DestinationPath $unpacked -Force;"
set "PS=%PS% $exe = Get-ChildItem -LiteralPath $unpacked -Recurse -Filter ffmpeg.exe | Select-Object -First 1;"
set "PS=%PS% Copy-Item -LiteralPath $exe.FullName -Destination (Join-Path $env:TOOLS 'ffmpeg.exe') -Force;"
set "PS=%PS% Remove-Item -LiteralPath $zip, $unpacked -Recurse -Force"

powershell -NoProfile -ExecutionPolicy Bypass -Command "%PS%"
exit /b 0

:convert
set "TARGET=%~dpn1.ogg"

if exist "%TARGET%" (
    echo Skipped, "%~n1.ogg" already exists beside it: "%~1"
    set /a FAILED+=1
    exit /b 0
)

"%FFMPEG%" -nostdin -v error -i "%~1" -map_metadata -1 -c:a libvorbis -q:a 5 -ar 48000 "%TARGET%"
if errorlevel 1 (
    echo Failed: "%~1"
    if exist "%TARGET%" del "%TARGET%"
    set /a FAILED+=1
    exit /b 0
)

del "%~1"
echo Converted: "%~1"
set /a CONVERTED+=1
exit /b 0

:fail
echo.
echo Sound conversion failed. See errors above.
pause
exit /b 1
