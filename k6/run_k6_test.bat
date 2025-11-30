@echo off
SETLOCAL

REM Check if k6 is installed
where k6 >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
    echo k6 is not installed or not in PATH. Please install k6 from https://k6.io/docs/getting-started/installation/
    GOTO :EOF
)

REM Check if the backend is running (optional, but good practice)
REM You might need to adjust the URL and port
curl -s -o NUL http://localhost:8080/
IF %ERRORLEVEL% NEQ 0 (
    echo Backend is not running on http://localhost:8080. Please start the backend application first.
    GOTO :EOF
)

echo Running k6 load test...
k6 run k6/scripts/test.js

ENDLOCAL