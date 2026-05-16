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
$owners | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

$stillInUse = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
if ($stillInUse) {
    Write-Host ("Port {0} still in use. Please stop it manually." -f $Port) -ForegroundColor Red
    exit 1
}

Write-Host "Stopped and cleaned up successfully." -ForegroundColor Green
