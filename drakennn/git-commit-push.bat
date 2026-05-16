@echo off
setlocal enabledelayedexpansion

git rev-parse --is-inside-work-tree >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Current directory is not a Git repository.
  exit /b 1
)

for /f %%i in ('git branch --show-current') do set BRANCH=%%i
if "%BRANCH%"=="" (
  echo [ERROR] Cannot detect current branch. Please checkout a branch first.
  exit /b 1
)

git add -A

git diff --cached --quiet
if not errorlevel 1 (
  echo [INFO] No changes to commit.
  exit /b 0
)

set MSG=%*
if "%MSG%"=="" set MSG=chore: auto sync %date% %time%

echo [INFO] Commit message: %MSG%
git commit -m "%MSG%"
if errorlevel 1 (
  echo [ERROR] Commit failed.
  exit /b 1
)

git push origin %BRANCH%
if errorlevel 1 (
  echo [ERROR] Push failed. Check remote URL and permissions.
  exit /b 1
)

echo [DONE] Committed and pushed to origin/%BRANCH%
endlocal
