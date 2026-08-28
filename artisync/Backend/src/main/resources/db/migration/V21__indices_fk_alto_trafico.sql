-- =============================================================================
-- V21: indices de clave foranea ausentes en las tablas de mayor trafico
-- (H-07 de la auditoria de estado del 2026-08-26)
-- =============================================================================
-- PostgreSQL NO crea indice automatico para una FK (a diferencia de MySQL).
-- pedidos, mensajes y notificaciones_sistema se crearon en V1 sin ningun
-- indice sobre sus columnas de clave foranea (V15/V17 ya aplicaron este mismo
-- patron sobre auditoria_eventos y usuario_roles/tokens_recuperacion/usuarios,
-- pero no llegaron a cubrir estas tres). Sin ellos, los metodos de repositorio
-- listados junto a cada indice degradan a seq scan a medida que las tablas
-- crecen.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. pedidos
-- -----------------------------------------------------------------------------

-- PedidoRepository.findByUsuarioClienteIdUsuario ("mis pedidos" del cliente).
CREATE INDEX IF NOT EXISTS idx_pedidos_id_usuario_cliente
    ON pedidos (id_usuario_cliente);

-- PedidoRepository.findByServicioPerfilIdPerfil y
-- findByServicioPerfilUsuarioIdUsuario ("mis pedidos" del creador, via join
-- contra servicios).
CREATE INDEX IF NOT EXISTS idx_pedidos_id_servicio
    ON pedidos (id_servicio);

-- FK sin caller de repositorio conocido hoy, pero sin indice cualquier borrado
-- o actualizacion de flujos_trabajo fuerza un seq scan de pedidos para
-- verificar la referencia.
CREATE INDEX IF NOT EXISTS idx_pedidos_id_flujo
    ON pedidos (id_flujo);


-- -----------------------------------------------------------------------------
-- 2. mensajes
-- -----------------------------------------------------------------------------

-- MensajeRepository.findBySalaIdSalaOrderByFechaHoraEnvioAsc: compuesto para
-- resolver el WHERE (id_sala) y el ORDER BY (fecha_hora_envio) de una pasada,
-- mismo patron que idx_auditoria_actor_fecha en V15.
CREATE INDEX IF NOT EXISTS idx_mensajes_sala_fecha
    ON mensajes (id_sala, fecha_hora_envio);

-- FK sin indice; la recorre cualquier borrado/consulta de "mensajes enviados
-- por este usuario".
CREATE INDEX IF NOT EXISTS idx_mensajes_id_remitente
    ON mensajes (id_remitente);


-- -----------------------------------------------------------------------------
-- 3. notificaciones_sistema
-- -----------------------------------------------------------------------------

-- NotificacionSistemaRepository.findByUsuarioIdUsuarioOrderByFechaEmisionDesc
-- (listado paginado de notificaciones del usuario).
CREATE INDEX IF NOT EXISTS idx_notificaciones_usuario_fecha
    ON notificaciones_sistema (id_usuario, fecha_emision DESC);

-- Parcial a proposito: countByUsuarioIdUsuarioAndEstaLeidaFalse y
-- marcarTodasLeidas solo filtran esta_leida = false, y la mayoria de filas
-- terminan leidas (mismo razonamiento que idx_auditoria_no_exitosos en V15).
CREATE INDEX IF NOT EXISTS idx_notificaciones_no_leidas
    ON notificaciones_sistema (id_usuario)
    WHERE esta_leida = false;

-- FK sin indice; sin ella, borrar o consultar un tipo de notificacion fuerza
-- un seq scan para verificar la referencia.
CREATE INDEX IF NOT EXISTS idx_notificaciones_id_tipo
    ON notificaciones_sistema (id_tipo_notificacion);
