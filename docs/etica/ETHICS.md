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

## Bitácora de auditoría del sistema (REQ-NF-013)

Desde V12__modulo_auditoria.sql el sistema registra, en la tabla inmutable `auditoria_eventos`,
un evento por cada operación sensible que un usuario realiza (login, cambios de rol y permisos,
liberación de fondos, gestión de catálogo, etc.), con actor, dirección IP y, cuando aplica, un
detalle estructurado del cambio.

- **Qué se registra y por qué**: la mínima información necesaria para reconstruir "quién hizo
  qué, cuándo, desde dónde y con qué resultado" — el correo del actor, su id de usuario, la IP y
  el user-agent de la petición, la acción y el módulo afectado, y un resumen no sensible del
  cambio. Es la base técnica que permite investigar un incidente de seguridad o disputar una
  decisión administrativa (p. ej. por qué se suspendió una cuenta).
- **Qué NUNCA se registra**: contraseñas (ni en claro ni hasheadas), tokens de sesión o de
  refresco, secretos de doble factor y códigos de respaldo, el contenido de mensajes de chat
  (que ya vive en `infracciones_mensaje` con su propio control de acceso) y los documentos de
  verificación de identidad (que `REQ-F-006` exige eliminar tras la decisión). Un sanitizador
  aplicado a cada evento antes de persistirlo enmascara además, por nombre de campo, cualquier
  valor que coincida con estos patrones, como defensa adicional ante un descuido al declarar
  qué se audita.
- **Quién puede leerla**: únicamente ADMIN, SOPORTE y AUDITOR_FINANCIERO, y solo el primero y el
  último pueden exportarla a CSV (permisos `AUDITORIA_VER` y `AUDITORIA_EXPORTAR`, deliberadamente
  separados porque exportar extrae datos personales del sistema en un archivo). El propio acceso
  a la bitácora — consultarla o exportarla — queda a su vez auditado.
- **Inmutabilidad frente al derecho al olvido**: la tabla es de solo inserción a nivel de base de
  datos (trigger que rechaza `UPDATE`/`DELETE`/`TRUNCATE`, y la cuenta de aplicación solo tiene
  concedidos `SELECT` e `INSERT`). Esto es una tensión real, no un descuido: la finalidad de
  seguridad de la bitácora exige que un evento no pueda alterarse ni borrarse después del hecho,
  lo que es incompatible con implementar un derecho al olvido como un simple borrado de filas. La
  postura del equipo es que esa finalidad prevalece, con una retención acotada en el tiempo; una
  purga futura solo podría ejecutarla el superusuario de la base de datos fuera de la aplicación,
  nunca la aplicación por sí sola — es decir, purgar la bitácora debe seguir siendo una decisión
  administrativa deliberada y auditable en sí misma, no una operación rutinaria.

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
