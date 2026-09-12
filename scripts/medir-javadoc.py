#!/usr/bin/env python3
"""Mide la cobertura de Javadoc sobre el alcance acordado en el criterio de la
guia §3.10 ("Los metodos publicos de servicios y controladores documentan
parametros, retorno y excepciones") y en docs/observaciones/PLAN-EXAMEN-FINAL.md
(T-23): controladores (controller/**) + interfaces de servicio (service/**,
excluyendo service/shared/** por ser adaptadores de infraestructura fuera de la
capa de dominio, y **/impl/** porque en Java un metodo que sobreescribe hereda
el Javadoc de la interfaz sin necesidad de repetirlo).

Un metodo publico cuenta como documentado si tiene un bloque /** ... */
inmediatamente antes de su declaracion (saltando anotaciones intermedias como
@Override o @Auditable(...), que pueden ocupar varias lineas).

Uso:
    python scripts/medir-javadoc.py            # resumen + archivos con deficit
    python scripts/medir-javadoc.py --verbose  # + cada metodo sin documentar

Exit code: 0 siempre que el script corra sin errores de E/S (esto mide
cobertura, no es un gate de CI); para eso usar `make javadoc`, que sí falla
si el propio comando `javadoc` encuentra errores de sintaxis.
"""
import os
import re
import sys

# En Windows, sys.stdout hereda el codepage de la consola (cp1252) salvo que
# se fuerce UTF-8: sin esto, redirigir la salida a un archivo (por ejemplo
# para citar la cifra en un documento) corrompe las tildes en disco, no solo
# en pantalla.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(RAIZ, "artisync", "Backend", "src", "main", "java", "uteq", "edu", "ec", "artisync")

# Firma de metodo publico de nivel superior dentro de una clase (controller) o
# interfaz (service): empieza literalmente por "public " (controladores) o,
# en una interfaz, cualquier linea que declare un metodo (sin cuerpo, termina
# en ");" o abre parentesis) que no sea a su vez una palabra clave de tipo.
FIRMA_CLASE = re.compile(
    r'^public\s+(?!class\b|interface\b|enum\b|record\b)(?:static\s+|final\s+)*'
    r'[\w<>\[\],.?\s]+?\s+([A-Za-z_]\w*)\s*\('
)
FIRMA_INTERFAZ = re.compile(
    r'^(?!.*\b(?:class|interface|enum|record)\b)(?!@)(?!default\b)(?!static\b)'
    r'[A-Za-z_][\w<>\[\],.?\s]*?\s+([A-Za-z_]\w*)\s*\('
)


def en_alcance(ruta_relativa):
    """True si el archivo cae dentro del denominador acordado en T-23."""
    partes = ruta_relativa.split(os.sep)
    if partes[0] == "controller":
        return True
    if partes[0] == "service" and "shared" not in partes and "impl" not in partes:
        return True
    return False


def analizar_archivo(ruta, es_interfaz):
    with open(ruta, encoding="utf-8") as f:
        lineas = f.read().split("\n")

    total = 0
    documentados = 0
    faltantes = []

    for i, linea in enumerate(lineas):
        cuerpo = linea.strip()
        if not cuerpo or cuerpo.startswith(("//", "*", "/*")):
            continue

        patron = FIRMA_INTERFAZ if es_interfaz else FIRMA_CLASE
        m = patron.match(cuerpo)
        if not m:
            continue
        if es_interfaz and (cuerpo.startswith("@") or cuerpo.startswith("default") or cuerpo.startswith("static")):
            continue

        nombre_metodo = m.group(1)
        total += 1

        j = i - 1
        while j >= 0 and (lineas[j].strip().startswith("@") or lineas[j].strip() == ""):
            j -= 1
        if j >= 0 and lineas[j].strip().endswith("*/"):
            documentados += 1
        else:
            faltantes.append((i + 1, nombre_metodo))

    return total, documentados, faltantes


def main():
    verbose = "--verbose" in sys.argv
    resultados = []

    for dirpath, _dirnames, filenames in os.walk(SRC):
        rel_dir = os.path.relpath(dirpath, SRC)
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            rel = os.path.join(rel_dir, fn) if rel_dir != "." else fn
            if not en_alcance(rel):
                continue
            ruta = os.path.join(dirpath, fn)
            with open(ruta, encoding="utf-8") as f:
                src = f.read()

            es_controlador = rel.split(os.sep)[0] == "controller"
            if es_controlador:
                if "class " not in src:
                    continue
                es_interfaz = False
            else:
                if not re.search(r"public interface ", src):
                    continue
                es_interfaz = True

            total, doc, faltantes = analizar_archivo(ruta, es_interfaz)
            if total == 0:
                continue
            resultados.append({
                "categoria": "controlador" if es_controlador else "interfaz de servicio",
                "archivo": rel.replace(os.sep, "/"),
                "total": total,
                "documentados": doc,
                "faltantes": faltantes,
            })

    if not resultados:
        print("ERROR: no se encontro ningun archivo en el alcance. Revisa la ruta SRC.")
        sys.exit(1)

    plural = {"controlador": "controladores", "interfaz de servicio": "interfaces de servicio"}
    for categoria in ("controlador", "interfaz de servicio"):
        sub = [r for r in resultados if r["categoria"] == categoria]
        total = sum(r["total"] for r in sub)
        doc = sum(r["documentados"] for r in sub)
        pct = 100 * doc / total if total else 0
        print(f"{plural[categoria]}: {len(sub)} archivos, {doc}/{total} métodos documentados ({pct:.1f}%)")

    total = sum(r["total"] for r in resultados)
    doc = sum(r["documentados"] for r in resultados)
    pct = 100 * doc / total if total else 0
    print(f"\nTOTAL (alcance §3.10, T-23): {len(resultados)} archivos, {doc}/{total} métodos documentados ({pct:.1f}%)")

    con_deficit = [r for r in resultados if r["documentados"] < r["total"]]
    if con_deficit:
        print(f"\n{len(con_deficit)} archivo(s) con déficit:")
        for r in sorted(con_deficit, key=lambda r: r["total"] - r["documentados"], reverse=True):
            faltan = r["total"] - r["documentados"]
            print(f"  - {r['archivo']}: {r['documentados']}/{r['total']} ({faltan} sin documentar)")
            if verbose:
                for ln, nombre in r["faltantes"]:
                    print(f"      L{ln}: {nombre}")
        sys.exit(0)

    print("\nOK: 100% de cobertura en el alcance declarado.")
    sys.exit(0)


if __name__ == "__main__":
    main()
