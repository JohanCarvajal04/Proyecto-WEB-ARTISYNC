#!/usr/bin/env bash
# =============================================================================
# audit-sql-dynamic.sh — Auditoria estatica de SQL dinamico (A.2.3 de la guia)
# =============================================================================
# La regla transversal 7 de la Guia de la Entrega Final califica automaticamente
# los criterios P1 y P3 como Insuficiente ante cualquier evidencia de SQL
# construido por concatenacion de entrada de usuario, tanto en el codigo Java
# como dentro de un procedimiento almacenado. Este script es la comprobacion
# automatica que exige el apartado A.2.3; se ejecuta en `make audit` y en CI.
#
# Comprueba cuatro cosas:
#
#   1. db/procs/*.sql        — sin EXECUTE IMMEDIATE / sp_executesql / EXECUTE
#                              con expresion construida (format() o ||).
#   2. Migraciones Flyway    — la misma regla.
#   3. Codigo Java           — sin @Query(nativeQuery = true) concatenada.
#   4. Codigo Java           — sin createNativeQuery/createQuery con concatenacion,
#                              que la guia prohibe explicitamente como via de
#                              invocacion de procedimientos (A.2.1).
#
# Salida: 0 si no hay hallazgos; 1 con el detalle en stderr si los hay.
# =============================================================================
set -uo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

DIR_PROCS="db/procs"
DIR_MIGRACIONES="artisync/Backend/src/main/resources/db/migration"
DIR_JAVA="artisync/Backend/src/main/java"

HALLAZGOS=0

titulo() { printf '\n== %s ==\n' "$1"; }

reportar() {
    # $1 = descripcion del hallazgo, $2 = salida de grep
    printf 'FALLO: %s\n' "$1" >&2
    printf '%s\n' "$2" >&2
    HALLAZGOS=$((HALLAZGOS + 1))
}

# -----------------------------------------------------------------------------
# 1 y 2 — SQL dinamico en rutinas y migraciones
# -----------------------------------------------------------------------------
auditar_sql() {
    local dir="$1" etiqueta="$2"

    if [[ ! -d "$dir" ]]; then
        echo "AVISO: no existe ${dir}; nada que auditar en ${etiqueta}."
        return
    fi

    local salida

    # EXECUTE IMMEDIATE (Oracle/PLSQL) y sp_executesql (SQL Server). No son
    # sintaxis de PostgreSQL, pero la guia los nombra explicitamente y su
    # aparicion indicaria SQL copiado de otro motor.
    salida="$(grep -rniE 'EXECUTE[[:space:]]+IMMEDIATE|sp_executesql' "$dir" --include='*.sql' 2>/dev/null)"
    [[ -n "$salida" ]] && reportar "${etiqueta}: EXECUTE IMMEDIATE / sp_executesql" "$salida"

    # EXECUTE de PL/pgSQL con la sentencia construida: format(...) o
    # concatenacion con ||. Un EXECUTE sobre una constante literal entrecomillada
    # no se marca, porque no hay interpolacion posible.
    salida="$(grep -rniE '^[[:space:]]*EXECUTE[[:space:]]+.*(\|\||format[[:space:]]*\()' "$dir" --include='*.sql' 2>/dev/null)"
    [[ -n "$salida" ]] && reportar "${etiqueta}: EXECUTE con expresion construida (|| o format())" "$salida"

    # EXECUTE sobre una variable: EXECUTE v_sql;
    salida="$(grep -rniE '^[[:space:]]*EXECUTE[[:space:]]+[a-z_][a-z0-9_]*[[:space:]]*(USING|;)' "$dir" --include='*.sql' 2>/dev/null)"
    [[ -n "$salida" ]] && reportar "${etiqueta}: EXECUTE sobre variable (SQL dinamico)" "$salida"
}

titulo "Rutinas versionadas (${DIR_PROCS})"
auditar_sql "$DIR_PROCS" "db/procs"

titulo "Migraciones Flyway (${DIR_MIGRACIONES})"
auditar_sql "$DIR_MIGRACIONES" "migraciones"

# -----------------------------------------------------------------------------
# 3 — @Query(nativeQuery = true) con concatenacion
# -----------------------------------------------------------------------------
titulo "Consultas nativas en Java (${DIR_JAVA})"
if [[ -d "$DIR_JAVA" ]]; then
    # Se buscan archivos con nativeQuery = true y, dentro de ellos, un @Query
    # cuyo texto se una con el operador +.
    archivos_nativos="$(grep -rlE 'nativeQuery[[:space:]]*=[[:space:]]*true' "$DIR_JAVA" --include='*.java' 2>/dev/null)"
    if [[ -n "$archivos_nativos" ]]; then
        salida="$(printf '%s\n' "$archivos_nativos" | xargs -r grep -nE '@Query\(.*\+' 2>/dev/null)"
        [[ -n "$salida" ]] && reportar "Java: @Query(nativeQuery=true) con concatenacion '+'" "$salida"
    fi

    # -------------------------------------------------------------------------
    # 4 — createNativeQuery / createQuery con concatenacion
    # -------------------------------------------------------------------------
    # El apartado A.2.1 prohibe invocar rutinas mediante concatenacion en
    # createNativeQuery(...). Se marca cualquier uso con el operador + en el
    # argumento, que es el indicio de interpolacion de entrada.
    salida="$(grep -rnE 'create(Native)?Query\([^)]*"[^"]*"[[:space:]]*\+' "$DIR_JAVA" --include='*.java' 2>/dev/null)"
    [[ -n "$salida" ]] && reportar "Java: createNativeQuery/createQuery con concatenacion '+'" "$salida"
else
    echo "AVISO: no existe ${DIR_JAVA}; se omite la auditoria de Java."
fi

# -----------------------------------------------------------------------------
# Resultado
# -----------------------------------------------------------------------------
echo
if [[ $HALLAZGOS -gt 0 ]]; then
    echo "AUDITORIA FALLIDA: ${HALLAZGOS} tipo(s) de hallazgo. Ver detalle arriba." >&2
    exit 1
fi

n_procs=0
[[ -d "$DIR_PROCS" ]] && n_procs="$(find "$DIR_PROCS" -maxdepth 1 -name '*.sql' -type f | wc -l | tr -d ' ')"

echo "AUDITORIA SUPERADA: sin SQL dinamico ni concatenacion."
echo "  - Rutinas auditadas en db/procs/: ${n_procs}"
echo "  - Migraciones y codigo Java: sin hallazgos."
echo
echo "(Auditoria estatica. La verificacion empirica del control A03 de OWASP"
echo " esta en docs/mediciones/sec/REPORTE-SEC.md, y el analisis con find-sec-bugs"
echo " en docs/mediciones/sec/static-analysis/.)"
