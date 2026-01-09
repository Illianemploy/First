@echo off
REM Quick sync script for Git + Android Studio workflow (Windows)

echo.
echo ============================================
echo  Checking Git status...
echo ============================================
echo.

REM Show current branch
for /f %%i in ('git branch --show-current') do set CURRENT_BRANCH=%%i
echo Current branch: %CURRENT_BRANCH%
echo.

REM Show status
echo Git status:
git status --short
echo.

REM Show recent commits
echo Recent commits:
git log --oneline -3
echo.

REM Ask if user wants to pull
set /p PULL="Pull latest changes? (y/n): "
if /i "%PULL%"=="y" (
    echo.
    echo Pulling from origin/%CURRENT_BRANCH%...
    git pull origin %CURRENT_BRANCH%
    echo.

    set /p CLEAN="Clean build? (y/n): "
    if /i "%CLEAN%"=="y" (
        echo.
        echo Cleaning...
        gradlew.bat clean
        echo.
        echo Clean complete!
        echo.
        echo Next steps in Android Studio:
        echo    1. File -^> Sync Project with Gradle Files
        echo    2. Build -^> Rebuild Project
    )
)

echo.
echo Sync check complete!
pause
