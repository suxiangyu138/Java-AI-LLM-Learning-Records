@echo off
setlocal enabledelayedexpansion

echo ============================================
echo   GitHub Contributions x10 Script
echo ============================================
echo.

REM Check if we are in a git repo
git rev-parse --git-dir >nul 2>&1
if not %errorlevel%==0 (
    echo [ERROR] Not a git repository!
    pause
    exit /b 1
)

REM Get current branch name
for /f "delims=" %%i in ('git rev-parse --abbrev-ref HEAD') do set BRANCH=%%i
echo Current branch: !BRANCH!
echo.

REM Count and remove previous consecutive "daily contribution" commits from HEAD
echo Checking for previous contribution commits...
set COUNT=0
for /f "delims=" %%i in ('git log --oneline -30') do (
    echo %%i | findstr /c:"daily contribution" >nul
    if !errorlevel! equ 0 (
        set /a COUNT+=1
    ) else (
        goto :do_reset
    )
)

:do_reset
if !COUNT! gtr 0 (
    echo Found !COUNT! previous contribution commits, removing...
    git reset --soft HEAD~!COUNT!
    echo Done. Changes preserved, old commits removed.
) else (
    echo No previous contribution commits found.
)
echo.

echo Generating 10 commits...
echo.

for /l %%n in (1, 1, 10) do (
    echo [%%n/10] Commit at !date! !time! >> contributions.txt
    git add contributions.txt
    git commit -m "docs: daily contribution #%%n"
    echo [%%n/10] Done
    timeout /t 1 /nobreak >nul
)

echo.
echo ============================================
echo   10 commits done! Pushing to remote...
echo ============================================
echo.

git push --force-with-lease origin !BRANCH!
if !errorlevel! equ 0 (
    echo Push success!
) else (
    echo Push failed. Trying with --force...
    git push --force origin !BRANCH!
    if !errorlevel! equ 0 (
        echo Force push success!
    ) else (
        echo Push failed. Check network or remote config.
    )
)

echo.
pause
