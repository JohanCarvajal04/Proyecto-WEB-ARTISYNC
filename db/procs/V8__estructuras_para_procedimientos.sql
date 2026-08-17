-- -----------------------------------------------------------------------------
-- Codigo publico de pedido (REQ-F-018) — consumido por fn_generar_codigo_pedido
-- -----------------------------------------------------------------------------
-- Se anade como NULL-able: los pedidos ya existentes no tienen codigo y se les
-- asigna de forma perezosa la primera vez que se consulta el pedido. La
-- restriccion UNIQUE garantiza la unicidad incluso si la secuencia se reinicia.
ALTER TABLE pedidos
    ADD COLUMN IF NOT EXISTS codigo_pedido VARCHAR(20);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_pedidos_codigo_pedido'
    ) THEN
        ALTER TABLE pedidos
            ADD CONSTRAINT uq_pedidos_codigo_pedido UNIQUE (codigo_pedido);
    END IF;
END
$$;

COMMENT ON COLUMN pedidos.codigo_pedido
    IS 'REQ-F-018 - Codigo publico ART-AAAA-NNNNNN. Lo asigna fn_generar_codigo_pedido.';

-- Secuencia del correlativo. Independiente de id_pedido para que el codigo
-- publico no revele el volumen real de pedidos de la plataforma.
CREATE SEQUENCE IF NOT EXISTS seq_codigo_pedido
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 1;

COMMENT ON SEQUENCE seq_codigo_pedido
    IS 'Correlativo de fn_generar_codigo_pedido. Ver db/procs/fn_generar_codigo_pedido.sql.';

-- -----------------------------------------------------------------------------
-- Indices de apoyo a las rutinas
-- -----------------------------------------------------------------------------
-- fn_cerrar_pedidos_vencidos filtra por fecha de entrega y recorre el historial
-- por pedido; fn_reporte_comisiones_creador y fn_catalogo_filtrado navegan
-- servicios por perfil. Sin estos indices las rutinas degradan a seq scan y la
-- medicion k6 del Bloque C dejaria de ser representativa.
CREATE INDEX IF NOT EXISTS idx_pedidos_fecha_entrega_estimada
    ON pedidos (fecha_entrega_estimada)
    WHERE fecha_entrega_estimada IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_historial_pedido_fecha
    ON historial_estados_pedido (id_pedido, fecha_transicion DESC);

CREATE INDEX IF NOT EXISTS idx_servicios_perfil
    ON servicios (id_perfil);

CREATE INDEX IF NOT EXISTS idx_servicios_subcategoria_estado
    ON servicios (id_subcategoria, estado_publicacion);
