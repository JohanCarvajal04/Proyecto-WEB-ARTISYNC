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
# Verificaciones anadidas para el criterio D0R de la Entrega Final:
#   6. Todo requisito Must esta en estado 'verificado'. Se admite la excepcion si
#      el id figura en docs/trazabilidad/excepciones-estado.txt con su motivo: asi
#      la deuda queda explicita y auditable en vez de invisible.
#   7. Todo Must en estado 'verificado' tiene prueba_automatizada no vacia.
#      Sin prueba no hay verificacion: solo implementacion.
#   8. estado dentro del enum {pendiente, implementado, verificado} (antes era
#      solo una advertencia).
#   9. El estado de cada requisito coincide entre SRS.md y la matriz. La matriz
#      es la fuente de verdad del estado; el SRS lo es del enunciado, la
#      prioridad y el criterio de aceptacion.
#  10. Ningun requisito Should queda en 'pendiente' (D0R admite solo
#      'implementado' o 'verificado'), salvo excepcion declarada.
#
# Uso:
#   scripts/validate-traceability.sh [ruta/a/matriz.csv] [ruta/a/SRS.md] [ruta/a/excepciones-estado.txt]
# Por defecto usa docs/trazabilidad/matriz.csv, docs/requisitos/SRS.md y
# docs/trazabilidad/excepciones-estado.txt relativos a la raiz del repositorio
# (detectada con git rev-parse).

set -uo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
MATRIZ="${1:-"$REPO_ROOT/docs/trazabilidad/matriz.csv"}"
SRS="${2:-"$REPO_ROOT/docs/requisitos/SRS.md"}"
EXCEPCIONES="${3:-"$REPO_ROOT/docs/trazabilidad/excepciones-estado.txt"}"

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

# --- Excepciones declaradas a la regla "Must => verificado" -------------
declare -A must_exceptions
if [[ -f "$EXCEPCIONES" ]]; then
    while IFS= read -r linea || [[ -n "$linea" ]]; do
        linea="${linea%%#*}"                       # descarta el motivo tras '#'
        linea="${linea//[[:space:]]/}"             # y cualquier espacio
        [[ -z "$linea" ]] && continue
        must_exceptions[$linea]=1
    done < "$EXCEPCIONES"
    echo "  Excepciones Must declaradas: ${#must_exceptions[@]} (ver $(basename "$EXCEPCIONES"))"
fi

# --- Estados declarados en el SRS, para contrastarlos con la matriz -----
declare -A srs_estado
if [[ -f "$SRS" ]]; then
    while IFS='=' read -r req_id req_estado; do
        [[ -z "$req_id" ]] && continue
        srs_estado[$req_id]="$req_estado"
    done < <(awk '
        match($0, /\*\*REQ-(F|NF)-[0-9][0-9][0-9]\*\*/) {
            cur = substr($0, RSTART + 2, RLENGTH - 4)
        }
        cur != "" && match($0, /Estado:[[:space:]]*[a-zA-Z]+/) {
            tok = substr($0, RSTART, RLENGTH)
            sub(/Estado:[[:space:]]*/, "", tok)
            print cur "=" tok
            cur = ""
        }
    ' "$SRS")
fi

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

    # 8. estado dentro del enum de A.3.3 (bloqueante desde la Entrega Final)
    if [[ "$estado" != "pendiente" && "$estado" != "implementado" && "$estado" != "verificado" ]]; then
        fail "Fila $row_num ($id_requisito): estado '$estado' fuera del enum {pendiente, implementado, verificado} de A.3.3."
    fi

    # 9. El estado no puede divergir entre el SRS y la matriz
    srs_val="${srs_estado[$id_requisito]:-}"
    if [[ -n "$srs_val" && "$srs_val" != "$estado" ]]; then
        fail "Fila $row_num ($id_requisito): estado '$estado' en la matriz pero '$srs_val' en SRS.md. La matriz es la fuente de verdad del estado: sincroniza el SRS."
    fi

    # Correspondencia obligatoria para requisitos Must
    if [[ "$prioridad" == "Must" ]]; then
        if [[ -z "$historia" && -z "$caso" && -z "$prueba" && -z "$evidencia" ]]; then
            fail "Fila $row_num ($id_requisito, Must): sin correspondencia en historia_usuario, caso_de_uso, prueba_automatizada ni evidencia_empirica."
        fi

        # 6. Must => verificado, salvo excepcion declarada con su motivo
        if [[ "$estado" != "verificado" ]]; then
            if [[ -z "${must_exceptions[$id_requisito]:-}" ]]; then
                fail "Fila $row_num ($id_requisito, Must): estado '$estado'. El criterio D0R exige 'verificado'; si aun no es posible, declara el motivo en $(basename "$EXCEPCIONES")."
            else
                echo "  [NOTA]  $id_requisito (Must, '$estado'): no verificado, con excepcion declarada."
            fi
        else
            # 7. No hay verificacion sin prueba automatizada que la sostenga
            if [[ -z "$prueba" ]]; then
                fail "Fila $row_num ($id_requisito, Must): estado 'verificado' con prueba_automatizada vacia. Sin prueba es 'implementado', no 'verificado'."
            fi
        fi
    fi

    # 10. Should => implementado o verificado (D0R), con la misma via de excepcion
    if [[ "$prioridad" == "Should" && "$estado" == "pendiente" ]]; then
        if [[ -z "${must_exceptions[$id_requisito]:-}" ]]; then
            fail "Fila $row_num ($id_requisito, Should): estado 'pendiente'. El criterio D0R admite solo 'implementado' o 'verificado' para Should; si se deja fuera de la version, declara el motivo en $(basename "$EXCEPCIONES")."
        else
            echo "  [NOTA]  $id_requisito (Should, 'pendiente'): fuera de alcance de v1.0.0, con excepcion declarada."
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
