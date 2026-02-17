@echo off
chcp 949 >nul 2>&1
setlocal enabledelayedexpansion

:: ============================================================
:: Common Member Management - Service Manager (Windows CMD)
:: ============================================================

:: Project root (script location)
set "ROOT_DIR=%~dp0"
cd /d "%ROOT_DIR%"

:: JAVA_HOME default
if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\ms-21.0.10" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.10"
    )
)

:: --- Main Loop ---
:MENU
cls
echo.
echo  +====================================================+
echo  ^|     Common Member Management - Service Manager      ^|
echo  +====================================================+
echo.
echo   [Service Control]
echo   1) Stop All Services (Backend + Frontend + DB)
echo.
echo   [DB]
echo   2) Start DB (Docker)
echo   3) Reset DB (Drop Volume + Re-init Schema/Seed)
echo.
echo   [Backend]
echo   4) Build + Run Backend (java -jar)
echo   5) Run Backend (Docker)
echo.
echo   [Frontend]
echo   6) Run Frontend (npm run dev)
echo   7) Run Frontend (Docker) [NOT IMPLEMENTED]
echo.
echo   [Full Stack]
echo   8) Run All - java -jar + npm
echo   9) Run All - Docker
echo.
echo   0) Exit
echo.

set /p "CHOICE=  Select [0-9]: "

if "%CHOICE%"=="1" goto STOP_ALL
if "%CHOICE%"=="2" goto START_DB
if "%CHOICE%"=="3" goto RESET_DB
if "%CHOICE%"=="4" goto START_BACKEND_JAR
if "%CHOICE%"=="5" goto START_BACKEND_DOCKER
if "%CHOICE%"=="6" goto START_FRONTEND_NPM
if "%CHOICE%"=="7" goto START_FRONTEND_DOCKER
if "%CHOICE%"=="8" goto START_ALL_LOCAL
if "%CHOICE%"=="9" goto START_ALL_DOCKER
if "%CHOICE%"=="0" goto EXIT

echo.
echo  [WARN] Invalid selection. Enter 0-9.
pause
goto MENU

:: ---------------------------------------------
:: 1) Stop All Services
:: ---------------------------------------------
:STOP_ALL
echo.
echo  -- Stop All Services --
echo.

echo  [INFO] Stopping Docker containers...
docker compose --profile full down 2>nul

echo  [INFO] Stopping backend Java processes...
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo list 2^>nul ^| findstr /i "PID"') do (
    wmic process where "ProcessId=%%i" get CommandLine 2>nul | findstr /i "admin-api user-api" >nul 2>&1 && (
        taskkill /pid %%i /f >nul 2>&1
    )
)

echo  [INFO] Stopping frontend Node processes...
for /f "tokens=2 delims=," %%i in ('tasklist /fi "imagename eq node.exe" /fo csv /nh 2^>nul') do (
    wmic process where "ProcessId=%%~i" get CommandLine 2>nul | findstr /i "next" >nul 2>&1 && (
        taskkill /pid %%~i /f >nul 2>&1
    )
)

echo  [INFO] All services stopped.
goto RETURN

:: ---------------------------------------------
:: 2) Start DB + Redis (Docker)
:: ---------------------------------------------
:START_DB
echo.
echo  -- Start DB + Redis (Docker) --
echo.
echo  [INFO] Starting MySQL (13306) + Redis (6379)...
docker compose up -d db redis
echo.
echo  [INFO] DB + Redis started.
echo  [INFO] MySQL: localhost:13306  ^|  Redis: localhost:6379
goto RETURN

:: ---------------------------------------------
:: 3) Reset DB (Drop Volume + Re-init)
:: ---------------------------------------------
:RESET_DB
echo.
echo  -- Reset DB --
echo.
echo  [WARN] This will DELETE all data and re-run schema/seed SQL.
echo  [WARN] All existing data will be lost!
echo.
set /p "CONFIRM=  Continue? (y/N): "
if /i not "%CONFIRM%"=="y" (
    echo  [INFO] Cancelled.
    goto RETURN
)

