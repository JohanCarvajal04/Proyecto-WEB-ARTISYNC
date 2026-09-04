#!/usr/bin/env python3
"""Recalcula desde los NDJSON crudos de k6: descriptivos, Mann-Whitney U,
A12 de Vargha-Delaney y d de Cohen, para las dos comparaciones caliente/frio
disponibles en docs/mediciones/perf/ (T-15, docs/observaciones/PLAN-EXAMEN-FINAL.md):

  - Catalogo   (publico, permitAll):  k6-run{1,2,3}.json      vs k6-cold-run{1,2,3}.json
  - Comisiones (protegido, JWT):      k6-auth-run{1..5}.json  vs k6-auth-cold-run{1..5}.json

La guia (Bloque C, S4.3) exige un test no parametrico para datos de latencia
(Mann-Whitney U) y un tamano de efecto ordinal (A12 de Vargha-Delaney), citando
a Arcuri y Briand. El Welch t-test y la d de Cohen tambien se calculan e
imprimen como contraste (el docente ya los habia calculado para el catalogo:
t=14.538, d=0.3065) pero NO son la conclusion metodologica correcta para este
tipo de dato -- se reportan solo para que quede explicito que se entendio el
matiz, no que se ignoro el numero ya dado.

No requiere edicion manual: basta con tener los NDJSON en docs/mediciones/perf/
y correr `python3 docs/mediciones/perf/analisis-inferencial.py` (o `make perf-stats`).
"""
import json
import math
from pathlib import Path

from scipy.stats import mannwhitneyu, ttest_ind

PERF_DIR = Path(__file__).resolve().parent

COMPARACIONES = [
    {
        "nombre": "Catalogo (publico) -- caliente vs frio",
        "url_contiene": "/api/v1/catalogo",
        "archivos_caliente": [PERF_DIR / f"k6-run{i}.json" for i in (1, 2, 3)],
        "archivos_frio": [PERF_DIR / f"k6-cold-run{i}.json" for i in (1, 2, 3)],
    },
    {
        "nombre": "Comisiones /api/v1/admin/reportes/finanzas (protegido) -- caliente vs frio",
        "url_contiene": "/reportes/finanzas",
        "archivos_caliente": [PERF_DIR / f"k6-auth-run{i}.json" for i in (1, 2, 3, 4, 5)],
        "archivos_frio": [PERF_DIR / f"k6-auth-cold-run{i}.json" for i in (1, 2, 3, 4, 5)],
    },
]


def cargar_http_req_duration(archivos, url_contiene):
    """Devuelve (valores_ms, statuses) de los puntos http_req_duration cuya
    tag `url` contiene `url_contiene` -- filtra las llamadas de setup()
    (login, resolucion de idPerfil) que no son parte de la carga medida."""
    valores = []
    statuses = []
    faltantes = [a for a in archivos if not a.exists()]
    if faltantes:
        nombres = ", ".join(a.name for a in faltantes)
        raise FileNotFoundError(
            f"faltan archivos NDJSON: {nombres} (correr `make bench` / `make bench-auth` "
            "/ `make bench-auth-cold` primero)"
        )
    for archivo in archivos:
        with archivo.open(encoding="utf-8") as f:
            for linea in f:
                linea = linea.strip()
                if not linea:
                    continue
                obj = json.loads(linea)
                if obj.get("type") != "Point" or obj.get("metric") != "http_req_duration":
                    continue
                data = obj["data"]
                tags = data.get("tags", {})
                if url_contiene not in tags.get("url", ""):
                    continue
                valores.append(data["value"])
                statuses.append(int(tags.get("status", "0")))
    return valores, statuses


def percentil(valores_ordenados, p):
    if not valores_ordenados:
        return float("nan")
    k = (len(valores_ordenados) - 1) * (p / 100)
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return valores_ordenados[int(k)]
    return valores_ordenados[f] + (valores_ordenados[c] - valores_ordenados[f]) * (k - f)


def descriptivos(valores, statuses):
    n = len(valores)
    ordenados = sorted(valores)
    media = sum(valores) / n
    mediana = percentil(ordenados, 50)
    varianza = sum((v - media) ** 2 for v in valores) / (n - 1)
    desv = math.sqrt(varianza)
    errores = sum(1 for s in statuses if s >= 500)
    return {
        "n": n,
        "media_ms": media,
        "mediana_ms": mediana,
        "desv_ms": desv,
        "p90_ms": percentil(ordenados, 90),
        "p95_ms": percentil(ordenados, 95),
        "p99_ms": percentil(ordenados, 99),
        "tasa_error_5xx": errores / n if n else float("nan"),
    }


