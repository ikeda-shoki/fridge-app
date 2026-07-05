@REM Maven Wrapper script for Windows
@echo off
setlocal enableextensions

set BASEDIR=%~dp0
set WRAPPER_JAR=%BASEDIR%.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%BASEDIR%.mvn\wrapper\maven-wrapper.properties

if not "%JAVA_HOME%" == "" (
    set JAVA_EXEC=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_EXEC=java
)

if not exist "%WRAPPER_JAR%" (
    for /f "tokens=2 delims==" %%a in ('findstr /C:"wrapperUrl" "%WRAPPER_PROPERTIES%"') do set DOWNLOAD_URL=%%a
    echo Downloading Maven Wrapper from: %DOWNLOAD_URL%
    powershell -Command "(New-Object System.Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%WRAPPER_JAR%')"
    if errorlevel 1 (
        echo ERROR: Failed to download maven-wrapper.jar >&2
        exit /b 1
    )
)

"%JAVA_EXEC%" -jar "%WRAPPER_JAR%" %*
