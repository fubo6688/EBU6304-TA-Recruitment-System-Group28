param(
    [int]$Port = 8080,
    [switch]$ForceKill
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-TomcatHome {
    if ($env:CATALINA_HOME -and (Test-Path $env:CATALINA_HOME)) {
        return $env:CATALINA_HOME
    }

    $candidates = @("D:/apache-tomcat*", "C:/apache-tomcat*", "C:/Program Files/Apache Software Foundation/Tomcat*")
    foreach ($pattern in $candidates) {
        $match = Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($match) { return $match.FullName }
    }

    throw "Tomcat not found. Please set CATALINA_HOME first."
}

$tomcatHome = Resolve-TomcatHome
$shutdownBat = Join-Path $tomcatHome "bin/shutdown.bat"
if (-not (Test-Path $shutdownBat)) {
    throw ("Tomcat shutdown script not found at {0}" -f $shutdownBat)
}

# Ensure shutdown.bat resolves Tomcat home correctly when invoked from project directory.
$env:CATALINA_HOME = $tomcatHome
$env:CATALINA_BASE = $tomcatHome

Write-Host ("Stopping Tomcat from {0}" -f $tomcatHome)
& $shutdownBat | Out-Null
Start-Sleep -Seconds 3

$listeners = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
if (-not $listeners) {
    Write-Host ("Tomcat stopped. Port {0} is free." -f $Port) -ForegroundColor Green
    exit 0
}

if (-not $ForceKill) {
    Write-Host ("Port {0} still in use. Re-run with -ForceKill." -f $Port) -ForegroundColor Yellow
    $listeners | Select-Object LocalAddress, LocalPort, OwningProcess | Format-Table -AutoSize
    exit 1
}

$owners = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($ownerPid in $owners) {
    $proc = Get-Process -Id $ownerPid -ErrorAction SilentlyContinue
    $procName = if ($proc) { $proc.ProcessName } else { "Unknown" }
    Write-Host ("Force stopping PID {0} ({1})" -f $ownerPid, $procName) -ForegroundColor Yellow

    # If the port owner is managed by a Windows service, stop the service first.
    $service = Get-CimInstance Win32_Service -ErrorAction SilentlyContinue |
        Where-Object { $_.ProcessId -eq $ownerPid } |
        Select-Object -First 1

    if ($service) {
        Write-Host ("Detected service host: {0} ({1}), attempting Stop-Service first." -f $service.Name, $service.DisplayName) -ForegroundColor Yellow
        try {
            Stop-Service -Name $service.Name -Force -ErrorAction Stop
            Start-Sleep -Seconds 1
        } catch {
            Write-Host ("Stop-Service failed for {0}: {1}" -f $service.Name, $_.Exception.Message) -ForegroundColor Yellow
        }
    }

    try {
        Stop-Process -Id $ownerPid -Force -ErrorAction Stop
    } catch {
        Write-Host ("Stop-Process failed for PID {0}: {1}" -f $ownerPid, $_.Exception.Message) -ForegroundColor Yellow
        # Final fallback for stubborn processes.
        try {
            & taskkill /PID $ownerPid /F | Out-Null
        } catch {
            Write-Host ("taskkill failed for PID {0}: {1}" -f $ownerPid, $_.Exception.Message) -ForegroundColor Yellow
        }
    }
}

Start-Sleep -Seconds 1

$stillInUse = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
if ($stillInUse) {
    Write-Host ("Port {0} still in use after -ForceKill." -f $Port) -ForegroundColor Red
    $stillInUse | Select-Object LocalAddress, LocalPort, OwningProcess | Format-Table -AutoSize
    Write-Host "Tip: run PowerShell as Administrator, then retry stop-dev.ps1 -ForceKill." -ForegroundColor Red
    exit 1
}

Write-Host "Stopped and cleaned up successfully." -ForegroundColor Green
