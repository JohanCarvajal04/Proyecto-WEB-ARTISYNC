# Checklist INCOSE

*International Council on Systems Engineering — INCOSE Guide to Writing Requirements v4. Nueve características de calidad para requisitos individuales (C1–C9) y seis para el conjunto de requisitos (C10–C15), según exige el Bloque A.3.1 de la guía de la Entrega Final.*

**Alcance:** los 37 requisitos de `docs/requisitos/SRS.md` (23 funcionales REQ-F-001..023, 14 no funcionales REQ-NF-001..014), contrastados contra `docs/trazabilidad/matriz.csv` y `docs/requisitos/CHANGELOG-REQ.md`.

**Fecha de evaluación:** 2026-08-17. **Nota de alcance:** esta es una evaluación muestral con ejemplos concretos citados, no una revisión exhaustiva de los 37 requisitos uno por uno — para la Entrega Final se recomienda completar la revisión ítem por ítem y anexarla al documento académico (Bloque A.3.1 exige "la lista de verificación INCOSE completa" como anexo).

---

## 1. Características individuales de los requisitos (C1–C9)

- [x] **C1 — Necessary (Necesario):** ¿define una función esencial?
  **Sí, en general.** Ejemplo: REQ-F-001 (registro con selección de rol Creador/Cliente) es la base del modelo de negocio de dos lados declarado en §2.1 del SRS — su ausencia invalidaría el propósito del sistema. No se identificaron requisitos claramente superfluos en la muestra revisada.

- [x] **C2 — Appropriate (Apropiado):** ¿el nivel de abstracción es el adecuado (ni una decisión de diseño, ni una meta vaga)?
  **Sí, mayormente.** REQ-F-011 ("precio ≥0.01 USD, al menos una imagen ≤10MB, descripción 20–2000 caracteres") tiene el nivel de detalle correcto: restricciones de negocio verificables, sin prescribir la implementación. Contraejemplo parcial: REQ-F-003 nombra explícitamente "JWT" — mezcla una decisión arquitectónica (justificada aparte en ADR-002) dentro del enunciado del requisito. Aceptable dado que la elección ya está fijada por ADR, pero técnicamente reduce la independencia del requisito respecto a la solución.

- [ ] **C3 — Unambiguous (No ambiguo):** ¿tiene una única interpretación clara?
  **Parcial.** La mayoría usa criterios numéricos verificables (REQ-F-003: "expiración de 24 horas"; REQ-NF-004: "LCP ≤2s"). **Contraejemplo real:** REQ-F-023 dice "selección aleatoria automática de ganadores" sin especificar el algoritmo de aleatoriedad, el manejo de empates, ni si un mismo usuario puede ganar más de un premio en el mismo sorteo — deja espacio a interpretación en la implementación (verificado: `fn_seleccionar_ganadores_sorteo.sql` sí toma una decisión concreta, pero el requisito en sí no la fija).

- [x] **C4 — Complete (Completo, a nivel de requisito individual):** ¿incluye toda la información necesaria para entenderlo sin buscar en otro lugar?
  **Sí.** Cada requisito en `SRS.md` sigue una plantilla consistente: Rationale, Prioridad MoSCoW, Criterio de aceptación, Verificación, Estado — no depende de contexto implícito no documentado.

- [ ] **C5 — Singular (Singular):** ¿describe una única capacidad, sin agrupar varias con "y/o"?
  **No, en al menos dos casos identificados.** REQ-NF-013 agrupa dos capacidades distintas en un solo enunciado: "Auditoría inmutable de transiciones de pedido y transacciones" (una restricción de integridad de datos) **y** "exportación CSV por el administrador" (una funcionalidad de reporting) — son verificables por separado y deberían ser dos requisitos. Caso similar en REQ-F-006, que combina la verificación de mayoría de edad **y** la eliminación obligatoria del documento tras la respuesta (dos obligaciones distintas: una de negocio, una de privacidad/retención de datos).

- [x] **C6 — Feasible (Factible):** ¿se puede implementar con la tecnología y los recursos del proyecto?
  **Sí, demostrado empíricamente.** De los 37 requisitos, 33 (89.2%) están en estado `implementado` o `verificado` en `matriz.csv` a la fecha de esta evaluación — la factibilidad ya está probada por la implementación real, no solo argumentada.

- [x] **C7 — Verifiable (Verificable):** ¿existe una forma medible u objetiva de comprobar que se cumple?
  **Sí, sistemáticamente.** Los 37 requisitos tienen un campo "Verificación" explícito en `SRS.md` (Test/análisis/demostración) y una columna `prueba_automatizada` en `matriz.csv`. Es la característica mejor cubierta del conjunto.

