@REM Maven wrapper batch script redirecting to the local Maven installation
@echo off
set "MVN_CMD=C:\Users\HP\Downloads\maven-mvnd-1.0.3-windows-amd64\mvn\bin\mvn.cmd"
if exist "%MVN_CMD%" (
    "%MVN_CMD%" %*
) else (
    mvn %*
)
