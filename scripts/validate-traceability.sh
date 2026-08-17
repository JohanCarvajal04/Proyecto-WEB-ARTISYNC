#!/usr/bin/env bash
# Valida docs/trazabilidad/matriz.csv contra la guia de la Tercera Entrega (A.3.3):
#
#   "El pipeline de CI ejecuta un script de validacion que rechaza commits en los
#    que se agregue un requisito sin correspondencia en al menos una historia,
#    un caso de uso o una prueba."
#
# Verificaciones (fallan el script, exit 1):
#   1. La cabecera de matriz.csv tiene las 11 columnas exigidas, en orden.
#   2. Cada id_requisito tiene el formato REQ-F-NNN / REQ-NF-NNN y es unico.
#   3. tipo in {funcional, no funcional}; prioridad_moscow in {Must, Should, Could, Won't}.
#   4. Todo requisito Must tiene correspondencia en al menos una de:
#      historia_usuario, caso_de_uso, prueba_automatizada, evidencia_empirica.
#      (evidencia_empirica se acepta ademas de lo literal de la guia porque es el
#      metodo de verificacion natural de los requisitos no funcionales, que no
#      siempre trazan a una historia de usuario individual — ver SRS.md 1.3/4.)
#   5. El 100% de los requisitos Must declarados en docs/requisitos/SRS.md
#      (patron **REQ-F-NNN** / **REQ-NF-NNN**) aparecen en la matriz.
#
# Advertencias (no bloquean el script, pero se listan):
#   - estado fuera del enum {pendiente, implementado, verificado} que declara A.3.3.
#
# Uso:
#   scripts/validate-traceability.sh [ruta/a/matriz.csv] [ruta/a/SRS.md]
# Por defecto usa docs/trazabilidad/matriz.csv y docs/requisitos/SRS.md relativos
# a la raiz del repositorio (detectada con git rev-parse).

set -uo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
MATRIZ="${1:-"$REPO_ROOT/docs/trazabilidad/matriz.csv"}"
SRS="${2:-"$REPO_ROOT/docs/requisitos/SRS.md"}"

EXPECTED_HEADER="id_requisito,tipo,prioridad_moscow,historia_usuario,caso_de_uso,modulo_codigo,endpoint_api,prueba_automatizada,tipo_acceso,evidencia_empirica,estado"

errors=0
warnings=0

fail() {
    echo "  [ERROR] $1"
    errors=$((errors + 1))
}

warn() {
    echo "  [WARN]  $1"
    warnings=$((warnings + 1))
}

if [[ ! -f "$MATRIZ" ]]; then
    echo "[ERROR] No se encontro la matriz de trazabilidad en: $MATRIZ"
    exit 1
fi

echo "== Validando $MATRIZ =="

# --- 1. Cabecera exacta -------------------------------------------------
actual_header="$(head -n1 "$MATRIZ" | tr -d '\r')"
if [[ "$actual_header" != "$EXPECTED_HEADER" ]]; then
    fail "Cabecera no coincide con la exigida por A.3.3."
    echo "          esperado: $EXPECTED_HEADER"
    echo "          actual:   $actual_header"
fi

# --- 2..4. Validacion fila a fila ---------------------------------------
declare -A seen_ids
row_num=1
while IFS=, read -r id_requisito tipo prioridad historia caso modulo endpoint prueba tipo_acceso evidencia estado; do
    row_num=$((row_num + 1))
    [[ -z "$id_requisito" ]] && continue

    # Formato de identificador
    if [[ ! "$id_requisito" =~ ^REQ-(F|NF)-[0-9]{3}$ ]]; then
        fail "Fila $row_num: id_requisito '$id_requisito' no cumple el formato REQ-F-NNN / REQ-NF-NNN."
    fi

    # Unicidad
    if [[ -n "${seen_ids[$id_requisito]:-}" ]]; then
        fail "Fila $row_num: id_requisito '$id_requisito' duplicado (primera aparicion en fila ${seen_ids[$id_requisito]})."
    else
        seen_ids[$id_requisito]="$row_num"
    fi

    # tipo
    if [[ "$tipo" != "funcional" && "$tipo" != "no funcional" ]]; then
        fail "Fila $row_num ($id_requisito): tipo '$tipo' invalido (esperado 'funcional' o 'no funcional')."
    fi

    # prioridad_moscow
    if [[ "$prioridad" != "Must" && "$prioridad" != "Should" && "$prioridad" != "Could" && "$prioridad" != "Won't" && "$prioridad" != "Wont" ]]; then
        fail "Fila $row_num ($id_requisito): prioridad_moscow '$prioridad' invalida (Must/Should/Could/Won't)."
    fi

    # estado (advertencia, no bloqueante: el proyecto usa 'parcial' ademas del
    # enum de la guia; se reporta para que el equipo decida formalizarlo o corregirlo)
    if [[ "$estado" != "pendiente" && "$estado" != "implementado" && "$estado" != "verificado" ]]; then
        warn "Fila $row_num ($id_requisito): estado '$estado' fuera del enum {pendiente, implementado, verificado} de A.3.3."
    fi

    # Correspondencia obligatoria para requisitos Must
    if [[ "$prioridad" == "Must" ]]; then
        if [[ -z "$historia" && -z "$caso" && -z "$prueba" && -z "$evidencia" ]]; then
            fail "Fila $row_num ($id_requisito, Must): sin correspondencia en historia_usuario, caso_de_uso, prueba_automatizada ni evidencia_empirica."
        fi
    fi
done < <(tail -n +2 "$MATRIZ" | tr -d '\r')

echo "  Filas de requisitos evaluadas: ${#seen_ids[@]}"

# --- 5. Cobertura 100% de los Must declarados en el SRS -----------------
if [[ -f "$SRS" ]]; then
    srs_ids="$(grep -oE '\*\*REQ-(F|NF)-[0-9]{3}\*\*' "$SRS" | tr -d '*' | sort -u)"
    missing=0
    while read -r req_id; do
        [[ -z "$req_id" ]] && continue
        if [[ -z "${seen_ids[$req_id]:-}" ]]; then
            fail "Requisito '$req_id' declarado en SRS.md pero ausente en la matriz de trazabilidad."
            missing=$((missing + 1))
        fi
    done <<< "$srs_ids"
    echo "  Requisitos en SRS.md: $(echo "$srs_ids" | grep -c .); ausentes en matriz: $missing"
else
    warn "No se encontro $SRS; se omite la verificacion de cobertura 100% contra el SRS."
fi

# --- Resultado ------------------------------------------------------------
echo "== Resumen: $errors error(es), $warnings advertencia(s) =="
if [[ $errors -gt 0 ]]; then
    echo "FALLO: la matriz de trazabilidad no cumple los requisitos obligatorios de A.3.3."
    exit 1
fi
echo "OK: la matriz de trazabilidad cumple las verificaciones obligatorias de A.3.3."
exit 0
