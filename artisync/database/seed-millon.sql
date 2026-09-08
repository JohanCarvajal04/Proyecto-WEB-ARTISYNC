-- ==============================================================================
-- ARTISYNC — Semilla masiva: ~1 000 000 de registros distribuidos en todas las
-- tablas del esquema, respetando integridad referencial y restricciones UNIQUE.
--
-- Prerequisitos:
--   • Las migraciones Flyway V1..V42 ya aplicadas
--   • El seed base (seed.sql) ya ejecutado (países, roles, permisos, admin)
--   • seed-medicion-referencia.sql ya ejecutado (categorías, subcategorías,
--     flujos, etapas)
--
-- Uso (Azure PostgreSQL):
--   psql "host=TU-SERVIDOR.postgres.database.azure.com port=5432
--         dbname=artisyncbd user=TU_USUARIO sslmode=require" \
--     -f database/seed-millon.sql
--
-- O localmente con Docker:
--   docker compose exec -T postgres psql -U pfc_user -d pfc_db \
--     < database/seed-millon.sql
--
-- Tiempo estimado: 5-15 minutos dependiendo de la máquina/red.
-- ==============================================================================

-- Protección: todo dentro de una transacción para rollback si algo falla.
BEGIN;

-- Optimizaciones temporales para inserción masiva
SET LOCAL synchronous_commit = OFF;
SET LOCAL work_mem = '256MB';

-- ==============================================================================
-- CONSTANTES DE DISTRIBUCIÓN
-- ==============================================================================
-- Total aproximado: ~1,000,000 registros
--
-- usuarios:              50,000   (base de todo)
-- perfiles_creadores:    10,000   (20% son creadores)
-- portafolios:           10,000   (1 por creador)
-- portafolio_items:      50,000   (5 por creador)
-- habilidades:              100   (catálogo)
-- creador_habilidades:   30,000   (3 por creador)
-- servicios:             30,000   (3 por creador)
-- servicio_etiquetas:    60,000   (2 por servicio)
-- servicio_atributos:    30,000   (1 por servicio)
-- pedidos:              100,000
-- historial_estados_pedido: 300,000 (3 estados por pedido)
-- contratos:             80,000
-- pagos_garantia:        80,000
-- transacciones_pago:    80,000
-- salas_chat:           100,000   (1 por pedido)
-- mensajes:             100,000   (1 por sala)
-- notificaciones_sistema: 50,000
-- seguidores:            50,000
-- comentarios_portafolio: 30,000
-- likes_portafolio:      30,000
-- resenas_servicios:     50,000
-- sorteos:                1,000
-- participantes_sorteo:  20,000
-- ≈ 1,031,100 registros

-- ==============================================================================
-- 0. ASEGURAR DATOS DE REFERENCIA
-- ==============================================================================

-- Habilidades (catálogo de 100)
INSERT INTO habilidades (nombre_habilidad)
SELECT 'Habilidad-' || i
FROM generate_series(1, 100) AS i
ON CONFLICT (nombre_habilidad) DO NOTHING;

-- Etiquetas (catálogo de 50)
INSERT INTO etiquetas (nombre_etiqueta)
SELECT 'tag-' || i
FROM generate_series(1, 50) AS i
ON CONFLICT (nombre_etiqueta) DO NOTHING;

-- Atributos dinámicos (catálogo de 20)
INSERT INTO atributos_dinamicos (nombre_atributo, tipo_dato)
SELECT 'Atributo-' || i, CASE WHEN i % 3 = 0 THEN 'TEXTO' WHEN i % 3 = 1 THEN 'NUMERO' ELSE 'BOOLEANO' END
FROM generate_series(1, 20) AS i
ON CONFLICT (nombre_atributo) DO NOTHING;

-- Motivos de rechazo (catálogo de 10)
INSERT INTO motivos_rechazo (descripcion_motivo)
SELECT 'Motivo de rechazo #' || i || ': ' || 
  CASE i
    WHEN 1 THEN 'No cumple con las especificaciones'
    WHEN 2 THEN 'Calidad insuficiente'
    WHEN 3 THEN 'Estilo no coincide con lo solicitado'
    WHEN 4 THEN 'Formato de entrega incorrecto'
    WHEN 5 THEN 'Resolución insuficiente'
    WHEN 6 THEN 'Colores incorrectos'
    WHEN 7 THEN 'Tipografía inadecuada'
    WHEN 8 THEN 'Falta contenido solicitado'
    WHEN 9 THEN 'Error en la composición'
    ELSE 'Requiere revisión general'
  END
FROM generate_series(1, 10) AS i
ON CONFLICT (descripcion_motivo) DO NOTHING;

