<!--
NOTA PARA EL EQUIPO (eliminar antes de la entrega final del PDF):
Este borrador fue redactado con asistencia de Claude Code a partir del contenido ya
verificado del repositorio (ver docs/etica/ai-disclosure.md). Campos marcados
`[PENDIENTE — ...]` requieren un dato que el equipo debe completar y que no puede
inferirse honestamente de lo que hay en el repositorio (ORCID, firma del docente,
DOI del dataset, fecha real de defensa, etc.).
-->

# UNIVERSIDAD TÉCNICA ESTATAL DE QUEVEDO

**Facultad de Ciencias de la Computación y Diseño Digital**
**Carrera de Ingeniería de Software**

---

## ARTISYNC: Plataforma web de intermediación entre creadores de contenido digital y clientes con estrategia híbrida de acceso a datos, verificación asistida por IA y evaluación empírica multidimensional

**Proyecto de Fin de Curso (PFC) — Aplicaciones Web, Quinto Nivel**
**Período Académico Presencial 2026-2027**

### Integrantes

| Nombre | Afiliación | ORCID |
|---|---|---|
| Bone Arroyo, Niurca Scarleth | UTEQ, FCI, Ingeniería de Software | https://orcid.org/0009-0006-1387-5019 |
| Carvajal Loor, Johan Stalin | UTEQ, FCI, Ingeniería de Software | https://orcid.org/0009-0008-9229-382X |
| Figueroa Morales, Bryan Javier | UTEQ, FCI, Ingeniería de Software | https://orcid.org/0009-0009-6357-4996 |
| Rios Cuyabazo, Jhon Kevin | UTEQ, FCI, Ingeniería de Software | https://orcid.org/0009-0003-7446-9450 |

**Docente-director:** Dr. Gleiston Cicerón Guerrero Ulloa, Ph.D. — gguerrero@uteq.edu.ec

**Fecha de cierre:** 17 de agosto de 2026 (semana 17)
**Etiqueta del artefacto:** `v1.0.0` — commit `d07656b` (verificado 01-09-2026: el tag local ya coincide con el de GitHub/Zenodo; ver `docs/informe-final/secciones/13-declaraciones.tex` §1 y `docs/observaciones/OBSERVACIONES.md`, OBS-AUTO-12)
**DOI del software (Zenodo):** `10.5281/zenodo.21978572` (archivo de `v1.0.0`; la versión anterior, `v0.9.0-rc`, quedó archivada aparte en `10.5281/zenodo.21730559`)
**DOI del dataset de mediciones (Zenodo):** `[PENDIENTE — depósito separado, ver Bloque D.3 de la guía; brecha declarada en docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md]`

---

## Resumen (español)

**Contexto y problema.** La economía creativa digital carece de plataformas que centralicen, en
un único entorno auditable, la contratación, la comunicación y el pago entre creadores de
contenido y clientes, obligando a ambas partes a fragmentar el proceso entre redes sociales,
mensajería externa y pasarelas de pago desconectadas — lo que eleva el riesgo de fraude, de
incumplimiento contractual y de pérdida de trazabilidad financiera. **Objetivo.** Diseñar,
implementar y evaluar empíricamente Artisync, una plataforma web que integra en un solo sistema
el ciclo completo de intermediación (perfil verificado, catálogo, mensajería moderada, contrato
con firma electrónica, pago con patrón *escrow* y funciones sociales), aplicando una estrategia
híbrida de acceso a datos (ORM para operaciones CRUD elementales, procedimientos almacenados
PL/pgSQL para operaciones multi-tabla) y verificación de identidad asistida por inteligencia
artificial. **Métodos.** Se siguió la metodología Design Science Research de Peffers en seis
actividades, con ingeniería de requisitos conforme a ISO/IEC/IEEE 29148:2018 y al marco de Pohl,
y evaluación empírica multidimensional: prueba de carga (k6, 50 VUs/30s), auditoría de seguridad
(6 controles OWASP + escaneo automatizado ZAP baseline + análisis estático SpotBugs/find-sec-bugs),
usabilidad (System Usability Scale, N=16), cobertura de código (JaCoCo) y calidad web (Lighthouse,
perfiles mobile y desktop). **Resultados principales.** El sistema alcanza p95 de 50.17 ms bajo
carga (umbral 200 ms), 0 % de errores HTTP ≥500, puntaje SUS de 61.25/100 (categoría D de Bangor,
IC 95 % [49.49, 73.01], por debajo del umbral de aceptabilidad del proyecto), cobertura JaCoCo de
72.0 % líneas / 62.5 % ramas, Lighthouse Performance
100/100 en desktop y 80–81/100 en mobile con Accessibility/Best Practices/SEO ≥93 en ambos
perfiles, los 6 controles OWASP mínimos evidenciados sin hallazgos altos en ZAP baseline
(0 FAIL-NEW), y 7 procedimientos almacenados conectados end-to-end al código en ejecución vía
JPA 2.1 (`@Query(nativeQuery=true)` parametrizado), sin concatenación de SQL dinámico detectada.
**Conclusiones.** La estrategia híbrida de acceso a datos y la verificación asistida por IA son
técnicamente viables dentro de las restricciones de un proyecto académico de 17 semanas, con
evidencia empírica reproducible; quedan como
brechas honestamente declaradas la usabilidad medida (por debajo del umbral de aceptabilidad), la
conexión completa de los procedimientos heredados de la
Tercera Entrega, la ejecución de tests inferenciales sobre las comparaciones de rendimiento, y el
despliegue en un ambiente de producción con dominio público.

