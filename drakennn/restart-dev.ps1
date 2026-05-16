param(
    [int]$Port = 8080,
    [switch]$NoBrowser,
    [switch]$ForceKill
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$stopScript = Join-Path $projectRoot "stop-dev.ps1"
$startScript = Join-Path $projectRoot "start-dev.ps1"

if (-not (Test-Path $stopScript)) {
    throw "stop-dev.ps1 not found: $stopScript"
}
if (-not (Test-Path $startScript)) {
    throw "start-dev.ps1 not found: $startScript"
}

Write-Step "Stopping current service"
$stopArgs = @("-ExecutionPolicy", "Bypass", "-File", $stopScript, "-Port", $Port)
if ($ForceKill) {
    $stopArgs += "-ForceKill"
}

& powershell @stopArgs
if ($LASTEXITCODE -ne 0) {
    if (-not $ForceKill) {
        Write-Host "`n==> stop-dev.ps1 did not fully release the port; retrying with -ForceKill" -ForegroundColor Yellow
        & powershell -ExecutionPolicy Bypass -File $stopScript -Port $Port -ForceKill
    }

    if ($LASTEXITCODE -ne 0) {
        throw "stop-dev.ps1 failed with exit code $LASTEXITCODE"
    }
}

Write-Step "Starting service again"
$startArgs = @("-ExecutionPolicy", "Bypass", "-File", $startScript, "-Port", $Port)
if ($NoBrowser) {
    $startArgs += "-NoBrowser"
}
$startArgs += "-ForceRestart"

& powershell @startArgs
if ($LASTEXITCODE -ne 0) {
    throw "start-dev.ps1 failed with exit code $LASTEXITCODE"
}

Write-Host "`nRestart completed." -ForegroundColor Green
