@echo off
REM Escaneo OWASP ZAP contra el módulo de cuarentena
REM Requiere: Docker Desktop corriendo

echo ============================================
echo  OWASP ZAP - API Scan - Modulo Cuarentena
echo ============================================
echo.

set API_URL=http://host.docker.internal:8080
set ZAP_IMAGE=ghcr.io/zaproxy/zaproxy:stable

docker run --rm -v "%cd%/zap-reports:/zap/wrk" ^
  %ZAP_IMAGE% ^
  zap-api-scan.py ^
  -t %API_URL%/v3/api-docs ^
  -f openapi ^
  -w /zap/wrk/zap_quarantine_report.md ^
  -r /zap/wrk/zap_quarantine_report.html ^
  -z "-config rules.cookie.ignoredCookies=JSESSIONID" ^
  -d

echo.
echo Escaneo completado. Reportes en ./zap-reports/
echo.