echo  [INFO] Stopping backend Java processes (prevent stale cache)...
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo list 2^>nul ^| findstr /i "PID"') do (
    wmic process where "ProcessId=%%i" get CommandLine 2>nul | findstr /i "admin-api user-api" >nul 2>&1 && (
        taskkill /pid %%i /f >nul 2>&1
    )
)

echo  [INFO] Stopping DB containers and removing volumes...
docker compose down db redis -v 2>nul

echo  [INFO] Restarting DB + Redis containers...
docker compose up -d db redis

echo  [INFO] Waiting for DB init (15s)...
timeout /t 15 /nobreak >nul

echo  [INFO] DB reset complete. Schema and seed data re-applied.
echo  [INFO] MySQL: localhost:13306  ^|  Redis: localhost:6379
echo  [WARN] Backend was stopped. Use option 4 to restart.
goto RETURN

:: ---------------------------------------------
:: 4) Build + Run Backend (java -jar)
:: ---------------------------------------------
:START_BACKEND_JAR
echo.
echo  -- Build + Run Backend (java -jar) --
echo.

:: Check JAVA_HOME
if not defined JAVA_HOME (
    echo  [ERROR] JAVA_HOME is not set.
    goto RETURN
)
echo  [INFO] JAVA_HOME: %JAVA_HOME%

:: Check DB is running
docker ps --format "{{.Names}}" 2>nul | findstr /i "member-mange-db" >nul 2>&1
if errorlevel 1 (
    echo  [WARN] DB container not running. Starting...
    docker compose up -d db redis
    echo  [INFO] Waiting for DB init (10s)...
    timeout /t 10 /nobreak >nul
)

:: Gradle build
echo  [INFO] Starting Gradle build...
cd /d "%ROOT_DIR%backend"
call gradlew.bat clean build -x test
if errorlevel 1 (
    echo  [ERROR] Gradle build failed.
    cd /d "%ROOT_DIR%"
    goto RETURN
)
cd /d "%ROOT_DIR%"

:: Kill existing backend processes
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo list 2^>nul ^| findstr /i "PID"') do (
    wmic process where "ProcessId=%%i" get CommandLine 2>nul | findstr /i "admin-api user-api" >nul 2>&1 && (
        taskkill /pid %%i /f >nul 2>&1
    )
)

:: Start admin-api (new window)
echo  [INFO] Starting admin-api (port 5000)...
for %%f in (backend\admin-api\build\libs\*.jar) do (
    start "admin-api" "%JAVA_HOME%\bin\java" -jar "%%f"
)

:: Start user-api (new window)
echo  [INFO] Starting user-api (port 6100)...
for %%f in (backend\user-api\build\libs\*.jar) do (
    start "user-api" "%JAVA_HOME%\bin\java" -jar "%%f"
)

echo.
echo  [INFO] Backend started.
echo  [INFO] Admin API: http://localhost:5000  ^|  User API: http://localhost:6100
goto RETURN

:: ---------------------------------------------
:: 5) Run Backend (Docker)
:: ---------------------------------------------
:START_BACKEND_DOCKER
echo.
echo  -- Run Backend (Docker) --
echo.
echo  [INFO] Building and starting admin-api + user-api containers...
docker compose --profile full up -d --build admin-api user-api db redis
echo.
echo  [INFO] Backend Docker services started.
echo  [INFO] Admin API: http://localhost:5000  ^|  User API: http://localhost:6100
goto RETURN

:: ---------------------------------------------
:: 6) Run Frontend (npm run dev)
:: ---------------------------------------------
:START_FRONTEND_NPM
echo.
echo  -- Run Frontend (npm run dev) --
echo.

:: admin frontend (new window)
echo  [INFO] Starting admin frontend (port 3010)...
start "admin-frontend" cmd /c "cd /d "%ROOT_DIR%frontend\admin" && npm run dev"

