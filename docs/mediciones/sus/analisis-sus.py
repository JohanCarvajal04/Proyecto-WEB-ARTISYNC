#!/usr/bin/env python3
"""Analisis del cuestionario System Usability Scale (SUS) para ARTISYNC.

Uso:
    python docs/mediciones/sus/analisis-sus.py > docs/mediciones/sus/salida-sus.txt

Lee docs/mediciones/sus/sus-raw.csv (ruta relativa al propio script, no al cwd),
valida su formato con las reglas descritas en instrucciones-formulario.md y
calcula el puntaje SUS (Brooke, 1986) por participante y agregado.

Solo biblioteca estandar: no requiere instalar dependencias.
"""

from __future__ import annotations

import csv
import hashlib
import statistics
import sys
from pathlib import Path

MIN_PARTICIPANTES = 10
UMBRAL_PROYECTO = 68.0
ENCABEZADO_ESPERADO = ["participante"] + [f"Q{i}" for i in range(1, 11)]

# Items impares (1,3,5,7,9): redactados en positivo -> contribucion = Xi - 1
# Items pares   (2,4,6,8,10): redactados en negativo -> contribucion = 5 - Xi
ITEMS_IMPARES = {1, 3, 5, 7, 9}

# Valores criticos de t de Student para IC 95% (dos colas, alpha=0.05), por
# grados de libertad (df = n-1). df 1..30; por encima se usa el limite normal.
TABLA_T95 = {
    1: 12.706, 2: 4.303, 3: 3.182, 4: 2.776, 5: 2.571,
    6: 2.447, 7: 2.365, 8: 2.306, 9: 2.262, 10: 2.228,
    11: 2.201, 12: 2.179, 13: 2.160, 14: 2.145, 15: 2.131,
    16: 2.120, 17: 2.110, 18: 2.101, 19: 2.093, 20: 2.086,
    21: 2.080, 22: 2.074, 23: 2.069, 24: 2.064, 25: 2.060,
    26: 2.056, 27: 2.052, 28: 2.048, 29: 2.045, 30: 2.042,
}
T_LIMITE_NORMAL = 1.96


class ErrorValidacionCsv(Exception):
    """Datos de entrada mal formados: no se puede calcular el SUS."""


def valor_critico_t(df: int) -> float:
    if df in TABLA_T95:
        return TABLA_T95[df]
    return T_LIMITE_NORMAL


def cargar_respuestas(ruta_csv: Path) -> list[dict]:
    if not ruta_csv.exists():
        raise ErrorValidacionCsv(f"No se encontro el archivo: {ruta_csv}")

    with ruta_csv.open(newline="", encoding="utf-8") as f:
        contenido = f.read()

    if not contenido.strip():
        raise ErrorValidacionCsv("El CSV esta vacio.")

    lector = csv.reader(contenido.splitlines())
    filas = list(lector)

    if not filas:
        raise ErrorValidacionCsv("El CSV no tiene filas.")

    encabezado = filas[0]
    if encabezado != ENCABEZADO_ESPERADO:
        raise ErrorValidacionCsv(
            "Encabezado invalido. Se esperaba exactamente: "
            f"{','.join(ENCABEZADO_ESPERADO)}\n"
            f"Se encontro: {','.join(encabezado)}\n"
            "Si esto es un export crudo de Google Forms (columna 'Marca temporal' "
            "y el texto completo de cada pregunta como encabezado), conviertelo "
            "primero siguiendo la seccion 6 de instrucciones-formulario.md."
        )

    filas_datos = [fila for fila in filas[1:] if fila]
    if not filas_datos:
        raise ErrorValidacionCsv(
            "El CSV tiene encabezado pero ninguna fila de datos "
            "(hoy solo tiene el encabezado, ver instrucciones-formulario.md:76)."
        )

    participantes = []
    codigos_vistos: set[str] = set()
    for i, fila in enumerate(filas_datos, start=2):
        if len(fila) != 11:
            raise ErrorValidacionCsv(
                f"Linea {i}: se esperaban 11 columnas (participante + Q1..Q10), "
                f"se encontraron {len(fila)}."
            )

        codigo = fila[0].strip()
        if not codigo:
            raise ErrorValidacionCsv(f"Linea {i}: codigo de participante vacio.")
        if codigo in codigos_vistos:
            raise ErrorValidacionCsv(
                f"Linea {i}: codigo de participante duplicado ({codigo})."
            )
        codigos_vistos.add(codigo)

        respuestas = []
        for j, crudo in enumerate(fila[1:], start=1):
            crudo = crudo.strip()
            try:
                valor = int(crudo)
            except ValueError:
                raise ErrorValidacionCsv(
                    f"Linea {i}, Q{j}: '{crudo}' no es un entero."
                )
            if not (1 <= valor <= 5):
                raise ErrorValidacionCsv(
                    f"Linea {i}, Q{j}: valor {valor} fuera de rango (debe ser 1..5)."
                )
            respuestas.append(valor)

        participantes.append({"codigo": codigo, "respuestas": respuestas})

    return participantes


