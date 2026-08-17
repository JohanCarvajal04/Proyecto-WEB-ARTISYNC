# Ética, consentimiento y disclosure — Artisync (Entrega Final, v1.0.0)

Cumple el Bloque G de la guía de la Entrega Final.

## Resguardo de consentimientos informados

Los formularios de consentimiento firmados por los 16 participantes de la prueba de usabilidad
(SUS, `docs/mediciones/sus/sus-raw.csv`) **no se suben al repositorio público**. Se archivan en el
Google Drive institucional del equipo (carpeta `Artisync — PFC / Consentimientos SUS`, acceso
restringido a los cuatro integrantes y al docente-director bajo solicitud). La plantilla vigente
está en [`docs/etica/consentimientos/plantilla.md`](consentimientos/plantilla.md); el
`.gitignore` de esa carpeta bloquea explícitamente la subida accidental de cualquier PDF firmado.

Cada participante se identifica en todos los datos crudos y reportes exclusivamente por su código
anónimo (`P01`…`P16`) — nunca por nombre, correo ni ningún otro dato personal. La correspondencia
entre código y persona real existe únicamente en el registro de consentimientos fuera del
repositorio, y solo el equipo tiene acceso a ella.

## Participación voluntaria y datos recogidos

- La participación en la prueba de usabilidad fue voluntaria, sin compensación económica, con
  posibilidad de abandonar la sesión en cualquier momento sin dar explicación (ver plantilla de
  consentimiento).
- No se grabó audio ni video de ninguna sesión.
- Los únicos datos recogidos por participante son: las 10 respuestas del cuestionario SUS y el
  perfil demográfico agregado y no identificante declarado en
  `docs/mediciones/sus/perfil-participantes.csv` (rango de edad, sexo, experiencia web,
  dispositivo) — sin nombre, correo ni identificador que permita reidentificación a partir del
  dataset publicado.

## Declaración de uso de asistentes de inteligencia artificial

Ver [`ai-disclosure.md`](ai-disclosure.md) para el detalle completo (herramienta, versión,
propósito, fases del proyecto donde se usó, y revisión posterior del equipo).

## Declaración de conflictos de interés

El equipo declara **ausencia de conflictos de interés**. Ningún integrante, ni el
docente-director, tiene una relación económica, contractual o de dependencia con terceros que
pueda sesgar los resultados reportados en este PFC.

## Declaración de financiamiento

El proyecto **no recibió financiamiento externo**. Se desarrolló íntegramente con recursos propios
del equipo (tiempo, equipos personales) y con los servicios de nivel gratuito o académico de los
proveedores cloud mencionados en `docs/despliegue/DEPLOYMENT.md` (sin costo económico para el
equipo a la fecha de este documento).
