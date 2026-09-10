#!/usr/bin/env bash
# Prueba ejecutable de REQ-NF-009: mata un contenedor real y verifica que
# `restart: unless-stopped` + healthcheck lo recupera automaticamente.
#
# No es una prueba JUnit porque ejercita al daemon de Docker, no al codigo de
# la aplicacion -- pero es tan ejecutable como una: se corre, imprime un
# veredicto y termina con codigo de salida 0 (recuperado) o 1 (no recuperado).
#
# Uso:
#   bash scripts/demo-recuperacion-docker.sh [servicio] [timeout_segundos]
# Por defecto: servicio=backend (nombre de servicio de docker-compose, no el
# nombre del contenedor), timeout=180s.
#
# Requiere que el stack ya este arriba (`make up` / `docker compose up -d`).
#
# Nota de honestidad (ver docs/mediciones/resiliencia/REPORTE-RECUPERACION.md):
# la ultima corrida real de este mismo procedimiento (2026-09-10, Docker
# Desktop + WSL2) NO se recupero automaticamente en 2 servicios distintos
# (backend, postgres) -- se espera que este script pueda reproducir esa
# misma falla o confirmar que ya se corrigio, segun el entorno donde se corra.

set -uo pipefail

SERVICIO="${1:-backend}"
TIMEOUT="${2:-180}"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
COMPOSE="docker compose -f $REPO_ROOT/artisync/docker-compose.yml"

CONTENEDOR="$($COMPOSE ps -q "$SERVICIO" 2>/dev/null)"
if [[ -z "$CONTENEDOR" ]]; then
    echo "[ERROR] El servicio '$SERVICIO' no esta corriendo. Levanta el stack primero (make up)."
    exit 2
fi

NOMBRE="$(docker inspect --format='{{.Name}}' "$CONTENEDOR" | tr -d '/')"

estado_actual() {
    docker inspect --format='{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-healthcheck{{end}}' "$CONTENEDOR" 2>/dev/null
}

antes="$(estado_actual)"
if [[ "$antes" != *"healthy"* && "$antes" != running* ]]; then
    echo "[ERROR] '$NOMBRE' no esta en un estado sano antes de la prueba ($antes). Aborta."
    exit 2
fi

echo "== REQ-NF-009: matando '$NOMBRE' (servicio '$SERVICIO') =="
echo "Estado antes: $antes"
kill_time=$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)
docker kill "$CONTENEDOR" >/dev/null
echo "Kill enviado: $kill_time"

recuperado=0
elapsed=0
paso=3
while [[ $elapsed -lt $TIMEOUT ]]; do
    sleep "$paso"
    elapsed=$((elapsed + paso))
    estado="$(estado_actual)"
    echo "  +${elapsed}s: $estado"
    if [[ "$estado" == running*healthy* || ( "$estado" == running* && "$estado" == *sin-healthcheck* ) ]]; then
        recuperado=1
        break
    fi
done

if [[ $recuperado -eq 1 ]]; then
    echo "== [OK] '$NOMBRE' se recupero automaticamente en ~${elapsed}s =="
    exit 0
else
    echo "== [FALLO] '$NOMBRE' NO se recupero automaticamente en ${TIMEOUT}s =="
    echo "   Restaurando manualmente para no dejar el entorno caido..."
    docker start "$CONTENEDOR" >/dev/null
    exit 1
fi
