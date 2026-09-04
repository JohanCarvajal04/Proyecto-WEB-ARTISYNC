# Informe de situación — Estudio de usabilidad (SUS) de ARTISYNC

> Este es un **informe de situación**:

> El propósito de este documento es informar con transparencia el estado real del estudio de
> usabilidad —incluida la brecha que tiene— y el plan del equipo para completarlo, no pedir una
> firma retroactiva que simule una aprobación que nunca existió. **No completar ninguna fecha con
> un valor anterior al real**: fabricar una fecha retroactiva para simular que esto se emitió
> antes de la recogida de datos sería exactamente el tipo de falta que la guía de desarrollo
> describe como irreparable si se descubre después del examen.

## 1. Situación actual

El equipo ARTISYNC recolectó datos de usabilidad (System Usability Scale) de 16 participantes
externos el **2026-08-16**, sin haber tramitado antes una aprobación formal de la actividad. La
guía de desarrollo de la Entrega Final exige, como requisito de piso (capítulo 5, §5.2, punto 1):

> _"Aprobación previa. Un documento del comité o de la instancia académica que corresponda, con
> fecha anterior a la recogida de datos, número o código de expediente, y el título del estudio.
> Emitido antes, no después."_

Esa condición —fecha anterior— ya no se puede cumplir de forma genuina: los datos ya se
recolectaron. El equipo informa esto con transparencia, en vez de omitirlo, y declara además la
limitación temporal en el capítulo de amenazas a la validez del documento académico (ver
`docs/mediciones/sus/PLAN-MEJORA-SUS.md`, Fase 3.1).

Este mismo estudio tuvo, además, un hallazgo de integridad de datos corregido el 2026-09-03: cinco
de las dieciséis respuestas del archivo analizado no coincidían con el export real del formulario.
Se corrigió y el resultado real es **61.25/100 (Bangor D)**, por debajo del umbral de aceptación
del proyecto (68) — ver `docs/mediciones/sus/REPORTE-SUS.md` para el detalle completo. Este
informe de situación reporta ese resultado tal cual, sin ajustarlo.

## 2. Ficha del estudio ya realizado

| Campo                                    | Valor                                                                                                                                                                                                                                                                             |
| ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Título del estudio                       | Evaluación de usabilidad percibida del sistema ARTISYNC mediante System Usability Scale (SUS)                                                                                                                                                                                     |
| Equipo responsable                       | ARTISYNC — Proyecto de Fin de Carrera, UTEQ-FCI, Ingeniería de Software                                                                                                                                                                                                           |
| Integrantes                              | Figueroa Morales Bryan Javier, Carvajal Loor Johan Stalin, Ríos Cuyabazo Jhon Kevinç                                                                                                                                                                                              |
| Instrumento                              | System Usability Scale (Brooke, 1996), 10 ítems, escala 1–5, traducción española (ver limitación metodológica declarada en `docs/mediciones/sus/REPORTE-SUS.md`)                                                                                                                  |
| Participantes                            | 16 personas externas al equipo de desarrollo, reclutadas por conveniencia                                                                                                                                                                                                         |
| Fecha de recogida de datos               | 2026-08-16                                                                                                                                                                                                                                                                        |
| Resultado                                | Media 61.25/100, mediana 66.25, DT 22.08, IC 95% [49.49, 73.01], Bangor D — **no supera** el umbral del proyecto (68)                                                                                                                                                             |
| Procedimiento de consentimiento aplicado | Consentimiento informado individual, firmado antes de cada sesión, según plantilla `docs/etica/consentimientos/plantilla.md` — evidencia verificable en `docs/mediciones/sus/REPORTE-SUS.md`, sección "Referencias de consentimiento" (16 hashes SHA-256 verificados, 2026-09-04) |
| Riesgos para los participantes           | Mínimos: uso de una aplicación web de prueba durante 15–20 minutos, sin recolección de datos identificables ni grabación de audio/video                                                                                                                                           |

## 3. Plan del equipo: rondas adicionales de evaluación SUS

El resultado actual (61.25, por debajo del umbral) queda **publicado tal cual, sin modificar**:
sustituirlo o descartarlo sería repetir el mismo problema de integridad de datos que ya se corrigió
en este estudio. Lo que el equipo propone en su lugar es **medir de nuevo, más adelante, sobre una
versión mejorada del sistema**, y reportar ambos resultados —el de referencia (2026-08-16) y el
posterior— de forma explícita y comparada, con su fecha y su commit, igual que ya se hizo en
`docs/mediciones/lighthouse/PLAN-MEJORA-LIGHTHOUSE.md` para el bloque de rendimiento web.

**Por qué hay margen razonable de mejora:** el ítem peor puntuado del SUS actual es Q10
("Necesité aprender muchas cosas antes de poder empezar a usar este sistema", 2.06/4), seguido de
Q6 y Q8 — un patrón que apunta a carga de aprendizaje inicial y consistencia percibida, no a una
falla puntual de una función. Son exactamente el tipo de problema que se puede atacar con cambios
concretos de UX (onboarding, mensajes de ayuda contextual, reducir pasos de las tareas guiadas)
antes de volver a medir.

**Condiciones para que la ronda adicional cuente como evidencia válida, no como un segundo intento
hasta que salga un número mejor:**

1. Debe aplicarse **después** de un cambio real y documentado en el sistema (no repetir la prueba
   sobre el mismo build sin ninguna modificación).
2. Debe seguir el mismo protocolo íntegro del capítulo 5 de la guía: aprobación previa **esta vez
   sí tramitada con fecha anterior a la recolección** (ya que se sabe con antelación cuándo se va
   a hacer), consentimiento informado individual, n≥15, mismo instrumento.
3. Se reporta el resultado **exista lo que exista** — si la segunda ronda también queda bajo el
   umbral, se publica igual, con la misma honestidad que el primer resultado.
4. Los datos crudos de ambas rondas se conservan por separado y versionados
   (`docs/mediciones/sus/sus-raw.csv` para la referencia; un archivo con fecha propia para cada
   ronda posterior), nunca se sobrescribe la evidencia anterior.

---

\*Este documento se conserva versionado en el repositorio (a diferencia de los consentimientos
individuales, no contiene datos personales de los participantes).
