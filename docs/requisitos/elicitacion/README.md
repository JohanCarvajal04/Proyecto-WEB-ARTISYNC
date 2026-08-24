# Evidencia de elicitación de requisitos

Esta carpeta documenta las técnicas de elicitación que produjeron el corpus de requisitos de Artisync, según exige el apartado B.6bis de la guía de la Entrega Final.

**Advertencia de honestidad metodológica.** Este archivo declara lo que el equipo hizo realmente, incluida la evidencia que **no** conservó. No se han reconstruido a posteriori notas de sesión, guiones de entrevista ni actas: fabricar esa evidencia después del hecho invalidaría precisamente lo que la trazabilidad pretende demostrar. Donde falta el material, se dice que falta.

## Técnicas aplicadas

| Técnica                          | ¿Se aplicó? | Evidencia conservada                                                                                     |
| -------------------------------- | ----------- | -------------------------------------------------------------------------------------------------------- |
| Análisis de documentos           | Sí          | `Entrega 1A.docx` (raíz del repositorio), corpus original de la semana del 4 de junio de 2026. Es la fuente de la que derivan los 37 requisitos actuales, referenciada en `../SRS.md` §1.4. |
| Prototipado de baja fidelidad    | Sí          | Seis wireframes en [`../../diagramas/wireframes/`](../../diagramas/wireframes/), usados para acordar el alcance de las pantallas principales antes de implementarlas. |
| Discusión interna del equipo     | Sí          | **Sin evidencia granular conservada.** No se levantaron actas ni notas de sesión.                          |
| Entrevistas semi-estructuradas   | No          | —                                                                                                          |
| Observación de usuarios          | No          | —                                                                                                          |
| Cuestionarios de elicitación     | No          | —                                                                                                          |
| Workshops con stakeholders       | No          | —                                                                                                          |

Sobre el prototipado: cinco de los seis wireframes corresponden al dominio de Artisync (catálogo, escrow y entrega, perfil de creador, pipeline de pedido, sala de operaciones). El sexto, `Pantalla_principal.jpg`, es una plantilla genérica de gestión de agencia que no corresponde al dominio; está registrado como observación abierta **OBS-05** en [`../../observaciones/OBSERVACIONES.md`](../../observaciones/OBSERVACIONES.md) y no debe leerse como evidencia de elicitación válida.

## Limitación y su efecto sobre la validez

El corpus se elicitó **sin usuarios finales externos**: no hubo entrevistas ni observación de Creadores o Clientes reales. Los requisitos provienen del análisis del dominio hecho por el propio equipo y de la comparación con plataformas existentes del sector.

Esto tiene dos consecuencias que conviene no disimular:

1. **Sesgo de constructo.** Los requisitos reflejan la comprensión que el equipo tiene del dominio, no necesariamente las necesidades declaradas por usuarios reales. La validación posterior con 16 participantes en el estudio SUS mide usabilidad del sistema construido, no la pertinencia de los requisitos que lo originaron.
2. **Estabilidad artificialmente alta.** La tasa de estabilidad de requisitos del 100 % (`../SRS.md` §7.4) es coherente con un corpus que nunca se renegoció con un cliente externo. En un proyecto con stakeholders reales, esa cifra sería sospechosa.

Ambas limitaciones se declaran en el capítulo de amenazas a la validez del documento académico, y son la razón por la que este proyecto se reporta como *engineering research* de alcance académico y no como un estudio de caso con stakeholders reales.

## Stakeholders identificados

La identificación de interesados sí se documentó formalmente, con la técnica de *stakeholder onion* de Alexander: ver el capítulo de ingeniería de requisitos del documento académico (`../../informe-final/secciones/04-ingenieria-requisitos.tex`, sección "Contexto y stakeholders").
