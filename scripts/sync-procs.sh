#!/usr/bin/env bash
# =============================================================================
# sync-procs.sh — Propaga db/procs/*.sql a la migracion repetible de Flyway
# =============================================================================
# db/procs/ es la ubicacion canonica de las rutinas (apartado A.2.1 de la guia).
# Flyway solo aplica lo que encuentra bajo classpath:db/migration, de modo que
# este script concatena los archivos canonicos en una unica migracion repetible:
#
#     artisync/Backend/src/main/resources/db/migration/R__procedimientos.sql
#
# Flyway reaplica las migraciones repetibles cuando cambia su checksum, asi que
# editar una rutina y ejecutar `make sync-procs` basta para propagarla.
#
# Modos:
#   sync-procs.sh            Regenera el archivo destino.
#   sync-procs.sh --check    No escribe: sale con codigo 1 si el destino esta
#                            desincronizado respecto de db/procs/. Lo usa CI
#                            para impedir que las dos copias diverjan.
# =============================================================================
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ORIGEN="${RAIZ}/db/procs"
DESTINO="${RAIZ}/artisync/Backend/src/main/resources/db/migration/R__procedimientos.sql"

MODO_CHECK=0
if [[ "${1:-}" == "--check" ]]; then
    MODO_CHECK=1
elif [[ $# -gt 0 ]]; then
    echo "Uso: $(basename "$0") [--check]" >&2
    exit 2
fi

if [[ ! -d "$ORIGEN" ]]; then
    echo "ERROR: no existe el directorio de rutinas ${ORIGEN}" >&2
    exit 1
fi

# Orden alfabetico estable e independiente de la configuracion regional: el
# archivo generado debe ser identico byte a byte en cualquier maquina, o el
# modo --check produciria falsos positivos en CI.
#
# Solo se admiten fn_*.sql, sp_*.sql y V8__*.sql: son los unicos patrones de
# rutina reales en db/procs/. Un glob abierto (*.sql) concatenaria CUALQUIER
# archivo que caiga aqui -- incluido, por ejemplo, un pg_dump completo --
# dentro de esta migracion REPETIBLE, que Flyway reaplica en cada arranque
# donde cambie su checksum.
mapfile -t ARCHIVOS < <(
    LC_ALL=C find "$ORIGEN" -maxdepth 1 -type f \
        \( -name 'fn_*.sql' -o -name 'sp_*.sql' -o -name 'V8__*.sql' \) | LC_ALL=C sort
)

if [[ ${#ARCHIVOS[@]} -eq 0 ]]; then
    echo "ERROR: ${ORIGEN} no contiene ningun archivo .sql" >&2
    exit 1
fi

mapfile -t INTRUSOS < <(
    LC_ALL=C find "$ORIGEN" -maxdepth 1 -type f -name '*.sql' \
        ! -name 'fn_*.sql' ! -name 'sp_*.sql' ! -name 'V8__*.sql' | LC_ALL=C sort
)
if [[ ${#INTRUSOS[@]} -gt 0 ]]; then
    echo "ERROR: ${ORIGEN} contiene archivos .sql que no son rutinas:" >&2
    printf '  - %s\n' "${INTRUSOS[@]#"$ORIGEN"/}" >&2
    echo "       Solo se admiten fn_*.sql, sp_*.sql y V8__*.sql. Mueve o elimina los demas." >&2
    exit 1
fi

TEMPORAL="$(mktemp)"
trap 'rm -f "$TEMPORAL"' EXIT

{
    echo "-- ==========================================================================="
    echo "-- R__procedimientos.sql — ARCHIVO GENERADO. NO EDITAR A MANO."
    echo "-- ==========================================================================="
    echo "--"
    echo "-- Generado por scripts/sync-procs.sh a partir de db/procs/*.sql, que es la"
    echo "-- ubicacion canonica de las rutinas (apartado A.2.1 de la guia de la Entrega"
    echo "-- Final). Para modificar una rutina se edita su archivo en db/procs/ y se"
    echo "-- ejecuta \`make sync-procs\`."
    echo "--"
    echo "-- Migracion REPETIBLE: Flyway la reaplica cada vez que cambia su checksum."
    echo "-- Todas las rutinas usan CREATE OR REPLACE, por lo que reaplicarla es inocuo."
    echo "--"
    echo "-- Rutinas incluidas (${#ARCHIVOS[@]}):"
    for archivo in "${ARCHIVOS[@]}"; do
        echo "--   - $(basename "$archivo")"
    done
    echo "-- ==========================================================================="
    echo

    for archivo in "${ARCHIVOS[@]}"; do
        echo
        echo "-- ---------------------------------------------------------------------------"
        echo "-- Origen: db/procs/$(basename "$archivo")"
        echo "-- ---------------------------------------------------------------------------"
        cat "$archivo"
        echo
    done
} > "$TEMPORAL"

if [[ $MODO_CHECK -eq 1 ]]; then
    if [[ ! -f "$DESTINO" ]]; then
        echo "FALLO: no existe ${DESTINO}. Ejecuta 'make sync-procs' y commitea el resultado." >&2
        exit 1
    fi
    if ! diff -u "$DESTINO" "$TEMPORAL" > /dev/null 2>&1; then
        echo "FALLO: R__procedimientos.sql esta desincronizado respecto de db/procs/." >&2
        echo "       Ejecuta 'make sync-procs' y commitea el resultado." >&2
        echo >&2
        diff -u "$DESTINO" "$TEMPORAL" >&2 || true
        exit 1
    fi
    echo "OK: R__procedimientos.sql sincronizado con db/procs/ (${#ARCHIVOS[@]} rutinas)."
    exit 0
fi

mkdir -p "$(dirname "$DESTINO")"
cp "$TEMPORAL" "$DESTINO"
echo "Generado ${DESTINO#"$RAIZ"/} a partir de ${#ARCHIVOS[@]} rutinas en db/procs/."
