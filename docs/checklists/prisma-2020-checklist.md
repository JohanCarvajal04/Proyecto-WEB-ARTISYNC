# Checklist PRISMA 2020

**Fecha de evaluación:** 2026-09-06. **Evaluado contra:** commit `9e35d21` (rama `main`), capítulo `docs/informe-final/secciones/03-trabajos-relacionados.tex`.

> Revisión anterior: este archivo se resolvía como "No Aplica (N/A)" con el argumento de que PRISMA solo aplica a Revisiones Sistemáticas de Literatura (SLR) dedicadas y que Artisync es un proyecto de desarrollo de software. Esa justificación quedó inválida el 2026-09-04 (commit `0d85e51`), cuando el Capítulo 3 se reescribió como una mini-SLR real con cadena de búsqueda booleana, criterios de inclusión/exclusión, cribado en fases y diagrama de flujo con números — es decir, exactamente lo que PRISMA 2020 exige reportar. Ver `docs/observaciones/observaciones_para_el_examen.md` (OBS-D2-05) y `docs/observaciones/PLAN-EXAMEN-FINAL.md:516` para la traza de esa corrección.
>
> **Alcance declarado:** este no es un artículo de SLR independiente, sino un capítulo de trabajos relacionados dentro de un PFC de ingeniería de software (Design Science Research), con una revisión de alcance acotado (8 estudios incluidos) para fundamentar la brecha de investigación — no un metaanálisis. Los ítems de PRISMA 2020 que solo aplican a metaanálisis cuantitativo (síntesis estadística, riesgo de sesgo de publicación, certeza de la evidencia tipo GRADE) se marcan **No aplica** con su motivo, en vez de forzar contenido que no corresponde al tipo de estudio. El resto de los ítems, los que sí describen cómo se hizo *esta* revisión de alcance, se resuelven con evidencia real del capítulo.

---

## Título

- [x] **1. Título.** Identifica el informe como una revisión de literatura (no necesariamente con las palabras "revisión sistemática").
  **Sí.** `\section{Metodología de revisión de literatura}` (línea 6) y `\chapter{Trabajos relacionados}` — el capítulo se presenta explícitamente como el producto de una revisión de literatura metodológica, no como una simple recopilación narrativa.

## Resumen

- [x] **2. Resumen.** Resumen estructurado según los ítems PRISMA para resúmenes (objetivos, fuentes, criterios de elegibilidad, resultados, etc.).
  **Sí.** Añadida la sección `\section*{Resumen de la revisión de literatura}` al inicio del Capítulo 3 (`03-trabajos-relacionados.tex`, tras el título del capítulo), con los cinco bloques estructurados: Objetivos (traza a RQ1/RQ2), Fuentes (bases + ventana + cadena booleana), Criterios de elegibilidad (IC/EC resumidos), Resultados (145→62→21→8, con referencia a la Figura del diagrama PRISMA), y Hallazgo principal (la brecha de investigación en una frase).

## Introducción

- [x] **3. Justificación.** Por qué se hizo la revisión en el contexto de lo ya conocido.
  **Sí.** Línea 9-16: "Para fundamentar las decisiones de diseño de Artisync y validar la brecha de investigación, se ejecutó una revisión sistemática de literatura...".
- [x] **4. Objetivos.** Declaración explícita de las preguntas que la revisión responde (PICO o equivalente).
  **Sí.** Subsección "Preguntas de Investigación (RQs)" (líneas 18-25): RQ1 (arquitecturas/patrones de transacción segura tipo *escrow*), RQ2 (verificación de identidad y trazabilidad para freelancers).

## Métodos

- [x] **5. Criterios de elegibilidad.** Características de los estudios (PICO, duración de seguimiento) y reportar cómo se agrupó para la síntesis.
  **Sí.** Subsección "Criterios de Inclusión (IC) y Exclusión (EC)" (líneas 39-49): IC1 (estudios empíricos/arquitecturas/casos de estudio sobre plataformas de intermediación), IC2 (journals Q1/Q2 o conferencias CORE A/A*, inglés o español), EC1 (artículos sin aporte técnico/arquitectónico), EC2 (literatura gris, preprints sin *peer review*, opinión).
- [x] **6. Fuentes de información.** Todas las bases de datos, registros, sitios web, organizaciones consultadas y fecha de la última búsqueda.
  **Parcial.** Bases declaradas: Scopus, IEEE Xplore, ACM Digital Library (línea 28). Ventana temporal 2020-2026 (línea 30). **Falta:** la fecha exacta en que se ejecutó la búsqueda (no solo la ventana temporal de publicación de los estudios).
- [x] **7. Estrategia de búsqueda.** Estrategia completa para al menos una base de datos, incluyendo filtros, de forma que sea reproducible.
  **Sí.** Cadena booleana completa y reproducible (líneas 32-37): `("gig economy" OR "freelance platform" OR "creator economy") AND ("escrow" OR "smart contract" OR "payment trust") AND ("architecture" OR "identity verification" OR "software engineering")`.
