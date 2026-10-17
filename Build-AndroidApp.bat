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
echo [3/3] Copying LuxNote Production APK...
if exist "app\build\outputs\apk\release\app-release.apk" (
    copy /Y "app\build\outputs\apk\release\app-release.apk" "%~dp0LuxNote.apk" >nul
) else if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    copy /Y "app\build\outputs\apk\release\app-release-unsigned.apk" "%~dp0LuxNote.apk" >nul
)

echo.
echo ========================================================
echo  BUILD SUCCESSFUL!
echo  APK Location: %~dp0LuxNote.apk
echo ========================================================
endlocal
