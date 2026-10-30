@echo off
setlocal

rem Applies a delivery from Claude, then keeps the asset library list and the sounds current. Drop
rem AdventureRPG_Delivery.zip into this folder and run this file: it deletes the files its Delivery.txt lists, unpacks
rem the zip over the project, copies the listed files from .claude\assets or .claude\catalogue into assets\, records
rem the delivery in .claude\Version.md, lists .claude\catalogue into .claude\assets\Catalogue.txt and converts every
rem sound under assets\sounds to .ogg. Without a zip it only lists the catalogue and converts the sounds. It never
rem commits: commit and push afterwards, so Claude builds the next delivery from current code. The block below is
rem read whole before it runs, so a delivery may replace this file while it runs.

set "CLAUDE=%~dp0"
set "SELF=%~f0"

(
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$text = [IO.File]::ReadAllText($env:SELF); & ([ScriptBlock]::Create($text.Substring($text.IndexOf([string][char]10 + '# PowerShell'))))"
    exit /b
)

# PowerShell

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem

$utf8 = New-Object Text.UTF8Encoding $false
$claude = (Get-Item -LiteralPath $env:CLAUDE).FullName.TrimEnd('\')
$root = Split-Path -Parent $claude
$library = Join-Path $claude 'assets'
$catalogue = Join-Path $claude 'catalogue'
$catalogueList = Join-Path $library 'Catalogue.txt'
$versionFile = Join-Path $claude 'Version.md'
$sounds = Join-Path $root 'assets\sounds'
$tools = Join-Path $env:LOCALAPPDATA 'AdventureRPG'
$ffmpegUrl = 'https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-lgpl.zip'
$manifestName = 'Delivery.txt'
$zipFilter = 'AdventureRPG_Delivery*.zip'
$soundExtensions = @('.wav', '.flac', '.mp3', '.aif', '.aiff')
$protectedPaths = @('.git', '.claude/assets', '.claude/catalogue', '.claude/Version.md')

# Paths \\

function Get-RelativePath([string] $path, [string] $what) {

    $relative = $path.Trim().Replace('\', '/')
    $segments = $relative.Split('/')

    if ($relative -eq '' -or $relative.Contains(':') -or $segments -contains '' -or
        $segments -contains '.' -or $segments -contains '..') {
        throw "$what is not a plain path inside the project: '$path'"
    }

    return $relative
}

function Test-Protected([string] $relative) {

    foreach ($protected in $protectedPaths) {
        if (($relative + '/').StartsWith($protected + '/', [StringComparison]::OrdinalIgnoreCase)) { return $true }
    }

    return $false
}

function Get-ProjectPath([string] $relative) {
    return Join-Path $root $relative.Replace('/', '\')
}

function Get-LibraryPath([string] $relative) {
    return Join-Path $claude $relative.Replace('/', '\')
}

function New-ProjectTarget([string] $relative) {

    $target = Get-ProjectPath $relative
    [IO.Directory]::CreateDirectory((Split-Path -Parent $target)) | Out-Null

    return $target
}

function Copy-ProjectFile([string] $source, [string] $relative) {
    [IO.File]::Copy($source, (New-ProjectTarget $relative), $true)
}

function Write-ProjectEntry($entry, [string] $relative) {
    [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, (New-ProjectTarget $relative), $true)
}

function Remove-ProjectFile([string] $relative) {

    $target = Get-ProjectPath $relative
    Remove-Item -LiteralPath $target -Force

    $folder = Split-Path -Parent $target
    while ($folder.Length -gt $root.Length -and
           -not (Get-ChildItem -LiteralPath $folder -Force | Select-Object -First 1)) {
        Remove-Item -LiteralPath $folder -Force
        $folder = Split-Path -Parent $folder
    }
}

# Manifest \\

function Get-Number([string] $value, [string] $what) {

    $number = 0
    if (-not [int]::TryParse($value, [ref] $number) -or $number -lt 0) {
        throw "$what must be a whole number, not '$value'"
    }

    return $number
}

function Read-Manifest([string] $text) {

    $manifest = @{
        Delivery = -1
        Base = -1
        Task = ''
        Deletes = New-Object 'Collections.Generic.List[string]'
        Copies = New-Object 'Collections.Generic.List[object]'
    }
    $lineNumber = 0

    foreach ($line in $text.Split([char]10)) {

        $lineNumber++
        $entry = $line.Trim()
        if ($entry -eq '' -or $entry.StartsWith('#')) { continue }

        $what = "$manifestName line $lineNumber"
        $space = $entry.IndexOf(' ')
        if ($space -lt 0) { throw "$what has no value: '$entry'" }

        $key = $entry.Substring(0, $space).ToUpperInvariant()
        $value = $entry.Substring($space + 1).Trim()

        switch ($key) {
            'DELIVERY' { $manifest.Delivery = Get-Number $value $what }
            'BASE' { $manifest.Base = Get-Number $value $what }
            'TASK' { $manifest.Task = $value }
            'DELETE' { $manifest.Deletes.Add((Get-RelativePath $value $what)) }
            'COPY' { $manifest.Copies.Add((Read-Copy $value $what)) }
            default { throw "$what has an unknown key '$key'" }
        }
    }

    if ($manifest.Delivery -lt 1 -or $manifest.Base -lt 0 -or $manifest.Task -eq '') {
        throw "$manifestName must give DELIVERY, BASE and TASK"
    }
    if ($manifest.Delivery -ne $manifest.Base + 1) {
        throw "$manifestName gives DELIVERY $($manifest.Delivery) on BASE $($manifest.Base); it must be BASE + 1"
    }

    return $manifest
}

function Read-Copy([string] $value, [string] $what) {

    $parts = $value.Split('>')
    if ($parts.Count -ne 2) { throw "$what must read 'COPY <source> > <target>': '$value'" }

    $source = Get-RelativePath $parts[0] $what
    $target = Get-RelativePath $parts[1] $what
    $origin = $source.Split('/')[0]

    if ($origin -ne 'assets' -and $origin -ne 'catalogue') {
        throw "$what copies from '$source'; a source starts with assets/ or catalogue/"
    }
    if (-not $target.StartsWith('assets/', [StringComparison]::OrdinalIgnoreCase)) {
        throw "$what copies to '$target'; a target is under assets/"
    }

    return New-Object PSObject -Property @{ Source = $source; Target = $target }
}

# Version \\

function Get-Delivery {

    if (-not (Test-Path -LiteralPath $versionFile)) { return 0 }

    foreach ($line in [IO.File]::ReadAllLines($versionFile, $utf8)) {
        if ($line -match '^Delivery:\s*(\d+)\s*$') { return [int] $Matches[1] }
    }

    throw "$versionFile has no 'Delivery: <number>' line"
}

function Write-Delivery($manifest) {

    $log = New-Object 'Collections.Generic.List[string]'
    $date = Get-Date -Format 'yyyy-MM-dd'

    if (Test-Path -LiteralPath $versionFile) {
        foreach ($line in [IO.File]::ReadAllLines($versionFile, $utf8)) {
            if ($line.StartsWith('- ')) { $log.Add($line) }
        }
    }
    $log.Add("- $($manifest.Delivery), $date, $($manifest.Task)")

    $text = @(
        '# Version',
        '',
        'The last delivery applied to this project. `.claude/Delivery.bat` writes this file each time it applies a',
        'delivery; never edit it by hand. Claude reads it from `main` on GitHub to know whether the last delivery was',
        'committed and pushed.',
        '',
        "Delivery: $($manifest.Delivery)",
        "Task: $($manifest.Task)",
        "Applied: $date",
        '',
        '## Log',
        ''
    ) + $log

    [IO.File]::WriteAllText($versionFile, ($text -join "`n") + "`n", $utf8)
}

# Git \\

function Invoke-Git {

    $ErrorActionPreference = 'Continue'
    if (-not (Get-Command git -CommandType Application -ErrorAction SilentlyContinue)) { return $null }

    $encoding = [Console]::OutputEncoding
    try {
        [Console]::OutputEncoding = $utf8
        $output = & git -C $root @args 2>$null
    }
    finally {
        [Console]::OutputEncoding = $encoding
    }

    if ($LASTEXITCODE -ne 0) { return $null }
    return $output
}

function Get-ChangedPaths {

    $changed = New-Object 'Collections.Generic.HashSet[string]' ([StringComparer]::OrdinalIgnoreCase)
    $status = Invoke-Git status --porcelain -z -uall
    if ($null -eq $status) { return ,$changed }

    $entries = ($status -join '').Split([char]0)
    for ($index = 0; $index -lt $entries.Count; $index++) {

        $entry = $entries[$index]
        if ($entry.Length -lt 4) { continue }

        $changed.Add($entry.Substring(3)) | Out-Null
        if ($entry[0] -eq 'R' -or $entry[0] -eq 'C') { $index++ }
    }

    return ,$changed
}

function Show-GitReminder {

    $changed = Get-ChangedPaths
    $ahead = Invoke-Git rev-list --count '@{u}..HEAD'

    if ($changed.Count -gt 0) { Write-Host "Reminder: $($changed.Count) changed files are not committed." }
    if ($null -ne $ahead -and [int] $ahead -gt 0) { Write-Host "Reminder: $ahead commits are not pushed." }
    if ($changed.Count -gt 0 -or ($null -ne $ahead -and [int] $ahead -gt 0)) {
        Write-Host 'Commit and push them, so Claude builds the next delivery from current code.'
    }
}

# Delivery \\

function Get-DeliveryZip {

    $zips = @(Get-ChildItem -LiteralPath $claude -File -Filter $zipFilter)
    if ($zips.Count -gt 1) {
        $names = ($zips | ForEach-Object { $_.Name }) -join ', '
        throw "More than one delivery zip in ${claude}: $names. Keep only the newest."
    }
    if ($zips.Count -eq 0) { return $null }

    return $zips[0]
}

function Read-ZipText($entry) {

    $reader = New-Object IO.StreamReader($entry.Open(), $utf8)
    try {
        return $reader.ReadToEnd()
    }
    finally {
        $reader.Dispose()
    }
}

function Get-ZipFiles($archive) {

    $files = New-Object 'Collections.Generic.List[object]'

    foreach ($entry in $archive.Entries) {

        if ($entry.Name -eq '') { continue }

        $relative = Get-RelativePath $entry.FullName 'A zip entry'
        if ($relative -eq $manifestName) { continue }
        if (Test-Protected $relative) { throw "The zip holds '$relative', which a delivery never writes" }

        $files.Add((New-Object PSObject -Property @{ Entry = $entry; Target = $relative }))
    }

    return ,$files
}

function Test-Delivery($manifest, $zipFiles) {

    $local = Get-Delivery
    if ($local -gt $manifest.Base) {
        throw ("This project is at delivery $local, but delivery $($manifest.Delivery) was built on delivery " +
               "$($manifest.Base) from GitHub, so delivery $local is not on GitHub. Commit and push it, then ask " +
               "Claude to rebuild delivery $($manifest.Delivery).")
    }
    if ($local -lt $manifest.Base) {
        throw ("This project is at delivery $local, but delivery $($manifest.Delivery) builds on delivery " +
               "$($manifest.Base). Pull from GitHub or apply delivery $($manifest.Base) first.")
    }

    foreach ($relative in $manifest.Deletes) {
        if (Test-Protected $relative) { throw "DELETE names '$relative', which a delivery never removes" }
        if (Test-Path -LiteralPath (Get-ProjectPath $relative) -PathType Container) {
            throw "DELETE names the folder '$relative'; a delivery deletes files"
        }
    }

    foreach ($copy in $manifest.Copies) {
        if (-not (Test-Path -LiteralPath (Get-LibraryPath $copy.Source) -PathType Leaf)) {
            throw "COPY source '.claude/$($copy.Source)' is not on this PC"
        }
    }

    $changed = Get-ChangedPaths
    $touched = @($manifest.Deletes) + @($zipFiles | ForEach-Object { $_.Target }) +
               @($manifest.Copies | ForEach-Object { $_.Target })
    return @($touched | Where-Object { $changed.Contains($_) } | Sort-Object -Unique)
}

function Confirm-Delivery($manifest, $zipFiles, $uncommitted) {

    Write-Host "Delivery $($manifest.Delivery): $($manifest.Task)"
    Write-Host ''
    foreach ($relative in $manifest.Deletes) { Write-Host "  Delete  $relative" }
    foreach ($file in $zipFiles) { Write-Host "  Write   $($file.Target)" }
    foreach ($copy in $manifest.Copies) { Write-Host "  Copy    .claude/$($copy.Source) > $($copy.Target)" }

    if ($uncommitted.Count -gt 0) {
        Write-Host ''
        Write-Host 'Uncommitted changes this delivery overwrites or deletes:' -ForegroundColor Yellow
        foreach ($relative in $uncommitted) { Write-Host "  $relative" -ForegroundColor Yellow }
    }

    Write-Host ''
    return (Read-Host 'Type Y and press Enter to apply it') -eq 'Y'
}

function Invoke-Delivery($zip) {

    $archive = [IO.Compression.ZipFile]::OpenRead($zip.FullName)
    try {
        $applied = Install-Delivery $zip $archive
    }
    finally {
        $archive.Dispose()
    }

    if ($applied) { Remove-Item -LiteralPath $zip.FullName -Force }
    return $applied
}

function Install-Delivery($zip, $archive) {

    $manifestEntry = $archive.GetEntry($manifestName)
    if ($null -eq $manifestEntry) { throw "$($zip.Name) has no $manifestName" }

    $manifest = Read-Manifest (Read-ZipText $manifestEntry)
    $zipFiles = Get-ZipFiles $archive
    $uncommitted = @(Test-Delivery $manifest $zipFiles)

    if (-not (Confirm-Delivery $manifest $zipFiles $uncommitted)) {
        Write-Host 'Cancelled. Nothing was changed.'
        return $false
    }

    $script:partial = $true

    foreach ($relative in $manifest.Deletes) {
        if (Test-Path -LiteralPath (Get-ProjectPath $relative) -PathType Leaf) { Remove-ProjectFile $relative }
        else { Write-Host "Already gone: $relative" }
    }
    foreach ($file in $zipFiles) { Write-ProjectEntry $file.Entry $file.Target }
    foreach ($copy in $manifest.Copies) { Copy-ProjectFile (Get-LibraryPath $copy.Source) $copy.Target }

    Write-Delivery $manifest
    $script:partial = $false

    Write-Host ''
    $counts = "deleted $($manifest.Deletes.Count), wrote $($zipFiles.Count), copied $($manifest.Copies.Count)"
    Write-Host "Applied delivery $($manifest.Delivery): $counts." -ForegroundColor Green
    return $true
}

# Catalogue \\

function Write-CatalogueList {

    if (-not (Test-Path -LiteralPath $catalogue -PathType Container)) {
        Write-Host "Catalogue folder not found, list left as it was: $catalogue"
        return
    }

    $catalogueRoot = (Get-Item -LiteralPath $catalogue).FullName.TrimEnd('\')
    $files = @(Get-ChildItem -LiteralPath $catalogueRoot -Recurse -File |
               ForEach-Object { $_.FullName.Substring($catalogueRoot.Length + 1).Replace('\', '/') } | Sort-Object)
    $text = if ($files.Count) { ($files -join [char]10) + [char]10 } else { '' }

    if (-not (Test-Path -LiteralPath $library)) { New-Item -ItemType Directory -Path $library | Out-Null }
    [IO.File]::WriteAllText($catalogueList, $text, $utf8)
    Write-Host "Listed $($files.Count) files in $catalogueList"
}

# Sounds \\

function Find-Ffmpeg {

    $command = Get-Command ffmpeg -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { return $command.Path }

    $portable = Join-Path $tools 'ffmpeg.exe'
    if (Test-Path -LiteralPath $portable) { return $portable }

    return $null
}

function Get-Ffmpeg {

    $ffmpeg = Find-Ffmpeg
    if ($ffmpeg) { return $ffmpeg }

    Write-Host 'ffmpeg was not found. Downloading a portable copy, about 175 MB, once...'
    if (-not (Test-Path -LiteralPath $tools)) { New-Item -ItemType Directory -Path $tools | Out-Null }

    $zip = Join-Path $tools 'ffmpeg.zip'
    $unpacked = Join-Path $tools 'ffmpeg'

    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $ffmpegUrl -OutFile $zip -UseBasicParsing
    Expand-Archive -LiteralPath $zip -DestinationPath $unpacked -Force

    $exe = Get-ChildItem -LiteralPath $unpacked -Recurse -Filter ffmpeg.exe | Select-Object -First 1
    Copy-Item -LiteralPath $exe.FullName -Destination (Join-Path $tools 'ffmpeg.exe') -Force
    Remove-Item -LiteralPath $zip, $unpacked -Recurse -Force

    $ffmpeg = Find-Ffmpeg
    if (-not $ffmpeg) { throw 'ffmpeg could not be downloaded. Check the internet connection and run this again.' }

    return $ffmpeg
}

function Convert-Sounds {

    if (-not (Test-Path -LiteralPath $sounds -PathType Container)) { throw "Sound folder not found: $sounds" }

    $files = @(Get-ChildItem -LiteralPath $sounds -Recurse -File |
               Where-Object { $soundExtensions -contains $_.Extension.ToLowerInvariant() })
    if ($files.Count -eq 0) {
        Write-Host 'No sounds to convert.'
        return
    }

    $ffmpeg = Get-Ffmpeg
    $converted = 0
    $failed = 0

    foreach ($file in $files) {

        $target = [IO.Path]::ChangeExtension($file.FullName, '.ogg')
        if (Test-Path -LiteralPath $target) {
            Write-Host "Skipped, $([IO.Path]::GetFileName($target)) already exists beside it: $($file.FullName)"
            $failed++
            continue
        }

        & $ffmpeg -nostdin -v error -i $file.FullName -map 0:a:0 -map_metadata -1 -c:a libvorbis -q:a 5 -ar 48000 `
            $target
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Failed: $($file.FullName)"
            if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Force }
            $failed++
            continue
        }

        Remove-Item -LiteralPath $file.FullName -Force
        Write-Host "Converted: $($file.FullName)"
        $converted++
    }

    Write-Host "Converted $converted sounds to .ogg, $failed failed."
    if ($failed -gt 0) { throw 'Sound conversion failed. See the files above.' }
}

# Run \\

$script:partial = $false

try {

    if (-not (Test-Path -LiteralPath (Join-Path $claude 'CLAUDE.md')) -or
        -not (Test-Path -LiteralPath (Join-Path $root 'gradlew.bat'))) {
        throw "Delivery.bat must sit in the project's .claude folder: $claude"
    }

    $zip = Get-DeliveryZip
    $applied = $true

    if ($zip) { $applied = Invoke-Delivery $zip }
    else { Write-Host "No $zipFilter in $claude. Listing the catalogue and converting sounds only." }

    if ($applied) {
        Write-Host ''
        Write-CatalogueList
        Convert-Sounds
        Write-Host ''
        Show-GitReminder
    }
}
catch {
    Write-Host ''
    Write-Host "Stopped: $($_.Exception.Message)" -ForegroundColor Red
    if ($script:partial) {
        Write-Host 'The delivery was partly applied; its zip is kept. Fix the cause and run this again.' `
            -ForegroundColor Red
    }
}

Write-Host ''
Read-Host 'Press Enter to close' | Out-Null
