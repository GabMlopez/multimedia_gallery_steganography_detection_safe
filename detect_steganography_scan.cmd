@echo off
REM Compila el proyecto y ejecuta el escáner CLI para generar un reporte en /reports
setlocal

if "%JAVA_HOME%"=="" (
  rem Intentar detectar java en PATH escribiendo la ruta a un archivo temporal
  where java > "%TEMP%\\java-path.txt" 2>nul
  if exist "%TEMP%\\java-path.txt" (
    set /p JAVA_EXE=<"%TEMP%\\java-path.txt"
    del "%TEMP%\\java-path.txt"
    for %%J in ("%JAVA_EXE%") do set "JAVA_BIN_DIR=%%~dpJ"
    if "%JAVA_BIN_DIR:~-1%"=="\" set "JAVA_BIN_DIR=%JAVA_BIN_DIR:~0,-1%"
    for %%K in ("%JAVA_BIN_DIR%\\..") do set "JAVA_HOME=%%~fK"
    echo Se detectó Java en: %JAVA_EXE%
    echo Ajustando JAVA_HOME temporalmente a: %JAVA_HOME%
  ) else (
    echo ERROR: JAVA_HOME no está configurado y 'java' no se encontró en PATH.
    echo Configure JAVA_HOME o agregue Java al PATH.
    echo Ejemplo: set JAVA_HOME=C:\Java\jdk-25.0.1
    goto end
  )
)

set PATH=%JAVA_HOME%\bin;%PATH%
cd /d %~dp0

echo Compilando proyecto (saltando tests)...
call .\mvnw.cmd -q package -DskipTests
if errorlevel 1 (
  echo Error en la compilación. Revisar salida.
  goto end
)

echo Ejecutando DetectSteganographyCli...
java -cp target\classes proyecto_software.Proyecto.Primer.Parcial.de.Software.Seguro.cli.DetectSteganographyCli

echo Escaneo completado. Revisa la carpeta reports para el archivo generado.

:end
endlocal
