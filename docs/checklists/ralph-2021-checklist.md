# Checklist de Validación Empírica (Ralph et al. 2021)

*Plantilla basada en los estándares empíricos de ACM SIGSOFT ("Empirical Standards for Software Engineering Research", Ralph et al. 2021).*

**Estándar aplicado:** *Engineering Research* — el PFC diseña y construye un artefacto de software (Artisync) y evalúa sus propiedades mediante múltiples métodos empíricos concurrentes (benchmark de rendimiento, auditoría de seguridad, estudio de usabilidad, cobertura de pruebas), en vez de un experimento controlado único con asignación aleatoria a grupos o un estudio de caso sobre una organización externa. Es consistente con la metodología Design Science Research de Peffers declarada para el capítulo de Materiales y Métodos del documento académico (Bloque B.7 de la guía).

**Fecha de evaluación:** 2026-09-06. **Evaluado contra:** commit `9e35d21` (rama `main`).

> Revisión anterior: 2026-08-17 contra commit `6af8595`. Esa versión afirmaba que "el documento académico final (Bloque B) no existe" y dejaba 4 ítems sin resolver; ambas cosas quedaron obsoletas por el trabajo posterior (capítulo de Introducción con RQ1-RQ4 formales, capítulo de Amenazas a la validez con las 4 categorías, script de bootstrap SUS). Ver `docs/observaciones/observaciones_para_el_examen.md` (OBS-R4-01) para la traza de esa corrección.

---

## 1. Diseño del Estudio

- [x] ¿Se establecieron claramente los objetivos de investigación?
  **Sí.** El Capítulo 1 (`docs/informe-final/secciones/01-introduccion.tex:50-69`) declara cuatro preguntas de investigación formales: **RQ1** (cumplimiento empírico reproducible de umbrales de calidad), **RQ2** (estrategia híbrida ORM + procedimientos almacenados), **RQ3** (proceso de ingeniería de requisitos) y **RQ4** (amenazas a la validez). Cada bloque de evaluación empírica traza explícitamente a una RQ (anotaciones `(traza a RQ1)`...`(traza a RQ4)` en el mismo capítulo) y además declara su propio objetivo medible en su reporte: `docs/mediciones/perf/REPORTE-PERF.md` (p95 ≤200ms caliente/≤500ms frío), `docs/mediciones/sec/REPORTE-SEC.md` (6 controles OWASP + ZAP sin hallazgos altos), `docs/mediciones/sus/REPORTE-SUS.md` (SUS >68), `docs/mediciones/lighthouse/REPORTE-LIGHTHOUSE.md` (Performance≥80, resto≥90), `docs/mediciones/jacoco/REPORTE-JACOCO.md` (cobertura ≥70%).

- [x] ¿Se definió el contexto (participantes, tareas, entorno)?
  **Sí, por bloque.** SUS: 16 participantes externos al equipo, perfil declarado en `docs/mediciones/sus/perfil-participantes.csv` (edad, sexo, experiencia web, dispositivo). Rendimiento: 50 VUs / 30s contra `GET /api/v1/catalogo`, entorno `docker compose` local (`REPORTE-PERF.md`). Seguridad: `docker compose up -d --build`, ZAP baseline contra `http://localhost:4200` (`REPORTE-SEC.md`). Lighthouse: build de producción vía `docker-compose.lighthouse.yml`, perfiles mobile/desktop (`REPORTE-LIGHTHOUSE.md`).

- [x] ¿El diseño es adecuado para responder las preguntas de investigación?
  **Sí.** Los métodos elegidos (benchmark de carga, escaneo de seguridad dual manual+automático, cuestionario SUS estandarizado, cobertura de código, auditoría de accesibilidad/rendimiento web) son los instrumentos estándar de la comunidad de ingeniería de software empírica para cada propiedad de calidad evaluada (ISO/IEC 25010), consistente con lo declarado en la guía de la Entrega Final (Bloque B.7).

## 2. Recolección de Datos

