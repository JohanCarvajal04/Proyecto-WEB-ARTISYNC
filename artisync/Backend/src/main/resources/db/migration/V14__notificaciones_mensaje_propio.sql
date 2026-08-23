-- =============================================================================
-- V14: Cada notificación guarda su propio texto
-- =============================================================================
--
-- notificaciones_sistema nunca tuvo columna de mensaje propia: el texto que
-- veía el usuario salía de tipos_notificacion.formato_mensaje, un campo
-- COMPARTIDO por todas las notificaciones del mismo evento
-- (NotificacionServiceImpl#notificar lo fija una sola vez, la primera vez
-- que se dispara ese tipo de evento, y nunca se vuelve a tocar).
--
-- El resultado: la entrega en tiempo real por WebSocket sí mostraba el texto
-- correcto (se arma en memoria a partir del mensaje real), pero en cuanto el
-- usuario recargaba la página y volvía a pedir /api/v1/notificaciones, TODAS
-- las notificaciones de un mismo tipo mostraban el texto de la primera que
-- se disparó — para "MENSAJE_RECIBIDO" eso significa que el segundo mensaje
-- en adelante aparecía en el listado con el contenido del primero.
--
-- Backfill: se copia el texto compartido que tenían hasta ahora, que es lo
-- más parecido a la verdad que se puede reconstruir para notificaciones ya
-- emitidas (su contenido real nunca se guardó en ningún lado).

ALTER TABLE notificaciones_sistema ADD COLUMN IF NOT EXISTS mensaje TEXT;

UPDATE notificaciones_sistema n
SET mensaje = t.formato_mensaje
FROM tipos_notificacion t
WHERE n.id_tipo_notificacion = t.id_tipo_notificacion
  AND n.mensaje IS NULL;
