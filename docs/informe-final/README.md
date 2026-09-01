<!--
NOTA PARA EL EQUIPO (eliminar antes de la entrega final del PDF):
Esta versión (v1.1.0 del borrador, 17-08-2026) reorganiza el documento
académico final en fuente LaTeX (.tex) con bibliografía IEEE (.bib), a
partir del borrador Markdown previo. Fue producida con asistencia de
Claude Code a partir del contenido ya verificado del repositorio y de una
verificación bibliográfica activa contra fuente primaria (ver más abajo).
Campos marcados `[PENDIENTE — ...]` (macro `\pendiente{}` en el .tex)
requieren un dato que el equipo debe completar y que no puede inferirse
honestamente de lo que hay en el repositorio (ORCID, firma del docente,
DOI del dataset, fecha real de defensa, capturas de CI, etc.).
-->

# Documento académico final — fuente LaTeX (v1.1.0)

Fuente LaTeX completa del documento técnico académico exigido por el
Bloque B de la guía de la Entrega Final, con bibliografía en formato IEEE
numérico, **ya compilada a PDF** (`Informe-Final-v1.0.0.pdf`, 73
páginas). Reemplaza la versión Markdown anterior de este borrador: el
contenido sustantivo es el mismo (mismas 13 secciones, mismas brechas
declaradas), reorganizado a `.tex`/`.bib` y con la bibliografía verificada
y ampliada.

## Estructura

```
docs/informe-final/
├── main.tex              # Documento maestro (clase report, una columna)
├── referencias.bib       # Bibliografía IEEE (37 entradas, ver más abajo)
├── Informe-Final-v1.0.0.pdf  # PDF ya compilado (73 páginas)
├── secciones/
│   ├── 00-portada-resumen.tex
│   ├── 01-introduccion.tex
│   ├── 02-marco-teorico.tex
│   ├── 03-trabajos-relacionados.tex
│   ├── 04-ingenieria-requisitos.tex
│   ├── 05-materiales-metodos.tex
│   ├── 06-diseno-arquitectura.tex
│   ├── 07-implementacion.tex
│   ├── 08-evaluacion-resultados.tex
│   ├── 09-12-discusion-conclusiones.tex  # Capítulos 9, 10, 11 y 12
│   ├── 13-declaraciones.tex
│   └── anexos.tex        # Anexos A–J (\appendix en main.tex)
└── README.md              # este archivo
```

## Cómo compilar

**Ya compilado:** `Informe-Final-v1.0.0.pdf` en esta misma carpeta es la
salida real de compilar `main.tex` (73 páginas, 0 citas/referencias
indefinidas, 0 `Overfull \hbox` al cierre de esta versión — ver
`main.log` la próxima vez que se recompile). No es un PDF de ejemplo ni
un mock: se generó el 17-08-2026 con TeX Live 2026 dentro del contenedor
`texlive/texlive:latest` (imagen ya usada en este equipo para
`docs/latex_seguridad_bd/`), sin depender de una instalación local de
TeX Live/MiKTeX en el host de desarrollo.

**Para recompilar tras editar los `.tex`**, con Docker (recomendado, no
requiere instalar nada más — el proyecto ya usa Docker Compose para todo
lo demás):

```bash
docker run --rm -v "$(pwd)/..:/docs" -w /docs/informe-final texlive/texlive:latest \
  bash -c "pdflatex -interaction=nonstopmode main.tex && \
           bibtex main && \
           makeglossaries main && \
           pdflatex -interaction=nonstopmode main.tex && \
           pdflatex -interaction=nonstopmode main.tex"
```

(Se monta la carpeta `docs/` completa, no solo `informe-final/`, porque
las figuras usan rutas relativas `../diagramas/` y `../mediciones/sus/`
hacia el resto de `docs/`.) `make docs` desde la raíz del repo hace lo
mismo si `pdflatex`/`bibtex` están instalados en el host; si no lo están,
usar el comando Docker de arriba y luego copiar el resultado:
`cp docs/informe-final/main.pdf docs/informe-final/Informe-Final-v1.0.0.pdf`.

