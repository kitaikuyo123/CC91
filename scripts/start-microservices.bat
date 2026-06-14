@echo off
setlocal enabledelayedexpansion

echo ========================================
echo   CC91 Microservices Backend Startup
echo ========================================
echo.

REM -- Required --
if "%DB_USERNAME%"=="" (
    set /p DB_USERNAME="MySQL username [root]: "
    if "!DB_USERNAME!"=="" set DB_USERNAME=root
)
if "%DB_PASSWORD%"=="" (
    set /p DB_PASSWORD="MySQL password: "
)

REM -- JWT --
if "%JWT_SECRET%"=="" (
    set JWT_SECRET=cc91-dev-secret-key-at-least-256-bits-long-for-local
    echo [INFO] Using dev JWT_SECRET
)

REM -- Internal token --
if "%INTERNAL_TOKEN%"=="" (
    set INTERNAL_TOKEN=cc91-internal-token-dev
    echo [INFO] Using dev INTERNAL_TOKEN
)

echo.
echo   DB_USERNAME     = %DB_USERNAME%
echo   JWT_SECRET      = ****
echo   INTERNAL_TOKEN  = ****
echo.

REM -- Service menu --
echo ----------------------------------------
echo   Select services to start:
echo ----------------------------------------
echo   0.  All services
echo   1.  Eureka Server      (8761)
echo   2.  API Gateway         (9000)
echo   3.  User Service        (8081)
echo   4.  Forum Service       (8082)
echo   5.  Notification Svc    (8083)
echo   6.  Content Service     (8085)
echo   7.  File Service        (8086)
echo ----------------------------------------
echo   Example: 0     = start all
echo            134   = Eureka + Gateway + User
echo            34567 = all business services
echo ----------------------------------------
echo.
set /p CHOICE="Enter numbers (e.g. 0 or 134): "

REM -- Parse selection --
set START_EUREKA=0
set START_GATEWAY=0
set START_USER=0
set START_FORUM=0
set START_NOTIFICATION=0
set START_CONTENT=0
set START_FILE=0

if "%CHOICE%"=="0" set "CHOICE=1234567"

echo.%CHOICE%| findstr "1">nul
if not errorlevel 1 set START_EUREKA=1
echo.%CHOICE%| findstr "2">nul
if not errorlevel 1 set START_GATEWAY=1
echo.%CHOICE%| findstr "3">nul
if not errorlevel 1 set START_USER=1
echo.%CHOICE%| findstr "4">nul
if not errorlevel 1 set START_FORUM=1
echo.%CHOICE%| findstr "5">nul
if not errorlevel 1 set START_NOTIFICATION=1
echo.%CHOICE%| findstr "6">nul
if not errorlevel 1 set START_CONTENT=1
echo.%CHOICE%| findstr "7">nul
if not errorlevel 1 set START_FILE=1

echo.
echo   Will start:
if "%START_EUREKA%"=="1"       echo    [1] Eureka Server
if "%START_GATEWAY%"=="1"      echo    [2] API Gateway
if "%START_USER%"=="1"         echo    [3] User Service
if "%START_FORUM%"=="1"        echo    [4] Forum Service
if "%START_NOTIFICATION%"=="1" echo    [5] Notification Service
if "%START_CONTENT%"=="1"      echo    [6] Content Service
if "%START_FILE%"=="1"         echo    [7] File Service
echo.

REM -- Eureka (must start first if selected) --
if "%START_EUREKA%"=="1" (
    echo [START] Eureka Server on port 8761 ...
    start "Eureka Server (8761)" cmd /k "cd /d %~dp0..\microservices\eureka-server && mvn spring-boot:run"
    echo [WAIT] 15s for Eureka to initialize ...
    timeout /t 15 /nobreak >nul
)

REM -- Gateway --
if "%START_GATEWAY%"=="1" (
    echo [START] API Gateway on port 9000 ...
    start "API Gateway (9000)" cmd /k "cd /d %~dp0..\microservices\gateway && mvn spring-boot:run"
)

REM -- User Service --
if "%START_USER%"=="1" (
    echo [START] User Service on port 8081 ...
    start "User Service (8081)" cmd /k "cd /d %~dp0..\microservices\user-service && mvn spring-boot:run"
)

REM -- Forum Service --
if "%START_FORUM%"=="1" (
    echo [START] Forum Service on port 8082 ...
    start "Forum Service (8082)" cmd /k "cd /d %~dp0..\microservices\forum-service && mvn spring-boot:run"
)

REM -- Notification Service --
if "%START_NOTIFICATION%"=="1" (
    echo [START] Notification Service on port 8083 ...
    start "Notification Service (8083)" cmd /k "cd /d %~dp0..\microservices\notification-service && mvn spring-boot:run"
)

REM -- Content Service --
if "%START_CONTENT%"=="1" (
    echo [START] Content Service on port 8085 ...
    start "Content Service (8085)" cmd /k "cd /d %~dp0..\microservices\content-service && mvn spring-boot:run"
)

REM -- File Service --
if "%START_FILE%"=="1" (
    echo [START] File Service on port 8086 ...
    start "File Service (8086)" cmd /k "cd /d %~dp0..\microservices\file-service && mvn spring-boot:run"
)

echo.
echo ========================================
echo   Done! Services are starting in separate windows.
echo.
echo   Eureka Dashboard:  http://localhost:8761
echo   API Gateway:       http://localhost:9000
echo   Gateway Health:    http://localhost:9000/actuator/health
echo.
echo   Wait ~30s for services to register,
echo   then check http://localhost:8761 to verify.
echo ========================================
echo.
pause
