param(
    [string]$RepoPath = ".",
    [int]$DebounceSeconds = 3
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($DebounceSeconds -lt 1) {
    throw "DebounceSeconds must be >= 1"
}

$repoFullPath = (Resolve-Path $RepoPath).Path
Set-Location $repoFullPath

git rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Current directory is not a Git repository: $repoFullPath"
}

Write-Host "[AutoStage] Watching: $repoFullPath"
Write-Host "[AutoStage] Debounce: $DebounceSeconds second(s)"
Write-Host "[AutoStage] Press Ctrl + C to stop"

$script:pending = $false
$script:lastEventAt = Get-Date

$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = $repoFullPath
$watcher.Filter = "*"
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true
$watcher.NotifyFilter = [System.IO.NotifyFilters]::FileName -bor [System.IO.NotifyFilters]::DirectoryName -bor [System.IO.NotifyFilters]::LastWrite

$onChange = {
    # Ignore events inside .git to avoid event loops.
    if ($Event.SourceEventArgs.FullPath -like "*\.git\*") {
        return
    }
    $script:pending = $true
    $script:lastEventAt = Get-Date
}

$subscriptions = @(
    Register-ObjectEvent -InputObject $watcher -EventName Changed -Action $onChange,
    Register-ObjectEvent -InputObject $watcher -EventName Created -Action $onChange,
    Register-ObjectEvent -InputObject $watcher -EventName Deleted -Action $onChange,
    Register-ObjectEvent -InputObject $watcher -EventName Renamed -Action $onChange
)

try {
    while ($true) {
        Start-Sleep -Milliseconds 800

        if (-not $script:pending) {
            continue
        }

        $elapsed = ((Get-Date) - $script:lastEventAt).TotalSeconds
        if ($elapsed -lt $DebounceSeconds) {
            continue
        }

        $script:pending = $false

        git add -A
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[AutoStage] git add failed; check repository status." -ForegroundColor Red
            continue
        }

        $hasStaged = git diff --cached --name-only
        if ($hasStaged) {
            Write-Host "[AutoStage] Staged changes at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Green
        }
    }
}
finally {
    foreach ($s in $subscriptions) {
        Unregister-Event -SubscriptionId $s.Id -ErrorAction SilentlyContinue
        Remove-Job -Id $s.Id -Force -ErrorAction SilentlyContinue
    }
    $watcher.Dispose()
}
