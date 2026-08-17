@echo off
REM Script de compilation du backend pour résoudre l'erreur PageResponse
REM Usage: compile-backend.bat

echo ========================================
echo Compilation du backend CMK-ERP
echo ========================================
echo.

REM Aller à la racine du projet
cd /d "%~dp0"

echo [1/3] Nettoyage des modules...
call mvn clean -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: Le nettoyage a echoue
    pause
    exit /b 1
)

echo.
echo [2/3] Compilation et installation de cmkerp-shared-kernel...
cd cmkerp-shared-kernel
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: La compilation de cmkerp-shared-kernel a echoue
    cd ..
    pause
    exit /b 1
)
cd ..

echo.
echo [3/3] Compilation et installation de cmkerp-stocks...
cd cmkerp-stocks
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: La compilation de cmkerp-stocks a echoue
    cd ..
    pause
    exit /b 1
)
cd ..

echo.
echo ========================================
echo Compilation terminee avec succes!
echo ========================================
echo.
echo Vous pouvez maintenant redemarrer le backend Spring Boot.
echo.
pause

