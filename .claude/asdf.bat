@echo off
setlocal

rem Copies the library files Claude needs to see for the MusicInstruments delivery from .claude\catalogue into
rem .claude\assets under the same paths, then, once you type Y, commits and pushes exactly those files so Claude can
rem read them. Nothing else is staged, committed or changed. Drop this file into .claude and run it; it deletes itself
rem after a successful push.

set "CLAUDE=%~dp0"
set "SELF=%~f0"

(
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$text = [IO.File]::ReadAllText($env:SELF); & ([ScriptBlock]::Create($text.Substring($text.IndexOf([string][char]10 + '# PowerShell'))))"
    exit /b
)

# PowerShell

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$claude = (Get-Item -LiteralPath $env:CLAUDE).FullName.TrimEnd('\')
$root = Split-Path -Parent $claude
$library = Join-Path $claude 'assets'
$catalogue = Join-Path $claude 'catalogue'
$message = 'Library files for MusicInstruments'

$requests = @(
    'VCSL-1.2.2-RC/Aerophones/Edge-blown Aerophones/Baroque Soprano Recorder - Sustain.sfz',
    'VCSL-1.2.2-RC/Aerophones/Edge-blown Aerophones/Baroque Soprano Recorder - Keyswitch.sfz',
    'VCSL-1.2.2-RC/Aerophones/Edge-blown Aerophones/Baroque Soprano Recorder/Sustain',
    'VCSL-1.2.2-RC/Chordophones/Composite Chordophones/Folk Harp.sfz',
    'VCSL-1.2.2-RC/Chordophones/Composite Chordophones/Folk Harp'
)

# Copy \\

function Get-RequestFiles([string] $request) {

    $source = Join-Path $catalogue $request.Replace('/', '\')

    if (Test-Path -LiteralPath $source -PathType Leaf) { return ,@($request) }
    if (-not (Test-Path -LiteralPath $source -PathType Container)) {
        throw "'.claude/catalogue/$request' is not on this PC"
    }

    $folder = (Get-Item -LiteralPath $source).FullName.TrimEnd('\')
    $files = @(Get-ChildItem -LiteralPath $folder -Recurse -File |
               ForEach-Object { $request + '/' + $_.FullName.Substring($folder.Length + 1).Replace('\', '/') })

    return ,$files
}

function Copy-LibraryFile([string] $relative) {

    $source = Join-Path $catalogue $relative.Replace('/', '\')
    $target = Join-Path $library $relative.Replace('/', '\')

    [IO.Directory]::CreateDirectory((Split-Path -Parent $target)) | Out-Null
    [IO.File]::Copy($source, $target, $true)
}

# Git \\

function Invoke-Git {

    & git -C $root --literal-pathspecs @args
    if ($LASTEXITCODE -ne 0) { throw "git $($args[0]) failed. The files are copied; nothing was pushed." }
}

function Push-LibraryFiles($files) {

    $paths = @($files | ForEach-Object { '.claude/assets/' + $_ })

    Invoke-Git add -- @paths
    Invoke-Git commit --quiet -m $message --only -- @paths
    Invoke-Git push --quiet
}

# Run \\

$pushed = $false

try {

    if (-not (Test-Path -LiteralPath (Join-Path $claude 'CLAUDE.md')) -or
        -not (Test-Path -LiteralPath (Join-Path $root 'gradlew.bat'))) {
        throw "This file must sit in the project's .claude folder: $claude"
    }
    if (-not (Get-Command git -CommandType Application -ErrorAction SilentlyContinue)) {
        throw 'Git was not found on this PC.'
    }

    $files = New-Object 'Collections.Generic.List[string]'
    foreach ($request in $requests) { foreach ($file in (Get-RequestFiles $request)) { $files.Add($file) } }

    foreach ($file in $files) { Copy-LibraryFile $file }
    Write-Host "Copied $($files.Count) files from .claude\catalogue into .claude\assets." -ForegroundColor Green
    Write-Host ''

    if ((Read-Host "Type Y and press Enter to commit and push only these $($files.Count) files") -eq 'Y') {
        Push-LibraryFiles $files
        $pushed = $true
        Write-Host "Pushed '$message'. Tell Claude they are up." -ForegroundColor Green
    }
    else {
        Write-Host 'Not pushed. The copies stay in .claude\assets; run this again to push them.'
    }
}
catch {
    Write-Host ''
    Write-Host "Stopped: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ''
Read-Host 'Press Enter to close' | Out-Null

if ($pushed) { Remove-Item -LiteralPath $env:SELF -Force }
