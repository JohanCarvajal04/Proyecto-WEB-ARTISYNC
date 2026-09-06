-- ==============================================================================
-- MIGRACIÓN V40: CUESTIONARIO (BRIEFING) LIGADO AL SERVICIO
-- ==============================================================================
--
-- RF-16 ya tenía un sistema de briefing completo (plantillas, preguntas,
-- envíos, respuestas inmutables), pero desacoplado de un servicio concreto:
-- la plantilla colgaba de id_perfil (el creador) y se enviaba manualmente
-- desde el chat, después de creado el pedido, así que el cliente podía no
-- llegar a responder nunca.
--
-- Esta migración no toca briefing_plantillas.id_perfil (la plantilla la
-- sigue creando el creador de forma independiente, en "Mis cuestionarios").
-- Solo agrega la relación servicio → plantilla, igual que V35 hizo con el
-- flujo de trabajo: nullable, sin bloquear la creación de pedidos si el
-- servicio no tiene un cuestionario asignado. Cuando sí lo tiene, el cliente
-- responde sus preguntas al crear el pedido (ver PedidoServicioImpl), en vez
-- de esperar un envío manual posterior.

ALTER TABLE servicios
    ADD COLUMN IF NOT EXISTS id_briefing_plantilla BIGINT
        REFERENCES briefing_plantillas(id_briefing_plantilla);

CREATE INDEX IF NOT EXISTS idx_servicios_id_briefing_plantilla ON servicios (id_briefing_plantilla);
