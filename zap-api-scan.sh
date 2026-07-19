#!/bin/bash
# Escaneo OWASP ZAP contra el módulo de cuarentena
# Requiere: Docker Desktop corriendo

echo "============================================"
echo " OWASP ZAP - API Scan - Modulo Cuarentena"
echo "============================================"
echo ""

API_URL="http://host.docker.internal:8080"
ZAP_IMAGE="ghcr.io/zaproxy/zaproxy:stable"

mkdir -p ./zap-reports

docker run --rm -v "$(pwd)/zap-reports:/zap/wrk" \
  $ZAP_IMAGE \
  zap-api-scan.py \
  -t "$API_URL/v3/api-docs" \
  -f openapi \
  -w /zap/wrk/zap_quarantine_report.md \
  -r /zap/wrk/zap_quarantine_report.html \
  -z "-config rules.cookie.ignoredCookies=JSESSIONID" \
  -d

echo ""
echo "Escaneo completado. Reportes en ./zap-reports/"
echo ""
