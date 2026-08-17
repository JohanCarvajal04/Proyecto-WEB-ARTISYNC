#!/bin/sh
# Sustituye el placeholder __BACKEND_INTERNAL__ de nginx.render.conf por la
# direccion interna real del Private Service de backend (host:puerto),
# provista por Render en la variable de entorno BACKEND_INTERNAL_URL
# (ver render.yaml, fromService -> property: hostport).
#
# Se usa sed sobre un token exclusivo en vez de envsubst porque envsubst
# tambien sustituye las variables propias de nginx (ej. $host, $remote_addr)
# si no se filtra explicitamente, lo cual romperia nginx.render.conf.
set -e

: "${BACKEND_INTERNAL_URL:?BACKEND_INTERNAL_URL no esta definida (revisa render.yaml / el dashboard de Render)}"

sed -i "s#__BACKEND_INTERNAL__#${BACKEND_INTERNAL_URL}#g" /etc/nginx/conf.d/default.conf

exec nginx -g "daemon off;"
