#!/bin/sh
# Genera proxy.docker.conf.json en tiempo de arranque para que /api, /actuator
# y /ws apunten al backend correcto: "http://backend:8080" en docker-compose
# (red interna de Docker) o la URL del servicio backend en Render/otro host
# via BACKEND_INTERNAL_URL. PORT lo asigna Render dinamicamente (por defecto
# 4200 para docker-compose local).
set -e

BACKEND_URL="${BACKEND_INTERNAL_URL:-http://backend:8080}"

cat > proxy.docker.conf.json <<EOF
{
  "/api": { "target": "${BACKEND_URL}", "secure": false, "changeOrigin": true, "xfwd": true },
  "/actuator": { "target": "${BACKEND_URL}", "secure": false, "changeOrigin": true },
  "/ws": { "target": "${BACKEND_URL}", "secure": false, "changeOrigin": true, "ws": true }
}
EOF

exec npx ng serve --host 0.0.0.0 --port "${PORT:-4200}" --proxy-config proxy.docker.conf.json --poll 2000