def cohen_d(x, y):
    nx, ny = len(x), len(y)
    mx, my = sum(x) / nx, sum(y) / ny
    vx = sum((v - mx) ** 2 for v in x) / (nx - 1)
    vy = sum((v - my) ** 2 for v in y) / (ny - 1)
    sp = math.sqrt(((nx - 1) * vx + (ny - 1) * vy) / (nx + ny - 2))
    return (mx - my) / sp


def vargha_delaney_a12(x, y):
    """A12: P(valor aleatorio de x > valor aleatorio de y) + 0.5*P(empate).
    Se deriva del estadistico U de Mann-Whitney (x como muestra 1):
    A12 = U1 / (n1 * n2). Interpretacion (Vargha & Delaney 2000): ~0.5 sin
    efecto; <0.29 o >0.71 efecto grande; 0.36-0.29/0.64-0.71 mediano;
    0.44-0.36/0.56-0.64 pequeno."""
    n1, n2 = len(x), len(y)
    u1, _ = mannwhitneyu(x, y, alternative="two-sided")
    return u1 / (n1 * n2)


def interpretar_a12(a12):
    d = abs(a12 - 0.5)
    if d < 0.06:
        return "efecto insignificante"
    if d < 0.14:
        return "efecto pequeno"
    if d < 0.21:
        return "efecto mediano"
    return "efecto grande"


def holm_bonferroni(p_valores):
    """Devuelve los p-valores ajustados (mismo orden de entrada)."""
    indices = sorted(range(len(p_valores)), key=lambda i: p_valores[i])
    m = len(p_valores)
    ajustados = [None] * m
    max_visto = 0.0
    for rank, i in enumerate(indices):
        ajustado = min((m - rank) * p_valores[i], 1.0)
        max_visto = max(max_visto, ajustado)
        ajustados[i] = max_visto
    return ajustados


def formatear_descriptivos(etiqueta, d):
    print(f"  {etiqueta}:")
    print(f"    n peticiones     : {d['n']}")
    print(f"    media (ms)       : {d['media_ms']:.4f}")
    print(f"    mediana (ms)     : {d['mediana_ms']:.4f}")
    print(f"    desv. estandar   : {d['desv_ms']:.4f}")
    print(f"    p90 / p95 / p99  : {d['p90_ms']:.4f} / {d['p95_ms']:.4f} / {d['p99_ms']:.4f}")
    print(f"    tasa error >=500 : {d['tasa_error_5xx'] * 100:.4f}%")


def main():
    resultados = []
    for comp in COMPARACIONES:
        caliente, statuses_c = cargar_http_req_duration(comp["archivos_caliente"], comp["url_contiene"])
        frio, statuses_f = cargar_http_req_duration(comp["archivos_frio"], comp["url_contiene"])

        t_stat, t_p = ttest_ind(caliente, frio, equal_var=False)
        d = cohen_d(caliente, frio)
        u_stat, u_p = mannwhitneyu(caliente, frio, alternative="two-sided")
        a12 = vargha_delaney_a12(caliente, frio)

        resultados.append({
            "comparacion": comp,
            "desc_caliente": descriptivos(caliente, statuses_c),
            "desc_frio": descriptivos(frio, statuses_f),
            "welch_t": t_stat,
            "welch_p": t_p,
            "cohen_d": d,
            "mw_u": u_stat,
            "mw_p": u_p,
            "a12": a12,
        })

    p_valores_mw = [r["mw_p"] for r in resultados]
    p_ajustados = holm_bonferroni(p_valores_mw)

    for r, p_adj in zip(resultados, p_ajustados):
        comp = r["comparacion"]
        print("=" * 78)
        print(comp["nombre"])
        print("=" * 78)
        formatear_descriptivos("Caliente", r["desc_caliente"])
        formatear_descriptivos("Frio", r["desc_frio"])
        print()
        print("  -- Contraste parametrico (no es el test correcto para latencias, se" )
        print("     reporta solo como contraste con el valor ya calculado) --")
        print(f"    Welch t-test     : t = {r['welch_t']:.3f}, p = {r['welch_p']:.6e}")
        print(f"    d de Cohen       : {r['cohen_d']:.4f}")
        print()
        print("  -- Test correcto para datos de latencia (no parametrico) --")
        print(f"    Mann-Whitney U   : U = {r['mw_u']:.1f}, p = {r['mw_p']:.6e}")
        print(f"    p ajustado (Holm-Bonferroni, {len(resultados)} comparaciones): {p_adj:.6e}")
        print(f"    A12 Vargha-Delaney: {r['a12']:.4f} ({interpretar_a12(r['a12'])})")
        print()


if __name__ == "__main__":
    main()
