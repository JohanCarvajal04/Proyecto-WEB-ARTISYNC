-- ==============================================================================
-- Servicios de relleno para las mediciones de rendimiento del Bloque C.1 (k6).
-- Requiere que ya se haya ejecutado seed-medicion-referencia.sql (subcategorías)
-- y que exista el perfil de creador de 'creador@test.com' (creado vía
-- POST /api/v1/perfiles, id_perfil obtenido dinámicamente por correo abajo).
--
-- Genera 200 servicios ACTIVO repartidos en round-robin sobre todas las
-- subcategorías existentes, para que /api/v1/catalogo no responda sobre una
-- tabla vacía. No es idempotente: pensado para correr una sola vez contra un
-- volumen fresco (docker compose down -v && up -d --build para reiniciar).
--
-- Uso:
--   docker compose exec -T postgres psql -U pfc_user -d pfc_db < database/seed-medicion-servicios.sql
-- ==============================================================================

DO $$
DECLARE
    v_id_perfil BIGINT;
    v_num_subcategorias INT;
BEGIN
    SELECT pc.id_perfil INTO v_id_perfil
    FROM perfiles_creadores pc
    JOIN usuarios u ON u.id_usuario = pc.id_usuario
    WHERE u.correo = 'creador@test.com';

    IF v_id_perfil IS NULL THEN
        RAISE EXCEPTION 'No existe perfil de creador para creador@test.com. Registrar el usuario y crear su perfil (POST /api/v1/perfiles) antes de correr este script.';
    END IF;

    SELECT COUNT(*) INTO v_num_subcategorias FROM subcategorias;
    IF v_num_subcategorias = 0 THEN
        RAISE EXCEPTION 'No hay subcategorías. Ejecutar seed-medicion-referencia.sql primero.';
    END IF;

    INSERT INTO servicios (id_perfil, id_subcategoria, titulo_servicio, descripcion_detallada, precio_base, url_miniatura)
    SELECT
        v_id_perfil,
        sub.id_subcategoria,
        'Servicio de medición #' || gs.n,
        'Descripción generada para la medición de rendimiento del Bloque C. Registro de relleno numero ' || gs.n || ' usado solo para poblar el catálogo antes de correr k6.',
        (10 + (gs.n % 90))::numeric(10,2),
        NULL
    FROM generate_series(1, 200) AS gs(n)
    JOIN LATERAL (
        SELECT id_subcategoria
        FROM subcategorias
        ORDER BY id_subcategoria
        OFFSET (gs.n % v_num_subcategorias) LIMIT 1
    ) AS sub ON TRUE;

    RAISE NOTICE 'Servicios insertados para id_perfil=%: %', v_id_perfil,
        (SELECT COUNT(*) FROM servicios WHERE id_perfil = v_id_perfil);
END $$;
