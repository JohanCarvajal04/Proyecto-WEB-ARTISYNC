#!/usr/bin/env python3
"""Resuelve cada DOI declarado en referencias.bib contra doi.org y contra Crossref/DataCite,
y deja evidencia verbatim para VERIFICACION.md (P11 de la guia del examen suspenso: "las 48
referencias con su DOI resuelto").

Es de solo lectura: no modifica referencias.bib, solo imprime.

Uso:
    python scripts/verificar-doi.py [ruta/a/referencias.bib]

Termina con codigo de salida 0 si todos los DOI resuelven, distinto de cero si alguno falla.
"""
import json
import re
import sys
import urllib.error
import urllib.request

RUTA_BIB = sys.argv[1] if len(sys.argv) > 1 else "docs/informe-final/referencias.bib"

NAVEGADOR_UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")


def extraer_dois(ruta):
    texto = open(ruta, encoding="utf-8").read()
    entradas = re.findall(r"@\w+\{([^,]+),((?:(?!\n@).)*)", texto, re.S)
    resultado = []
    for clave, cuerpo in entradas:
        m = re.search(r'doi\s*=\s*[{"]([^}"]+)[}"]', cuerpo, re.I)
        if m:
            resultado.append((clave, m.group(1)))
    return resultado


def resolver_doi(doi):
    """Sigue la redireccion real de doi.org (GET, UA de navegador): lo que importa es que
    doi.org RECONOZCA el DOI y redirija a una pagina real, no que el sitio final permita
    trafico automatizado (muchos editores academicos -- ACM, IEEE, MDPI -- devuelven 403 a
    peticiones sin navegador real aunque el DOI sea perfectamente valido)."""
    url = f"https://doi.org/{doi}"
    req = urllib.request.Request(url, headers={"User-Agent": NAVEGADOR_UA, "Accept": "text/html"})
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return resp.status, resp.geturl()
    except urllib.error.HTTPError as e:
        return e.code, str(e.geturl() if hasattr(e, "geturl") else "")
    except Exception as e:
        return None, str(e)


def datacite_meta(doi):
    """Respaldo para DOI del prefijo 10.48550 (arXiv) y otros registrados en DataCite en vez
    de Crossref -- Crossref devuelve 404 para estos aunque el DOI sea real."""
    url = f"https://api.datacite.org/dois/{doi}"
    req = urllib.request.Request(url, headers={"User-Agent": "artisync-verificacion-p11/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            data = json.loads(resp.read().decode("utf-8"))
        attrs = data["data"]["attributes"]
        titulo = (attrs.get("titles") or [{}])[0].get("title", "")
        publisher = attrs.get("publisher", "DataCite")
        return titulo, "", publisher
    except Exception as e:
        return None, None, str(e)


def crossref_meta(doi):
    """Fuente independiente de doi.org: confirma que el DOI declarado en referencias.bib
    corresponde a una obra real registrada en Crossref (o, si no esta ahi, en DataCite), con
    su titulo y venue."""
    url = f"https://api.crossref.org/works/{doi}"
    req = urllib.request.Request(
        url, headers={"User-Agent": "artisync-verificacion-p11/1.0 (mailto:carvajalstalin.10@gmail.com)"})
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            data = json.loads(resp.read().decode("utf-8"))
        msg = data["message"]
        titulo = (msg.get("title") or [""])[0]
        venue = (msg.get("container-title") or [""])[0]
        editor = msg.get("publisher", "")
        return titulo, venue, editor
    except Exception:
        return datacite_meta(doi)


def main():
    entradas = extraer_dois(RUTA_BIB)
    fallos = []
    for clave, doi in entradas:
        status, resolved_url = resolver_doi(doi)
        titulo, venue, editor = crossref_meta(doi)
        # Criterio real de validez: doi.org redirigio a ALGUNA URL real (no un DOI no
        # encontrado) Y Crossref/DataCite devuelve metadatos reales para ese DOI. El codigo
        # HTTP final del editor (bloqueo anti-bot, comun en ACM/IEEE/MDPI) no es el criterio.
        doi_reconocido = resolved_url and "doi.org" not in resolved_url.split("://", 1)[-1].split("/", 1)[0]
        ok = bool(doi_reconocido and titulo)
        marca = "OK   " if ok else "FALLA"
        print(f"{marca} {clave:15s} doi={doi:35s} http_final={status} -> {resolved_url}")
        print(f"      Metadatos: titulo={titulo!r} venue/publisher={venue or editor!r}")
        if not ok:
            fallos.append(clave)

    print()
    print(f"Total: {len(entradas)} DOI verificados, {len(fallos)} fallidos.")
    if fallos:
        print("FALLIDOS:", fallos)
        sys.exit(1)


if __name__ == "__main__":
    main()
