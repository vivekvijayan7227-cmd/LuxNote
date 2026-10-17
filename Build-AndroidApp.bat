@echo off
setlocal
echo ========================================================
echo  LuxNote (Android Edition) - Build Tool
echo ========================================================

@rem Discover JAVA_HOME if not already set in environment
if "%JAVA_HOME%"=="" (
    if exist "C:\Users\Vivek\AppData\Local\Programs\jdk-21" (
        set "JAVA_HOME=C:\Users\Vivek\AppData\Local\Programs\jdk-21"
    )
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

cd /d "%~dp0HiResPlayer-Android"

echo.
echo [1/3] Running Unit Tests (DSP Engine, MediaStore Observer, Storage Resolver)...
call gradlew.bat testDebugUnitTest
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Unit tests failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [2/3] Assembling LuxNote Release APK...
call gradlew.bat assembleRelease
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Release APK assembly failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [3/4] Verifying and Copying Production Release APK...
if not exist "app\build\outputs\apk\release\app-release.apk" (
    echo [ERROR] Signed release APK not found!
    echo [ERROR] Release builds must be properly signed with production credentials.
    exit /b 1
)

copy /Y "app\build\outputs\apk\release\app-release.apk" "%~dp0LuxNote.apk" >nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to copy production APK to root directory!
    exit /b %ERRORLEVEL%
)

if not exist "%~dp0LuxNote.apk" (
    echo [ERROR] Verified LuxNote.apk does not exist at destination!
    exit /b 1
)

echo.
echo [4/4] Generating SHA-256 Checksum...
powershell -NoProfile -Command "(Get-FileHash -Path '%~dp0LuxNote.apk' -Algorithm SHA256).Hash + '  LuxNote.apk'" > "%~dp0LuxNote.apk.sha256"

echo.
echo ========================================================
echo  BUILD & RELEASE VERIFICATION SUCCESSFUL!
echo  Signed APK: %~dp0LuxNote.apk
echo  Checksum:   %~dp0LuxNote.apk.sha256
echo ========================================================
endlocal
