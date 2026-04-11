param(
    [int]$Port = 8080,
    [switch]$NoBrowser,
    [switch]$ForceRestart
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Ensure current session can find javac/java even if PATH is missing JDK bin.
$detectedJavaHome = $env:JAVA_HOME
if (-not $detectedJavaHome) {
    $detectedJavaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")
}
if ($detectedJavaHome -and (Test-Path (Join-Path $detectedJavaHome "bin/javac.exe"))) {
    $env:JAVA_HOME = $detectedJavaHome
    $env:Path = (Join-Path $env:JAVA_HOME "bin") + ";" + $env:Path
}

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Resolve-TomcatHome {
    if ($env:CATALINA_HOME -and (Test-Path $env:CATALINA_HOME)) {
        return $env:CATALINA_HOME
    }

    $candidates = @(
        "D:/apache-tomcat*",
        "C:/apache-tomcat*",
        "C:/Program Files/Apache Software Foundation/Tomcat*",
        "D:/Program Files/Apache Software Foundation/Tomcat*"
    )

    foreach ($pattern in $candidates) {
        $matched = Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($matched) {
            return $matched.FullName
        }
    }

    throw "Tomcat not found. Please set CATALINA_HOME first."
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $projectRoot "backend"
$backendWebInf = Join-Path $backendDir "WEB-INF"
$rootWebInf = Join-Path $projectRoot "WEB-INF"
$persistentDataDir = Join-Path $projectRoot "data"

if (-not (Test-Path $persistentDataDir)) {
    New-Item -ItemType Directory -Path $persistentDataDir | Out-Null
}

# Force application runtime data to stay in workspace data/ so restart/deploy won't reset records.
$env:TA_DATA_DIR = $persistentDataDir

Write-Step "Checking Java and Tomcat"
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw "javac not found. Please install JDK and configure PATH."
}

$tomcatHome = Resolve-TomcatHome
$startupBat = Join-Path $tomcatHome "bin/startup.bat"
$shutdownBat = Join-Path $tomcatHome "bin/shutdown.bat"
if (-not (Test-Path $startupBat)) {
    throw "Tomcat startup script not found at: $startupBat"
}

Write-Host "Project: $projectRoot"
Write-Host "Tomcat : $tomcatHome"
Write-Host "Data   : $persistentDataDir"

Write-Step "Compiling backend Java sources"
$classesDir = Join-Path $backendWebInf "classes"
if (-not (Test-Path $classesDir)) {
    New-Item -ItemType Directory -Path $classesDir | Out-Null
} else {
    Get-ChildItem -Path $classesDir -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}

$javaFiles = Get-ChildItem -Path (Join-Path $backendDir "src/com/ta") -Recurse -Filter "*.java" |
    ForEach-Object { $_.FullName }

if (-not $javaFiles -or $javaFiles.Count -eq 0) {
    throw "No Java files found under backend/src/com/ta"
}

$compileArgs = @(
    "-encoding", "UTF-8",
    "-cp", (Join-Path $backendWebInf "lib/*"),
    "-d", $classesDir
) + $javaFiles

& javac @compileArgs
if ($LASTEXITCODE -ne 0) {
    throw "Backend compile failed with exit code $LASTEXITCODE"
}

Write-Step "Syncing WEB-INF to root"
if (-not (Test-Path $rootWebInf)) {
    New-Item -ItemType Directory -Path $rootWebInf | Out-Null
}
if (-not (Test-Path (Join-Path $rootWebInf "classes"))) {
    New-Item -ItemType Directory -Path (Join-Path $rootWebInf "classes") | Out-Null
} else {
    Get-ChildItem -Path (Join-Path $rootWebInf "classes") -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}
if (-not (Test-Path (Join-Path $rootWebInf "lib"))) {
    New-Item -ItemType Directory -Path (Join-Path $rootWebInf "lib") | Out-Null
}

Copy-Item -Path (Join-Path $backendWebInf "web.xml") -Destination (Join-Path $rootWebInf "web.xml") -Force
Copy-Item -Path (Join-Path $classesDir "*") -Destination (Join-Path $rootWebInf "classes") -Recurse -Force

$jsonLib = Get-ChildItem -Path (Join-Path $backendWebInf "lib") -Filter "json*.jar" -ErrorAction SilentlyContinue |
    Select-Object -First 1
if ($jsonLib) {
    Copy-Item -Path $jsonLib.FullName -Destination (Join-Path $rootWebInf "lib/$($jsonLib.Name)") -Force
}

Write-Step "Deploying to Tomcat webapps contexts"
$contexts = @("ta-system", "MyRecruitmentSystem")
$robocopyExe = Join-Path $env:SystemRoot "System32/robocopy.exe"

foreach ($contextName in $contexts) {
    $appDir = Join-Path $tomcatHome "webapps/$contextName"
    if (-not (Test-Path $appDir)) {
        New-Item -ItemType Directory -Path $appDir | Out-Null
    }

    $tomcatWorkDir = Join-Path $tomcatHome "work/Catalina/localhost/$contextName"
    if (Test-Path $tomcatWorkDir) {
        Remove-Item -Path $tomcatWorkDir -Recurse -Force -ErrorAction SilentlyContinue
    }

    Write-Host "Deploying context: /$contextName"
    $robocopyArgs = @(
        $projectRoot,
        $appDir,
        "/MIR",
        "/R:2",
        "/W:1",
        "/XD", ".git", ".vscode", "target", "backend", "data", "Page Design(Version 1)",
        "/XF", "*.ps1", "*.bat"
    )

    & $robocopyExe @robocopyArgs | Out-Null
    $rc = $LASTEXITCODE
    if ($rc -gt 7) {
        throw "robocopy failed for context $contextName with exit code $rc"
    }
}

Write-Step "Starting Tomcat"
$portInUse = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
if ($ForceRestart -and (Test-Path $shutdownBat)) {
    & $shutdownBat | Out-Null
    Start-Sleep -Seconds 2
    $portInUse = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
}

if (-not $portInUse) {
    & $startupBat | Out-Null
    Start-Sleep -Seconds 2
} else {
    Write-Host "Tomcat already listening on port $Port, skip startup."
}

$url = "http://localhost:$Port/ta-system/login.html"
Write-Host "`nDone. Open: $url" -ForegroundColor Green

if (-not $NoBrowser) {
    Start-Process $url
}