Sin Docker, requiere una distribución LaTeX (TeX Live o MiKTeX) con
`pdflatex`, `bibtex`, `makeglossaries` y los paquetes estándar (`babel`,
`booktabs`, `listings`, `hyperref`, `glossaries`, `titlesec`, `fancyhdr`,
`microtype`, `hyphenat`):

```bash
cd docs/informe-final
pdflatex main.tex
bibtex main
makeglossaries main
pdflatex main.tex
pdflatex main.tex   # segunda pasada para resolver referencias cruzadas y el glosario
```

O, si `latexmk` está disponible: `latexmk -pdf -bibtex main.tex`.

### ¿Por qué no `IEEEtran`?

La guía pide "formato de citas IEEE", que es un **estilo de citas**
(numérico, `\bibliographystyle{ieeetr}`), no necesariamente la **clase de
documento** `IEEEtran` (pensada para papers de conferencia/journal a dos
columnas). Este documento es un PFC/tesis de una columna con portada
institucional, por lo que se usó la clase `report` estándar con el estilo
de citas IEEE numérico. Si el docente-director exige explícitamente la
clase `IEEEtran`, es un cambio acotado al preámbulo de `main.tex` y a la
plantilla de portada (`00-portada-resumen.tex`); el contenido de los
capítulos y `referencias.bib` no cambiarían.

### `make docs`

El `Makefile` raíz del repositorio compilaba antes con `pandoc` sobre los
`.md` de esta carpeta. Esa carpeta ya no contiene Markdown: el objetivo
`docs` del `Makefile` fue actualizado para invocar `pdflatex`/`bibtex`
sobre `main.tex` (ver `Makefile`, objetivo `docs`).

## Estado de la bibliografía

El borrador anterior declaraba que solo 5 de las $\sim$30 referencias
citadas estaban "verificadas contra fuente primaria sin ambigüedad" y que
ninguna estaba clasificada explícitamente como "alto impacto". Para esta
versión se ejecutó una verificación activa (búsqueda web dirigida,
17-08-2026) de cada referencia marcada `[PENDIENTE: verificar cita]` en el
borrador, más una búsqueda dirigida de referencias adicionales
relevantes a los temas del proyecto (plataformas multilaterales, pagos en
garantía, inyección SQL, cobertura de pruebas, microservicios, validez
estadística en ingeniería de software).

**Resultado:**

- **37 referencias** en `referencias.bib` (frente a las $\sim$29 del
  borrador anterior), todas verificadas contra una fuente accesible
  (DOI, editorial, sitio oficial o repositorio institucional) — ninguna
  se dejó como cita "de memoria" sin contrastar.
- De esas 37, **5 son estándares/RFC** (ISO/IEC/IEEE 29148, ISO/IEC
  25010, RFC 7519, OWASP Top 10, OWASP Cheat Sheet), que se cuentan aparte
  por ser normativa técnica, no literatura académica.
- De las **32 restantes** (literatura académica/técnica citable hacia el
  mínimo de 30 que exige la guía), **16 se clasifican como "alto
  impacto"** (Scopus/JCR Q1–Q2, o venue ICSE/FSE/ASE/MSR/EASE/ESEM) contra
  el criterio explícito de la rúbrica — ver el detalle completo,
  referencia por referencia, en el Anexo J (`secciones/anexos.tex`).

**Por qué nos detuvimos en 16 (y no en 20).** Se evaluaron varios
candidatos adicionales de venues de alto impacto temáticamente cercanos al
proyecto (JWT/OAuth, accesibilidad web automatizada, CI/CD) y ninguno
produjo un resultado suficientemente inequívoco en la búsqueda como para
citarlo con la misma confianza que el resto de esta bibliografía —o el
venue exacto no correspondía a los listados en la rúbrica (p. ej. un
candidato de continuous integration resultó ser ICSME, no MSR). Se
prefirió declarar la brecha de 4 referencias de alto impacto en vez de
forzar una clasificación optimista o citar una fuente no verificada con
la confianza suficiente. **Esta sigue siendo una brecha real**: cerrarla
requiere la revisión de literatura reducida recomendada en el
Capítulo 3, §3.1 (`secciones/03-trabajos-relacionados.tex`) y en el
Anexo I, no solo más búsquedas puntuales como esta.

