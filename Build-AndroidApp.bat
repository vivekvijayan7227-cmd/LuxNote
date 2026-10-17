@echo off
setlocal enabledelayedexpansion
echo ========================================================
echo  LuxNote (Android Edition) - Build ^& Verification Tool
echo ========================================================

@rem Portable Java discovery: check JAVA_HOME first, then standard paths
if not defined JAVA_HOME (
    for %%p in (
        "%ProgramFiles%\Java\jdk-21"
        "%ProgramFiles%\Eclipse Adoptium\jdk-21"
        "%LOCALAPPDATA%\Programs\jdk-21"
        "%ProgramFiles%\Android\Android Studio\jbr"
    ) do (
        if exist "%%~p\bin\javac.exe" (
            set "JAVA_HOME=%%~p"
            goto :found_java
        )
    )
)
:found_java

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
    where javac.exe >nul 2>&1
    if !ERRORLEVEL! NEQ 0 (
        echo [WARNING] JAVA_HOME is not set and javac not found on PATH.
    )
)

cd /d "%~dp0HiResPlayer-Android"

echo.
echo [1/4] Running Unit ^& Parity Tests (DSP Engine, MediaStore, Storage)...
call gradlew.bat testDebugUnitTest --no-daemon
if !ERRORLEVEL! NEQ 0 (
    echo [ERROR] Unit tests failed!
    exit /b !ERRORLEVEL!
)

echo.
echo [2/4] Assembling LuxNote Production Release APK...
call gradlew.bat assembleRelease --no-daemon --no-configuration-cache
if !ERRORLEVEL! NEQ 0 (
    echo [ERROR] Release APK assembly failed! Ensure keystore.properties or environment credentials are set.
    exit /b !ERRORLEVEL!
)

if not exist "app\build\outputs\apk\release\app-release.apk" (
    echo [ERROR] Signed release APK not found at app\build\outputs\apk\release\app-release.apk!
    exit /b 1
)

echo.
echo [3/4] Performing Cryptographic Signature ^& Certificate Fingerprint Verification...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\verify-release-apk.ps1" -ApkPath "app\build\outputs\apk\release\app-release.apk" -ExpectedCertificateSha256 "992566A7DE642165F51526D7B5E0E0A6EB6CCB8954919B1DC82D2B348857B31A"
if !ERRORLEVEL! NEQ 0 (
    echo [ERROR] Cryptographic signature or certificate identity verification failed!
    exit /b !ERRORLEVEL!
)
echo [OK] APK signature and production certificate identity cryptographically verified.

copy /Y "app\build\outputs\apk\release\app-release.apk" "%~dp0LuxNote.apk" >nul
if !ERRORLEVEL! NEQ 0 (
    echo [ERROR] Failed to copy production APK to root directory!
    exit /b !ERRORLEVEL!
)

echo.
echo [4/4] Generating SHA-256 Checksum...
powershell -NoProfile -Command "(Get-FileHash -Path '%~dp0LuxNote.apk' -Algorithm SHA256).Hash + '  LuxNote.apk'" > "%~dp0LuxNote.apk.sha256"

echo.
echo ========================================================
echo  BUILD ^& RELEASE VERIFICATION SUCCESSFUL!
echo  Signed APK: %~dp0LuxNote.apk
echo  Checksum:   %~dp0LuxNote.apk.sha256
echo ========================================================
endlocal