**Palabras clave:** ingeniería de requisitos; procedimientos almacenados; verificación de
identidad; arquitectura de microservicios; usabilidad de software; seguridad web

---

## Abstract (English)

**Context and problem.** The digital creative economy lacks platforms that centralize, within a
single auditable environment, the contracting, communication, and payment process between content
creators and clients, forcing both parties to fragment the workflow across social media, external
messaging, and disconnected payment gateways — raising the risk of fraud, contractual
non-compliance, and loss of financial traceability. **Objective.** To design, implement, and
empirically evaluate Artisync, a web platform that integrates the full intermediation cycle
(verified profile, catalog, moderated messaging, electronically signed contract, escrow-pattern
payment, and social features) into a single system, applying a hybrid data-access strategy (ORM
for elementary CRUD operations, PL/pgSQL stored procedures for multi-table operations) and
AI-assisted identity verification. **Methods.** The study followed Peffers' Design Science
Research methodology across six activities, with requirements engineering conforming to
ISO/IEC/IEEE 29148:2018 and Pohl's framework, and multidimensional empirical evaluation: load
testing (k6, 50 VUs/30s), security auditing (6 OWASP controls plus automated ZAP baseline scanning
and SpotBugs/find-sec-bugs static analysis), usability (System Usability Scale, N=16), code
coverage (JaCoCo), and web quality (Lighthouse, mobile and desktop profiles). **Main results.**
The system achieves a p95 of 50.17 ms under load (200 ms threshold), 0% of HTTP ≥500 errors, a SUS
score of 61.25/100 (Bangor grade D, 95% CI [49.49, 73.01], below the project's acceptability
threshold), JaCoCo coverage of 72.0% lines / 62.5%
branches, Lighthouse Performance of 100/100 on desktop and 80–81/100 on mobile with
Accessibility/Best Practices/SEO ≥93 on both profiles, all 6 minimum OWASP controls evidenced with
no high findings in the ZAP baseline scan (0 FAIL-NEW), and 7 stored procedures connected
end-to-end to the running code via JPA 2.1 (parameterized `@Query(nativeQuery=true)`), with no
dynamic SQL concatenation detected. **Conclusions.** The hybrid data-access strategy and
AI-assisted verification are technically viable within the constraints of a 17-week academic
project, backed by reproducible empirical evidence; honestly
declared gaps remain in the measured usability (below the acceptability threshold), fully
connecting the stored procedures inherited from the third
deliverable, running inferential tests on the performance comparisons, and deploying to a
production environment with a public domain.

**Keywords:** requirements engineering; stored procedures; identity verification; microservices
architecture; software usability; web security

---

## Índice general

`[PENDIENTE — generar automáticamente al compilar con pandoc --toc, ver Makefile: make docs]`

## Índice de figuras

`[PENDIENTE — se genera al incorporar las figuras de docs/mediciones/ y docs/diagramas/ al capítulo 8]`

## Índice de tablas

`[PENDIENTE — se genera al compilar; cada capítulo de este borrador ya numera sus tablas]`

## Índice de listados de código

`[PENDIENTE — ver Capítulo 7, Implementación: 3 a 6 listados con caption y etiqueta]`

## Lista de siglas y acrónimos

| Sigla | Expansión |
|---|---|
| ADR | Architecture Decision Record |
| API | Application Programming Interface |
| CI | Continuous Integration |
| CRUD | Create, Read, Update, Delete |
| DSR | Design Science Research |
| GQM | Goal-Question-Metric |
| IC | Intervalo de Confianza |
| IMRaD | Introduction, Methods, Results and Discussion |
| JPA | Jakarta Persistence API |
| JWT | JSON Web Token |
| MoSCoW | Must, Should, Could, Won't |
| ORM | Object-Relational Mapping |
| OWASP | Open Web Application Security Project |
| PFC | Proyecto de Fin de Curso |
| PITR | Point-In-Time Recovery |
| RBAC | Role-Based Access Control |
| RPO | Recovery Point Objective |
| RTO | Recovery Time Objective |
| SRS | Software Requirements Specification |
| SP | Stored Procedure (Procedimiento Almacenado) |
| SUS | System Usability Scale |
| TOTP | Time-based One-Time Password |
| WAL | Write-Ahead Log |