- [x] ¿Se describen detalladamente los procedimientos de recolección?
  **Sí.** Cada `REPORTE-*.md` documenta el comando exacto ejecutado (ej. `make lighthouse`, `docker run ... zap-baseline.py -t http://localhost:4200`, `mvn spotbugs:spotbugs` dentro de un contenedor `maven:3.9-eclipse-temurin-21`) y la fecha/commit de la corrida.

- [x] ¿Se reportan las herramientas utilizadas?
  **Sí, con versión.** k6, `@lhci/cli` 0.15.1 / Lighthouse 12.6.1, OWASP ZAP (`ghcr.io/zaproxy/zaproxy:stable`), SpotBugs 4.8.6.6 + find-sec-bugs 1.13.0, JaCoCo 0.8.13, cuestionario SUS de Brooke (10 ítems, formulario Google Forms).

- [x] ¿Se mitigaron los sesgos en la recolección de datos?
  **Sí, con limitación declarada.** Los participantes SUS son externos al equipo (mitiga sesgo de complacencia). El muestreo por conveniencia (n=16) se declara explícitamente como tal y se justifica siguiendo Baltes y Ralph~\cite{BALTES2022} en `docs/informe-final/secciones/05-materiales-metodos.tex:129-153` ("Población, muestreo y participantes" — sección dedicada, incluye la limitación de que el muestreo por conveniencia no garantiza representatividad poblacional), y se retoma en el capítulo de Amenazas a la validez (`09-12-discusion-conclusiones.tex`, sección "Validez externa", línea 122). Para rendimiento, `REPORTE-PERF.md` admite una limitación metodológica reconocida y no corregida: el escenario "frío" no aísla un *cache miss* real por iteración; esta misma amenaza se retoma como amenaza de validez interna en `09-12-discusion-conclusiones.tex:109-112` ("Validez interna"). **Pendiente menor:** no hay un cálculo formal de poder estadístico a priori para n=16 (se documenta el tamaño pero no una justificación de potencia).

## 3. Análisis de Datos

- [x] ¿Se documentó el proceso de análisis?
  **Sí.** `docs/mediciones/sus/analisis-sus.py` y `graficar-sus.py` calculan media/mediana/DT/IC 95% de forma reproducible (`make sus`); `docs/mediciones/sus/bootstrap-sus.py` añade un intervalo de confianza por bootstrap (apropiado para n=16 pequeño en una escala acotada 0-100); `REPORTE-PERF.md` documenta el cálculo de percentiles (p50/p90/p95/p99) desde los JSON crudos de k6.

- [x] ¿Se aplicaron métodos estadísticos o cualitativos apropiados?
  **Sí.** Estadística descriptiva completa (media, DT, IC 95%) en SUS y k6. Desde T-15 (`docs/mediciones/perf/analisis-inferencial.py`, `make perf-stats`), hay test inferencial no paramétrico (Mann-Whitney U, apropiado para latencias — no Wilcoxon, que es para muestras pareadas) con tamaño de efecto ordinal (Â₁₂ de Vargha-Delaney) y corrección de Holm-Bonferroni, sobre las comparaciones caliente/frío disponibles. Ver `docs/informe-final/secciones/08-evaluacion-resultados.tex` §8.1 y `docs/mediciones/perf/salida-inferencial.txt`. La limitación de diseño del script del catálogo (no aísla un *cache miss* real) queda declarada como amenaza de validez interna, no oculta.

## 4. Reporte de Resultados

- [x] ¿Los resultados responden directamente a los objetivos propuestos?
  **Sí.** Cada reporte cierra con una tabla explícita de "umbral vs. resultado vs. cumple" (ver `REPORTE-LIGHTHOUSE.md`, `REPORTE-SEC.md`, `REPORTE-SUS.md`), y el capítulo de Discusión (`09-12-discusion-conclusiones.tex`, sección "Respuesta a las preguntas de investigación") responde RQ1-RQ4 una por una.