-- Tipos de notificación
INSERT INTO tipos_notificacion (nombre_evento, formato_mensaje)
VALUES
  ('PEDIDO_CREADO', 'Se ha creado un nuevo pedido #{{id}}'),
  ('PEDIDO_AVANZADO', 'Tu pedido #{{id}} ha avanzado a la etapa {{etapa}}'),
  ('MENSAJE_RECIBIDO', 'Tienes un nuevo mensaje en el pedido #{{id}}'),
  ('CONTRATO_FIRMADO', 'El contrato del pedido #{{id}} ha sido firmado'),
  ('PAGO_LIBERADO', 'Se han liberado los fondos del pedido #{{id}}'),
  ('NUEVO_SEGUIDOR', '{{nombre}} ha comenzado a seguirte'),
  ('NUEVO_COMENTARIO', 'Nuevo comentario en tu portafolio'),
  ('SORTEO_GANADOR', '¡Felicidades! Has ganado el sorteo {{titulo}}'),
  ('INFRACCION_DETECTADA', 'Se ha detectado una infracción en tu cuenta'),
  ('VERIFICACION_APROBADA', 'Tu verificación de identidad ha sido aprobada')
ON CONFLICT (nombre_evento) DO NOTHING;

-- Estados de verificación (normalmente los crea V7, pero aseguramos)
INSERT INTO estados_verificacion (nombre_estado)
VALUES ('PENDIENTE'), ('APROBADO'), ('RECHAZADO'), ('REQUIERE_ACLARACION')
ON CONFLICT (nombre_estado) DO NOTHING;

-- ==============================================================================
-- 1. USUARIOS (50,000)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando 50,000 usuarios...'; END $$;

INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, id_pais, fecha_nacimiento, estado_cuenta)
SELECT
  'Usuario' || i,
  'Apellido' || (i % 500),
  'usuario' || i || '@artisync-seed.test',
  -- BCrypt hash de 'TestPass123!' (no importa para datos de prueba)
  '$2a$12$O26tVGE2jZ/6rNDZJYaKyOfDPE0.8E9HIbISLR4nXySuy.nvvycjK',
  1 + (i % (SELECT COUNT(*) FROM pais))::INT,
  '1985-01-01'::DATE + (i % 14000) * INTERVAL '1 day',
  CASE WHEN i % 50 = 0 THEN FALSE ELSE TRUE END  -- 2% inactivos
FROM generate_series(1, 50000) AS i
ON CONFLICT (correo) DO NOTHING;

