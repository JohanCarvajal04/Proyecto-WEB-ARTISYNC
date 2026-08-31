-- =============================================================================
-- V27: propuestas de cambio de precio/fecha de entrega, con consentimiento mutuo
-- =============================================================================
-- Antes, PATCH /pedidos/{id}/terminos aplicaba el cambio de forma unilateral e
-- inmediata: cualquiera de las dos partes (cliente o creador) podia cambiar
-- precio_pactado o fecha_entrega_estimada sin que la otra confirmara nada.
--
-- Esta tabla respalda el nuevo flujo propone/acepta en
-- PedidoServicioImpl#proponerTerminos / #aceptarPropuestaTerminos: el cambio
-- solo se aplica al pedido cuando la CONTRAPARTE del proponente acepta la
-- propuesta, y esa misma aceptacion es lo que genera el contrato (si aun no
-- existia) con el precio y la fecha ya acordados.
-- =============================================================================

CREATE TABLE propuestas_terminos_pedido (
    id_propuesta BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_usuario_propuso BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    precio_propuesto DECIMAL(10,2),
    fecha_entrega_propuesta TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion TIMESTAMP
);

-- Defensa en profundidad: el servicio ya rechaza crear una propuesta si hay
-- otra PENDIENTE para el mismo pedido, pero dos peticiones concurrentes
-- podrian pasar ambas ese chequeo antes de que ninguna se guarde. El indice
-- unico parcial hace que la segunda insercion falle a nivel de base de datos.
CREATE UNIQUE INDEX ux_propuestas_terminos_pedido_pendiente
    ON propuestas_terminos_pedido(id_pedido)
    WHERE estado = 'PENDIENTE';

CREATE INDEX idx_propuestas_terminos_pedido ON propuestas_terminos_pedido(id_pedido);
