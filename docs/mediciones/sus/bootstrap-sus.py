#!/usr/bin/env python3
"""Intervalo de confianza por bootstrap percentil para la media SUS (ARTISYNC).

Verificacion independiente del IC parametrico (t de Student) que ya calcula
analisis-sus.py. Util porque n=16 es pequeno y una escala acotada 0-100 no
siempre sigue una distribucion normal, que es el supuesto detras del IC con
t de Student.

Reutiliza cargar_respuestas() y puntaje_sus() de analisis-sus.py: los 16
puntajes individuales son exactamente los mismos que reporta ese script, no
una segunda implementacion de la formula de Brooke que pueda divergir.

Uso:
    python3 docs/mediciones/sus/bootstrap-sus.py > docs/mediciones/sus/salida-bootstrap-sus.txt

Solo biblioteca estandar (incluye random.seed fijo para reproducibilidad
exacta: cualquiera que ejecute este script sobre el mismo sus-raw.csv debe
obtener el mismo intervalo, no solo uno "parecido").
"""

from __future__ import annotations

import importlib.util
import random
import statistics
import sys
from pathlib import Path


def _cargar_analisis_sus():
    """Carga analisis-sus.py como modulo (el guion en el nombre de archivo
    impide un `import` normal), para reusar exactamente su cargar_respuestas()
    y puntaje_sus() en vez de reimplementar la formula de Brooke aparte."""
    ruta = Path(__file__).resolve().parent / "analisis-sus.py"
    spec = importlib.util.spec_from_file_location("analisis_sus", ruta)
    modulo = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(modulo)
    return modulo


_analisis_sus = _cargar_analisis_sus()
cargar_respuestas = _analisis_sus.cargar_respuestas
puntaje_sus = _analisis_sus.puntaje_sus

N_REMUESTREOS = 10_000
SEMILLA = 20260904  # fecha de esta corrida (AAAAMMDD), documentada para reproducibilidad


def bootstrap_percentil(puntajes: list[float], n_remuestreos: int, semilla: int) -> tuple[float, float]:
    rng = random.Random(semilla)
    n = len(puntajes)
    medias_remuestreadas = []
    for _ in range(n_remuestreos):
        muestra = [puntajes[rng.randrange(n)] for _ in range(n)]
        medias_remuestreadas.append(statistics.mean(muestra))
    medias_remuestreadas.sort()
    idx_inf = int(0.025 * n_remuestreos)
    idx_sup = int(0.975 * n_remuestreos) - 1
    return medias_remuestreadas[idx_inf], medias_remuestreadas[idx_sup]


def main() -> int:
    ruta_csv = Path(__file__).resolve().parent / "sus-raw.csv"
    participantes = cargar_respuestas(ruta_csv)
    puntajes = [puntaje_sus(p["respuestas"]) for p in participantes]

    media = statistics.mean(puntajes)
    ic_inf, ic_sup = bootstrap_percentil(puntajes, N_REMUESTREOS, SEMILLA)

    print("=== Bootstrap percentil sobre la media SUS (verificacion independiente del IC parametrico) ===")
    print(f"Archivo de entrada: {ruta_csv.name}")
    print(f"n participantes: {len(puntajes)}")
    print(f"Remuestreos: {N_REMUESTREOS}")
    print(f"Semilla: {SEMILLA} (fija, para reproducibilidad exacta)")
    print()
    print(f"Media observada: {media:.2f}")
    print(f"IC 95% bootstrap percentil: [{ic_inf:.2f}, {ic_sup:.2f}]")
    print()
    print("Comparar contra el IC parametrico (t de Student) de salida-sus.txt.")
    print("Si ambos intervalos son consistentes (se superponen ampliamente), eso")
    print("respalda que el supuesto de normalidad del IC parametrico no esta")
    print("distorsionando demasiado el resultado pese al tamano de muestra pequeno.")

    return 0


if __name__ == "__main__":
    sys.exit(main())
