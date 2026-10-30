@echo off
setlocal

rem Lists every file in .claude\catalogue (the personal library on this PC) into .claude\assets\Catalogue.txt,
rem one path per line relative to the catalogue folder, so Claude can pick files from it without the library
rem being committed.

set "SOURCE=%~dp0catalogue"
set "TARGET=%~dp0assets\Catalogue.txt"

if not exist "%SOURCE%\" (
    echo Catalogue folder not found: "%SOURCE%"
    goto :fail
)
if not exist "%~dp0assets\" mkdir "%~dp0assets"

set "PS=$root = (Resolve-Path -LiteralPath $env:SOURCE).Path.TrimEnd('\');"
set "PS=%PS% $files = @(Get-ChildItem -LiteralPath $root -Recurse -File |"
set "PS=%PS% ForEach-Object { $_.FullName.Substring($root.Length + 1).Replace('\', '/') } | Sort-Object);"
set "PS=%PS% $text = if ($files.Count) { ($files -join [char]10) + [char]10 } else { '' };"
set "PS=%PS% [IO.File]::WriteAllText($env:TARGET, $text, (New-Object Text.UTF8Encoding $false));"
set "PS=%PS% Write-Host ('Listed ' + $files.Count + ' files in ' + $env:TARGET)"

powershell -NoProfile -ExecutionPolicy Bypass -Command "%PS%"
if errorlevel 1 goto :fail

exit /b 0

:fail
echo.
echo Catalogue listing failed. See errors above.
pause
exit /b 1