- [x] **C8 — Correct (Correcto):** ¿el enunciado refleja fielmente la necesidad real, sin errores?
  **Cumplida desde el 21-08-2026 (antes parcial).** La revisión anterior encontró que `SRS.md` y `matriz.csv` declaraban estados distintos para REQ-F-006 y REQ-F-007, sin poder determinar cuál era correcto. La reconciliación posterior confirmó que la divergencia era mucho mayor de lo detectado: **23 de los 37 requisitos** discrepaban, y en ambos sentidos. Se resolvió requisito por requisito, no en bloque:
  - Se declaró `matriz.csv` fuente de verdad del estado (es la única que exige prueba y evidencia en la misma fila) y `SRS.md` fuente del enunciado, la prioridad y el criterio de aceptación.
  - Se corrigieron **dos estados inflados**: REQ-F-009 figuraba como `verificado` sin tener servicio ni endpoint —se implementó la rodaja vertical y hoy el estado es cierto—, y REQ-F-010 se bajó a `pendiente`, que es su estado real.
  - Se eliminó el valor `parcial`, ajeno al enum de A.3.3.
  - Se corrigieron notas obsoletas que afirmaban carencias ya resueltas (p. ej. REQ-NF-003 decía que faltaban los claims `iss`/`aud`/`jti`, y REQ-NF-014 que faltaba la prueba de firma inválida; ambas existen).

  La corrección no depende de la disciplina del equipo: `scripts/validate-traceability.sh` falla si los dos documentos vuelven a divergir.

- [ ] **C9 — Conforming (Conforme):** ¿sigue el patrón de redacción estándar `[condición] [sujeto] shall [acción] [objeto] [restricción]` de ISO/IEC/IEEE 29148:2018?
  **No, de forma consistente.** Los requisitos en `SRS.md` están redactados en prosa descriptiva en español ("El sistema debe permitir...", "Control de acceso basado en roles..."), no en el patrón sintáctico formal de condición-sujeto-shall-acción-objeto-restricción que exige explícitamente el Bloque A.3.1/B.6bis de la guía y las 42 reglas de INCOSE v4 para redacción de enunciados. Esto es consistente entre los 37 requisitos (no es un error puntual, es el estilo de redacción elegido) — requiere reescritura sistemática para alcanzar conformidad total, no solo ajustes puntuales.

## 2. Características del conjunto de requisitos (C10–C15)

- [ ] **C10 — Complete (Completo, a nivel de conjunto):** ¿cubre todas las necesidades conocidas de los stakeholders?
  **Parcial, y honestamente autodeclarado.** El propio `SRS.md` §6 admite la brecha: REQ-F-009, 010 (funciones sociales — seguidores, comentarios) siguen `pendiente` en `matriz.csv` a la fecha de esta evaluación. Nota positiva: REQ-F-014, 015, 016 (chat, filtro de mensajes, briefing) y REQ-F-023 (sorteos), que el SRS marcaba como dependientes de módulos no implementados en julio, ya aparecen `verificado` en la matriz actual — el conjunto se completó parcialmente entre la Tercera Entrega y hoy, pero no en su totalidad.

- [x] **C11 — Consistent (Consistente):** ¿no hay contradicciones entre dos o más requisitos?
  **Sí, a nivel de contenido semántico.** No se encontraron requisitos que se contradigan entre sí en su enunciado (ej. ningún par de requisitos exige comportamientos incompatibles). La única inconsistencia detectada es documental entre SRS.md y matriz.csv (ver C8), no una contradicción requisito-contra-requisito.

- [x] **C12 — Feasible (Factible, a nivel de conjunto):** ¿el conjunto completo es implementable con los recursos del proyecto?
  **Sí, demostrado.** El equipo ya implementó el 89.2% del conjunto (33/37) a través de cuatro entregas (1A→1B→3→Final) con recursos de un proyecto académico de 17 semanas — evidencia empírica directa de factibilidad del conjunto, no solo estimación.

- [x] **C13 — Comprehensible (Comprensible):** ¿el conjunto está organizado de forma que un tercero pueda entenderlo?
  **Sí.** `SRS.md` agrupa los requisitos por módulo funcional (Seguridad, Perfiles, Catálogo, Comunicación, Legal/Entregables, Social) con una plantilla uniforme por requisito y una sección de evolución (§6) que explica los cambios desde la Entrega 1A — estructura clara y navegable.