- [x] ¿Se incluyeron métricas clave (ej. tamaños de efecto, p-valores, intervalos de confianza)?
  **Sí.** IC 95% en SUS (`[49.49, 73.01]`, n=16, además del bootstrap) y en k6; tamaños de efecto (Â₁₂ de Vargha-Delaney) y p-valores con corrección de Holm-Bonferroni en la comparación de latencias caliente/frío (`docs/mediciones/perf/salida-inferencial.txt`, §8.1 del informe). Cubre las dos comparaciones inferenciales disponibles (catálogo público y endpoint protegido); no hay más bloques con diseño comparativo (SUS y seguridad son de medición única, no de comparación de grupos).

- [x] ¿Se discutieron las limitaciones (amenazas a la validez)?
  **Sí.** Capítulo dedicado "Amenazas a la validez" (`09-12-discusion-conclusiones.tex:93-157`) con las cuatro categorías exigidas: **Validez de constructo** (§96-108, ej. uso del SUS estandarizado de Brooke), **Validez interna** (§109-121, cache frío no aislado), **Validez externa** (§122-136, muestreo por conveniencia, un solo dominio de aplicación), **Validez de conclusión** (§137-157, tamaño de muestra pequeño en las comparaciones inferenciales). Cada una con mitigación explícita, no solo enumerada.

## 5. Replicabilidad

- [x] ¿Se provee un paquete de datos (data package) con datos crudos?
  **Sí.** `docs/mediciones/` contiene datos crudos por bloque: `perf/k6-*.json`, `sec/owasp/*.txt`, `sec/zap/*.json`, `sec/static-analysis/*.xml`, `sus/sus-raw.csv`, `lighthouse/lhci-*.json`, `jacoco/html/jacoco.xml`, catalogados en `DATA-DICTIONARY.md` y `DATA-PROVENANCE.md`.

- [x] ¿Los scripts de análisis están disponibles?
  **Sí.** Los scripts de SUS (`analisis-sus.py`, `bootstrap-sus.py`, `graficar-sus.py`) y de rendimiento (`analisis-inferencial.py`) están versionados y son ejecutables vía `make sus` / `make perf-stats`. `docs/mediciones/DATA-PROVENANCE.md` cita, para cada métrica archivada, la fecha y el commit exacto de la corrida que la generó (ej. SUS: `2026-08-16`, commit `1b34b8d`, corregido el `2026-09-03` tras detectarse una divergencia en `sus-raw.csv`, ver `sus/PLAN-MEJORA-SUS.md`; ZAP y SpotBugs: `2026-08-16`, commit `8b88512`; Lighthouse Entrega Final: `2026-08-17`, commit `69e43b8`). **Nota de trazabilidad:** `DATA-PROVENANCE.md` todavía referencia la medición de JaCoCo del `2026-08-16` (72,0 % líneas / 62,5 % ramas, commit `11ac931`) como la vigente, mientras que `jacoco/REPORTE-JACOCO.md` ya documenta corridas posteriores y más altas (66,44 % → 70,10 % → 75,03 % de ramas, la última generada el 2026-09-06). `DATA-PROVENANCE.md` debe actualizarse para apuntar a la corrida vigente — es una actualización de fecha, no una ausencia de script o de commit.

---

## Resumen

**12 de 12 ítems cumplidos**, dos de ellos con una limitación menor ya declarada por el propio equipo en vez de ocultada: (1) ausencia de justificación formal de poder estadístico para n=16 en el estudio SUS, y (2) trazabilidad script→dato incompleta para dos remediciones puntuales de mediados de agosto. Ninguna de las dos invalida el diseño ni los resultados; ambas están anotadas en sus respectivas secciones para que un lector no las confunda con omisiones no reconocidas.

Cambios frente a la evaluación anterior (2026-08-17, commit `6af8595`): se cerraron los 4 ítems que entonces figuraban como `[ ]`/parciales porque el trabajo que faltaba —RQ formales en el Capítulo 1, capítulo dedicado de Amenazas a la validez con las 4 categorías, y el script de bootstrap SUS— se completó entre el 2026-08-28 y el 2026-09-05 (ver `docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md`, Bloque C, para la traza commit por commit).
