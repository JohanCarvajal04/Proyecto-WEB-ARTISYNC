# Metadatos para el depósito Zenodo del dataset de mediciones

Este archivo no es parte del dataset en sí; es una guía de copiar/pegar para publicar
el depósito manual en Zenodo (Bloque D.3 de la guía de la Entrega Final). El depósito
del **software** ya existe (`10.5281/zenodo.21978572`, vía integración GitHub↔Zenodo);
este es un depósito **separado**, porque esa integración ata el DOI a todo el
repositorio por tag, no a un subdirectorio.

## Paquete a subir

`artisync-dataset-mediciones-v1.0.0.zip` — contenido de `docs/mediciones/` en el
commit `d07656b` (el mismo commit que archivó el software v1.0.0), generado con:

```bash
git archive --format=zip -o artisync-dataset-mediciones-v1.0.0.zip d07656b -- docs/mediciones
```

Incluye: `perf/` (k6, 3 corridas frías + 3 calientes), `sec/` (evidencia OWASP, ZAP,
análisis estático), `sus/` (CSV crudo N=16 + reporte), `lighthouse/` (mobile+desktop,
3 corridas cada uno), `jacoco/` (XML + HTML), y `DATA-DICTIONARY.md` /
`DATA-PROVENANCE.md` como documentación autocontenida del propio dataset.

## Pasos en zenodo.org

1. Iniciar sesión con la cuenta Zenodo del equipo (la misma que ya tiene el depósito
   del software, para mantener la autoría consistente).
2. **New upload → New deposit** (NO usar el flujo "GitHub" — ese ya está enlazado al
   repositorio completo para el software).
3. Subir `artisync-dataset-mediciones-v1.0.0.zip`.
4. Rellenar el formulario con los campos de abajo.
5. Publicar y copiar el DOI resultante.

## Campos del formulario

| Campo | Valor |
|---|---|
| Tipo de subida (Upload type) | **Dataset** |
| Título | Artisync — Dataset de mediciones empíricas (rendimiento, seguridad, usabilidad, cobertura, calidad web) v1.0.0 |
| Autores | Bone Arroyo, Niurca Scarleth (ORCID 0009-0002-2219-2800) · Carvajal Loor, Johan Stalin (ORCID 0009-0008-9229-382X) · Figueroa Morales, Bryan Javier (ORCID 0009-0009-6357-4996) · Rios Cuyabazo, Jhon Kevin (ORCID 0009-0003-7446-9450) — todos afiliados a Universidad Técnica Estatal de Quevedo |
| Descripción | Dataset de mediciones empíricas cuantitativas producidas durante la Entrega Final (v1.0.0) del PFC Artisync: rendimiento (k6, 50 VUs/30s, cache frío y caliente), seguridad (6 controles OWASP + escaneo ZAP baseline + análisis estático SpotBugs/find-sec-bugs), usabilidad (SUS, N=16), cobertura de pruebas (JaCoCo, líneas/ramas/complejidad) y calidad web (Lighthouse, perfiles mobile y desktop, 3 corridas cada uno). Cada variable está documentada en DATA-DICTIONARY.md con tipo, unidad, umbral y fuente; la procedencia de cada medición (script, commit) está en DATA-PROVENANCE.md. Complementa al software archivado en 10.5281/zenodo.21978572, siguiendo el principio de citación independiente de software y datos. |
| Licencia | **Creative Commons Attribution 4.0 International (CC BY 4.0)** — distinta de la licencia MIT del software |
| Versión | 1.0.0 |
| Idioma | Español (spa) |
| Palabras clave | web, artisync, dataset, mediciones empíricas, k6, OWASP, SUS, JaCoCo, Lighthouse, PFC |
| Identificadores relacionados | `10.5281/zenodo.21978572` — relación **"Is supplement to"** (el software) · `https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC` — relación **"Is supplement to"** |
| Tipo de acceso | Open Access |

## Después de publicar

Enviar el DOI resultante para que se actualicen (commit separado, `OBS-AUTO-13`):

- `README.md` (reemplazar la nota "Pendiente: depositar el dataset...")
- `docs/informe-final/secciones/00-portada-resumen.tex` y `13-declaraciones.tex`
- `docs/checklists/fair-checklist.md` (ítems Findable #1/#3, Accessible #3, Reusable #2)
- `docs/observaciones/OBSERVACIONES.md` (nueva fila `OBS-AUTO-13`)
- `docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md` (nota de actualización fechada)
- `docs/mediciones/DATA-DICTIONARY.md` / `DATA-PROVENANCE.md`
- Recompilar `docs/informe-final/Informe-Final-v1.0.0.pdf` si el toolchain LaTeX/Docker está disponible.
