#!/usr/bin/env python3
"""Recalcula el SHA-256 de los 16 consentimientos escaneados de la prueba SUS y los contrasta
contra la tabla "Referencias de consentimiento" de docs/mediciones/sus/REPORTE-SUS.md
(P14 de la guia del examen suspenso: consentimientos informados del SUS).

Es de solo lectura: no modifica REPORTE-SUS.md ni los PDF, solo imprime.

Uso (en el equipo que tiene el escaneo, con G: montada):
    python scripts/verificar-consentimientos-sus.py G:\\EPSCAN

Si los PDF estan en otra ruta o con otro nombre, pasarla como argumento:
    python scripts/verificar-consentimientos-sus.py /ruta/a/la/carpeta

Mapeo asumido (documentado en REPORTE-SUS.md): p0.PDF -> P01, p1.PDF -> P02, ..., p15.PDF -> P16
(orden numerico del archivo -> orden del codigo de participante). Si el equipo escaneo o
nombro los archivos en otro orden, este script lo va a marcar como MISMATCH -- no asumir
que el codigo esta mal, comprobar primero el orden real de escaneo.

Termina con codigo de salida 0 si los 16 hashes coinciden exactamente con REPORTE-SUS.md,
distinto de cero si falta algun archivo o si algun hash no coincide.
"""
import hashlib
import re
import sys
from pathlib import Path

RUTA_REPORTE = "docs/mediciones/sus/REPORTE-SUS.md"

# Mapeo declarado en REPORTE-SUS.md: pN.PDF (0-indexado) -> P(N+1) (1-indexado, 2 digitos)
CODIGOS = [f"P{n:02d}" for n in range(1, 17)]  # P01..P16
ARCHIVOS = [f"p{n}.PDF" for n in range(0, 16)]  # p0.PDF..p15.PDF


def extraer_hashes_declarados(ruta_reporte):
    texto = Path(ruta_reporte).read_text(encoding="utf-8")
    filas = re.findall(r"\|\s*(P\d\d)\s*\|\s*[\d-]+\s*\|\s*`sha256:([0-9a-f]{64})`\s*\|", texto)
    return {codigo: h for codigo, h in filas}


def sha256_archivo(ruta):
    h = hashlib.sha256()
    with open(ruta, "rb") as f:
        for bloque in iter(lambda: f.read(1 << 20), b""):
            h.update(bloque)
    return h.hexdigest()


def main():
    carpeta = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(r"G:\EPSCAN")
    declarados = extraer_hashes_declarados(RUTA_REPORTE)

    if len(declarados) != 16:
        print(f"ERROR: se esperaban 16 filas con hash en {RUTA_REPORTE}, se encontraron {len(declarados)}.")
        sys.exit(2)

    print(f"Carpeta de origen: {carpeta}")
    print(f"Referencia: {RUTA_REPORTE}\n")

    ok = True
    faltantes = []
    for codigo, archivo in zip(CODIGOS, ARCHIVOS):
        ruta = carpeta / archivo
        if not ruta.exists():
            print(f"FALTA  {codigo}  <-  {archivo}  (archivo no encontrado en {carpeta})")
            faltantes.append(archivo)
            ok = False
            continue
        calculado = sha256_archivo(ruta)
        esperado = declarados.get(codigo)
        if calculado == esperado:
            print(f"OK     {codigo}  <-  {archivo}  sha256:{calculado}")
        else:
            print(f"MISMATCH {codigo}  <-  {archivo}")
            print(f"         esperado (REPORTE-SUS.md): sha256:{esperado}")
            print(f"         calculado ahora:           sha256:{calculado}")
            ok = False

    hashes_calculados = []
    for archivo in ARCHIVOS:
        ruta = carpeta / archivo
        if ruta.exists():
            hashes_calculados.append(sha256_archivo(ruta))
    duplicados = len(hashes_calculados) - len(set(hashes_calculados))

    print()
    if faltantes:
        print(f"Archivos faltantes: {len(faltantes)} de 16 ({', '.join(faltantes)})")
    if duplicados:
        print(f"ADVERTENCIA: {duplicados} hash(es) calculados se repiten entre si -- mismo "
              f"problema de escaner/impresora que el equipo ya detecto y corrigio una vez "
              f"(ver REPORTE-SUS.md, nota 'Primer intento con hallazgo real').")

    if ok and not duplicados:
        print("Total: 16/16 hashes coinciden exactamente con REPORTE-SUS.md. 0 fallidos.")
        sys.exit(0)
    else:
        print("Total: verificacion NO cerrada. Revisar los archivos marcados arriba antes de "
              "declarar P14 como comprobado.")
        sys.exit(1)


if __name__ == "__main__":
    main()
