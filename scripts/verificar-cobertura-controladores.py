#!/usr/bin/env python3
"""Verifica que todos los paquetes uteq.edu.ec.artisync.controller.* superen el 70% de
cobertura en lineas Y en ramas (P5 de la guia del examen suspenso), leyendo el
jacoco.csv que deja `mvn test` en artisync/Backend/target/site/jacoco/jacoco.csv.

Es de solo lectura. Termina con codigo de salida 0 si los 9 paquetes de controlador
superan el umbral, distinto de cero si alguno no lo alcanza.
"""
import csv
import sys

RUTA_CSV = sys.argv[1] if len(sys.argv) > 1 else "artisync/Backend/target/site/jacoco/jacoco.csv"
UMBRAL = 70.0

pkgs = {}
with open(RUTA_CSV, encoding="utf-8") as f:
    for row in csv.DictReader(f):
        pkg = row["PACKAGE"]
        if not pkg.startswith("uteq.edu.ec.artisync.controller"):
            continue
        lm, lc = int(row["LINE_MISSED"]), int(row["LINE_COVERED"])
        bm, bc = int(row["BRANCH_MISSED"]), int(row["BRANCH_COVERED"])
        d = pkgs.setdefault(pkg, [0, 0, 0, 0])
        d[0] += lm
        d[1] += lc
        d[2] += bm
        d[3] += bc

bajo_umbral = []
for pkg, (lm, lc, bm, bc) in sorted(pkgs.items()):
    lt, bt = lm + lc, bm + bc
    lp = (lc / lt * 100) if lt else 100.0
    bp = (bc / bt * 100) if bt else 100.0
    ok = lp >= UMBRAL and bp >= UMBRAL
    marca = "OK  " if ok else "BAJO"
    print(f"{marca} {pkg}: lineas {lc}/{lt} ({lp:.1f}%)  ramas {bc}/{bt} ({bp:.1f}%)")
    if not ok:
        bajo_umbral.append(pkg)

print()
print(f"Total: {len(pkgs)} paquetes de controlador, {len(bajo_umbral)} bajo el {UMBRAL:.0f}%.")
if bajo_umbral:
    print("BAJO UMBRAL:", bajo_umbral)
    sys.exit(1)
