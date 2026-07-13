@REM Maven Wrapper script for Windows
@echo off
setlocal enableextensions

@REM maven-wrapper.jar is a library jar without a Main-Class manifest entry,
@REM so it must be launched with -classpath + launcher class, not with -jar.
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@REM %~dp0 ends with a backslash, which would escape the closing quote of the
@REM -D argument below. Strip it.
set "BASEDIR=%~dp0"
set "BASEDIR=%BASEDIR:~0,-1%"
set "WRAPPER_JAR=%BASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%BASEDIR%\.mvn\wrapper\maven-wrapper.properties"

if not "%JAVA_HOME%" == "" (
    set "JAVA_EXEC=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXEC=java"
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

"%JAVA_EXEC%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%BASEDIR%" %WRAPPER_LAUNCHER% %*