**Estilo de citas:** IEEE numérico, `\bibliographystyle{ieeetr}` (orden
de aparición en el texto, no alfabético). Decisión pendiente en el
borrador anterior, ya resuelta en esta versión conforme a lo pedido para
la entrega.

## Brechas heredadas que siguen abiertas

Convertir el documento a `.tex`/`.bib` con bibliografía verificada resolvió
la brecha de formato de citas y una parte sustancial de la brecha de
verificación bibliográfica, pero **no resuelve** las brechas que dependían
de información que solo el equipo tiene o de trabajo que no es de
redacción:

- ~~Registrar ORCID de cada integrante~~ — resuelto: los 4 ORCID reales ya
  están en `secciones/00-portada-resumen.tex` (18-08-2026).
- ~~DOI de Zenodo y tag `v1.0.0`~~ — resuelto: el tag `v1.0.0` existe en
  GitHub y Zenodo generó el DOI `10.5281/zenodo.21978572` para esa
  versión; ya declarado en portada y en el Capítulo 13.
- ~~Discrepancia del tag `v1.0.0` local~~ — resuelto (01-09-2026): el tag
  `v1.0.0` local apuntaba a un commit (`5b61f86`) distinto del tag
  `v1.0.0` en GitHub/Zenodo (`d07656b`), por haberse recreado localmente
  sin sincronizar. Se corrigió realineando el tag local con el remoto
  (`git tag -d v1.0.0 && git fetch origin tag v1.0.0`), sin tocar nada ya
  publicado en GitHub ni en Zenodo (el DOI es inmutable). Registrado en
  `docs/observaciones/OBSERVACIONES.md` (OBS-AUTO-12). El trabajo posterior
  a `d07656b` (refactor de permisos, endurecimiento de seguridad) se
  etiquetó por separado como `v1.1.0`, sin alterar el cierre de la Entrega
  Final.
- ~~Agradecimientos~~ — resuelto: sección completada en
  `secciones/13-declaraciones.tex` (18-08-2026).
- El DOI del dataset de mediciones (Zenodo, separado del software) sigue
  sin existir — confirmado contra la API de Zenodo el 18-08-2026, no solo
  heredado del borrador anterior. Sigue siendo Bloque D.3 pendiente.
- Completar la bibliografía hasta 30+ referencias con 20+ de alto impacto
  (hoy: 32 académicas, 16 de alto impacto — ver arriba), lo que depende de
  ejecutar la revisión de literatura reducida del Capítulo 3, §3.1.
- Verificar edición/DOI exactos de las referencias que en el Anexo J
  quedaron marcadas "No" en alto impacto pero cuya fuente primaria el
  equipo quiera reforzar de todas formas (p. ej. confirmar si `WULFERT2022`
  ya tiene factor de impacto JCR asignado a la fecha de la defensa).
- Resolver la nuance de conformidad de invocación de procedimientos
  (Capítulo 7, §7.4).
- Decidir el estilo de citas fue resuelto (IEEE numérico); falta que el
  equipo lo confirme con el docente-director si prefiere autor-año.
- Adjuntar capturas del pipeline CI (Anexo H) — la compilación a PDF en sí
  ya no es una brecha (ver `Informe-Final-v1.0.0.pdf` y la sección "Cómo
  compilar" arriba).
- Los `[PENDIENTE ...]` restantes en el propio texto (`\pendiente{...}`
  en cada `.tex`) son la lista completa y autoritativa; buscar
  `\pendiente{` en `secciones/` para ubicarlos todos. El PDF compilado los
  resalta en **morado y negrita** para que sean imposibles de pasar por
  alto en una revisión visual.