- [x] **C14 — Able to be validated (Factible de validar):** ¿se puede confirmar que el conjunto satisface la necesidad original?
  **Sí, vía trazabilidad.** `docs/trazabilidad/matriz.csv` conecta cada requisito a historia de usuario → caso de uso → módulo de código → endpoint → prueba automatizada → evidencia empírica, permitiendo validar el conjunto contra el sistema entregado de forma sistemática (aunque, como señala `INFORME-BRECHAS-ENTREGA-FINAL.md`, la columna `tipo_acceso` no se actualizó tras la integración de los procedimientos almacenados de hoy — una brecha de mantenimiento de la matriz, no de su diseño).

- [x] **C15 — Correct (Correcto, a nivel de conjunto):** ¿el conjunto refleja fielmente el estado real del sistema, sin errores agregados?
  **Cumplida desde el 21-08-2026 (antes parcial).** Resuelta junto con C8: el conjunto ya es una fuente de verdad única, con la regla de precedencia declarada explícitamente en la cabecera del SRS y comprobada de forma automática en cada ejecución del CI. Los tres requisitos que no alcanzan el estado que su prioridad exige (REQ-NF-001, REQ-NF-009, REQ-F-010) no se ocultan: figuran en `docs/trazabilidad/excepciones-estado.txt` con su motivo y su condición de cierre, de modo que el conjunto declara su propia deuda en vez de aparentar completitud.

---

## Métricas de calidad de requisitos (exigidas por A.3.1 y B.6bis de la guía)

Calculadas desde `docs/trazabilidad/matriz.csv` (37 filas) el 2026-08-21. La versión anterior de esta tabla, fechada el 17-08-2026, declaraba cifras que no se correspondían con la matriz (Must 29 / Should 7 frente a los 26 / 10 reales, y 67.6 % verificado frente al 75.7 %); se recalculó desde el archivo, no desde la versión previa de la tabla.

| Métrica | Valor |
|---|---|
| Número total de requisitos | 37 (23 funcionales, 14 no funcionales) |
| Distribución por tipo | Funcionales 62.2 % (23/37) · No funcionales 37.8 % (14/37) |
| Distribución por prioridad MoSCoW | Must: 26 (70.3 %) · Should: 10 (27.0 %) · Could: 1 (2.7 %) · Won't: 0 |
| Distribución por estrategia de acceso | CRUD-ORM: 24 · SP: 8 · sin acceso a datos: 5 |
| % verificado (estado=`verificado`) | 75.7 % (28/37) |
| % implementado o verificado (no pendiente) | 91.9 % (34/37) |
| % pendiente | 8.1 % (3/37 — REQ-F-010, y REQ-NF-001/REQ-NF-009 en `implementado` por dependencia externa) |
| Must verificados | 92.3 % (24/26); los 2 restantes con excepción declarada |
| Requisitos con prueba automatizada | 81.1 % (30/37); ninguno figura como `verificado` sin prueba |
| Cambios registrados en `CHANGELOG-REQ.md` | 4 entradas (v0.3.0, v0.9.0-rc, v1.0.0 del 20-08 y la reconciliación del 21-08, esta última con detalle por requisito) |
| Tasa de estabilidad (1 − modificados/totales) | **100 % (1 − 0/37)**: el corpus mantiene los mismos 37 requisitos desde la Entrega 1A, sin altas, bajas ni cambios de enunciado o alcance. La salvedad que anotaba la versión anterior de esta tabla —cambios de estado sin registrar en el changelog— quedó resuelta: los cambios de estado se registran ahora requisito por requisito y, además, no cuentan como modificaciones de la especificación. Se mantiene, eso sí, la cautela metodológica: una estabilidad del 100 % es esperable en un proyecto académico de alcance cerrado y sin cliente externo que renegocie requisitos, y no debe leerse como mérito de la especificación. |

---

## Resumen

**11 de 15 características cumplidas** (C1, C2, C4, C6, C7, C8, C11, C12, C13, C14, C15), **2 parciales con evidencia concreta** (C3, C10) y **2 no cumplidas** (C5, C9).

Respecto a la revisión del 17-08-2026, C8 y C15 pasan de parciales a cumplidas: la inconsistencia entre `SRS.md` y `matriz.csv` está resuelta —y resultó afectar a 23 requisitos, no solo a REQ-F-006/007— con una regla de precedencia declarada y comprobada automáticamente en el CI.

Quedan dos hallazgos accionables, ambos de redacción y no de coherencia:

1. **C5 (Singular):** desdoblar REQ-NF-013 y revisar REQ-F-006, que agrupan varias capacidades en un solo enunciado.
2. **C9 (Conforming):** si el equipo busca conformidad estricta con INCOSE v4, reescribir los 37 enunciados al patrón `[condición] [sujeto] shall [acción] [objeto] [restricción]`. Es el esfuerzo mayor y el único que exige reescribir el SRS completo en lugar de corregir casos puntuales; se declara abiertamente como no cumplido en vez de darlo por bueno.