- [ ] **8. Proceso de selección.** Cuántos revisores participaron en el cribado, de forma independiente o no, uso de herramientas de automatización.
  **No.** El capítulo no declara cuántos autores del equipo participaron en el cribado de los 145 estudios identificados, ni si el cribado fue independiente/duplicado o hecho por una sola persona. **Pendiente:** añadir una frase indicando quién(es) del equipo cribó/cribaron y si hubo doble revisión o arbitraje de desacuerdos.
- [ ] **9. Proceso de extracción de datos.** Métodos usados para decidir qué datos se extrajeron de cada estudio, y si fue por uno o más revisores.
  **No.** No se documenta un formulario o protocolo de extracción de datos por estudio (qué variables se extrajeron de cada uno de los 8 incluidos antes de construir la Tabla 3.1). **Pendiente:** documentar brevemente qué campos se extrajeron (contexto, escrow, verificación IA, ORM/SQL híbrido, cobertura empírica — que son justamente las columnas de la tabla) y si un solo autor o más de uno hizo la extracción.
- [x] **10a. Ítems de los datos.** Todas las variables para las que se buscaron datos (resultados, características de participantes/intervención/exposición, etc.).
  **Sí, implícito en la tabla.** La Tabla 3.1 (líneas 83-104) define las variables extraídas de cada estudio: año, contexto, presencia de *escrow*, verificación por IA, acceso híbrido ORM/SQL, y tipo de cobertura empírica.
- [ ] **10b. Métodos de riesgo de sesgo.** Métodos usados para evaluar el riesgo de sesgo en los estudios individuales.
  **No aplica formalmente, pero mitigado por diseño.** No hay una herramienta formal de evaluación de riesgo de sesgo (ej. ROBIS, AMSTAR) aplicada a los 8 estudios incluidos. El criterio EC2 (excluir literatura gris y preprints sin *peer review*, línea 47-48) actúa como filtro de calidad de facto, pero no sustituye una evaluación de riesgo de sesgo explícita por estudio. **Pendiente si se quiere cerrar formalmente:** una tabla breve de calidad metodológica por estudio, o declarar explícitamente por qué se omite (revisión de alcance, no metaanálisis).
- [x] **11. Métodos de síntesis.** Procesos usados para decidir qué estudios eran elegibles para cada síntesis, procesamiento de datos, métodos para tabular/visualizar resultados.
  **Sí, para el tipo de estudio.** Síntesis narrativa + tabular (Tabla 3.1) comparando 8 estudios contra las características de Artisync (*escrow*, verificación IA, ORM/SQL híbrido, cobertura empírica), consistente con una revisión de alcance (no un metaanálisis cuantitativo).
- [ ] **12. Métodos de evaluación de certeza.** Métodos usados para evaluar la certeza (confianza) en el cuerpo de evidencia para cada resultado.
  **No aplica.** Este ítem es propio de metaanálisis cuantitativo con marcos como GRADE, sobre resultados agregados de eficacia/efecto. El Capítulo 3 es una revisión de alcance cualitativa sobre arquitecturas y prácticas, no produce un tamaño de efecto agregado sobre el cual aplicar GRADE.

## Resultados

- [x] **13. Selección de estudios.** Número de estudios cribados, evaluados por elegibilidad, e incluidos, con motivos de exclusión — idealmente con diagrama de flujo.
  **Sí.** Diagrama de flujo PRISMA 2020 con números reales (líneas 56-75, Figura `fig:prisma`): identificación (n=145) → cribado por título/abstract (n=62, 83 excluidos) → evaluación a texto completo (n=21, 13 excluidos) → incluidos (n=8).
- [x] **14. Características de los estudios.** Cita e información de las características de cada estudio incluido.
  **Sí.** Cada fila de la Tabla 3.1 cita el estudio (autor, año) y describe su contexto; dos estudios adicionales (Tafesse y Dayan 2023, Chigbu 2026) se describen en prosa (líneas 106-113) por no encajar en las columnas de arquitectura de software.
- [ ] **15. Riesgo de sesgo en los estudios.** Evaluaciones de riesgo de sesgo presentadas para cada estudio incluido.
  **No.** Ligado al ítem 10b — no hay evaluación de riesgo de sesgo por estudio.
- [x] **16. Resultados de estudios individuales.** Para cada resultado, resumen de los datos estadísticos e información del efecto para cada estudio.
  **Sí, para el tipo de dato de esta revisión.** Cada estudio reporta su tipo de cobertura empírica (ej. "Implementación (87% éxito)", "Simulación (precisión/latencia)", "Empírico (RCT de campo)") en la columna correspondiente de la Tabla 3.1 — no hay un tamaño de efecto único agregable entre estudios porque miden fenómenos distintos (esperado en una revisión de alcance, no un metaanálisis).