:: user frontend (new window)
echo  [INFO] Starting user frontend (port 3020)...
start "user-frontend" cmd /c "cd /d "%ROOT_DIR%frontend\user" && npm run dev"

echo.
echo  [INFO] Frontend started.
echo  [INFO] Admin: http://localhost:3010  ^|  User: http://localhost:3020
goto RETURN

:: ---------------------------------------------
:: 7) Run Frontend (Docker) - NOT IMPLEMENTED
:: ---------------------------------------------
:START_FRONTEND_DOCKER
echo.
echo  -- Run Frontend (Docker) --
echo.
echo  [WARN] Frontend Dockerfile not implemented yet.
echo  [WARN] Create frontend\admin\Dockerfile and frontend\user\Dockerfile first.
goto RETURN

:: ---------------------------------------------
:: 8) Run All - java -jar + npm
:: ---------------------------------------------
:START_ALL_LOCAL
echo.
echo  -- Run All (java -jar + npm) --
echo.

:: Start DB
echo  [INFO] Starting MySQL + Redis containers...
docker compose up -d db redis
echo  [INFO] Waiting for DB init (10s)...
timeout /t 10 /nobreak >nul

:: Check JAVA_HOME
if not defined JAVA_HOME (
    echo  [ERROR] JAVA_HOME is not set.
    goto RETURN
)

:: Gradle build
echo  [INFO] Starting Gradle build...
cd /d "%ROOT_DIR%backend"
call gradlew.bat clean build -x test
if errorlevel 1 (
    echo  [ERROR] Gradle build failed.
    cd /d "%ROOT_DIR%"
    goto RETURN
)
cd /d "%ROOT_DIR%"

:: Start admin-api
echo  [INFO] Starting admin-api (port 5000)...
for %%f in (backend\admin-api\build\libs\*.jar) do (
    start "admin-api" "%JAVA_HOME%\bin\java" -jar "%%f"
)

:: Start user-api
echo  [INFO] Starting user-api (port 6100)...
for %%f in (backend\user-api\build\libs\*.jar) do (
    start "user-api" "%JAVA_HOME%\bin\java" -jar "%%f"
)

echo  [INFO] Waiting for backend startup (5s)...
timeout /t 5 /nobreak >nul

:: Start frontend
echo  [INFO] Starting admin frontend (port 3010)...
start "admin-frontend" cmd /c "cd /d "%ROOT_DIR%frontend\admin" && npm run dev"

echo  [INFO] Starting user frontend (port 3020)...
start "user-frontend" cmd /c "cd /d "%ROOT_DIR%frontend\user" && npm run dev"

echo.
echo  ===============================================
echo    All services started!
echo    Admin API   : http://localhost:5000
echo    User  API   : http://localhost:6100
echo    Admin Front : http://localhost:3010
echo    User  Front : http://localhost:3020
echo    MySQL       : localhost:13306
echo    Redis       : localhost:6379
echo  ===============================================
goto RETURN

:: ---------------------------------------------
:: 9) Run All - Docker
:: ---------------------------------------------
:START_ALL_DOCKER
echo.
echo  -- Run All (Docker) --
echo.
echo  [INFO] Building and starting all Docker containers...
docker compose --profile full up -d --build
echo.
echo  ===============================================
echo    Docker services started!
echo    Admin API   : http://localhost:5000
echo    User  API   : http://localhost:6100
echo    MySQL       : localhost:13306
echo    Redis       : localhost:6379
echo    [WARN] Frontend not in Docker - use npm run dev
echo  ===============================================
goto RETURN

:: ---------------------------------------------
:: Return to menu
:: ---------------------------------------------
:RETURN
echo.
pause
goto MENU

:: ---------------------------------------------
:: Exit
:: ---------------------------------------------
:EXIT
echo.
echo  [INFO] Exiting.
endlocal
exit /b 0
