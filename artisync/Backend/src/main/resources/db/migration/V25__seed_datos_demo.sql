-- ==============================================================================
-- MIGRACIÓN V24: DATOS SEMILLA DE DEMOSTRACIÓN PARA TODA LA BASE DE DATOS
-- ==============================================================================
--
-- A diferencia de V1 (roles/permisos/países/admin) y V13 (plantilla de
-- contrato), esta migración puebla el resto del modelo de datos con un
-- escenario de demostración completo y coherente: usuarios de cada rol,
-- perfiles y portafolios de creador, catálogo de servicios, pedidos en
-- distintos estados, contratos, pagos en garantía, chat, notificaciones,
-- red social (seguidores, comentarios, likes, reseñas, sorteos) y
-- verificación asistida por IA.
--
-- ADVERTENCIA: al vivir en db/migration, Flyway la aplica en TODOS los
-- entornos que usan este classpath (dev, medición y también Azure si algún
-- día se despliega este mismo jar contra una base real) — spring.flyway.locations
-- no distingue entorno (ver application.properties). Los usuarios creados
-- usan correos *@artisync.demo claramente ficticios (salvo tres cuentas de
-- staff *@artisync.com) y una contraseña compartida y documentada, nunca
-- datos de una persona real.
--
-- Es IDEMPOTENTE (ON CONFLICT / IF NOT EXISTS en cada inserción): reejecutar
-- la migración —o restaurar un baseline— no duplica filas.
--
-- Usuarios de demostración (todos comparten contraseña):
--   contraseña: Artisync2026!
--   hash BCrypt costo 12 (igual que AuthConfig.java: new BCryptPasswordEncoder(12))
--
--   Staff:
--     moderador@artisync.com   (rol MODERADOR)
--     soporte@artisync.com     (rol SOPORTE)
--     auditor@artisync.com     (rol AUDITOR_FINANCIERO)
--   Creadores:
--     valentina.reyes@artisync.demo  — Ilustradora & Concept Artist
--     mateo.fernandez@artisync.demo  — Diseñador Gráfico & Motion Artist
--     sofia.navarro@artisync.demo    — Modeladora 3D & Animadora
--   Clientes:
--     carlos.mendoza@artisync.demo
--     lucia.paredes@artisync.demo
--     diego.salinas@artisync.demo
--
-- Deliberadamente NO se siembran (son datos de sesión/seguridad efímeros, sin
-- valor como fixture estático): sesiones_usuario, tokens_recuperacion,
-- autenticacion_dos_factores, codigos_respaldo_2fa.
-- ==============================================================================

DO $$
DECLARE
    -- Roles
    v_rol_creador   BIGINT;
    v_rol_cliente   BIGINT;
    v_rol_moderador BIGINT;
    v_rol_soporte   BIGINT;
    v_rol_auditor   BIGINT;

    -- Usuarios (staff, creadores, clientes)
    v_usr_moderador BIGINT;
    v_usr_soporte   BIGINT;
    v_usr_auditor   BIGINT;
    v_usr_valentina BIGINT;
    v_usr_mateo     BIGINT;
    v_usr_sofia     BIGINT;
    v_usr_carlos    BIGINT;
    v_usr_lucia     BIGINT;
    v_usr_diego     BIGINT;

    -- Perfiles de creador
    v_perfil_valentina BIGINT;
    v_perfil_mateo     BIGINT;
    v_perfil_sofia     BIGINT;

    -- Portafolios y sus ítems
    v_portafolio_valentina BIGINT;
    v_portafolio_mateo     BIGINT;
    v_portafolio_sofia     BIGINT;
    v_item_valentina_1 BIGINT;
    v_item_valentina_2 BIGINT;
    v_item_mateo_1     BIGINT;
    v_item_sofia_1     BIGINT;

    -- Categorías / subcategorías
    v_cat_diseno       BIGINT;
    v_cat_ilustracion  BIGINT;
    v_cat_animacion    BIGINT;
    v_cat_3d           BIGINT;
    v_cat_audio        BIGINT;
    v_sub_logotipos    BIGINT;
    v_sub_identidad    BIGINT;
    v_sub_retratos     BIGINT;
    v_sub_conceptart   BIGINT;
    v_sub_2d           BIGINT;
    v_sub_motion       BIGINT;
    v_sub_personajes   BIGINT;
    v_sub_props        BIGINT;
    v_sub_composicion  BIGINT;
    v_sub_locucion     BIGINT;

    -- Flujos de trabajo y etapas
    v_flujo_estandar  BIGINT;
    v_flujo_express   BIGINT;
    v_etapa_borrador  BIGINT;
    v_etapa_produccion BIGINT;
    v_etapa_revision  BIGINT;
    v_etapa_entrega   BIGINT;

    -- Servicios
    v_srv_retrato    BIGINT;
    v_srv_conceptart BIGINT;
    v_srv_logo       BIGINT;
    v_srv_manual     BIGINT;
    v_srv_modelo3d   BIGINT;
    v_srv_prop3d     BIGINT;

    -- Atributos dinámicos
    v_attr_formato    BIGINT;
    v_attr_tiempo     BIGINT;
    v_attr_resolucion BIGINT;

    -- Motivos de rechazo
    v_motivo1 BIGINT;

    -- Pedidos
    v_pedido1 BIGINT; -- Carlos  -> Retrato (completado, con reseña)
    v_pedido2 BIGINT; -- Lucía   -> Logotipo (en producción)
    v_pedido3 BIGINT; -- Diego   -> Modelo 3D (recién iniciado, sin contrato aún)
    v_pedido4 BIGINT; -- Carlos  -> Manual de identidad (con ticket, completado)

    -- Contratos / pagos
    v_plantilla BIGINT;
    v_contrato1 BIGINT;
    v_contrato2 BIGINT;
    v_contrato4 BIGINT;
    v_pago1 BIGINT;
    v_pago2 BIGINT;
    v_pago4 BIGINT;

    -- Comunicación
    v_sala1 BIGINT;
    v_sala2 BIGINT;
    v_sala3 BIGINT;
    v_sala4 BIGINT;
    v_mensaje2 BIGINT;

    -- Tipos de notificación
    v_tipo_mensaje      BIGINT;
    v_tipo_pedido       BIGINT;
    v_tipo_pago         BIGINT;
    v_tipo_seguidor     BIGINT;
    v_tipo_certificado  BIGINT;
    v_tipo_sorteo       BIGINT;

    -- Briefing
    v_briefing_plantilla BIGINT;
    v_briefing_p1 BIGINT;
    v_briefing_p2 BIGINT;
    v_briefing_p3 BIGINT;
    v_briefing_enviado BIGINT;

    -- Sorteos
    v_sorteo1 BIGINT;
    v_sorteo2 BIGINT;

    -- Estados de verificación (ya sembrados por V7)
    v_estado_pendiente BIGINT;
    v_estado_aprobado  BIGINT;

    -- Hash de contraseña demo: "Artisync2026!" (bcrypt costo 12)
    v_hash_demo CONSTANT VARCHAR := '$2a$12$vpFFPFxgQEwEyNpQ5NUWn.NrP50kVyrUDPqeUc2rwrPy9AYqZ8u7C';