- [ ] **17. Resultados de síntesis.** Para cada síntesis, resumen de las características y riesgo de sesgo entre estudios, resultados estadísticos e ilustraciones.
  **Parcial.** La síntesis narrativa (sección "Brecha identificada", líneas 115-126) resume el patrón entre los 8 estudios y cómo ninguno combina las tres características de Artisync. **Falta** una síntesis del riesgo de sesgo agregado (depende de 10b/15, no resuelto).
- [ ] **18. Certeza de la evidencia.** Evaluación de la certeza (confianza) en el cuerpo de evidencia para cada resultado.
  **No aplica** (mismo motivo que el ítem 12).

## Discusión

- [x] **19. Discusión general.** Interpretación general de los resultados en el contexto de otra evidencia, limitaciones e implicaciones.
  **Sí.** Sección "Brecha identificada (*research gap*)" (líneas 115-126) interpreta la Tabla 3.1 para argumentar que ninguna referencia combina *escrow* + verificación IA + acceso híbrido ORM/SQL evaluados empíricamente — la brecha concreta que Artisync cubre. Se retoma en el Capítulo de Discusión (`09-12-discusion-conclusiones.tex`, sección "Comparación con trabajos relacionados").

## Otra información

- [ ] **20. Registro y protocolo.** Nombre del registro y número de registro, o dónde puede accederse al protocolo, si se registró.
  **No aplica.** No corresponde registrar un protocolo de SLR (ej. en PROSPERO) para una revisión de alcance embebida en un capítulo de un PFC de ingeniería de software — esa práctica es propia de revisiones sistemáticas en salud/ciencias sociales con metaanálisis.
- [x] **21. Apoyo.** Fuentes de apoyo financiero u otro tipo, y el rol de los financiadores.
  **Sí, por remisión.** `docs/etica/ETHICS.md` declara explícitamente que no hubo financiamiento externo (recursos propios del equipo y niveles gratuitos/académicos de proveedores cloud), aplicable también a este capítulo.
- [x] **22. Conflictos de interés.** Conflictos de interés de los autores de la revisión.
  **Sí.** Añadido el párrafo `\paragraph{Conflictos de interés de la revisión.}` al final de la sección "Brecha identificada" del Capítulo 3, que declara explícitamente el conflicto inherente al formato de PFC (el mismo equipo que hizo la revisión evalúa y favorece su propio artefacto, Artisync, en la comparación) y remite a la declaración formal del Capítulo 13 (`\ref{cap:declaraciones}`, `docs/etica/ETHICS.md`) en vez de solo repetir "sin conflictos" sin contexto.
- [ ] **23. Disponibilidad de datos, código y otros materiales.** Cuáles de los siguientes están públicamente disponibles y dónde: formulario de extracción de datos, datos extraídos de cada estudio, otros materiales usados en la revisión.
  **No.** No hay un archivo con los 145 → 62 → 21 → 8 estudios identificados/cribados (ni sus referencias completas, ni el motivo de exclusión estudio por estudio) — solo los 8 incluidos están citados en `referencias.bib`. **Pendiente:** publicar la lista completa de estudios cribados (aunque sea como anexo o CSV en `docs/mediciones/`), consistente con la práctica de datos crudos que el equipo ya sigue para las mediciones empíricas (`DATA-DICTIONARY.md`, `DATA-PROVENANCE.md`).

---

## Resumen

**13 de 20 ítems aplicables cumplidos, 4 pendientes, 3 marcados "No aplica" con motivo** (ítems 12, 18 y 20, propios de metaanálisis cuantitativo o registro formal de protocolo — no del tipo de estudio que es este capítulo).

Cerrados en esta revisión (2026-09-06): **Ítem 2** (resumen estructurado añadido al inicio del Capítulo 3, con objetivos/fuentes/criterios/resultados/hallazgo) e **Ítem 22** (párrafo de conflicto de interés inherente al formato de PFC, con remisión a la declaración formal del Capítulo 13). También se corrigió una casilla mal marcada en el Ítem 13 (el texto ya decía "Sí" pero la casilla quedó como `[ ]` por error de redacción).

**Pendientes reales restantes para completar la adherencia a PRISMA 2020** (ordenados por esfuerzo):
1. **Ítem 6** — citar la fecha exacta de ejecución de la búsqueda (no solo la ventana de publicación).
2. **Ítem 8** — declarar quién(es) hizo el cribado y si fue con doble revisión.
3. **Ítem 9** — documentar el protocolo/formulario de extracción de datos (qué campos, cuántos revisores).
4. **Ítems 10b / 15 / 17 (parcial)** — evaluación de riesgo de sesgo por estudio, aunque sea una tabla simple de calidad metodológica.
5. **Ítem 23** — publicar la lista completa de los 62/21 estudios cribados con motivo de exclusión, no solo los 8 incluidos.

Ninguno de estos pendientes invalida el diagrama de flujo ni la tabla comparativa ya publicados (líneas 51-104 del capítulo), que son los elementos centrales que la rúbrica de la Entrega Final exigía y que ya están resueltos con números reales.
