@echo off
REM Detectar esteganografía usando las pruebas del proyecto
REM Ejecutar desde la raíz del proyecto: detect_steganography.cmd

setlocal

if "%JAVA_HOME%"=="" (
  echo ERROR: JAVA_HOME no está configurado.
  echo Configure JAVA_HOME a su JDK antes de ejecutar este script.
  echo Ejemplo: set JAVA_HOME=C:\Java\jdk-25.0.1
  goto end
)

set PATH=%JAVA_HOME%\bin;%PATH%
cd /d %~dp0

echo =============================================
echo Ejecutando pruebas de deteccion de esteganografia
echo =============================================

call .\mvnw.cmd test -Dtest=FileStorageServiceTest#isValidImage_withValidJPEG_returnsTrue
call .\mvnw.cmd test -Dtest=FileStorageServiceTest#isValidImage_withEmptyFile_returnsFalse
call .\mvnw.cmd test -Dtest=FileStorageServiceTest#isValidImage_withInvalidFile_returnsFalse
call .\mvnw.cmd test -Dtest=FileStorageServiceTest#detectSteganography_withCleanJPEG_returnsFalse
call .\mvnw.cmd test -Dtest=FileStorageServiceTest#detectSteganography_withEOFAnomaly_returnsTrue
call .\mvnw.cmd test -Dtest=FileStorageServiceTest#detectSteganography_withPKZipSignature_returnsTrue
call .\mvnw.cmd test -Dtest=FileStorageServiceTest#getSteganographyReport_withEOFAnomaly_returnsReport
call .\mvnw.cmd test -Dtest=FileStorageServiceTest#getSteganographyReport_withCleanJPEG_returnsNull

echo.
echo Todas las pruebas de deteccion de esteganografia han terminado.
echo Si quieres ejecutar la clase completa, usa: .\mvnw.cmd test -Dtest=FileStorageServiceTest

:end
endlocal