BEGIN

    -- ==========================================================================
    -- 0. Verificación de requisitos previos
    -- ==========================================================================
    SELECT id_rol INTO v_rol_creador   FROM roles WHERE nombre_rol = 'CREADOR';
    SELECT id_rol INTO v_rol_cliente   FROM roles WHERE nombre_rol = 'CLIENTE';
    SELECT id_rol INTO v_rol_moderador FROM roles WHERE nombre_rol = 'MODERADOR';
    SELECT id_rol INTO v_rol_soporte   FROM roles WHERE nombre_rol = 'SOPORTE';
    SELECT id_rol INTO v_rol_auditor   FROM roles WHERE nombre_rol = 'AUDITOR_FINANCIERO';

    IF v_rol_creador IS NULL OR v_rol_cliente IS NULL OR v_rol_moderador IS NULL
       OR v_rol_soporte IS NULL OR v_rol_auditor IS NULL THEN
        RAISE EXCEPTION 'V24: faltan roles base sembrados por V1__schema_inicial.sql.';
    END IF;

    SELECT id_plantilla INTO v_plantilla FROM plantillas_contrato WHERE version_legal = 'v1.0';
    IF v_plantilla IS NULL THEN
        RAISE EXCEPTION 'V24: falta la plantilla de contrato v1.0 sembrada por V13__seed_plantilla_contrato.sql.';
    END IF;

    SELECT id_estado_verificacion INTO v_estado_pendiente FROM estados_verificacion WHERE nombre_estado = 'PENDIENTE';
    SELECT id_estado_verificacion INTO v_estado_aprobado  FROM estados_verificacion WHERE nombre_estado = 'APROBADO';
    IF v_estado_pendiente IS NULL OR v_estado_aprobado IS NULL THEN
        RAISE EXCEPTION 'V24: falta estados_verificacion sembrado por V7__verificacion_asistida_ia.sql.';
    END IF;

    -- ==========================================================================
    -- 1. USUARIOS DEMO (staff, creadores, clientes)
    -- ==========================================================================
    SELECT id_usuario INTO v_usr_moderador FROM usuarios WHERE correo = 'moderador@artisync.com';
    IF v_usr_moderador IS NULL THEN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, fecha_nacimiento, estado_cuenta)
        VALUES ('Ana', 'Torres', 'moderador@artisync.com', v_hash_demo, '1992-03-14', true)
        RETURNING id_usuario INTO v_usr_moderador;
    END IF;
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT v_usr_moderador, v_rol_moderador
    WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = v_usr_moderador AND id_rol = v_rol_moderador);

    SELECT id_usuario INTO v_usr_soporte FROM usuarios WHERE correo = 'soporte@artisync.com';
    IF v_usr_soporte IS NULL THEN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, fecha_nacimiento, estado_cuenta)
        VALUES ('Luis', 'Vera', 'soporte@artisync.com', v_hash_demo, '1990-07-02', true)
        RETURNING id_usuario INTO v_usr_soporte;
    END IF;
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT v_usr_soporte, v_rol_soporte
    WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = v_usr_soporte AND id_rol = v_rol_soporte);

    SELECT id_usuario INTO v_usr_auditor FROM usuarios WHERE correo = 'auditor@artisync.com';
    IF v_usr_auditor IS NULL THEN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, fecha_nacimiento, estado_cuenta)
        VALUES ('Marta', 'Ríos', 'auditor@artisync.com', v_hash_demo, '1988-11-30', true)
        RETURNING id_usuario INTO v_usr_auditor;
    END IF;
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT v_usr_auditor, v_rol_auditor
    WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = v_usr_auditor AND id_rol = v_rol_auditor);

    -- Creadores
    SELECT id_usuario INTO v_usr_valentina FROM usuarios WHERE correo = 'valentina.reyes@artisync.demo';
    IF v_usr_valentina IS NULL THEN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, id_pais, fecha_nacimiento, estado_cuenta)
        SELECT 'Valentina', 'Reyes', 'valentina.reyes@artisync.demo', v_hash_demo, p.id_pais, '1997-05-22', true
        FROM pais p WHERE p.nombre_pais = 'Colombia'
        RETURNING id_usuario INTO v_usr_valentina;
    END IF;
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT v_usr_valentina, v_rol_creador
    WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = v_usr_valentina AND id_rol = v_rol_creador);

    SELECT id_usuario INTO v_usr_mateo FROM usuarios WHERE correo = 'mateo.fernandez@artisync.demo';
    IF v_usr_mateo IS NULL THEN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, id_pais, fecha_nacimiento, estado_cuenta)
        SELECT 'Mateo', 'Fernández', 'mateo.fernandez@artisync.demo', v_hash_demo, p.id_pais, '1994-09-10', true
        FROM pais p WHERE p.nombre_pais = 'Argentina'
        RETURNING id_usuario INTO v_usr_mateo;
    END IF;
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT v_usr_mateo, v_rol_creador
    WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = v_usr_mateo AND id_rol = v_rol_creador);

    SELECT id_usuario INTO v_usr_sofia FROM usuarios WHERE correo = 'sofia.navarro@artisync.demo';
    IF v_usr_sofia IS NULL THEN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, id_pais, fecha_nacimiento, estado_cuenta)
        SELECT 'Sofía', 'Navarro', 'sofia.navarro@artisync.demo', v_hash_demo, p.id_pais, '1999-01-18', true
        FROM pais p WHERE p.nombre_pais = 'México'
        RETURNING id_usuario INTO v_usr_sofia;
    END IF;
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT v_usr_sofia, v_rol_creador
    WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = v_usr_sofia AND id_rol = v_rol_creador);

    -- Clientes
    SELECT id_usuario INTO v_usr_carlos FROM usuarios WHERE correo = 'carlos.mendoza@artisync.demo';
    IF v_usr_carlos IS NULL THEN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, id_pais, fecha_nacimiento, estado_cuenta)
        SELECT 'Carlos', 'Mendoza', 'carlos.mendoza@artisync.demo', v_hash_demo, p.id_pais, '1993-04-05', true
        FROM pais p WHERE p.nombre_pais = 'Perú'
        RETURNING id_usuario INTO v_usr_carlos;
    END IF;
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT v_usr_carlos, v_rol_cliente
    WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = v_usr_carlos AND id_rol = v_rol_cliente);

    SELECT id_usuario INTO v_usr_lucia FROM usuarios WHERE correo = 'lucia.paredes@artisync.demo';
    IF v_usr_lucia IS NULL THEN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, id_pais, fecha_nacimiento, estado_cuenta)
        SELECT 'Lucía', 'Paredes', 'lucia.paredes@artisync.demo', v_hash_demo, p.id_pais, '1996-08-27', true
        FROM pais p WHERE p.nombre_pais = 'Chile'
        RETURNING id_usuario INTO v_usr_lucia;
    END IF;
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT v_usr_lucia, v_rol_cliente
    WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = v_usr_lucia AND id_rol = v_rol_cliente);

    SELECT id_usuario INTO v_usr_diego FROM usuarios WHERE correo = 'diego.salinas@artisync.demo';
    IF v_usr_diego IS NULL THEN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, id_pais, fecha_nacimiento, estado_cuenta)
        SELECT 'Diego', 'Salinas', 'diego.salinas@artisync.demo', v_hash_demo, p.id_pais, '1998-12-12', true
        FROM pais p WHERE p.nombre_pais = 'Ecuador'
        RETURNING id_usuario INTO v_usr_diego;
    END IF;
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT v_usr_diego, v_rol_cliente
    WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = v_usr_diego AND id_rol = v_rol_cliente);

    -- ==========================================================================
    -- 2. PERFILES DE CREADOR, HABILIDADES, PORTAFOLIOS E ÍTEMS
    -- ==========================================================================
    SELECT id_perfil INTO v_perfil_valentina FROM perfiles_creadores WHERE id_usuario = v_usr_valentina;
    IF v_perfil_valentina IS NULL THEN
        INSERT INTO perfiles_creadores (id_usuario, biografia, url_red_social, titulo_profesional)
        VALUES (v_usr_valentina,
                'Ilustradora digital especializada en retratos y concept art, con más de 6 años de experiencia en proyectos editoriales e independientes.',
                'https://instagram.com/valentina.art',
                'Ilustradora & Concept Artist')
        RETURNING id_perfil INTO v_perfil_valentina;
    END IF;

    SELECT id_perfil INTO v_perfil_mateo FROM perfiles_creadores WHERE id_usuario = v_usr_mateo;
    IF v_perfil_mateo IS NULL THEN
        INSERT INTO perfiles_creadores (id_usuario, biografia, url_red_social, titulo_profesional)
        VALUES (v_usr_mateo,
                'Diseñador gráfico y motion artist enfocado en identidad de marca para pequeñas empresas y creadores de contenido.',
                'https://behance.net/mateo.fernandez',
                'Diseñador Gráfico & Motion Artist')
        RETURNING id_perfil INTO v_perfil_mateo;
    END IF;

    SELECT id_perfil INTO v_perfil_sofia FROM perfiles_creadores WHERE id_usuario = v_usr_sofia;
    IF v_perfil_sofia IS NULL THEN
        INSERT INTO perfiles_creadores (id_usuario, biografia, url_red_social, titulo_profesional)
        VALUES (v_usr_sofia,
                'Modeladora y animadora 3D especializada en personajes y props para videojuegos indie.',
                'https://artstation.com/sofia.navarro',
                'Modeladora 3D & Animadora')
        RETURNING id_perfil INTO v_perfil_sofia;
    END IF;

    -- Catálogo de habilidades
    INSERT INTO habilidades (nombre_habilidad) VALUES
        ('Ilustración Digital'), ('Diseño de Personajes'), ('Branding'), ('Motion Graphics'),
        ('Modelado 3D'), ('Rigging'), ('Animación 2D'), ('Composición Musical'),
        ('Edición de Video'), ('Concept Art')
    ON CONFLICT (nombre_habilidad) DO NOTHING;

    INSERT INTO creador_habilidades (id_perfil, id_habilidad, nivel_dominio)
    SELECT v_perfil_valentina, h.id_habilidad, x.nivel
    FROM habilidades h
    JOIN (VALUES ('Ilustración Digital','Experto'), ('Concept Art','Avanzado'), ('Diseño de Personajes','Avanzado')) AS x(nombre, nivel)
      ON x.nombre = h.nombre_habilidad
    WHERE NOT EXISTS (SELECT 1 FROM creador_habilidades ch WHERE ch.id_perfil = v_perfil_valentina AND ch.id_habilidad = h.id_habilidad);

    INSERT INTO creador_habilidades (id_perfil, id_habilidad, nivel_dominio)
    SELECT v_perfil_mateo, h.id_habilidad, x.nivel
    FROM habilidades h
    JOIN (VALUES ('Branding','Experto'), ('Motion Graphics','Avanzado'), ('Ilustración Digital','Intermedio')) AS x(nombre, nivel)
      ON x.nombre = h.nombre_habilidad
    WHERE NOT EXISTS (SELECT 1 FROM creador_habilidades ch WHERE ch.id_perfil = v_perfil_mateo AND ch.id_habilidad = h.id_habilidad);

    INSERT INTO creador_habilidades (id_perfil, id_habilidad, nivel_dominio)
    SELECT v_perfil_sofia, h.id_habilidad, x.nivel
    FROM habilidades h
    JOIN (VALUES ('Modelado 3D','Experto'), ('Rigging','Avanzado'), ('Animación 2D','Intermedio')) AS x(nombre, nivel)
      ON x.nombre = h.nombre_habilidad
    WHERE NOT EXISTS (SELECT 1 FROM creador_habilidades ch WHERE ch.id_perfil = v_perfil_sofia AND ch.id_habilidad = h.id_habilidad);

    -- Portafolios (opciones_personalizacion es JSONB desde V6, ya sin color_plantilla)
    SELECT id_portafolio INTO v_portafolio_valentina FROM portafolios WHERE id_perfil = v_perfil_valentina;
    IF v_portafolio_valentina IS NULL THEN
        INSERT INTO portafolios (id_perfil, total_visitas_acumuladas, es_publico, opciones_personalizacion)
        VALUES (v_perfil_valentina, 1240, true,
                jsonb_build_object('primary','#7C3AED','secondary','#6c757d','bg','#f8f9fa','text','#212529','surface','#ffffff'))
        RETURNING id_portafolio INTO v_portafolio_valentina;
    END IF;

    SELECT id_portafolio INTO v_portafolio_mateo FROM portafolios WHERE id_perfil = v_perfil_mateo;
    IF v_portafolio_mateo IS NULL THEN
        INSERT INTO portafolios (id_perfil, total_visitas_acumuladas, es_publico, opciones_personalizacion)
        VALUES (v_perfil_mateo, 860, true,
                jsonb_build_object('primary','#0EA5E9','secondary','#334155','bg','#f1f5f9','text','#0f172a','surface','#ffffff'))
        RETURNING id_portafolio INTO v_portafolio_mateo;
    END IF;

    SELECT id_portafolio INTO v_portafolio_sofia FROM portafolios WHERE id_perfil = v_perfil_sofia;
    IF v_portafolio_sofia IS NULL THEN
        INSERT INTO portafolios (id_perfil, total_visitas_acumuladas, es_publico, opciones_personalizacion)
        VALUES (v_perfil_sofia, 430, true,
                jsonb_build_object('primary','#F97316','secondary','#57534e','bg','#fafaf9','text','#1c1917','surface','#ffffff'))
        RETURNING id_portafolio INTO v_portafolio_sofia;
    END IF;

    -- Ítems de portafolio (sin UNIQUE natural: se guarda por combinación titulo+portafolio)
    INSERT INTO portafolio_items (id_portafolio, titulo_obra, descripcion_obra, url_archivo_multimedia)
    SELECT v_portafolio_valentina, 'Retrato realista - Comisión privada', 'Retrato a color por encargo, técnica digital mixta.', 'https://picsum.photos/seed/valentina1/900/700'
    WHERE NOT EXISTS (SELECT 1 FROM portafolio_items WHERE id_portafolio = v_portafolio_valentina AND titulo_obra = 'Retrato realista - Comisión privada');
    SELECT id_item_portafolio INTO v_item_valentina_1 FROM portafolio_items WHERE id_portafolio = v_portafolio_valentina AND titulo_obra = 'Retrato realista - Comisión privada';

    INSERT INTO portafolio_items (id_portafolio, titulo_obra, descripcion_obra, url_archivo_multimedia)
    SELECT v_portafolio_valentina, 'Concept art - Guerrera del bosque', 'Diseño de personaje original para proyecto de fantasía.', 'https://picsum.photos/seed/valentina2/900/700'
    WHERE NOT EXISTS (SELECT 1 FROM portafolio_items WHERE id_portafolio = v_portafolio_valentina AND titulo_obra = 'Concept art - Guerrera del bosque');
    SELECT id_item_portafolio INTO v_item_valentina_2 FROM portafolio_items WHERE id_portafolio = v_portafolio_valentina AND titulo_obra = 'Concept art - Guerrera del bosque';

    INSERT INTO portafolio_items (id_portafolio, titulo_obra, descripcion_obra, url_archivo_multimedia)
    SELECT v_portafolio_mateo, 'Identidad visual - Café Aroma', 'Logotipo y manual de marca para cafetería local.', 'https://picsum.photos/seed/mateo1/900/700'
    WHERE NOT EXISTS (SELECT 1 FROM portafolio_items WHERE id_portafolio = v_portafolio_mateo AND titulo_obra = 'Identidad visual - Café Aroma');
    SELECT id_item_portafolio INTO v_item_mateo_1 FROM portafolio_items WHERE id_portafolio = v_portafolio_mateo AND titulo_obra = 'Identidad visual - Café Aroma';

    INSERT INTO portafolio_items (id_portafolio, titulo_obra, descripcion_obra, url_archivo_multimedia)
    SELECT v_portafolio_sofia, 'Personaje 3D - Explorador espacial', 'Modelo low-poly listo para videojuego, con texturizado PBR.', 'https://picsum.photos/seed/sofia1/900/700'
    WHERE NOT EXISTS (SELECT 1 FROM portafolio_items WHERE id_portafolio = v_portafolio_sofia AND titulo_obra = 'Personaje 3D - Explorador espacial');
    SELECT id_item_portafolio INTO v_item_sofia_1 FROM portafolio_items WHERE id_portafolio = v_portafolio_sofia AND titulo_obra = 'Personaje 3D - Explorador espacial';

    -- ==========================================================================
    -- 3. CATÁLOGO: CATEGORÍAS, SUBCATEGORÍAS Y FLUJOS DE TRABAJO (RF-19)
    -- ==========================================================================
    INSERT INTO categorias (nombre_categoria, estado_activa) VALUES
        ('Diseño Gráfico', true), ('Ilustración Digital', true), ('Animación', true),
        ('Modelado 3D', true), ('Producción de Audio', true)
    ON CONFLICT (nombre_categoria) DO NOTHING;

    SELECT id_categoria INTO v_cat_diseno      FROM categorias WHERE nombre_categoria = 'Diseño Gráfico';
    SELECT id_categoria INTO v_cat_ilustracion FROM categorias WHERE nombre_categoria = 'Ilustración Digital';
    SELECT id_categoria INTO v_cat_animacion   FROM categorias WHERE nombre_categoria = 'Animación';
    SELECT id_categoria INTO v_cat_3d          FROM categorias WHERE nombre_categoria = 'Modelado 3D';
    SELECT id_categoria INTO v_cat_audio       FROM categorias WHERE nombre_categoria = 'Producción de Audio';

    INSERT INTO subcategorias (id_categoria, nombre_subcategoria)
    SELECT v_cat_diseno, 'Logotipos' WHERE NOT EXISTS (SELECT 1 FROM subcategorias WHERE id_categoria = v_cat_diseno AND nombre_subcategoria = 'Logotipos');
    SELECT id_subcategoria INTO v_sub_logotipos FROM subcategorias WHERE id_categoria = v_cat_diseno AND nombre_subcategoria = 'Logotipos';

    INSERT INTO subcategorias (id_categoria, nombre_subcategoria)
    SELECT v_cat_diseno, 'Identidad de Marca' WHERE NOT EXISTS (SELECT 1 FROM subcategorias WHERE id_categoria = v_cat_diseno AND nombre_subcategoria = 'Identidad de Marca');
    SELECT id_subcategoria INTO v_sub_identidad FROM subcategorias WHERE id_categoria = v_cat_diseno AND nombre_subcategoria = 'Identidad de Marca';

    INSERT INTO subcategorias (id_categoria, nombre_subcategoria)
    SELECT v_cat_ilustracion, 'Retratos' WHERE NOT EXISTS (SELECT 1 FROM subcategorias WHERE id_categoria = v_cat_ilustracion AND nombre_subcategoria = 'Retratos');
    SELECT id_subcategoria INTO v_sub_retratos FROM subcategorias WHERE id_categoria = v_cat_ilustracion AND nombre_subcategoria = 'Retratos';

    INSERT INTO subcategorias (id_categoria, nombre_subcategoria)
    SELECT v_cat_ilustracion, 'Concept Art' WHERE NOT EXISTS (SELECT 1 FROM subcategorias WHERE id_categoria = v_cat_ilustracion AND nombre_subcategoria = 'Concept Art');
    SELECT id_subcategoria INTO v_sub_conceptart FROM subcategorias WHERE id_categoria = v_cat_ilustracion AND nombre_subcategoria = 'Concept Art';

    INSERT INTO subcategorias (id_categoria, nombre_subcategoria)
    SELECT v_cat_animacion, '2D' WHERE NOT EXISTS (SELECT 1 FROM subcategorias WHERE id_categoria = v_cat_animacion AND nombre_subcategoria = '2D');
    SELECT id_subcategoria INTO v_sub_2d FROM subcategorias WHERE id_categoria = v_cat_animacion AND nombre_subcategoria = '2D';

    INSERT INTO subcategorias (id_categoria, nombre_subcategoria)
    SELECT v_cat_animacion, 'Motion Graphics' WHERE NOT EXISTS (SELECT 1 FROM subcategorias WHERE id_categoria = v_cat_animacion AND nombre_subcategoria = 'Motion Graphics');
    SELECT id_subcategoria INTO v_sub_motion FROM subcategorias WHERE id_categoria = v_cat_animacion AND nombre_subcategoria = 'Motion Graphics';

    INSERT INTO subcategorias (id_categoria, nombre_subcategoria)
    SELECT v_cat_3d, 'Personajes' WHERE NOT EXISTS (SELECT 1 FROM subcategorias WHERE id_categoria = v_cat_3d AND nombre_subcategoria = 'Personajes');
    SELECT id_subcategoria INTO v_sub_personajes FROM subcategorias WHERE id_categoria = v_cat_3d AND nombre_subcategoria = 'Personajes';

    INSERT INTO subcategorias (id_categoria, nombre_subcategoria)
    SELECT v_cat_3d, 'Props y Escenarios' WHERE NOT EXISTS (SELECT 1 FROM subcategorias WHERE id_categoria = v_cat_3d AND nombre_subcategoria = 'Props y Escenarios');
    SELECT id_subcategoria INTO v_sub_props FROM subcategorias WHERE id_categoria = v_cat_3d AND nombre_subcategoria = 'Props y Escenarios';

    INSERT INTO subcategorias (id_categoria, nombre_subcategoria)
    SELECT v_cat_audio, 'Composición Musical' WHERE NOT EXISTS (SELECT 1 FROM subcategorias WHERE id_categoria = v_cat_audio AND nombre_subcategoria = 'Composición Musical');
    SELECT id_subcategoria INTO v_sub_composicion FROM subcategorias WHERE id_categoria = v_cat_audio AND nombre_subcategoria = 'Composición Musical';

    INSERT INTO subcategorias (id_categoria, nombre_subcategoria)
    SELECT v_cat_audio, 'Locución' WHERE NOT EXISTS (SELECT 1 FROM subcategorias WHERE id_categoria = v_cat_audio AND nombre_subcategoria = 'Locución');
    SELECT id_subcategoria INTO v_sub_locucion FROM subcategorias WHERE id_categoria = v_cat_audio AND nombre_subcategoria = 'Locución';

    -- Flujos de trabajo (sin UNIQUE natural en el esquema)
    INSERT INTO flujos_trabajo (nombre_flujo, descripcion_flujo)
    SELECT 'Flujo Estándar ARTISYNC', 'Flujo de 4 etapas: borrador, producción, revisión del cliente y entrega final.'
    WHERE NOT EXISTS (SELECT 1 FROM flujos_trabajo WHERE nombre_flujo = 'Flujo Estándar ARTISYNC');
    SELECT id_flujo INTO v_flujo_estandar FROM flujos_trabajo WHERE nombre_flujo = 'Flujo Estándar ARTISYNC';

    INSERT INTO flujos_trabajo (nombre_flujo, descripcion_flujo)
    SELECT 'Flujo Express ARTISYNC', 'Flujo abreviado de 3 etapas para encargos rápidos de animación.'
    WHERE NOT EXISTS (SELECT 1 FROM flujos_trabajo WHERE nombre_flujo = 'Flujo Express ARTISYNC');
    SELECT id_flujo INTO v_flujo_express FROM flujos_trabajo WHERE nombre_flujo = 'Flujo Express ARTISYNC';

    INSERT INTO etapas_flujo (nombre_etapa) VALUES
        ('Borrador Recibido'), ('En Producción'), ('En Revisión del Cliente'), ('Entrega Final')
    ON CONFLICT (nombre_etapa) DO NOTHING;

    SELECT id_etapa INTO v_etapa_borrador   FROM etapas_flujo WHERE nombre_etapa = 'Borrador Recibido';
    SELECT id_etapa INTO v_etapa_produccion FROM etapas_flujo WHERE nombre_etapa = 'En Producción';
    SELECT id_etapa INTO v_etapa_revision   FROM etapas_flujo WHERE nombre_etapa = 'En Revisión del Cliente';
    SELECT id_etapa INTO v_etapa_entrega    FROM etapas_flujo WHERE nombre_etapa = 'Entrega Final';

    INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final)
    SELECT v_flujo_estandar, v_etapa_borrador, 1, false
    WHERE NOT EXISTS (SELECT 1 FROM flujo_etapas_config WHERE id_flujo = v_flujo_estandar AND id_etapa = v_etapa_borrador);
    INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final)
    SELECT v_flujo_estandar, v_etapa_produccion, 2, false
    WHERE NOT EXISTS (SELECT 1 FROM flujo_etapas_config WHERE id_flujo = v_flujo_estandar AND id_etapa = v_etapa_produccion);
    INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final)
    SELECT v_flujo_estandar, v_etapa_revision, 3, false
    WHERE NOT EXISTS (SELECT 1 FROM flujo_etapas_config WHERE id_flujo = v_flujo_estandar AND id_etapa = v_etapa_revision);
    INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final)
    SELECT v_flujo_estandar, v_etapa_entrega, 4, true
    WHERE NOT EXISTS (SELECT 1 FROM flujo_etapas_config WHERE id_flujo = v_flujo_estandar AND id_etapa = v_etapa_entrega);

    INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final)
    SELECT v_flujo_express, v_etapa_produccion, 1, false
    WHERE NOT EXISTS (SELECT 1 FROM flujo_etapas_config WHERE id_flujo = v_flujo_express AND id_etapa = v_etapa_produccion);
    INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final)
    SELECT v_flujo_express, v_etapa_revision, 2, false
    WHERE NOT EXISTS (SELECT 1 FROM flujo_etapas_config WHERE id_flujo = v_flujo_express AND id_etapa = v_etapa_revision);
    INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final)
    SELECT v_flujo_express, v_etapa_entrega, 3, true
    WHERE NOT EXISTS (SELECT 1 FROM flujo_etapas_config WHERE id_flujo = v_flujo_express AND id_etapa = v_etapa_entrega);

    UPDATE categorias SET id_flujo = v_flujo_estandar
    WHERE id_categoria IN (v_cat_diseno, v_cat_ilustracion, v_cat_3d, v_cat_audio) AND id_flujo IS NULL;
    UPDATE categorias SET id_flujo = v_flujo_express
    WHERE id_categoria = v_cat_animacion AND id_flujo IS NULL;

    -- ==========================================================================
    -- 4. SERVICIOS, ATRIBUTOS DINÁMICOS Y ETIQUETAS
    -- ==========================================================================
    INSERT INTO servicios (id_perfil, id_subcategoria, titulo_servicio, descripcion_detallada, precio_base, url_miniatura, limite_revisiones_base)
    SELECT v_perfil_valentina, v_sub_retratos, 'Retrato Digital Personalizado',
           'Retrato digital a todo color a partir de una fotografía de referencia, con dos revisiones incluidas.', 45.00,
           'https://picsum.photos/seed/srv-retrato/500/500', 2
    WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE id_perfil = v_perfil_valentina AND titulo_servicio = 'Retrato Digital Personalizado');
    SELECT id_servicio INTO v_srv_retrato FROM servicios WHERE id_perfil = v_perfil_valentina AND titulo_servicio = 'Retrato Digital Personalizado';

    INSERT INTO servicios (id_perfil, id_subcategoria, titulo_servicio, descripcion_detallada, precio_base, url_miniatura, limite_revisiones_base)
    SELECT v_perfil_valentina, v_sub_conceptart, 'Concept Art de Personaje',
           'Diseño de personaje original para videojuegos o novelas gráficas, incluye hoja de expresiones.', 120.00,
           'https://picsum.photos/seed/srv-conceptart/500/500', 1
    WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE id_perfil = v_perfil_valentina AND titulo_servicio = 'Concept Art de Personaje');
    SELECT id_servicio INTO v_srv_conceptart FROM servicios WHERE id_perfil = v_perfil_valentina AND titulo_servicio = 'Concept Art de Personaje';

    INSERT INTO servicios (id_perfil, id_subcategoria, titulo_servicio, descripcion_detallada, precio_base, url_miniatura, limite_revisiones_base)
    SELECT v_perfil_mateo, v_sub_logotipos, 'Diseño de Logotipo Profesional',
           'Logotipo vectorial con tres propuestas iniciales y ajustes ilimitados sobre la opción elegida.', 80.00,
           'https://picsum.photos/seed/srv-logo/500/500', 3
    WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE id_perfil = v_perfil_mateo AND titulo_servicio = 'Diseño de Logotipo Profesional');
    SELECT id_servicio INTO v_srv_logo FROM servicios WHERE id_perfil = v_perfil_mateo AND titulo_servicio = 'Diseño de Logotipo Profesional';

    INSERT INTO servicios (id_perfil, id_subcategoria, titulo_servicio, descripcion_detallada, precio_base, url_miniatura, limite_revisiones_base)
    SELECT v_perfil_mateo, v_sub_identidad, 'Manual de Identidad de Marca',
           'Manual completo de marca: paleta de color, tipografía, usos del logotipo y aplicaciones.', 250.00,
           'https://picsum.photos/seed/srv-manual/500/500', 2
    WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE id_perfil = v_perfil_mateo AND titulo_servicio = 'Manual de Identidad de Marca');
    SELECT id_servicio INTO v_srv_manual FROM servicios WHERE id_perfil = v_perfil_mateo AND titulo_servicio = 'Manual de Identidad de Marca';

    INSERT INTO servicios (id_perfil, id_subcategoria, titulo_servicio, descripcion_detallada, precio_base, url_miniatura, limite_revisiones_base)
    SELECT v_perfil_sofia, v_sub_personajes, 'Modelado de Personaje 3D',
           'Modelado, texturizado y rig básico de un personaje 3D listo para animación o videojuego.', 300.00,
           'https://picsum.photos/seed/srv-modelo3d/500/500', 1
    WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE id_perfil = v_perfil_sofia AND titulo_servicio = 'Modelado de Personaje 3D');
    SELECT id_servicio INTO v_srv_modelo3d FROM servicios WHERE id_perfil = v_perfil_sofia AND titulo_servicio = 'Modelado de Personaje 3D';

    INSERT INTO servicios (id_perfil, id_subcategoria, titulo_servicio, descripcion_detallada, precio_base, url_miniatura, limite_revisiones_base)
    SELECT v_perfil_sofia, v_sub_props, 'Prop 3D para Videojuego',
           'Modelado de un objeto/prop optimizado en low-poly con texturizado PBR.', 150.00,
           'https://picsum.photos/seed/srv-prop3d/500/500', 1
    WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE id_perfil = v_perfil_sofia AND titulo_servicio = 'Prop 3D para Videojuego');
    SELECT id_servicio INTO v_srv_prop3d FROM servicios WHERE id_perfil = v_perfil_sofia AND titulo_servicio = 'Prop 3D para Videojuego';

    -- Atributos dinámicos
    INSERT INTO atributos_dinamicos (nombre_atributo, tipo_dato) VALUES
        ('Formato de entrega', 'TEXTO'), ('Tiempo de entrega (días)', 'NUMERO'), ('Resolución', 'TEXTO')
    ON CONFLICT (nombre_atributo) DO NOTHING;

    SELECT id_atributo INTO v_attr_formato    FROM atributos_dinamicos WHERE nombre_atributo = 'Formato de entrega';
    SELECT id_atributo INTO v_attr_tiempo     FROM atributos_dinamicos WHERE nombre_atributo = 'Tiempo de entrega (días)';
    SELECT id_atributo INTO v_attr_resolucion FROM atributos_dinamicos WHERE nombre_atributo = 'Resolución';

    INSERT INTO servicio_atributos (id_servicio, id_atributo, valor_asignado)
    SELECT v_srv_retrato, v_attr_formato, 'PNG y PSD'
    WHERE NOT EXISTS (SELECT 1 FROM servicio_atributos WHERE id_servicio = v_srv_retrato AND id_atributo = v_attr_formato);
    INSERT INTO servicio_atributos (id_servicio, id_atributo, valor_asignado)
    SELECT v_srv_retrato, v_attr_resolucion, '4000x4000 px'
    WHERE NOT EXISTS (SELECT 1 FROM servicio_atributos WHERE id_servicio = v_srv_retrato AND id_atributo = v_attr_resolucion);
    INSERT INTO servicio_atributos (id_servicio, id_atributo, valor_asignado)
    SELECT v_srv_modelo3d, v_attr_formato, 'FBX y OBJ'
    WHERE NOT EXISTS (SELECT 1 FROM servicio_atributos WHERE id_servicio = v_srv_modelo3d AND id_atributo = v_attr_formato);
    INSERT INTO servicio_atributos (id_servicio, id_atributo, valor_asignado)
    SELECT v_srv_modelo3d, v_attr_tiempo, '10'
    WHERE NOT EXISTS (SELECT 1 FROM servicio_atributos WHERE id_servicio = v_srv_modelo3d AND id_atributo = v_attr_tiempo);

    -- Etiquetas
    INSERT INTO etiquetas (nombre_etiqueta) VALUES
        ('anime'), ('realista'), ('minimalista'), ('corporativo'), ('fantasia'), ('low-poly')
    ON CONFLICT (nombre_etiqueta) DO NOTHING;

    INSERT INTO servicio_etiquetas (id_servicio, id_etiqueta)
    SELECT v_srv_retrato, e.id_etiqueta FROM etiquetas e WHERE e.nombre_etiqueta IN ('realista', 'fantasia')
      AND NOT EXISTS (SELECT 1 FROM servicio_etiquetas se WHERE se.id_servicio = v_srv_retrato AND se.id_etiqueta = e.id_etiqueta);
    INSERT INTO servicio_etiquetas (id_servicio, id_etiqueta)
    SELECT v_srv_logo, e.id_etiqueta FROM etiquetas e WHERE e.nombre_etiqueta IN ('minimalista', 'corporativo')
      AND NOT EXISTS (SELECT 1 FROM servicio_etiquetas se WHERE se.id_servicio = v_srv_logo AND se.id_etiqueta = e.id_etiqueta);
    INSERT INTO servicio_etiquetas (id_servicio, id_etiqueta)
    SELECT v_srv_modelo3d, e.id_etiqueta FROM etiquetas e WHERE e.nombre_etiqueta IN ('fantasia', 'low-poly')
      AND NOT EXISTS (SELECT 1 FROM servicio_etiquetas se WHERE se.id_servicio = v_srv_modelo3d AND se.id_etiqueta = e.id_etiqueta);

    -- ==========================================================================
    -- 5. MOTIVOS DE RECHAZO, PEDIDOS, HISTORIAL Y TICKETS
    -- ==========================================================================
    INSERT INTO motivos_rechazo (descripcion_motivo) VALUES
        ('El diseño no cumple con el brief acordado'),
        ('Errores tipográficos u ortográficos'),
        ('Formato de archivo incorrecto'),
        ('Resolución de imagen insuficiente')
    ON CONFLICT (descripcion_motivo) DO NOTHING;
    SELECT id_motivo INTO v_motivo1 FROM motivos_rechazo WHERE descripcion_motivo = 'El diseño no cumple con el brief acordado';

    -- Pedido 1: Carlos -> Retrato (completado, con reseña)
    INSERT INTO pedidos (id_usuario_cliente, id_servicio, id_flujo, fecha_inicio, fecha_entrega_estimada, precio_pactado)
    SELECT v_usr_carlos, v_srv_retrato, v_flujo_estandar, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '13 days', 45.00
    WHERE NOT EXISTS (SELECT 1 FROM pedidos WHERE id_usuario_cliente = v_usr_carlos AND id_servicio = v_srv_retrato);
    SELECT id_pedido INTO v_pedido1 FROM pedidos WHERE id_usuario_cliente = v_usr_carlos AND id_servicio = v_srv_retrato;

    -- Pedido 2: Lucía -> Logotipo (en producción)
    INSERT INTO pedidos (id_usuario_cliente, id_servicio, id_flujo, fecha_inicio, fecha_entrega_estimada, precio_pactado)
    SELECT v_usr_lucia, v_srv_logo, v_flujo_estandar, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '4 days', 80.00
    WHERE NOT EXISTS (SELECT 1 FROM pedidos WHERE id_usuario_cliente = v_usr_lucia AND id_servicio = v_srv_logo);
    SELECT id_pedido INTO v_pedido2 FROM pedidos WHERE id_usuario_cliente = v_usr_lucia AND id_servicio = v_srv_logo;

    -- Pedido 3: Diego -> Modelo 3D (recién iniciado, sin contrato aún)
    INSERT INTO pedidos (id_usuario_cliente, id_servicio, id_flujo, fecha_inicio, fecha_entrega_estimada, precio_pactado)
    SELECT v_usr_diego, v_srv_modelo3d, v_flujo_estandar, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '13 days', 300.00
    WHERE NOT EXISTS (SELECT 1 FROM pedidos WHERE id_usuario_cliente = v_usr_diego AND id_servicio = v_srv_modelo3d);
    SELECT id_pedido INTO v_pedido3 FROM pedidos WHERE id_usuario_cliente = v_usr_diego AND id_servicio = v_srv_modelo3d;

    -- Pedido 4: Carlos -> Manual de identidad (con ticket de revisión, completado)
    INSERT INTO pedidos (id_usuario_cliente, id_servicio, id_flujo, fecha_inicio, fecha_entrega_estimada, precio_pactado)
    SELECT v_usr_carlos, v_srv_manual, v_flujo_estandar, CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '25 days', 250.00
    WHERE NOT EXISTS (SELECT 1 FROM pedidos WHERE id_usuario_cliente = v_usr_carlos AND id_servicio = v_srv_manual);
    SELECT id_pedido INTO v_pedido4 FROM pedidos WHERE id_usuario_cliente = v_usr_carlos AND id_servicio = v_srv_manual;

    -- Historial de estados
    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v_pedido1, v_etapa_borrador, CURRENT_TIMESTAMP - INTERVAL '20 days', 'Pedido creado y brief recibido.'
    WHERE NOT EXISTS (SELECT 1 FROM historial_estados_pedido WHERE id_pedido = v_pedido1 AND id_etapa = v_etapa_borrador);
    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v_pedido1, v_etapa_produccion, CURRENT_TIMESTAMP - INTERVAL '17 days', 'Boceto inicial en curso.'
    WHERE NOT EXISTS (SELECT 1 FROM historial_estados_pedido WHERE id_pedido = v_pedido1 AND id_etapa = v_etapa_produccion);
    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v_pedido1, v_etapa_revision, CURRENT_TIMESTAMP - INTERVAL '15 days', 'Cliente revisando el retrato.'
    WHERE NOT EXISTS (SELECT 1 FROM historial_estados_pedido WHERE id_pedido = v_pedido1 AND id_etapa = v_etapa_revision);
    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v_pedido1, v_etapa_entrega, CURRENT_TIMESTAMP - INTERVAL '13 days', 'Entrega final aprobada por el cliente.'
    WHERE NOT EXISTS (SELECT 1 FROM historial_estados_pedido WHERE id_pedido = v_pedido1 AND id_etapa = v_etapa_entrega);

    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v_pedido2, v_etapa_borrador, CURRENT_TIMESTAMP - INTERVAL '5 days', 'Pedido creado.'
    WHERE NOT EXISTS (SELECT 1 FROM historial_estados_pedido WHERE id_pedido = v_pedido2 AND id_etapa = v_etapa_borrador);
    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v_pedido2, v_etapa_produccion, CURRENT_TIMESTAMP - INTERVAL '2 days', 'Primer boceto del logotipo compartido.'
    WHERE NOT EXISTS (SELECT 1 FROM historial_estados_pedido WHERE id_pedido = v_pedido2 AND id_etapa = v_etapa_produccion);

    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v_pedido3, v_etapa_borrador, CURRENT_TIMESTAMP - INTERVAL '1 day', 'Pedido creado, esperando formalización de contrato.'
    WHERE NOT EXISTS (SELECT 1 FROM historial_estados_pedido WHERE id_pedido = v_pedido3 AND id_etapa = v_etapa_borrador);

    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v_pedido4, v_etapa_borrador, CURRENT_TIMESTAMP - INTERVAL '40 days', 'Pedido creado.'
    WHERE NOT EXISTS (SELECT 1 FROM historial_estados_pedido WHERE id_pedido = v_pedido4 AND id_etapa = v_etapa_borrador);
    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v_pedido4, v_etapa_produccion, CURRENT_TIMESTAMP - INTERVAL '35 days', 'Primera propuesta de manual de marca.'
    WHERE NOT EXISTS (SELECT 1 FROM historial_estados_pedido WHERE id_pedido = v_pedido4 AND id_etapa = v_etapa_produccion);
    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v_pedido4, v_etapa_revision, CURRENT_TIMESTAMP - INTERVAL '30 days', 'Cliente solicita ajustes vía ticket de revisión.'
    WHERE NOT EXISTS (SELECT 1 FROM historial_estados_pedido WHERE id_pedido = v_pedido4 AND id_etapa = v_etapa_revision);
    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v_pedido4, v_etapa_entrega, CURRENT_TIMESTAMP - INTERVAL '25 days', 'Manual de marca corregido y entregado.'
    WHERE NOT EXISTS (SELECT 1 FROM historial_estados_pedido WHERE id_pedido = v_pedido4 AND id_etapa = v_etapa_entrega);

    -- Ticket de revisión sobre el pedido 4
    INSERT INTO tickets_revision (id_pedido, id_motivo, descripcion_cliente, costo_adicional_generado, estado_ticket)
    SELECT v_pedido4, v_motivo1, 'La paleta de color no coincide con la aprobada en el brief inicial, se solicita corrección.', 0.00, 'Resuelto'
    WHERE NOT EXISTS (SELECT 1 FROM tickets_revision WHERE id_pedido = v_pedido4);

    -- ==========================================================================
    -- 6. CONTRATOS, ENTREGABLES, PAGOS EN GARANTÍA Y TRANSACCIONES
    -- ==========================================================================
    INSERT INTO contratos (id_pedido, id_plantilla, hash_firma_cliente, hash_firma_creador, limite_revisiones, fecha_formalizacion)
    SELECT v_pedido1, v_plantilla, 'sha256:demo-firma-cliente-carlos-001', 'sha256:demo-firma-creador-valentina-001', 2, CURRENT_TIMESTAMP - INTERVAL '19 days'
    WHERE NOT EXISTS (SELECT 1 FROM contratos WHERE id_pedido = v_pedido1);
    SELECT id_contrato INTO v_contrato1 FROM contratos WHERE id_pedido = v_pedido1;

    INSERT INTO contratos (id_pedido, id_plantilla, hash_firma_cliente, hash_firma_creador, limite_revisiones, fecha_formalizacion)
    SELECT v_pedido2, v_plantilla, 'sha256:demo-firma-cliente-lucia-002', 'sha256:demo-firma-creador-mateo-002', 3, CURRENT_TIMESTAMP - INTERVAL '5 days'
    WHERE NOT EXISTS (SELECT 1 FROM contratos WHERE id_pedido = v_pedido2);
    SELECT id_contrato INTO v_contrato2 FROM contratos WHERE id_pedido = v_pedido2;

    INSERT INTO contratos (id_pedido, id_plantilla, hash_firma_cliente, hash_firma_creador, limite_revisiones, fecha_formalizacion)
    SELECT v_pedido4, v_plantilla, 'sha256:demo-firma-cliente-carlos-004', 'sha256:demo-firma-creador-mateo-004', 2, CURRENT_TIMESTAMP - INTERVAL '39 days'
    WHERE NOT EXISTS (SELECT 1 FROM contratos WHERE id_pedido = v_pedido4);
    SELECT id_contrato INTO v_contrato4 FROM contratos WHERE id_pedido = v_pedido4;
    -- Pedido 3 se deja intencionalmente sin contrato: demuestra el estado "recién creado, pendiente de formalizar".

    -- Entregables finales
    INSERT INTO entregables_finales (id_pedido, url_version_marca_agua, url_version_limpia, esta_liberado)
    SELECT v_pedido1, 'https://picsum.photos/seed/entrega1-marca/900/700', 'https://picsum.photos/seed/entrega1-limpia/900/700', true
    WHERE NOT EXISTS (SELECT 1 FROM entregables_finales WHERE id_pedido = v_pedido1);
    INSERT INTO entregables_finales (id_pedido, url_version_marca_agua, url_version_limpia, esta_liberado)
    SELECT v_pedido2, 'https://picsum.photos/seed/entrega2-marca/900/700', NULL, false
    WHERE NOT EXISTS (SELECT 1 FROM entregables_finales WHERE id_pedido = v_pedido2);
    INSERT INTO entregables_finales (id_pedido, url_version_marca_agua, url_version_limpia, esta_liberado)
    SELECT v_pedido4, 'https://picsum.photos/seed/entrega4-marca/900/700', 'https://picsum.photos/seed/entrega4-limpia/900/700', true
    WHERE NOT EXISTS (SELECT 1 FROM entregables_finales WHERE id_pedido = v_pedido4);

    -- Pagos en garantía (escrow)
    INSERT INTO pagos_garantia (id_contrato, id_orden_paypal, monto_retenido, estado_fondos)
    SELECT v_contrato1, 'PAYPAL-DEMO-0001', 45.00, 'Liberado' WHERE NOT EXISTS (SELECT 1 FROM pagos_garantia WHERE id_contrato = v_contrato1);
    SELECT id_pago INTO v_pago1 FROM pagos_garantia WHERE id_contrato = v_contrato1;

    INSERT INTO pagos_garantia (id_contrato, id_orden_paypal, monto_retenido, estado_fondos)
    SELECT v_contrato2, 'PAYPAL-DEMO-0002', 80.00, 'Retenido' WHERE NOT EXISTS (SELECT 1 FROM pagos_garantia WHERE id_contrato = v_contrato2);
    SELECT id_pago INTO v_pago2 FROM pagos_garantia WHERE id_contrato = v_contrato2;

    INSERT INTO pagos_garantia (id_contrato, id_orden_paypal, monto_retenido, estado_fondos)
    SELECT v_contrato4, 'PAYPAL-DEMO-0004', 250.00, 'Liberado' WHERE NOT EXISTS (SELECT 1 FROM pagos_garantia WHERE id_contrato = v_contrato4);
    SELECT id_pago INTO v_pago4 FROM pagos_garantia WHERE id_contrato = v_contrato4;

    -- Transacciones de pago
    INSERT INTO transacciones_pago (id_pago, tipo_transaccion, monto, fecha_ejecucion)
    SELECT v_pago1, 'RETENCION', 45.00, CURRENT_TIMESTAMP - INTERVAL '19 days'
    WHERE NOT EXISTS (SELECT 1 FROM transacciones_pago WHERE id_pago = v_pago1 AND tipo_transaccion = 'RETENCION');
    INSERT INTO transacciones_pago (id_pago, tipo_transaccion, monto, fecha_ejecucion)
    SELECT v_pago1, 'LIBERACION', 45.00, CURRENT_TIMESTAMP - INTERVAL '13 days'
    WHERE NOT EXISTS (SELECT 1 FROM transacciones_pago WHERE id_pago = v_pago1 AND tipo_transaccion = 'LIBERACION');

    INSERT INTO transacciones_pago (id_pago, tipo_transaccion, monto, fecha_ejecucion)
    SELECT v_pago2, 'RETENCION', 80.00, CURRENT_TIMESTAMP - INTERVAL '5 days'
    WHERE NOT EXISTS (SELECT 1 FROM transacciones_pago WHERE id_pago = v_pago2 AND tipo_transaccion = 'RETENCION');

    INSERT INTO transacciones_pago (id_pago, tipo_transaccion, monto, fecha_ejecucion)
    SELECT v_pago4, 'RETENCION', 250.00, CURRENT_TIMESTAMP - INTERVAL '39 days'
    WHERE NOT EXISTS (SELECT 1 FROM transacciones_pago WHERE id_pago = v_pago4 AND tipo_transaccion = 'RETENCION');
    INSERT INTO transacciones_pago (id_pago, tipo_transaccion, monto, fecha_ejecucion)
    SELECT v_pago4, 'LIBERACION', 250.00, CURRENT_TIMESTAMP - INTERVAL '25 days'
    WHERE NOT EXISTS (SELECT 1 FROM transacciones_pago WHERE id_pago = v_pago4 AND tipo_transaccion = 'LIBERACION');

    -- ==========================================================================
    -- 7. COMUNICACIÓN: SALAS DE CHAT, MENSAJES, ADJUNTOS Y NOTIFICACIONES
    -- ==========================================================================
    INSERT INTO salas_chat (id_pedido, sala_activa)
    SELECT v_pedido1, false WHERE NOT EXISTS (SELECT 1 FROM salas_chat WHERE id_pedido = v_pedido1);
    SELECT id_sala INTO v_sala1 FROM salas_chat WHERE id_pedido = v_pedido1;

    INSERT INTO salas_chat (id_pedido, sala_activa)
    SELECT v_pedido2, true WHERE NOT EXISTS (SELECT 1 FROM salas_chat WHERE id_pedido = v_pedido2);
    SELECT id_sala INTO v_sala2 FROM salas_chat WHERE id_pedido = v_pedido2;

    INSERT INTO salas_chat (id_pedido, sala_activa)
    SELECT v_pedido3, true WHERE NOT EXISTS (SELECT 1 FROM salas_chat WHERE id_pedido = v_pedido3);
    SELECT id_sala INTO v_sala3 FROM salas_chat WHERE id_pedido = v_pedido3;

    INSERT INTO salas_chat (id_pedido, sala_activa)
    SELECT v_pedido4, false WHERE NOT EXISTS (SELECT 1 FROM salas_chat WHERE id_pedido = v_pedido4);
    SELECT id_sala INTO v_sala4 FROM salas_chat WHERE id_pedido = v_pedido4;

    INSERT INTO mensajes (id_sala, id_remitente, cuerpo_mensaje, fecha_hora_envio, leido)
    SELECT v_sala1, v_usr_carlos, '¡Excelente trabajo, me encantó el retrato!', CURRENT_TIMESTAMP - INTERVAL '13 days', true
    WHERE NOT EXISTS (SELECT 1 FROM mensajes WHERE id_sala = v_sala1 AND cuerpo_mensaje = '¡Excelente trabajo, me encantó el retrato!');
    INSERT INTO mensajes (id_sala, id_remitente, cuerpo_mensaje, fecha_hora_envio, leido)
    SELECT v_sala1, v_usr_valentina, '¡Muchas gracias Carlos! Fue un placer trabajar en tu encargo.', CURRENT_TIMESTAMP - INTERVAL '13 days', true
    WHERE NOT EXISTS (SELECT 1 FROM mensajes WHERE id_sala = v_sala1 AND cuerpo_mensaje = '¡Muchas gracias Carlos! Fue un placer trabajar en tu encargo.');

    INSERT INTO mensajes (id_sala, id_remitente, cuerpo_mensaje, fecha_hora_envio, leido)
    SELECT v_sala2, v_usr_lucia, 'Hola Mateo, ¿cómo va el logotipo? Quisiera ver un primer boceto.', CURRENT_TIMESTAMP - INTERVAL '3 days', true
    WHERE NOT EXISTS (SELECT 1 FROM mensajes WHERE id_sala = v_sala2 AND cuerpo_mensaje = 'Hola Mateo, ¿cómo va el logotipo? Quisiera ver un primer boceto.');
    INSERT INTO mensajes (id_sala, id_remitente, cuerpo_mensaje, fecha_hora_envio, leido)
    SELECT v_sala2, v_usr_mateo, '¡Hola Lucía! Te comparto el primer boceto en el archivo adjunto.', CURRENT_TIMESTAMP - INTERVAL '2 days', false
    WHERE NOT EXISTS (SELECT 1 FROM mensajes WHERE id_sala = v_sala2 AND cuerpo_mensaje = '¡Hola Lucía! Te comparto el primer boceto en el archivo adjunto.');
    SELECT id_mensaje INTO v_mensaje2 FROM mensajes WHERE id_sala = v_sala2 AND cuerpo_mensaje = '¡Hola Lucía! Te comparto el primer boceto en el archivo adjunto.';

    INSERT INTO documentos_adjuntos (id_mensaje, url_archivo, tipo_mime, peso_bytes)
    SELECT v_mensaje2, 'https://picsum.photos/seed/boceto-logo/800/600', 'image/png', 245000
    WHERE NOT EXISTS (SELECT 1 FROM documentos_adjuntos WHERE id_mensaje = v_mensaje2);

    -- Tipos de notificación (catálogo)
    INSERT INTO tipos_notificacion (nombre_evento, formato_mensaje) VALUES
        ('MENSAJE_RECIBIDO', 'Tienes un nuevo mensaje en tu pedido.'),
        ('PEDIDO_ACTUALIZADO', 'Tu pedido cambió de estado.'),
        ('PAGO_LIBERADO', 'Se liberaron los fondos de tu pedido.'),
        ('NUEVO_SEGUIDOR', 'Tienes un nuevo seguidor.'),
        ('CERTIFICADO_REVISADO', 'Tu certificado de verificación fue revisado.'),
        ('SORTEO_GANADOR', '¡Felicidades, ganaste un sorteo!')
    ON CONFLICT (nombre_evento) DO NOTHING;

    SELECT id_tipo_notificacion INTO v_tipo_mensaje     FROM tipos_notificacion WHERE nombre_evento = 'MENSAJE_RECIBIDO';
    SELECT id_tipo_notificacion INTO v_tipo_pedido      FROM tipos_notificacion WHERE nombre_evento = 'PEDIDO_ACTUALIZADO';
    SELECT id_tipo_notificacion INTO v_tipo_pago        FROM tipos_notificacion WHERE nombre_evento = 'PAGO_LIBERADO';
    SELECT id_tipo_notificacion INTO v_tipo_seguidor    FROM tipos_notificacion WHERE nombre_evento = 'NUEVO_SEGUIDOR';
    SELECT id_tipo_notificacion INTO v_tipo_certificado FROM tipos_notificacion WHERE nombre_evento = 'CERTIFICADO_REVISADO';
    SELECT id_tipo_notificacion INTO v_tipo_sorteo      FROM tipos_notificacion WHERE nombre_evento = 'SORTEO_GANADOR';

    INSERT INTO notificaciones_sistema (id_usuario, id_tipo_notificacion, mensaje, esta_leida)
    SELECT v_usr_mateo, v_tipo_mensaje, 'Lucía Paredes te escribió: "¿cómo va el logotipo?"', true
    WHERE NOT EXISTS (SELECT 1 FROM notificaciones_sistema WHERE id_usuario = v_usr_mateo AND id_tipo_notificacion = v_tipo_mensaje);
    INSERT INTO notificaciones_sistema (id_usuario, id_tipo_notificacion, mensaje, esta_leida)
    SELECT v_usr_lucia, v_tipo_pedido, 'Tu pedido "Diseño de Logotipo Profesional" pasó a "En Producción".', false
    WHERE NOT EXISTS (SELECT 1 FROM notificaciones_sistema WHERE id_usuario = v_usr_lucia AND id_tipo_notificacion = v_tipo_pedido);
    INSERT INTO notificaciones_sistema (id_usuario, id_tipo_notificacion, mensaje, esta_leida)
    SELECT v_usr_carlos, v_tipo_pago, 'Se liberó el pago de tu pedido "Retrato Digital Personalizado".', true
    WHERE NOT EXISTS (SELECT 1 FROM notificaciones_sistema WHERE id_usuario = v_usr_carlos AND id_tipo_notificacion = v_tipo_pago);
    INSERT INTO notificaciones_sistema (id_usuario, id_tipo_notificacion, mensaje, esta_leida)
    SELECT v_usr_valentina, v_tipo_seguidor, 'Carlos Mendoza ahora te sigue.', false
    WHERE NOT EXISTS (SELECT 1 FROM notificaciones_sistema WHERE id_usuario = v_usr_valentina AND id_tipo_notificacion = v_tipo_seguidor);

    -- Infracción de mensaje (demo de moderación)
    INSERT INTO infracciones_mensaje (id_usuario, id_pedido, mensaje_original, patron_detectado)
    SELECT v_usr_mateo, v_pedido2, 'Mejor sigamos por WhatsApp al 555-0102, es más rápido.', 'CONTACTO_EXTERNO'
    WHERE NOT EXISTS (SELECT 1 FROM infracciones_mensaje WHERE id_usuario = v_usr_mateo AND id_pedido = v_pedido2);

    -- ==========================================================================
    -- 8. BRIEFING INTERACTIVO
    -- ==========================================================================
    INSERT INTO briefing_plantillas (id_perfil, nombre_plantilla)
    SELECT v_perfil_valentina, 'Brief de Retrato'
    WHERE NOT EXISTS (SELECT 1 FROM briefing_plantillas WHERE id_perfil = v_perfil_valentina AND nombre_plantilla = 'Brief de Retrato');
    SELECT id_briefing_plantilla INTO v_briefing_plantilla FROM briefing_plantillas WHERE id_perfil = v_perfil_valentina AND nombre_plantilla = 'Brief de Retrato';

    INSERT INTO briefing_preguntas (id_briefing_plantilla, texto_pregunta, numero_orden)
    SELECT v_briefing_plantilla, '¿Qué estilo prefieres para tu retrato (realista, semi-realista, caricatura)?', 1
    WHERE NOT EXISTS (SELECT 1 FROM briefing_preguntas WHERE id_briefing_plantilla = v_briefing_plantilla AND numero_orden = 1);
    SELECT id_pregunta INTO v_briefing_p1 FROM briefing_preguntas WHERE id_briefing_plantilla = v_briefing_plantilla AND numero_orden = 1;

    INSERT INTO briefing_preguntas (id_briefing_plantilla, texto_pregunta, numero_orden)
    SELECT v_briefing_plantilla, '¿Tienes una foto de referencia en alta resolución?', 2
    WHERE NOT EXISTS (SELECT 1 FROM briefing_preguntas WHERE id_briefing_plantilla = v_briefing_plantilla AND numero_orden = 2);
    SELECT id_pregunta INTO v_briefing_p2 FROM briefing_preguntas WHERE id_briefing_plantilla = v_briefing_plantilla AND numero_orden = 2;

    INSERT INTO briefing_preguntas (id_briefing_plantilla, texto_pregunta, numero_orden)
    SELECT v_briefing_plantilla, '¿Deseas incluir algún elemento adicional (fondo, accesorios)?', 3
    WHERE NOT EXISTS (SELECT 1 FROM briefing_preguntas WHERE id_briefing_plantilla = v_briefing_plantilla AND numero_orden = 3);
    SELECT id_pregunta INTO v_briefing_p3 FROM briefing_preguntas WHERE id_briefing_plantilla = v_briefing_plantilla AND numero_orden = 3;

    INSERT INTO briefing_enviados (id_pedido, id_briefing_plantilla, completado)
    SELECT v_pedido1, v_briefing_plantilla, true
    WHERE NOT EXISTS (SELECT 1 FROM briefing_enviados WHERE id_pedido = v_pedido1 AND id_briefing_plantilla = v_briefing_plantilla);
    SELECT id_briefing_enviado INTO v_briefing_enviado FROM briefing_enviados WHERE id_pedido = v_pedido1 AND id_briefing_plantilla = v_briefing_plantilla;

    INSERT INTO briefing_respuestas (id_briefing_enviado, id_pregunta, texto_respuesta)
    VALUES (v_briefing_enviado, v_briefing_p1, 'Prefiero un estilo realista, similar a tus trabajos anteriores.')
    ON CONFLICT (id_briefing_enviado, id_pregunta) DO NOTHING;
    INSERT INTO briefing_respuestas (id_briefing_enviado, id_pregunta, texto_respuesta)
    VALUES (v_briefing_enviado, v_briefing_p2, 'Sí, adjunto una foto en 4000x3000 px tomada con buena iluminación.')
    ON CONFLICT (id_briefing_enviado, id_pregunta) DO NOTHING;
    INSERT INTO briefing_respuestas (id_briefing_enviado, id_pregunta, texto_respuesta)
    VALUES (v_briefing_enviado, v_briefing_p3, 'Me gustaría un fondo neutro, sin accesorios adicionales.')
    ON CONFLICT (id_briefing_enviado, id_pregunta) DO NOTHING;

    -- ==========================================================================
    -- 9. SOCIAL: SEGUIDORES, COMENTARIOS, LIKES, RESEÑAS Y SORTEOS
    -- ==========================================================================
    INSERT INTO seguidores (id_usuario_seguidor, id_perfil_creador)
    VALUES (v_usr_carlos, v_perfil_valentina), (v_usr_lucia, v_perfil_valentina),
           (v_usr_lucia, v_perfil_mateo), (v_usr_diego, v_perfil_sofia)
    ON CONFLICT (id_usuario_seguidor, id_perfil_creador) DO NOTHING;

    INSERT INTO comentarios_portafolio (id_item_portafolio, id_usuario_autor, texto_comentario)
    SELECT v_item_valentina_1, v_usr_carlos, '¡Increíble trabajo, se ve tan realista!'
    WHERE NOT EXISTS (SELECT 1 FROM comentarios_portafolio WHERE id_item_portafolio = v_item_valentina_1 AND id_usuario_autor = v_usr_carlos);
    INSERT INTO comentarios_portafolio (id_item_portafolio, id_usuario_autor, texto_comentario)
    SELECT v_item_mateo_1, v_usr_lucia, 'Me encanta la paleta de colores que usaste.'
    WHERE NOT EXISTS (SELECT 1 FROM comentarios_portafolio WHERE id_item_portafolio = v_item_mateo_1 AND id_usuario_autor = v_usr_lucia);

    INSERT INTO likes_portafolio (id_item_portafolio, id_usuario)
    VALUES (v_item_valentina_1, v_usr_carlos), (v_item_valentina_1, v_usr_lucia),
           (v_item_mateo_1, v_usr_lucia), (v_item_sofia_1, v_usr_diego)
    ON CONFLICT (id_item_portafolio, id_usuario) DO NOTHING;

    INSERT INTO resenas_servicios (id_pedido, calificacion_estrellas, texto_resena)
    SELECT v_pedido1, 5, 'Quedé encantado con el resultado, superó mis expectativas.'
    WHERE NOT EXISTS (SELECT 1 FROM resenas_servicios WHERE id_pedido = v_pedido1);
    INSERT INTO resenas_servicios (id_pedido, calificacion_estrellas, texto_resena)
    SELECT v_pedido4, 4, 'Muy buen trabajo, aunque tomó un poco más de tiempo del esperado.'
    WHERE NOT EXISTS (SELECT 1 FROM resenas_servicios WHERE id_pedido = v_pedido4);

    -- Sorteo activo (requiere seguir al creador)
    INSERT INTO sorteos (id_perfil_creador, titulo_sorteo, descripcion_premios, cantidad_ganadores, fecha_inicio, fecha_cierre, estado_sorteo, requiere_seguidor)
    SELECT v_perfil_valentina, 'Sorteo de Ilustración Digital', 'Un retrato digital personalizado totalmente gratis.', 1,
           CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '5 days', 'Activo', true
    WHERE NOT EXISTS (SELECT 1 FROM sorteos WHERE id_perfil_creador = v_perfil_valentina AND titulo_sorteo = 'Sorteo de Ilustración Digital');
    SELECT id_sorteo INTO v_sorteo1 FROM sorteos WHERE id_perfil_creador = v_perfil_valentina AND titulo_sorteo = 'Sorteo de Ilustración Digital';

    INSERT INTO participantes_sorteo (id_sorteo, id_usuario)
    VALUES (v_sorteo1, v_usr_carlos), (v_sorteo1, v_usr_lucia)
    ON CONFLICT (id_sorteo, id_usuario) DO NOTHING;

    -- Sorteo ya finalizado (demuestra el ciclo completo con ganador)
    INSERT INTO sorteos (id_perfil_creador, titulo_sorteo, descripcion_premios, cantidad_ganadores, fecha_inicio, fecha_cierre, estado_sorteo, requiere_seguidor)
    SELECT v_perfil_mateo, 'Pack de Recursos de Branding', 'Un kit de plantillas de identidad de marca en formato editable.', 1,
           CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '16 days', 'Finalizado', true
    WHERE NOT EXISTS (SELECT 1 FROM sorteos WHERE id_perfil_creador = v_perfil_mateo AND titulo_sorteo = 'Pack de Recursos de Branding');
    SELECT id_sorteo INTO v_sorteo2 FROM sorteos WHERE id_perfil_creador = v_perfil_mateo AND titulo_sorteo = 'Pack de Recursos de Branding';

    INSERT INTO participantes_sorteo (id_sorteo, id_usuario, es_ganador, fecha_notificacion_premio)
    VALUES (v_sorteo2, v_usr_lucia, true, CURRENT_TIMESTAMP - INTERVAL '16 days')
    ON CONFLICT (id_sorteo, id_usuario) DO NOTHING;

    INSERT INTO notificaciones_sistema (id_usuario, id_tipo_notificacion, mensaje, esta_leida)
    SELECT v_usr_lucia, v_tipo_sorteo, '¡Felicidades! Ganaste el sorteo "Pack de Recursos de Branding".', true
    WHERE NOT EXISTS (SELECT 1 FROM notificaciones_sistema WHERE id_usuario = v_usr_lucia AND id_tipo_notificacion = v_tipo_sorteo);

    -- ==========================================================================
    -- 10. VERIFICACIÓN ASISTIDA POR IA (certificados_ia)
    -- ==========================================================================
    INSERT INTO certificados_ia (id_usuario, id_estado_verificacion, url_documento_s3, puntaje_confianza_ia,
                                  tipo_documento, veredicto_ia, razon_ia, id_moderador, fecha_decision, nota_moderador, documento_eliminado)
    SELECT v_usr_valentina, v_estado_aprobado, 's3://artisync-demo/verificacion/valentina-dni.pdf', 0.94, 'IDENTIDAD',
           'COINCIDE', 'Los datos del documento coinciden con el registro del usuario con alta confianza.',
           v_usr_moderador, CURRENT_TIMESTAMP - INTERVAL '60 days', 'Documento verificado correctamente, datos coinciden con el registro.', true
    WHERE NOT EXISTS (SELECT 1 FROM certificados_ia WHERE id_usuario = v_usr_valentina AND tipo_documento = 'IDENTIDAD');

    INSERT INTO certificados_ia (id_usuario, id_estado_verificacion, url_documento_s3, puntaje_confianza_ia,
                                  tipo_documento, veredicto_ia, razon_ia, documento_eliminado)
    SELECT v_usr_mateo, v_estado_pendiente, 's3://artisync-demo/verificacion/mateo-dni.pdf', 0.61, 'IDENTIDAD',
           'REQUIERE_REVISION', 'La calidad de la imagen dificulta confirmar automáticamente la coincidencia de los datos.', false
    WHERE NOT EXISTS (SELECT 1 FROM certificados_ia WHERE id_usuario = v_usr_mateo AND tipo_documento = 'IDENTIDAD');

    INSERT INTO notificaciones_sistema (id_usuario, id_tipo_notificacion, mensaje, esta_leida)
    SELECT v_usr_valentina, v_tipo_certificado, 'Tu certificado de identidad fue APROBADO.', true
    WHERE NOT EXISTS (SELECT 1 FROM notificaciones_sistema WHERE id_usuario = v_usr_valentina AND id_tipo_notificacion = v_tipo_certificado);

    -- ==========================================================================
    -- 11. AUDITORÍA (ilustrativa; en producción la llena la aplicación en vivo)
    -- ==========================================================================
    INSERT INTO auditoria_eventos (id_usuario_actor, correo_actor, modulo_auditoria, accion_auditoria, resultado_evento,
                                    entidad_afectada, id_entidad_afectada, direccion_ip, metodo_http, ruta_solicitud)
    SELECT v_usr_carlos, 'carlos.mendoza@artisync.demo', 'SEGURIDAD', 'LOGIN', 'EXITO', NULL, NULL, '190.12.45.10', 'POST', '/api/v1/auth/login'
    WHERE NOT EXISTS (SELECT 1 FROM auditoria_eventos WHERE correo_actor = 'carlos.mendoza@artisync.demo' AND accion_auditoria = 'LOGIN' AND resultado_evento = 'EXITO');

    INSERT INTO auditoria_eventos (id_usuario_actor, correo_actor, modulo_auditoria, accion_auditoria, resultado_evento,
                                    entidad_afectada, id_entidad_afectada, detalle_cambio, direccion_ip, metodo_http, ruta_solicitud)
    SELECT v_usr_valentina, 'valentina.reyes@artisync.demo', 'CATALOGO', 'SERVICIO_CREAR', 'EXITO', 'servicios', v_srv_retrato,
           jsonb_build_object('titulo', 'Retrato Digital Personalizado'), '201.34.10.5', 'POST', '/api/v1/servicios'
    WHERE NOT EXISTS (SELECT 1 FROM auditoria_eventos WHERE correo_actor = 'valentina.reyes@artisync.demo' AND accion_auditoria = 'SERVICIO_CREAR' AND id_entidad_afectada = v_srv_retrato);

    INSERT INTO auditoria_eventos (id_usuario_actor, correo_actor, modulo_auditoria, accion_auditoria, resultado_evento,
                                    entidad_afectada, id_entidad_afectada, direccion_ip, metodo_http, ruta_solicitud)
    SELECT v_usr_carlos, 'carlos.mendoza@artisync.demo', 'PEDIDOS', 'PEDIDO_CREAR', 'EXITO', 'pedidos', v_pedido1, '190.12.45.10', 'POST', '/api/v1/pedidos'
    WHERE NOT EXISTS (SELECT 1 FROM auditoria_eventos WHERE correo_actor = 'carlos.mendoza@artisync.demo' AND accion_auditoria = 'PEDIDO_CREAR' AND id_entidad_afectada = v_pedido1);

    INSERT INTO auditoria_eventos (id_usuario_actor, correo_actor, modulo_auditoria, accion_auditoria, resultado_evento,
                                    entidad_afectada, id_entidad_afectada, direccion_ip, metodo_http, ruta_solicitud)
    SELECT v_usr_auditor, 'auditor@artisync.com', 'FINANZAS', 'FONDOS_LIBERAR', 'EXITO', 'pagos_garantia', v_pago1, '10.0.0.5', 'POST', '/api/v1/pagos/liberar'
    WHERE NOT EXISTS (SELECT 1 FROM auditoria_eventos WHERE correo_actor = 'auditor@artisync.com' AND accion_auditoria = 'FONDOS_LIBERAR' AND id_entidad_afectada = v_pago1);

    INSERT INTO auditoria_eventos (id_usuario_actor, correo_actor, modulo_auditoria, accion_auditoria, resultado_evento, direccion_ip, metodo_http, ruta_solicitud)
    SELECT NULL, 'desconocido@ejemplo.com', 'SEGURIDAD', 'LOGIN', 'FALLIDO', '45.67.89.10', 'POST', '/api/v1/auth/login'
    WHERE NOT EXISTS (SELECT 1 FROM auditoria_eventos WHERE correo_actor = 'desconocido@ejemplo.com' AND accion_auditoria = 'LOGIN' AND resultado_evento = 'FALLIDO');

    RAISE NOTICE 'V24: seed de datos demo completado. Usuarios demo: 9 (3 staff, 3 creadores, 3 clientes). Pedidos: 4. Servicios: 6. Contraseña de todos: Artisync2026!';
END $$;
