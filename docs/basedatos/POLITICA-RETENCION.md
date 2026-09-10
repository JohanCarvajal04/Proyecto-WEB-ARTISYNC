# Política de retención y purga de datos

Origen: hallazgo H-08 de la auditoría de estado del 2026-08-26
(`REPORTE-ESTADO-artisync-20260826-1550.md`) — "sin política de purga/retención para tablas de
alto volumen". Este documento fija qué se purga automáticamente, cada cuánto, y qué queda
explícitamente fuera de alcance y por qué.

## Qué se purga

| Tabla | Rutina | Scheduler | Cron | Criterio |
|---|---|---|---|---|
| `sesiones_usuario` | `sp_purgar_datos_seguridad` | `SeguridadPurgaScheduler` | `0 30 3 * * *` | `fecha_expiracion < ahora` |
| `tokens_recuperacion` | `sp_purgar_datos_seguridad` | `SeguridadPurgaScheduler` | `0 30 3 * * *` | usados, o generados hace más de 24h |
| `codigos_respaldo_2fa` | `sp_purgar_datos_seguridad` | `SeguridadPurgaScheduler` | `0 30 3 * * *` | solo los ya consumidos (`usado = TRUE`) |
| `notificaciones_sistema` | `sp_purgar_notificaciones` | `NotificacionesPurgaScheduler` | `0 0 4 * * *` | leídas con más de 90 días de antigüedad |

Todas las rutinas son `PROCEDURE` (no `FUNCTION`) que confirman por lotes
(`FOR UPDATE SKIP LOCKED` + `COMMIT`), documentadas en detalle en
[`docs/basedatos/CATALOGO-SP.md`](CATALOGO-SP.md) (secciones 18 y 19). Los tres schedulers corren
en horas escalonadas (3:00, 3:30, 4:00) para no competir por E/S de disco entre sí.

## Qué NO se purga, y por qué

### `auditoria_eventos` — no se purga por diseño

Bloqueada estructuralmente, no solo por decisión operativa:

- `trg_auditoria_eventos_inmutable` (`BEFORE UPDATE OR DELETE`, `V15__modulo_auditoria.sql`) rechaza
  cualquier `UPDATE`/`DELETE` con `RAISE EXCEPTION` (SQLSTATE `42501`).
- `trg_auditoria_eventos_no_truncate` (`BEFORE TRUNCATE`) cierra también esa vía de escape.
- La cuenta de aplicación `artisync_app` tiene el privilegio `DELETE` revocado explícitamente sobre
  esta tabla (`REVOKE ALL ... GRANT SELECT, INSERT`).

Esto implementa REQ-NF-013 (bitácora de auditoría de solo inserción) y está verificado por
`EventoAuditoriaInmutabilidadIT` (4 tests: UPDATE, DELETE, TRUNCATE, flush de Hibernate).
Purgar esta tabla exigiría un cambio estructural — particionar por rango de `fecha_evento` y usar
`DETACH PARTITION` + `DROP TABLE` (los triggers de fila no se disparan al eliminar una partición
completa) — que está fuera del alcance de H-08 y requeriría revisar la política de inmutabilidad
en sí, no solo añadir una rutina de purga.

### `mensajes` — fuera de alcance por los adjuntos

`documentos_adjuntos.id_mensaje` referencia a `mensajes` con `ON DELETE CASCADE`: borrar un mensaje
borra en cascada sus adjuntos. El problema no es la fila en sí, sino que `documentos_adjuntos.url_archivo`
apunta a un blob en almacenamiento externo (Azure Blob / disco local) que **no** se limpia solo —
a diferencia de `VerificacionScheduler`, que sí llama a `almacenamiento.eliminar(...)` antes de
descartar un certificado, no existe hoy ningún mecanismo que borre el archivo físico al purgar un
mensaje. Purgar `mensajes` sin resolver antes esa limpieza de storage dejaría blobs huérfanos
acumulándose indefinidamente — un problema distinto (y potencialmente peor) que el que se busca
resolver.

Si en el futuro se aborda la purga de `mensajes`, debe incluir la limpieza del storage como parte
de la misma rutina o de un paso previo, no como una rutina SQL aislada.

## Qué se retiene con plazo declarado (datos personales sensibles)

Origen: REQ-NF-018 (protección de datos personales), auditoría externa del SRS v1.3.0 del
2026-09-08 — a diferencia de la tabla anterior, estas categorías no tienen hoy una rutina SQL
de purga automática por lote: el mecanismo de cierre es el flujo de supresión a solicitud del
titular (`PrivacidadService`, `POST /api/v1/usuarios/me/solicitud-supresion` y
`POST /api/v1/admin/usuarios/{id}/supresion`), no un scheduler. No prometer aquí una
automatización que todavía no existe.

| Tabla | Plazo declarado | Justificación | Acción al vencer / al solicitarse |
|---|---|---|---|
| `certificados_ia` | Hasta la decisión terminal de la verificación (`APROBADO`/`RECHAZADO`); el documento binario ya se borra en ese momento (`VerificacionServicioImpl`, llamada a `almacenamiento.eliminar(...)`) | Un documento de identidad o certificado profesional es dato sensible; no hay necesidad de negocio de conservar sus metadatos indefinidamente tras la decisión | El flujo de supresión limpia `datos_extraidos_ia`, `hash_documento` y `razon_ia`, y fuerza el borrado del binario si seguía pendiente |
| `datos_pago_creador` | Mientras la cuenta del creador esté activa; al ejecutarse la supresión, salvo obligación contable pendiente | El correo de PayPal es un dato de pago; debe poder anonimizarse igual que el resto del perfil cuando el titular lo solicita | El flujo de supresión anonimiza `correo_paypal`, salvo que el creador tenga un contrato con fondos aún retenidos en garantía (`pagos_garantia.estado_fondos = 'Retenido'`) — en ese caso el dato se conserva y la excepción legal se declara en la respuesta y en el evento de auditoría `USUARIO_ANONIMIZAR` |
| `contratos` | Sin plazo de purga propio: es evidencia legal del acuerdo entre las partes | `contratos` no tiene campos personales propios (solo hashes de firma y URLs); el riesgo de datos personales de esta categoría vive en `usuarios`, `certificados_ia` y `datos_pago_creador`, no en la fila del contrato en sí | Queda fuera del alcance de la supresión a solicitud — ver REQ-NF-020 para la re-verificación del hash de firma, que gobierna la integridad del documento, no su retención |

`mensajes` sigue fuera de alcance por el mismo motivo ya documentado arriba (limpieza de
adjuntos en storage externo sin resolver) y no se toca por REQ-NF-018.

## Cómo cambiar la retención

Los parámetros (`p_tamano_lote`, `p_dias_retencion`) son argumentos de la rutina invocada desde el
scheduler correspondiente — hoy constantes en el propio scheduler Java
(`NotificacionesPurgaScheduler.TAMANO_LOTE`, `DIAS_RETENCION`), no configuración externa. Cambiar
la retención de notificaciones de 90 días requiere editar esa constante y redeployar; no hay
`@Value` ni propiedad en `application.properties` para esto todavía.
