#!/usr/bin/env bash
# Prueba ejecutable de REQ-NF-001a/b/c contra el dominio publico real.
#
# A diferencia de una prueba JUnit, esta verificacion depende necesariamente de
# un analisis externo (SSL Labs) contra el despliegue real -- no es simulable
# ni reproducible sin red. El script formaliza en codigo (con asserts y codigo
# de salida) lo que docs/mediciones/sec/ssl-labs/REPORTE-SSL-LABS.md documenta
# como corrida puntual, para que pueda re-ejecutarse y re-verificarse sin
# repetir los pasos manuales.
#
# Uso:
#   bash scripts/verificar-tls-ssllabs.sh [dominio]
# Por defecto usa el dominio publico declarado en render.yaml.
#
# Codigo de salida: 0 si los 3 sub-requisitos cumplen; 1 si alguno falla.

set -uo pipefail

HOST="${1:-artisync-frontend.onrender.com}"
errors=0

echo "== REQ-NF-001a/b/c contra $HOST =="

# --- REQ-NF-001a: redireccion forzada de HTTP a HTTPS ---------------------
redirect_headers="$(curl -sS -i --max-time 15 "http://$HOST" 2>&1)"
status_line="$(echo "$redirect_headers" | head -n1)"
location="$(echo "$redirect_headers" | grep -i '^Location:' | tr -d '\r')"

if echo "$status_line" | grep -qE 'HTTP/[0-9.]+ 30[18]' && echo "$location" | grep -qi 'https://'; then
    echo "  [OK]    REQ-NF-001a: $status_line -> $location"
else
    echo "  [FALLO] REQ-NF-001a: se esperaba 301/308 a https://, se obtuvo: $status_line / $location"
    errors=$((errors + 1))
fi

# --- REQ-NF-001b/c: protocolos aceptados via API de SSL Labs --------------
echo "  Consultando API de SSL Labs (puede tardar 1-3 minutos)..."
resp=""
for i in $(seq 1 20); do
    resp="$(curl -sS --max-time 20 "https://api.ssllabs.com/api/v3/analyze?host=$HOST&all=done&fromCache=on&maxAge=24")"
    status="$(echo "$resp" | grep -o '"status":"[A-Z_]*"' | head -1 | cut -d'"' -f4)"
    [[ "$status" == "READY" || "$status" == "ERROR" ]] && break
    sleep 15
done

if [[ "$status" != "READY" ]]; then
    echo "  [FALLO] SSL Labs no devolvio READY (ultimo estado: $status). No se puede evaluar 001b/001c."
    errors=$((errors + 1))
else
    protocols="$(echo "$resp" | grep -o '"protocols":\[[^]]*\]')"

    # ids SSL Labs: 768=SSLv3, 769=TLS1.0, 770=TLS1.1, 771=TLS1.2, 772=TLS1.3
    if echo "$protocols" | grep -qE '"id":768|"id":769|"id":770'; then
        echo "  [FALLO] REQ-NF-001b: el servidor todavia acepta un protocolo obsoleto (SSLv3/TLS1.0/TLS1.1): $protocols"
        errors=$((errors + 1))
    else
        echo "  [OK]    REQ-NF-001b: ningun protocolo obsoleto aceptado. Protocolos reportados: $protocols"
    fi

    if echo "$protocols" | grep -q '"id":772'; then
        echo "  [OK]    REQ-NF-001c: TLS 1.3 (id 772) esta entre los protocolos aceptados por el servidor."
    else
        echo "  [FALLO] REQ-NF-001c: TLS 1.3 no aparece entre los protocolos aceptados: $protocols"
        errors=$((errors + 1))
    fi

    grade="$(echo "$resp" | grep -o '"grade":"[A-F+-]*"' | head -1 | cut -d'"' -f4)"
    echo "  Grade SSL Labs: ${grade:-desconocido}"
fi

echo "== Resumen: $errors fallo(s) =="
[[ $errors -gt 0 ]] && exit 1
exit 0