-- Asignar roles a usuarios (todos empiezan como CLIENTE, 20% también son CREADOR)
INSERT INTO usuario_roles (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM usuarios u
CROSS JOIN roles r
WHERE r.nombre_rol = 'CLIENTE'
  AND u.correo LIKE '%@artisync-seed.test'
ON CONFLICT DO NOTHING;

INSERT INTO usuario_roles (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM usuarios u
CROSS JOIN roles r
WHERE r.nombre_rol = 'CREADOR'
  AND u.correo LIKE '%@artisync-seed.test'
  AND u.id_usuario % 5 = 0   -- 20% son creadores
ON CONFLICT DO NOTHING;

-- ==============================================================================
-- 2. PERFILES DE CREADORES (10,000)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando perfiles de creadores...'; END $$;

INSERT INTO perfiles_creadores (id_usuario, biografia, url_red_social, titulo_profesional)
SELECT 
  u.id_usuario,
  'Creador profesional con experiencia en diseño digital. Especialista #' || ROW_NUMBER() OVER (ORDER BY u.id_usuario),
  'https://portfolio.example.com/creador' || u.id_usuario,
  CASE (u.id_usuario % 8)
    WHEN 0 THEN 'Diseñador Gráfico'
    WHEN 1 THEN 'Ilustrador Digital'
    WHEN 2 THEN 'Animador 2D/3D'
    WHEN 3 THEN 'Modelador 3D'
    WHEN 4 THEN 'Productor Musical'
    WHEN 5 THEN 'Director de Arte'
    WHEN 6 THEN 'Concept Artist'
    ELSE 'Artista Multimedia'
  END
FROM usuarios u
JOIN usuario_roles ur ON ur.id_usuario = u.id_usuario
JOIN roles r ON r.id_rol = ur.id_rol AND r.nombre_rol = 'CREADOR'
WHERE u.correo LIKE '%@artisync-seed.test'
ON CONFLICT (id_usuario) DO NOTHING;

-- ==============================================================================
-- 3. PORTAFOLIOS (10,000) + ITEMS (50,000)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando portafolios e items...'; END $$;

INSERT INTO portafolios (id_perfil, es_publico, opciones_personalizacion, total_visitas_acumuladas)
SELECT
  pc.id_perfil,
  CASE WHEN pc.id_perfil % 10 = 0 THEN FALSE ELSE TRUE END,
  jsonb_build_object(
    'primary', CASE (pc.id_perfil % 6)
      WHEN 0 THEN '#1A1A2E'
      WHEN 1 THEN '#16213E'
      WHEN 2 THEN '#0F3460'
      WHEN 3 THEN '#E94560'
      WHEN 4 THEN '#533483'
      ELSE '#2B2D42'
    END,
    'secondary', '#6c757d',
    'bg', '#f8f9fa',
    'text', '#212529',
    'surface', '#ffffff'
  ),
  (pc.id_perfil * 7 % 10000)
FROM perfiles_creadores pc
WHERE EXISTS (
  SELECT 1 FROM usuarios u 
  WHERE u.id_usuario = pc.id_usuario AND u.correo LIKE '%@artisync-seed.test'
)
ON CONFLICT (id_perfil) DO NOTHING;

INSERT INTO portafolio_items (id_portafolio, titulo_obra, descripcion_obra, url_archivo_multimedia, fecha_subida)
SELECT 
  p.id_portafolio,
  'Obra ' || item_num || ' - Creador ' || p.id_portafolio,
  'Descripción de la obra número ' || item_num || '. Arte digital creado con técnicas modernas de ' ||
    CASE item_num % 5
      WHEN 0 THEN 'ilustración vectorial'
      WHEN 1 THEN 'pintura digital'
      WHEN 2 THEN 'modelado poligonal'
      WHEN 3 THEN 'composición fotográfica'
      ELSE 'animación cuadro a cuadro'
    END,
  'https://storage.artisync.test/obras/' || p.id_portafolio || '/item-' || item_num || '.webp',
  NOW() - (item_num * 30 + p.id_portafolio % 60) * INTERVAL '1 day'
FROM portafolios p
CROSS JOIN generate_series(1, 5) AS item_num
WHERE EXISTS (
  SELECT 1 FROM perfiles_creadores pc
  JOIN usuarios u ON u.id_usuario = pc.id_usuario
  WHERE pc.id_perfil = p.id_perfil AND u.correo LIKE '%@artisync-seed.test'
);

-- ==============================================================================
-- 4. HABILIDADES DE CREADORES (30,000)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando habilidades de creadores...'; END $$;

INSERT INTO creador_habilidades (id_perfil, id_habilidad, nivel_dominio)
SELECT DISTINCT ON (pc.id_perfil, h.id_habilidad)
  pc.id_perfil,
  h.id_habilidad,
  CASE (pc.id_perfil + h.id_habilidad) % 4
    WHEN 0 THEN 'Principiante'
    WHEN 1 THEN 'Intermedio'
    WHEN 2 THEN 'Avanzado'
    ELSE 'Experto'
  END
FROM perfiles_creadores pc
CROSS JOIN LATERAL (
  SELECT id_habilidad FROM habilidades
  WHERE id_habilidad % 33 IN (pc.id_perfil % 33, (pc.id_perfil + 7) % 33, (pc.id_perfil + 19) % 33)
  LIMIT 3
) h
WHERE EXISTS (
  SELECT 1 FROM usuarios u 
  WHERE u.id_usuario = pc.id_usuario AND u.correo LIKE '%@artisync-seed.test'
);

-- ==============================================================================
-- 5. SERVICIOS (30,000) + ETIQUETAS + ATRIBUTOS
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando servicios...'; END $$;

-- Nota: desde V37 un servicio se asocia a subcategorías via la tabla puente
-- servicio_subcategorias (N:M), no por una columna id_subcategoria directa.
INSERT INTO servicios (id_perfil, titulo_servicio, descripcion_detallada,
                       precio_base, tipo_item, estado_publicacion, limite_revisiones_base, url_miniatura)
SELECT
  pc.id_perfil,
  CASE serv_num
    WHEN 1 THEN 'Diseño ' || pc.id_perfil || '-A'
    WHEN 2 THEN 'Ilustración ' || pc.id_perfil || '-B'
    ELSE 'Proyecto ' || pc.id_perfil || '-C'
  END,
  'Servicio profesional ofrecido por el creador. Incluye revisiones, archivos fuente en alta resolución y soporte post-entrega. Número de referencia: ' || pc.id_perfil || '-' || serv_num,
  ROUND((50 + (pc.id_perfil * serv_num % 950))::NUMERIC, 2),
  CASE WHEN serv_num = 3 THEN 'PAQUETE' ELSE 'SERVICIO' END,
  CASE WHEN pc.id_perfil % 20 = 0 THEN 'PAUSADO' ELSE 'ACTIVO' END,
  1 + serv_num,
  'https://storage.artisync.test/thumbnails/' || pc.id_perfil || '/serv-' || serv_num || '.webp'
FROM perfiles_creadores pc
CROSS JOIN generate_series(1, 3) AS serv_num
WHERE EXISTS (
  SELECT 1 FROM usuarios u
  WHERE u.id_usuario = pc.id_usuario AND u.correo LIKE '%@artisync-seed.test'
);

DO $$ BEGIN RAISE NOTICE '>>> Asignando subcategorías a servicios...'; END $$;

-- Cada servicio se asigna a 1 subcategoría (modelo N:M desde V37)
INSERT INTO servicio_subcategorias (id_servicio, id_subcategoria)
SELECT s.id_servicio,
  (SELECT id_subcategoria FROM subcategorias ORDER BY id_subcategoria OFFSET s.id_servicio % (SELECT COUNT(*) FROM subcategorias) LIMIT 1)
FROM servicios s
WHERE EXISTS (
  SELECT 1 FROM perfiles_creadores pc
  JOIN usuarios u ON u.id_usuario = pc.id_usuario
  WHERE pc.id_perfil = s.id_perfil AND u.correo LIKE '%@artisync-seed.test'
)
ON CONFLICT (id_servicio, id_subcategoria) DO NOTHING;

DO $$ BEGIN RAISE NOTICE '>>> Insertando etiquetas de servicios...'; END $$;

-- Etiquetas de servicios (2 por servicio = ~60,000)
INSERT INTO servicio_etiquetas (id_servicio, id_etiqueta)
SELECT s.id_servicio, e.id_etiqueta
FROM servicios s
CROSS JOIN LATERAL (
  SELECT id_etiqueta FROM etiquetas
  WHERE id_etiqueta IN (1 + s.id_servicio % 50, 1 + (s.id_servicio + 13) % 50)
  LIMIT 2
) e
WHERE EXISTS (
  SELECT 1 FROM perfiles_creadores pc
  JOIN usuarios u ON u.id_usuario = pc.id_usuario
  WHERE pc.id_perfil = s.id_perfil AND u.correo LIKE '%@artisync-seed.test'
);

DO $$ BEGIN RAISE NOTICE '>>> Insertando atributos de servicios...'; END $$;

-- Atributos de servicios (1 por servicio = ~30,000)
INSERT INTO servicio_atributos (id_servicio, id_atributo, valor_asignado)
SELECT 
  s.id_servicio,
  (SELECT id_atributo FROM atributos_dinamicos ORDER BY id_atributo OFFSET s.id_servicio % (SELECT COUNT(*) FROM atributos_dinamicos) LIMIT 1),
  CASE s.id_servicio % 5
    WHEN 0 THEN '1920x1080'
    WHEN 1 THEN 'RGB / 300 DPI'
    WHEN 2 THEN 'Formato vectorial SVG'
    WHEN 3 THEN '30 segundos'
    ELSE 'Incluido'
  END
FROM servicios s
WHERE EXISTS (
  SELECT 1 FROM perfiles_creadores pc
  JOIN usuarios u ON u.id_usuario = pc.id_usuario
  WHERE pc.id_perfil = s.id_perfil AND u.correo LIKE '%@artisync-seed.test'
);

-- ==============================================================================
-- 6. FLUJOS DE TRABAJO POR CREADOR (necesario por V28)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Asegurando flujos de trabajo por creador...'; END $$;

-- Cada creador necesita al menos un flujo (V28: uk_flujos_trabajo_creador_nombre)
-- Usamos el flujo estándar existente como referencia y creamos uno por creador
INSERT INTO flujos_trabajo (nombre_flujo, descripcion_flujo, id_usuario_creador)
SELECT 
  'Flujo de ' || u.nombres,
  'Flujo de trabajo personalizado del creador ' || u.nombres,
  u.id_usuario
FROM perfiles_creadores pc
JOIN usuarios u ON u.id_usuario = pc.id_usuario
WHERE u.correo LIKE '%@artisync-seed.test'
  AND NOT EXISTS (
    SELECT 1 FROM flujos_trabajo ft WHERE ft.id_usuario_creador = u.id_usuario
  )
ON CONFLICT ON CONSTRAINT uk_flujos_trabajo_creador_nombre DO NOTHING;

-- Asociar etapas a los flujos de creadores (usando las etapas existentes)
INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final, requiere_entregable)
SELECT ft.id_flujo, ef.id_etapa, ef_order.num, ef_order.es_final, ef_order.req_ent
FROM flujos_trabajo ft
CROSS JOIN (
  SELECT e.id_etapa, ROW_NUMBER() OVER (ORDER BY e.id_etapa) AS num,
         CASE WHEN ROW_NUMBER() OVER (ORDER BY e.id_etapa) = (SELECT COUNT(*) FROM etapas_flujo) THEN TRUE ELSE FALSE END AS es_final,
         CASE WHEN e.nombre_etapa = 'En Producción' THEN TRUE ELSE FALSE END AS req_ent
  FROM etapas_flujo e
) ef_order
JOIN etapas_flujo ef ON ef.id_etapa = ef_order.id_etapa
WHERE ft.id_flujo NOT IN (SELECT DISTINCT fec.id_flujo FROM flujo_etapas_config fec)
ON CONFLICT ON CONSTRAINT uk_flujo_etapas_config_unica DO NOTHING;

-- ==============================================================================
-- 7. PEDIDOS (100,000)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando 100,000 pedidos...'; END $$;

-- Clientes hacen pedidos a servicios de creadores.
-- Nota de rendimiento: se precalculan arrays una sola vez (CTEs) y se indexan
-- por posicion en O(1). La version anterior usaba ORDER BY ... OFFSET dentro
-- de un LATERAL evaluado 100,000 veces, con OFFSET de hasta 40,000 -- un
-- patron O(n*m) que en la practica no terminaba en un tiempo razonable
-- contra una base remota.
WITH pool_clientes AS (
  SELECT array_agg(id_usuario ORDER BY id_usuario) AS ids
  FROM usuarios
  WHERE correo LIKE '%@artisync-seed.test' AND id_usuario % 5 != 0
),
pool_servicios AS (
  SELECT
    array_agg(s.id_servicio ORDER BY s.id_servicio) AS ids_servicio,
    array_agg(s.precio_base ORDER BY s.id_servicio) AS precios,
    array_agg(
      (SELECT ft.id_flujo FROM flujos_trabajo ft
       WHERE ft.id_usuario_creador = pc.id_usuario LIMIT 1)
      ORDER BY s.id_servicio
    ) AS ids_flujo
  FROM servicios s
  JOIN perfiles_creadores pc ON pc.id_perfil = s.id_perfil
  JOIN usuarios u ON u.id_usuario = pc.id_usuario
  WHERE u.correo LIKE '%@artisync-seed.test'
)
INSERT INTO pedidos (id_usuario_cliente, id_servicio, id_flujo, fecha_inicio, fecha_entrega_estimada, precio_pactado)
SELECT
  pcli.ids[1 + (i % array_length(pcli.ids, 1))],
  pser.ids_servicio[1 + (i % array_length(pser.ids_servicio, 1))],
  pser.ids_flujo[1 + (i % array_length(pser.ids_flujo, 1))],
  NOW() - (i * 3 + pcli.ids[1 + (i % array_length(pcli.ids, 1))] % 30) * INTERVAL '1 hour',
  NOW() + (7 + i % 30) * INTERVAL '1 day',
  pser.precios[1 + (i % array_length(pser.precios, 1))] + (i % 200)
FROM generate_series(1, 100000) AS i
CROSS JOIN pool_clientes pcli
CROSS JOIN pool_servicios pser;

-- ==============================================================================
-- 8. HISTORIAL DE ESTADOS DE PEDIDO (300,000 — 3 por pedido)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando historial de estados de pedido...'; END $$;

INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
SELECT 
  p.id_pedido,
  (SELECT id_etapa FROM etapas_flujo ORDER BY id_etapa OFFSET (step - 1) LIMIT 1),
  p.fecha_inicio + step * INTERVAL '2 days',
  'Transición automática de prueba - paso ' || step
FROM pedidos p
CROSS JOIN generate_series(1, 3) AS step
WHERE p.id_pedido > (SELECT MIN(id_pedido) FROM pedidos)  -- evitar el primero por si tiene datos
  AND EXISTS (SELECT 1 FROM etapas_flujo OFFSET (step - 1) LIMIT 1);  -- solo si hay suficientes etapas

-- ==============================================================================
-- 9. CONTRATOS (80,000) + PAGOS + TRANSACCIONES
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando contratos...'; END $$;

-- Necesitamos la plantilla de contrato
INSERT INTO contratos (id_pedido, id_plantilla, hash_firma_cliente, hash_firma_creador, limite_revisiones)
SELECT 
  p.id_pedido,
  (SELECT id_plantilla FROM plantillas_contrato ORDER BY id_plantilla DESC LIMIT 1),
  md5('firma-cliente-' || p.id_pedido),
  md5('firma-creador-' || p.id_pedido),
  1 + (p.id_pedido % 3)
FROM pedidos p
WHERE p.id_pedido % 5 IN (0, 1, 2, 3)  -- ~80% de los pedidos
ON CONFLICT (id_pedido) DO NOTHING;

DO $$ BEGIN RAISE NOTICE '>>> Insertando pagos en garantía...'; END $$;

INSERT INTO pagos_garantia (id_contrato, id_orden_paypal, monto_retenido, estado_fondos)
SELECT 
  c.id_contrato,
  'PAYPAL-ORD-' || c.id_contrato || '-' || LEFT(md5(c.id_contrato::TEXT), 8),
  p.precio_pactado,
  CASE c.id_contrato % 4
    WHEN 0 THEN 'Retenido'
    WHEN 1 THEN 'Liberado'
    WHEN 2 THEN 'Retenido'
    ELSE 'Devuelto'
  END
FROM contratos c
JOIN pedidos p ON p.id_pedido = c.id_pedido
ON CONFLICT (id_contrato) DO NOTHING;

DO $$ BEGIN RAISE NOTICE '>>> Insertando transacciones de pago...'; END $$;

INSERT INTO transacciones_pago (id_pago, tipo_transaccion, monto, fecha_ejecucion)
SELECT 
  pg.id_pago,
  CASE pg.estado_fondos
    WHEN 'Liberado' THEN 'LIBERACION'
    WHEN 'Devuelto' THEN 'DEVOLUCION'
    ELSE 'RETENCION'
  END,
  pg.monto_retenido,
  NOW() - (pg.id_pago % 365) * INTERVAL '1 day'
FROM pagos_garantia pg;

-- ==============================================================================
-- 10. SALAS DE CHAT (100,000) + MENSAJES (100,000)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando salas de chat y mensajes...'; END $$;

INSERT INTO salas_chat (id_pedido, sala_activa)
SELECT p.id_pedido, CASE WHEN p.id_pedido % 10 = 0 THEN FALSE ELSE TRUE END
FROM pedidos p
ON CONFLICT (id_pedido) DO NOTHING;

INSERT INTO mensajes (id_sala, id_remitente, cuerpo_mensaje, leido)
SELECT 
  sc.id_sala,
  p.id_usuario_cliente,
  'Mensaje de prueba para el pedido #' || p.id_pedido || '. ' ||
  CASE sc.id_sala % 6
    WHEN 0 THEN 'Hola, quisiera saber el estado de mi pedido.'
    WHEN 1 THEN '¿Podrías enviarme un avance del trabajo?'
    WHEN 2 THEN 'Me gustaría hacer una pequeña modificación al diseño.'
    WHEN 3 THEN 'Excelente trabajo, estoy muy satisfecho.'
    WHEN 4 THEN '¿Cuándo estará listo el entregable final?'
    ELSE 'Gracias por la actualización.'
  END,
  CASE WHEN sc.id_sala % 3 = 0 THEN TRUE ELSE FALSE END
FROM salas_chat sc
JOIN pedidos p ON p.id_pedido = sc.id_pedido;

-- ==============================================================================
-- 11. NOTIFICACIONES (50,000)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando notificaciones del sistema...'; END $$;

WITH pool_usuarios AS (
  SELECT array_agg(id_usuario ORDER BY id_usuario) AS ids
  FROM usuarios WHERE correo LIKE '%@artisync-seed.test'
),
pool_tipos AS (
  SELECT array_agg(id_tipo_notificacion ORDER BY id_tipo_notificacion) AS ids
  FROM tipos_notificacion
)
INSERT INTO notificaciones_sistema (id_usuario, id_tipo_notificacion, esta_leida)
SELECT
  pu.ids[1 + (i % array_length(pu.ids, 1))],
  pt.ids[1 + (i % array_length(pt.ids, 1))],
  CASE WHEN i % 3 = 0 THEN TRUE ELSE FALSE END
FROM generate_series(1, 50000) AS i
CROSS JOIN pool_usuarios pu
CROSS JOIN pool_tipos pt;

-- ==============================================================================
-- 12. SEGUIDORES (50,000)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando seguidores...'; END $$;

WITH pool_seguidores AS (
  SELECT array_agg(id_usuario ORDER BY id_usuario) AS ids
  FROM usuarios WHERE correo LIKE '%@artisync-seed.test' AND id_usuario % 5 != 0
),
pool_creadores AS (
  SELECT array_agg(id_perfil ORDER BY id_perfil) AS ids
  FROM perfiles_creadores
)
INSERT INTO seguidores (id_usuario_seguidor, id_perfil_creador, notificaciones_activas)
SELECT DISTINCT ON (ps.ids[1 + (i % array_length(ps.ids, 1))], pc.ids[1 + (i % array_length(pc.ids, 1))])
  ps.ids[1 + (i % array_length(ps.ids, 1))],
  pc.ids[1 + (i % array_length(pc.ids, 1))],
  CASE WHEN i % 4 = 0 THEN FALSE ELSE TRUE END
FROM generate_series(1, 50000) AS i
CROSS JOIN pool_seguidores ps
CROSS JOIN pool_creadores pc
ON CONFLICT (id_usuario_seguidor, id_perfil_creador) DO NOTHING;

-- ==============================================================================
-- 13. COMENTARIOS Y LIKES EN PORTAFOLIO (60,000 total)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando comentarios y likes...'; END $$;

WITH pool_items AS (
  SELECT array_agg(id_item_portafolio ORDER BY id_item_portafolio) AS ids
  FROM portafolio_items
),
pool_usuarios AS (
  SELECT array_agg(id_usuario ORDER BY id_usuario) AS ids
  FROM usuarios WHERE correo LIKE '%@artisync-seed.test'
)
INSERT INTO comentarios_portafolio (id_item_portafolio, id_usuario_autor, texto_comentario, estado_moderacion)
SELECT
  pi.ids[1 + (i % array_length(pi.ids, 1))],
  pu.ids[1 + (i % array_length(pu.ids, 1))],
  CASE i % 8
    WHEN 0 THEN '¡Increíble trabajo! Me encanta la composición.'
    WHEN 1 THEN 'Los colores son espectaculares, gran técnica.'
    WHEN 2 THEN 'Me inspira mucho tu estilo artístico.'
    WHEN 3 THEN '¿Qué herramientas usaste para este trabajo?'
    WHEN 4 THEN 'Hermoso, ¿aceptas encargos personalizados?'
    WHEN 5 THEN 'Nivel profesional, se nota la experiencia.'
    WHEN 6 THEN 'Me gustaría aprender esta técnica.'
    ELSE 'Excelente obra, muy creativa.'
  END,
  CASE WHEN i % 50 = 0 THEN 'Censurado' ELSE 'Activo' END
FROM generate_series(1, 30000) AS i
CROSS JOIN pool_items pi
CROSS JOIN pool_usuarios pu;

WITH pool_items AS (
  SELECT array_agg(id_item_portafolio ORDER BY id_item_portafolio) AS ids
  FROM portafolio_items
),
pool_usuarios AS (
  SELECT array_agg(id_usuario ORDER BY id_usuario) AS ids
  FROM usuarios WHERE correo LIKE '%@artisync-seed.test'
)
INSERT INTO likes_portafolio (id_item_portafolio, id_usuario)
SELECT DISTINCT ON (pi.ids[1 + (i % array_length(pi.ids, 1))], pu.ids[1 + ((i * 7) % array_length(pu.ids, 1))])
  pi.ids[1 + (i % array_length(pi.ids, 1))],
  pu.ids[1 + ((i * 7) % array_length(pu.ids, 1))]
FROM generate_series(1, 30000) AS i
CROSS JOIN pool_items pi
CROSS JOIN pool_usuarios pu
ON CONFLICT (id_item_portafolio, id_usuario) DO NOTHING;

-- ==============================================================================
-- 14. RESEÑAS DE SERVICIOS (50,000)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando reseñas de servicios...'; END $$;

INSERT INTO resenas_servicios (id_pedido, calificacion_estrellas, texto_resena)
SELECT 
  p.id_pedido,
  1 + (p.id_pedido % 5),  -- 1 a 5 estrellas
  CASE (p.id_pedido % 5)
    WHEN 0 THEN 'Servicio excepcional, superó mis expectativas. Muy recomendado.'
    WHEN 1 THEN 'Buen trabajo, entrega a tiempo y buena comunicación.'
    WHEN 2 THEN 'Calidad aceptable, pero podría mejorar en detalles.'
    WHEN 3 THEN 'Excelente experiencia, volveré a contratar sin duda.'
    ELSE 'El resultado final es justo lo que necesitaba.'
  END
FROM pedidos p
WHERE p.id_pedido % 2 = 0  -- ~50% de los pedidos tienen reseña
ON CONFLICT (id_pedido) DO NOTHING;

-- ==============================================================================
-- 15. SORTEOS (1,000) + PARTICIPANTES (20,000)
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando sorteos y participantes...'; END $$;

-- Nota: desde V41 el premio es una entidad individual (premios_sorteo), no un
-- campo de texto libre en sorteos.
INSERT INTO sorteos (id_perfil_creador, titulo_sorteo, cantidad_ganadores,
                     fecha_inicio, fecha_cierre, estado_sorteo, requiere_seguidor)
SELECT
  pc.id_perfil,
  'Sorteo ' || ROW_NUMBER() OVER (ORDER BY pc.id_perfil) || ' - ' ||
    CASE (pc.id_perfil % 5)
      WHEN 0 THEN 'Diseño gratis'
      WHEN 1 THEN 'Comisión personalizada'
      WHEN 2 THEN 'Pack de assets'
      WHEN 3 THEN 'Mentoría 1-a-1'
      ELSE 'Descuento exclusivo'
    END,
  1 + pc.id_perfil % 3,
  NOW() - (pc.id_perfil % 60) * INTERVAL '1 day',
  NOW() + (30 + pc.id_perfil % 90) * INTERVAL '1 day',
  CASE WHEN pc.id_perfil % 5 = 0 THEN 'Cerrado' ELSE 'Activo' END,
  CASE WHEN pc.id_perfil % 3 = 0 THEN TRUE ELSE FALSE END
FROM perfiles_creadores pc
WHERE EXISTS (
  SELECT 1 FROM usuarios u
  WHERE u.id_usuario = pc.id_usuario AND u.correo LIKE '%@artisync-seed.test'
)
AND pc.id_perfil % 10 = 0  -- ~10% de los creadores tienen sorteo = ~1,000
;

DO $$ BEGIN RAISE NOTICE '>>> Insertando premios de sorteo...'; END $$;

-- Un premio individual por cada "ganador" configurado (modelo desde V41).
-- Se excluyen sorteos que ya tuvieran premios (preexistentes antes del seed,
-- p.ej. datos de demo) para no chocar con su UNIQUE (id_sorteo, orden).
INSERT INTO premios_sorteo (id_sorteo, descripcion_premio, orden)
SELECT s.id_sorteo,
  'Premio: servicio digital valorado en $' || (50 + s.id_perfil_creador % 500) || ' USD.',
  gs.orden
FROM sorteos s
CROSS JOIN LATERAL generate_series(1, s.cantidad_ganadores) AS gs(orden)
WHERE NOT EXISTS (SELECT 1 FROM premios_sorteo ps WHERE ps.id_sorteo = s.id_sorteo);

WITH pool_usuarios AS (
  SELECT array_agg(id_usuario ORDER BY id_usuario) AS ids
  FROM usuarios WHERE correo LIKE '%@artisync-seed.test' AND id_usuario % 5 != 0
)
INSERT INTO participantes_sorteo (id_sorteo, id_usuario, es_ganador)
SELECT DISTINCT ON (s.id_sorteo, pu.ids[1 + ((s.id_sorteo * 7 + i) % array_length(pu.ids, 1))])
  s.id_sorteo,
  pu.ids[1 + ((s.id_sorteo * 7 + i) % array_length(pu.ids, 1))],
  CASE WHEN i <= s.cantidad_ganadores THEN TRUE ELSE FALSE END
FROM sorteos s
CROSS JOIN generate_series(1, 20) AS i
CROSS JOIN pool_usuarios pu
WHERE EXISTS (
  SELECT 1 FROM perfiles_creadores pc
  JOIN usuarios u ON u.id_usuario = pc.id_usuario
  WHERE pc.id_perfil = s.id_perfil_creador AND u.correo LIKE '%@artisync-seed.test'
)
ON CONFLICT (id_sorteo, id_usuario) DO NOTHING;

-- ==============================================================================
-- 16. ENTREGABLES FINALES
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando entregables finales...'; END $$;

INSERT INTO entregables_finales (id_pedido, url_version_marca_agua, url_version_limpia, esta_liberado)
SELECT 
  p.id_pedido,
  'https://storage.artisync.test/entregables/' || p.id_pedido || '/watermark.webp',
  'https://storage.artisync.test/entregables/' || p.id_pedido || '/clean.webp',
  CASE WHEN p.id_pedido % 3 = 0 THEN TRUE ELSE FALSE END
FROM pedidos p
WHERE p.id_pedido % 4 IN (0, 1);  -- ~50% de los pedidos tienen entregable

-- ==============================================================================
-- 17. TICKETS DE REVISIÓN
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando tickets de revisión...'; END $$;

INSERT INTO tickets_revision (id_pedido, id_motivo, descripcion_cliente, costo_adicional_generado, estado_ticket)
SELECT 
  p.id_pedido,
  (SELECT id_motivo FROM motivos_rechazo ORDER BY id_motivo OFFSET p.id_pedido % (SELECT COUNT(*) FROM motivos_rechazo) LIMIT 1),
  'Solicitud de revisión: necesito ajustes en el entregable del pedido #' || p.id_pedido,
  CASE WHEN p.id_pedido % 3 = 0 THEN (p.precio_pactado * 0.15)::DECIMAL(10,2) ELSE 0.00 END,
  CASE p.id_pedido % 3
    WHEN 0 THEN 'Abierto'
    WHEN 1 THEN 'Resuelto'
    ELSE 'Cerrado'
  END
FROM pedidos p
WHERE p.id_pedido % 8 = 0;  -- ~12.5% de los pedidos tienen ticket

-- ==============================================================================
-- 18. INFRACCIONES DE MENSAJE
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Insertando infracciones de mensaje...'; END $$;

INSERT INTO infracciones_mensaje (id_usuario, id_pedido, mensaje_original, patron_detectado)
SELECT 
  p.id_usuario_cliente,
  p.id_pedido,
  'Mensaje con contenido inapropiado detectado automáticamente.',
  CASE p.id_pedido % 4
    WHEN 0 THEN 'CONTACTO_EXTERNO'
    WHEN 1 THEN 'LENGUAJE_OFENSIVO'
    WHEN 2 THEN 'SPAM'
    ELSE 'PAGO_FUERA_PLATAFORMA'
  END
FROM pedidos p
WHERE p.id_pedido % 100 = 0;  -- ~1% de los pedidos generan infracción

-- ==============================================================================
-- VERIFICACIÓN FINAL: CONTEO DE REGISTROS
-- ==============================================================================
DO $$ BEGIN RAISE NOTICE '>>> Conteo final de registros:'; END $$;

DO $$
DECLARE
  total BIGINT := 0;
  cnt BIGINT;
BEGIN
  -- Tablas principales
  SELECT COUNT(*) INTO cnt FROM usuarios;           RAISE NOTICE '  usuarios:                    %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM usuario_roles;      RAISE NOTICE '  usuario_roles:               %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM perfiles_creadores;  RAISE NOTICE '  perfiles_creadores:           %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM portafolios;         RAISE NOTICE '  portafolios:                  %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM portafolio_items;    RAISE NOTICE '  portafolio_items:             %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM habilidades;         RAISE NOTICE '  habilidades:                  %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM creador_habilidades; RAISE NOTICE '  creador_habilidades:          %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM servicios;           RAISE NOTICE '  servicios:                    %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM servicio_etiquetas;  RAISE NOTICE '  servicio_etiquetas:           %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM servicio_atributos;  RAISE NOTICE '  servicio_atributos:           %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM flujos_trabajo;      RAISE NOTICE '  flujos_trabajo:               %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM pedidos;             RAISE NOTICE '  pedidos:                      %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM historial_estados_pedido; RAISE NOTICE '  historial_estados_pedido:     %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM contratos;           RAISE NOTICE '  contratos:                    %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM pagos_garantia;      RAISE NOTICE '  pagos_garantia:               %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM transacciones_pago;  RAISE NOTICE '  transacciones_pago:           %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM salas_chat;          RAISE NOTICE '  salas_chat:                   %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM mensajes;            RAISE NOTICE '  mensajes:                     %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM notificaciones_sistema; RAISE NOTICE '  notificaciones_sistema:       %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM seguidores;          RAISE NOTICE '  seguidores:                   %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM comentarios_portafolio; RAISE NOTICE '  comentarios_portafolio:       %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM likes_portafolio;    RAISE NOTICE '  likes_portafolio:             %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM resenas_servicios;   RAISE NOTICE '  resenas_servicios:            %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM sorteos;             RAISE NOTICE '  sorteos:                      %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM participantes_sorteo; RAISE NOTICE '  participantes_sorteo:         %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM entregables_finales; RAISE NOTICE '  entregables_finales:          %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM tickets_revision;    RAISE NOTICE '  tickets_revision:             %', cnt; total := total + cnt;
  SELECT COUNT(*) INTO cnt FROM infracciones_mensaje; RAISE NOTICE '  infracciones_mensaje:         %', cnt; total := total + cnt;
  
  RAISE NOTICE '  ─────────────────────────────────────';
  RAISE NOTICE '  TOTAL REGISTROS:              %', total;
END $$;

COMMIT;

DO $$ BEGIN RAISE NOTICE '✅ Seed masivo completado exitosamente.'; END $$;
