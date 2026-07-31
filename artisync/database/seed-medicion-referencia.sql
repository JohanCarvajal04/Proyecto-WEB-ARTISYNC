-- ==============================================================================
-- Datos de referencia para las mediciones del Bloque C (Tercera Entrega)
-- No forma parte de las migraciones Flyway versionadas: es un insumo de prueba,
-- no un cambio de esquema. Seguro de re-ejecutar (ON CONFLICT DO NOTHING donde
-- hay UNIQUE); no borra nada existente.
--
-- Uso:
--   docker compose exec -T postgres psql -U pfc_user -d pfc_db < database/seed-medicion-referencia.sql
-- ==============================================================================

-- Categorías y subcategorías del catálogo (vacío en Flyway: sin esto, ningún
-- servicio puede crearse — servicios.id_subcategoria es NOT NULL).
INSERT INTO categorias (nombre_categoria, estado_activa) VALUES
    ('Diseño Gráfico', TRUE),
    ('Ilustración Digital', TRUE),
    ('Animación', TRUE),
    ('Modelado 3D', TRUE),
    ('Producción de Audio', TRUE)
ON CONFLICT (nombre_categoria) DO NOTHING;

INSERT INTO subcategorias (id_categoria, nombre_subcategoria)
SELECT c.id_categoria, s.nombre
FROM categorias c
JOIN (VALUES
    ('Diseño Gráfico', 'Logotipos'),
    ('Diseño Gráfico', 'Identidad de Marca'),
    ('Ilustración Digital', 'Retratos'),
    ('Ilustración Digital', 'Concept Art'),
    ('Animación', '2D'),
    ('Animación', 'Motion Graphics'),
    ('Modelado 3D', 'Personajes'),
    ('Modelado 3D', 'Props y Escenarios'),
    ('Producción de Audio', 'Composición Musical'),
    ('Producción de Audio', 'Locución')
) AS s(categoria, nombre) ON s.categoria = c.nombre_categoria
WHERE NOT EXISTS (
    SELECT 1 FROM subcategorias sc
    WHERE sc.id_categoria = c.id_categoria AND sc.nombre_subcategoria = s.nombre
);

-- Flujo de trabajo mínimo (vacío en Flyway: sin esto, PedidoServicioImpl.crearPedido
-- revienta con "No hay flujos de trabajo configurados en el sistema").
INSERT INTO flujos_trabajo (nombre_flujo, descripcion_flujo)
SELECT 'Flujo Estándar de Medición', 'Flujo de 4 etapas usado como fixture para las mediciones del Bloque C'
WHERE NOT EXISTS (SELECT 1 FROM flujos_trabajo WHERE nombre_flujo = 'Flujo Estándar de Medición');

INSERT INTO etapas_flujo (nombre_etapa) VALUES
    ('Borrador Recibido'),
    ('En Producción'),
    ('En Revisión del Cliente'),
    ('Entrega Final')
ON CONFLICT (nombre_etapa) DO NOTHING;

INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final)
SELECT f.id_flujo, e.id_etapa, o.numero_orden, o.es_final
FROM flujos_trabajo f
JOIN (VALUES
    ('Borrador Recibido', 1, FALSE),
    ('En Producción', 2, FALSE),
    ('En Revisión del Cliente', 3, FALSE),
    ('Entrega Final', 4, TRUE)
) AS o(nombre_etapa, numero_orden, es_final) ON TRUE
JOIN etapas_flujo e ON e.nombre_etapa = o.nombre_etapa
WHERE f.nombre_flujo = 'Flujo Estándar de Medición'
  AND NOT EXISTS (
    SELECT 1 FROM flujo_etapas_config fec
    WHERE fec.id_flujo = f.id_flujo AND fec.id_etapa = e.id_etapa
  );