def puntaje_sus(respuestas: list[int]) -> float:
    total = 0.0
    for idx, valor in enumerate(respuestas, start=1):
        if idx in ITEMS_IMPARES:
            total += valor - 1
        else:
            total += 5 - valor
    return total * 2.5


def contribucion_normalizada(respuestas: list[int]) -> list[float]:
    """Contribucion 0-4 por item, ya con la inversion de items pares aplicada."""
    salida = []
    for idx, valor in enumerate(respuestas, start=1):
        salida.append(float(valor - 1) if idx in ITEMS_IMPARES else float(5 - valor))
    return salida


def interpretacion_bangor(media: float) -> str:
    # Bangor, Kortum & Miller (2009) - grados de aceptabilidad sobre SUS 0-100.
    if media >= 84.1:
        return "A+ (excelente / mejor imaginable)"
    if media >= 80.8:
        return "A (excelente)"
    if media >= 78.9:
        return "A-"
    if media >= 77.2:
        return "B+"
    if media >= 74.1:
        return "B"
    if media >= 72.6:
        return "B-"
    if media >= 71.1:
        return "C+"
    if media >= 65.0:
        return "C (aceptable, promedio)"
    if media >= 62.7:
        return "C-"
    if media >= 51.7:
        return "D"
    return "F (no aceptable / peor imaginable)"


def sha256_archivo(ruta: Path) -> str:
    h = hashlib.sha256()
    with ruta.open("rb") as f:
        h.update(f.read())
    return h.hexdigest()


def main() -> int:
    if len(sys.argv) > 1:
        ruta_csv = Path(sys.argv[1])
    else:
        ruta_csv = Path(__file__).resolve().parent / "sus-raw.csv"

    try:
        participantes = cargar_respuestas(ruta_csv)
    except ErrorValidacionCsv as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    n = len(participantes)
    puntajes = [puntaje_sus(p["respuestas"]) for p in participantes]

    media = statistics.mean(puntajes)
    mediana = statistics.median(puntajes)
    dt = statistics.stdev(puntajes) if n > 1 else 0.0
    minimo = min(puntajes)
    maximo = max(puntajes)

    if n > 1:
        df = n - 1
        t = valor_critico_t(df)
        error_estandar = dt / (n ** 0.5)
        margen = t * error_estandar
        ic_inf, ic_sup = media - margen, media + margen
    else:
        df = 0
        t = float("nan")
        ic_inf = ic_sup = media

    # Media normalizada (0-4) por item, para comparar Q1..Q10 en la misma escala.
    matriz_normalizada = [contribucion_normalizada(p["respuestas"]) for p in participantes]
    medias_por_item = [
        statistics.mean(fila[i] for fila in matriz_normalizada) for i in range(10)
    ]

    sha = sha256_archivo(ruta_csv)

    print("=== Reporte de Usabilidad SUS (System Usability Scale) ===")
    print(f"Archivo de entrada: {ruta_csv.name}")
    print(f"SHA-256: {sha}")
    print()
    print(f"n participantes: {n}")
    if n < MIN_PARTICIPANTES:
        print(
            f"ADVERTENCIA: n={n} esta por debajo del minimo metodologico de "
            f"{MIN_PARTICIPANTES} participantes externos (ver "
            "instrucciones-formulario.md:34). Los resultados se reportan igual, "
            "pero no deben leerse como concluyentes."
        )
    print()

    print("=== Puntajes SUS individuales ===")
    for p, puntaje in zip(participantes, puntajes):
        print(f"{p['codigo']}: {puntaje:.2f}")
    print()

    print("=== Agregados ===")
    print(f"Media: {media:.2f}")
    print(f"Mediana: {mediana:.2f}")
    print(f"Desviacion tipica (muestral, n-1): {dt:.2f}")
    print(f"Minimo: {minimo:.2f}")
    print(f"Maximo: {maximo:.2f}")
    if n > 1:
        print(f"Grados de libertad: {df}")
        print(f"Valor critico t(0.975, df={df}): {t:.3f}")
        print(f"IC 95%: [{ic_inf:.2f}, {ic_sup:.2f}]")
    else:
        print("IC 95%: no calculable con n<=1")
    print()

    print(f"Interpretacion (escala de Bangor): {interpretacion_bangor(media)}")
    umbral_txt = "SUPERA" if media > UMBRAL_PROYECTO else "NO SUPERA"
    print(f"Umbral del proyecto (>{UMBRAL_PROYECTO}): {umbral_txt} ({media:.2f})")
    print()

    print("=== Media normalizada por item (contribucion 0-4, tras invertir pares) ===")
    for i, m in enumerate(medias_por_item, start=1):
        print(f"Q{i}: {m:.2f}")
    peor_idx = min(range(10), key=lambda i: medias_por_item[i])
    print(f"Item con peor puntaje promedio: Q{peor_idx + 1} ({medias_por_item[peor_idx]:.2f}/4)")

    return 0


if __name__ == "__main__":
    sys.exit(main())
