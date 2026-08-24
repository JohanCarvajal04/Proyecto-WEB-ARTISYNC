-- ===========================================================================
-- R__procedimientos.sql — ARCHIVO GENERADO. NO EDITAR A MANO.
-- ===========================================================================
--
-- Generado por scripts/sync-procs.sh a partir de db/procs/*.sql, que es la
-- ubicacion canonica de las rutinas (apartado A.2.1 de la guia de la Entrega
-- Final). Para modificar una rutina se edita su archivo en db/procs/ y se
-- ejecuta `make sync-procs`.
--
-- Migracion REPETIBLE: Flyway la reaplica cada vez que cambia su checksum.
-- Todas las rutinas usan CREATE OR REPLACE, por lo que reaplicarla es inocuo.
--
-- Rutinas incluidas (27):
--   - V8__estructuras_para_procedimientos.sql
--   - fn_calificacion_promedio_creador.sql
--   - fn_cambiar_contrasena.sql
--   - fn_cambiar_estado_cuenta.sql
--   - fn_catalogo_filtrado.sql
--   - fn_cerrar_pedidos_vencidos.sql
--   - fn_configurar_2fa.sql
--   - fn_consumir_codigo_respaldo_2fa.sql
--   - fn_crear_rol.sql
--   - fn_crear_usuario_admin.sql
--   - fn_desactivar_2fa.sql
--   - fn_eliminar_rol.sql
--   - fn_generar_codigo_pedido.sql
--   - fn_guardar_pais.sql
--   - fn_liberar_fondos_escrow.sql
--   - fn_permisos_efectivos_usuario.sql
--   - fn_registrar_infraccion.sql
--   - fn_registrar_usuario.sql
--   - fn_reporte_comisiones_creador.sql
--   - fn_resolver_estado_login.sql
--   - fn_restablecer_contrasena.sql
--   - fn_revocar_sesiones_usuario.sql
--   - fn_seleccionar_ganadores_sorteo.sql
--   - fn_sincronizar_permisos_rol.sql
--   - fn_sincronizar_roles_usuario.sql
--   - fn_solicitar_recuperacion.sql
--   - sp_purgar_datos_seguridad.sql
-- ===========================================================================


-- ---------------------------------------------------------------------------
-- Origen: db/procs/V8__estructuras_para_procedimientos.sql
-- ---------------------------------------------------------------------------
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


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_calificacion_promedio_creador.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_calificacion_promedio_creador
-- Categoria funcional: calculos agregados           Requisito: REQ-F-009
-- =============================================================================
-- Calcula la calificacion media (1..5) de un creador agregando las resenas de
-- todos los pedidos servidos por sus servicios. Recorre tres tablas:
--   resenas_servicios -> pedidos -> servicios (filtrando por id_perfil).
--
-- Sustituye a la consulta JPQL con AVG y dos JOIN de
--   repository/social/ResenaServicioRepository.calcularPromedioByCreadorIdPerfil
--
-- Devuelve NULL cuando el creador aun no acumula resenas: la ausencia de
-- calificacion es semanticamente distinta de una calificacion de 0.0, y el
-- frontend las presenta de forma distinta ("Sin valoraciones" vs "0 estrellas").
--
-- El resultado se redondea a dos decimales en el propio motor para que todos
-- los consumidores (API REST, fn_catalogo_filtrado, reportes) reciban
-- exactamente el mismo valor y no se introduzcan discrepancias de redondeo
-- entre capas.
--
-- Seguridad: parametro formal tipado BIGINT; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_calificacion_promedio_creador(
    p_id_perfil BIGINT
)
RETURNS NUMERIC(3,2)
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_promedio NUMERIC(3,2);
BEGIN
    IF p_id_perfil IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT ROUND(AVG(r.calificacion_estrellas)::NUMERIC, 2)
      INTO v_promedio
      FROM resenas_servicios r
      JOIN pedidos   p ON p.id_pedido   = r.id_pedido
      JOIN servicios s ON s.id_servicio = p.id_servicio
     WHERE s.id_perfil = p_id_perfil
       AND r.calificacion_estrellas IS NOT NULL;

    RETURN v_promedio;
END;
$$;

COMMENT ON FUNCTION fn_calificacion_promedio_creador(BIGINT)
    IS 'REQ-F-009 - Calculo agregado: calificacion media 1..5 de un creador. NULL si no tiene resenas.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_cambiar_contrasena.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_cambiar_contrasena
-- Categoria funcional: validaciones cruzadas                    Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §6 — corrige la anomalia A7.
-- =============================================================================
-- Aplica un cambio de contrasena de forma condicionada: solo si el hash
-- almacenado sigue siendo EXACTAMENTE el que Java verifico con BCrypt antes
-- de invocar esta funcion (compare-and-swap).
--
-- Sustituye a UserServiceImpl.changePassword (parte de escritura), que hacia
-- un UPDATE incondicional tras la verificacion: usuario.setContrasenaHash(...)
-- + save(). Si dos peticiones concurrentes cambiaban la contrasena del mismo
-- usuario (dos pestanas, un cliente reintentando tras un timeout aparente),
-- la segunda en escribir pisaba silenciosamente el resultado de la primera --
-- ninguna de las dos se enteraba de que "gano" la otra (actualizacion perdida).
--
-- BCrypt en si permanece fuera del motor (la comparacion de la contrasena
-- ACTUAL contra el hash se sigue haciendo en Java, con passwordEncoder.matches,
-- antes de invocar esta funcion): lo que se traslada al motor es la ESCRITURA
-- condicionada, usando el propio hash verificado como testigo de version. Si
-- el hash cambio entre la verificacion en Java y este UPDATE, el predicado
-- "contrasena_hash = p_hash_esperado" no coincide y la fila no se actualiza
-- (0 filas afectadas), sin necesidad de un SELECT ... FOR UPDATE previo.
--
-- Lanza excepcion (ERRCODE 40001, serialization_failure: el codigo estandar
-- de PostgreSQL para "otra transaccion se te adelanto") si 0 filas resultaron
-- afectadas, para que la capa Java pueda distinguir "contrasena actual
-- incorrecta" (validado antes, en Java) de "alguien mas cambio la contrasena
-- justo ahora" (aqui).
--
-- Devuelve TRUE si el cambio se aplico.
--
-- Seguridad: parametros formales tipados (los hashes BCrypt, nunca la
-- contrasena en texto plano); sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_cambiar_contrasena(
    p_id_usuario    BIGINT,
    p_hash_esperado VARCHAR(255),
    p_hash_nuevo    VARCHAR(255)
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_afectadas INTEGER;
BEGIN
    IF p_id_usuario IS NULL OR p_hash_esperado IS NULL OR p_hash_nuevo IS NULL THEN
        RAISE EXCEPTION 'fn_cambiar_contrasena: todos los parametros son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    -- El predicado contrasena_hash = p_hash_esperado es el compare-and-swap:
    -- bajo READ COMMITTED, si otra transaccion ya cambio la contrasena y
    -- confirmo, PostgreSQL re-evalua este WHERE sobre esa version nueva
    -- (EvalPlanQual) y el predicado deja de cumplirse -- ROW_COUNT queda en 0
    -- sin que ninguna de las dos escrituras se pierda en silencio.
    UPDATE usuarios
       SET contrasena_hash = p_hash_nuevo
     WHERE id_usuario = p_id_usuario
       AND contrasena_hash = p_hash_esperado;

    GET DIAGNOSTICS v_afectadas = ROW_COUNT;

    IF v_afectadas = 0 THEN
        RAISE EXCEPTION 'La contrasena fue modificada por otra sesion. Vuelve a intentarlo.'
            USING ERRCODE = '40001';
    END IF;

    RETURN TRUE;
END;
$$;

COMMENT ON FUNCTION fn_cambiar_contrasena(BIGINT, VARCHAR, VARCHAR)
    IS 'Fase 3 concurrencia - UPDATE condicionado (compare-and-swap sobre el hash) que aplica un cambio de contrasena solo si nadie mas la cambio primero, eliminando la actualizacion perdida (A7).';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_cambiar_estado_cuenta.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_cambiar_estado_cuenta
-- Categoria funcional: validaciones cruzadas                    Requisito: REQ-NF (concurrencia)
-- Fase 1 de docs/basedatos/PLAN-CONCURRENCIA-SP.md — corrige la anomalia A6 (compuesta).
-- =============================================================================
-- Cambia estado_cuenta de un usuario y, solo si la cuenta pasa de activa a
-- inactiva, revoca sus sesiones (fn_revocar_sesiones_usuario) en la MISMA
-- transaccion. Unifica el par "cambiar estado + revocar sesiones" que hoy se
-- repite, con ligeras variaciones, en:
--   - AdminUserServiceImpl.changeEstado
--   - AdminUserServiceImpl.deleteUser (soft-delete: estadoCuenta = false)
--   - UserServiceImpl.deleteOwnAccount (idem)
--   - la rama de estadoCuenta dentro de AdminUserServiceImpl.updateUser
--
-- Anomalia que corrige: sin SELECT ... FOR UPDATE, dos administradores
-- operando sobre el mismo usuario a la vez (uno reactivandolo, otro
-- desactivandolo) pueden pisarse: el que desactiva puede leer un
-- estado_cuenta ya obsoleto y decidir, incorrectamente, NO revocar sesiones
-- -- exactamente el caso en que mas importa hacerlo. El FOR UPDATE serializa
-- ambas operaciones: la segunda espera a que la primera confirme y lee su
-- resultado real, nunca un valor a medias (actualizacion perdida cerrada).
--
-- Devuelve las filas de fn_revocar_sesiones_usuario (jti + segundos
-- restantes) cuando hubo transicion activa->inactiva, o un conjunto vacio si
-- no la hubo (incluye reactivar una cuenta, o "cambiar" a un estado igual al
-- actual).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_cambiar_estado_cuenta(
    p_id_usuario BIGINT,
    p_estado     BOOLEAN
)
RETURNS TABLE (jti VARCHAR(36), segundos_restantes INTEGER)
LANGUAGE plpgsql
AS $$
DECLARE
    v_estado_anterior BOOLEAN;
BEGIN
    IF p_id_usuario IS NULL OR p_estado IS NULL THEN
        RAISE EXCEPTION 'fn_cambiar_estado_cuenta: p_id_usuario y p_estado son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    -- FOR UPDATE: serializa dos cambios de estado concurrentes del mismo
    -- usuario. Sin el, el segundo escritor podria leer un estado_anterior ya
    -- superado por el primero y decidir mal si corresponde revocar sesiones.
    SELECT estado_cuenta INTO v_estado_anterior
      FROM usuarios
     WHERE id_usuario = p_id_usuario
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario no encontrado con ID: %', p_id_usuario
            USING ERRCODE = 'P0002';
    END IF;

    UPDATE usuarios
       SET estado_cuenta = p_estado
     WHERE id_usuario = p_id_usuario;

    IF v_estado_anterior AND NOT p_estado THEN
        RETURN QUERY SELECT * FROM fn_revocar_sesiones_usuario(p_id_usuario);
    END IF;

    RETURN;
END;
$$;

COMMENT ON FUNCTION fn_cambiar_estado_cuenta(BIGINT, BOOLEAN)
    IS 'Fase 1 concurrencia - Cambia estado_cuenta y revoca sesiones (transicion activa->inactiva) atomicamente bajo SELECT FOR UPDATE, unificando el patron repetido en changeEstado/deleteUser/deleteOwnAccount.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_catalogo_filtrado.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_catalogo_filtrado
-- Categoria funcional: consultas multi-tabla        Requisito: REQ-F-013
-- =============================================================================
-- Devuelve la pagina del catalogo publico que satisface una combinacion de
-- filtros que cruza cinco tablas (servicios, subcategorias, categorias,
-- servicio_etiquetas, etiquetas) mas la agregacion de resenas del creador.
--
-- Sustituye a la Specification dinamica de Java
-- (specification/catalogo/ServicioSpecification.java), que construia el
-- predicado en tiempo de ejecucion desde la capa de servicio.
--
-- Contrato de tipo de retorno: JSONB. Se devuelve un documento en lugar de un
-- refcursor para que la rutina sea invocable con @Procedure de Spring Data JPA
-- bajo el modo por defecto del driver (escapeSyntaxCallMode=select); ver
-- db/procs/README.md, seccion "Por que fn_ y no sp_".
--
-- El predicado se evalua UNA sola vez: el total de coincidencias se obtiene con
-- la funcion ventana COUNT(*) OVER () sobre el mismo conjunto que se pagina, de
-- modo que total y elementos no pueden divergir.
--
-- Seguridad: todos los filtros son parametros formales tipados. No hay
-- concatenacion ni EXECUTE dinamico: el predicado se neutraliza con el patron
-- "(p_x IS NULL OR columna = p_x)", que deja el filtro inactivo cuando el
-- parametro no se envia. El unico operador || presente concatena los comodines
-- de un ILIKE sobre un parametro ya tipado como VARCHAR, no construye SQL.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_catalogo_filtrado(
    p_id_categoria    BIGINT        DEFAULT NULL,
    p_id_subcategoria BIGINT        DEFAULT NULL,
    p_precio_min      NUMERIC(10,2) DEFAULT NULL,
    p_precio_max      NUMERIC(10,2) DEFAULT NULL,
    p_etiqueta        VARCHAR(50)   DEFAULT NULL,
    p_texto           VARCHAR(150)  DEFAULT NULL,
    p_limite          INTEGER       DEFAULT 20,
    p_desplazamiento  INTEGER       DEFAULT 0
)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_limite   INTEGER;
    v_offset   INTEGER;
    v_total    BIGINT  := 0;
    v_elementos JSONB;
BEGIN
    -- Normalizacion defensiva de la paginacion: evita que un limite negativo o
    -- desmesurado llegue al motor. 100 es el tope declarado en el SRS (REQ-NF-004).
    v_limite := LEAST(GREATEST(COALESCE(p_limite, 20), 1), 100);
    v_offset := GREATEST(COALESCE(p_desplazamiento, 0), 0);

    WITH filtrados AS (
        SELECT s.id_servicio,
               s.titulo_servicio,
               s.descripcion_detallada,
               s.precio_base,
               s.url_miniatura,
               s.tipo_item,
               s.id_perfil,
               sc.id_subcategoria,
               sc.nombre_subcategoria,
               c.id_categoria,
               c.nombre_categoria,
               COUNT(*) OVER () AS total_coincidencias
          FROM servicios s
          JOIN subcategorias sc ON sc.id_subcategoria = s.id_subcategoria
          JOIN categorias    c  ON c.id_categoria     = sc.id_categoria
         WHERE s.estado_publicacion = 'ACTIVO'
           AND c.estado_activa IS TRUE
           AND (p_id_categoria    IS NULL OR c.id_categoria     = p_id_categoria)
           AND (p_id_subcategoria IS NULL OR sc.id_subcategoria = p_id_subcategoria)
           AND (p_precio_min      IS NULL OR s.precio_base     >= p_precio_min)
           AND (p_precio_max      IS NULL OR s.precio_base     <= p_precio_max)
           AND (p_texto           IS NULL OR s.titulo_servicio ILIKE '%' || p_texto || '%')
           AND (p_etiqueta IS NULL OR EXISTS (
                   SELECT 1
                     FROM servicio_etiquetas se
                     JOIN etiquetas e ON e.id_etiqueta = se.id_etiqueta
                    WHERE se.id_servicio = s.id_servicio
                      AND e.nombre_etiqueta = p_etiqueta))
         ORDER BY s.id_servicio
         LIMIT v_limite OFFSET v_offset
    )
    SELECT COALESCE(MAX(f.total_coincidencias), 0),
           COALESCE(
               jsonb_agg(
                   jsonb_build_object(
                       'idServicio',        f.id_servicio,
                       'titulo',            f.titulo_servicio,
                       'descripcion',       f.descripcion_detallada,
                       'precioBase',        f.precio_base,
                       'urlMiniatura',      f.url_miniatura,
                       'tipoItem',          f.tipo_item,
                       'idPerfil',          f.id_perfil,
                       'idSubcategoria',    f.id_subcategoria,
                       'subcategoria',      f.nombre_subcategoria,
                       'idCategoria',       f.id_categoria,
                       'categoria',         f.nombre_categoria,
                       'calificacionMedia', fn_calificacion_promedio_creador(f.id_perfil),
                       'etiquetas',         COALESCE((
                             SELECT jsonb_agg(e.nombre_etiqueta ORDER BY e.nombre_etiqueta)
                               FROM servicio_etiquetas se
                               JOIN etiquetas e ON e.id_etiqueta = se.id_etiqueta
                              WHERE se.id_servicio = f.id_servicio), '[]'::JSONB)
                   )
                   ORDER BY f.id_servicio
               ),
               '[]'::JSONB)
      INTO v_total, v_elementos
      FROM filtrados f;

    RETURN jsonb_build_object(
        'total',     v_total,
        'limite',    v_limite,
        'offset',    v_offset,
        'elementos', COALESCE(v_elementos, '[]'::JSONB)
    );
END;
$$;

COMMENT ON FUNCTION fn_catalogo_filtrado(BIGINT, BIGINT, NUMERIC, NUMERIC, VARCHAR, VARCHAR, INTEGER, INTEGER)
    IS 'REQ-F-013 - Consulta multi-tabla del catalogo publico con filtros combinados. Devuelve JSONB {total, limite, offset, elementos[]}.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_cerrar_pedidos_vencidos.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_cerrar_pedidos_vencidos
-- Categoria funcional: actualizaciones masivas      Requisito: REQ-F-019
-- =============================================================================
-- Cierra en bloque los pedidos cuya fecha de entrega estimada ya vencio y que
-- todavia no alcanzaron una etapa final de su flujo de trabajo. El cierre se
-- materializa como una transicion nueva en historial_estados_pedido hacia la
-- etapa final configurada para el flujo del pedido (flujo_etapas_config con
-- es_etapa_final = TRUE y el numero_orden mas alto).
--
-- El modelo de datos de Artisync no guarda el estado en la tabla pedidos: el
-- estado vigente es la ultima fila de historial_estados_pedido. Por eso el
-- cierre es un INSERT y no un UPDATE, y por eso la rutina es idempotente: un
-- pedido que ya esta en etapa final queda excluido por el predicado.
--
-- Ejecutar esta operacion como una sola sentencia en el motor, en lugar de
-- iterar en Java pedido por pedido, evita el patron N+1 que tenia
-- PedidoServicioImpl y garantiza que todas las transiciones compartan la misma
-- transaccion y la misma marca de tiempo.
--
-- Devuelve el numero de pedidos cerrados.
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_cerrar_pedidos_vencidos(
    p_dias_gracia INTEGER DEFAULT 0
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_gracia   INTEGER;
    v_limite   TIMESTAMP;
    v_cerrados INTEGER := 0;
BEGIN
    -- Un periodo de gracia negativo cerraria pedidos aun vigentes.
    v_gracia := GREATEST(COALESCE(p_dias_gracia, 0), 0);
    v_limite := CURRENT_TIMESTAMP - (v_gracia * INTERVAL '1 day');

    WITH etapa_final_por_flujo AS (
        -- Etapa final de cada flujo. DISTINCT ON toma la de mayor numero_orden
        -- cuando un flujo declara mas de una etapa terminal.
        SELECT DISTINCT ON (fec.id_flujo)
               fec.id_flujo,
               fec.id_etapa
          FROM flujo_etapas_config fec
         WHERE fec.es_etapa_final IS TRUE
         ORDER BY fec.id_flujo, fec.numero_orden DESC
    ),
    ultimo_estado AS (
        SELECT DISTINCT ON (h.id_pedido)
               h.id_pedido,
               h.id_etapa
          FROM historial_estados_pedido h
         ORDER BY h.id_pedido, h.fecha_transicion DESC, h.id_historial_estado DESC
    ),
    vencidos AS (
        SELECT p.id_pedido,
               ef.id_etapa
          FROM pedidos p
          JOIN etapa_final_por_flujo ef ON ef.id_flujo = p.id_flujo
          LEFT JOIN ultimo_estado ue    ON ue.id_pedido = p.id_pedido
         WHERE p.fecha_entrega_estimada IS NOT NULL
           AND p.fecha_entrega_estimada < v_limite
           -- Excluye los que ya estan en una etapa final (idempotencia).
           AND (ue.id_etapa IS NULL OR ue.id_etapa <> ef.id_etapa)
    )
    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v.id_pedido,
           v.id_etapa,
           CURRENT_TIMESTAMP,
           'Cierre automatico por vencimiento de la fecha de entrega estimada '
           || '(fn_cerrar_pedidos_vencidos, dias de gracia: ' || v_gracia || ')'
      FROM vencidos v;

    GET DIAGNOSTICS v_cerrados = ROW_COUNT;
    RETURN v_cerrados;
END;
$$;

COMMENT ON FUNCTION fn_cerrar_pedidos_vencidos(INTEGER)
    IS 'REQ-F-019 - Actualizacion masiva: cierra los pedidos vencidos insertando su transicion a etapa final. Idempotente. Devuelve el numero de pedidos cerrados.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_configurar_2fa.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_configurar_2fa
-- Categoria funcional: validaciones cruzadas + escritura multi-tabla   Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §7 — corrige la anomalia A4.
-- =============================================================================
-- Inicia o reinicia la configuracion de 2FA de un usuario: fija la nueva
-- llave secreta TOTP y reemplaza el juego completo de codigos de respaldo.
--
-- Sustituye a TwoFactorServiceImpl.setup2Fa (parte de escritura): upsert de
-- autenticacion_dos_factores + DELETE de codigos anteriores + 8 INSERT
-- individuales de codigos_respaldo_2fa -- 10 viajes no atomicos a la base. Si
-- el proceso fallaba a mitad del bucle de codigos (timeout, caida de
-- conexion), el usuario quedaba con un secreto TOTP nuevo pero un juego de
-- codigos de respaldo incompleto, sin ningun aviso.
--
-- El upsert usa ON CONFLICT sobre autenticacion_dos_factores.id_usuario, que
-- ya es UNIQUE (V1__schema_inicial.sql): no hay ventana entre "verificar si
-- existe" e "insertar o actualizar". El borrado + alta de los 8 codigos ocurre
-- en la MISMA transaccion que el upsert del secreto: es imposible observar un
-- secreto nuevo emparejado con codigos de respaldo del secreto anterior.
--
-- Los codigos de respaldo llegan ya hasheados (SHA-256, calculado en Java
-- antes de invocar esta funcion, igual que en fn_restablecer_contrasena): la
-- funcion nunca ve el codigo en texto plano.
--
-- Devuelve el numero de codigos de respaldo insertados (normalmente 8).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_configurar_2fa(
    p_id_usuario    BIGINT,
    p_llave_secreta VARCHAR(255),
    p_hashes        TEXT[]
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_total INTEGER := 0;
BEGIN
    IF p_id_usuario IS NULL OR p_llave_secreta IS NULL THEN
        RAISE EXCEPTION 'fn_configurar_2fa: p_id_usuario y p_llave_secreta son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = p_id_usuario) THEN
        RAISE EXCEPTION 'Usuario no encontrado con ID: %', p_id_usuario
            USING ERRCODE = 'P0002';
    END IF;

    -- Upsert atomico: nunca hay una ventana en la que el registro no exista y
    -- dos peticiones concurrentes de setup2Fa intenten ambas un INSERT.
    INSERT INTO autenticacion_dos_factores (id_usuario, llave_secreta, esta_habilitado)
    VALUES (p_id_usuario, p_llave_secreta, FALSE)
    ON CONFLICT (id_usuario)
    DO UPDATE SET llave_secreta = EXCLUDED.llave_secreta, esta_habilitado = FALSE;

    -- Borrado + alta de los codigos en la MISMA transaccion que el upsert de
    -- arriba: un secreto nuevo siempre viene acompanado de SU juego completo
    -- de codigos de respaldo, nunca de uno parcial o del anterior.
    DELETE FROM codigos_respaldo_2fa WHERE id_usuario = p_id_usuario;

    -- CR-01 (revision de codigo): GET DIAGNOSTICS captura el ROW_COUNT de la
    -- ULTIMA sentencia ejecutada, no de este INSERT en particular. Antes vivia
    -- fuera de este IF, asi que con p_hashes NULL/vacio devolvia el ROW_COUNT
    -- del DELETE de arriba (hasta 8) en vez de 0, contradiciendo el contrato
    -- documentado ("Devuelve el numero de codigos de respaldo insertados").
    -- v_total se inicializa en 0 arriba para que ese caso devuelva el valor
    -- correcto sin necesidad de un ELSE.
    IF p_hashes IS NOT NULL AND array_length(p_hashes, 1) > 0 THEN
        INSERT INTO codigos_respaldo_2fa (id_usuario, codigo_hash, usado)
        SELECT p_id_usuario, h, FALSE
          FROM unnest(p_hashes) AS h;

        GET DIAGNOSTICS v_total = ROW_COUNT;
    END IF;

    RETURN v_total;
END;
$$;

COMMENT ON FUNCTION fn_configurar_2fa(BIGINT, VARCHAR, TEXT[])
    IS 'Fase 3 concurrencia - Upsert atomico del secreto TOTP + reemplazo completo de codigos de respaldo en una unica transaccion, eliminando el estado a medias (A4) de la version en 10 pasos.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_consumir_codigo_respaldo_2fa.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_consumir_codigo_respaldo_2fa
-- Categoria funcional: validaciones cruzadas                    Requisito: REQ-NF (concurrencia)
-- Fase 1 de docs/basedatos/PLAN-CONCURRENCIA-SP.md — corrige la anomalia A1.
-- =============================================================================
-- Marca un codigo de respaldo 2FA como usado, si y solo si sigue sin usar.
--
-- Sustituye a TwoFactorServiceImpl.validarCodigoOBackup() en la rama de
-- codigos de respaldo, que hacia: SELECT de todos los codigos no usados del
-- usuario -> comparar el hash en un bucle Java -> UPDATE del que coincide.
-- Ese patron read-modify-write NO es atomico: dos peticiones concurrentes con
-- el mismo codigo leen ambas usado = FALSE antes de que ninguna escriba, y
-- ambas terminan devolviendo TRUE. Un codigo de un solo uso quedaba
-- consumible dos veces (actualizacion perdida) -- un bypass de segundo factor.
--
-- Por que esta forma lo corrige: es una UNICA sentencia UPDATE con el propio
-- "usado = FALSE" como predicado. Bajo READ COMMITTED (el nivel del proyecto,
-- ver docs/basedatos/PLAN-CONCURRENCIA-SP.md §0.4), cuando un UPDATE encuentra
-- una fila que otra transaccion ya modifico y confirmo, PostgreSQL re-evalua
-- el WHERE sobre esa version nueva (EvalPlanQual) antes de aplicar el cambio.
-- Si la primera transaccion ya puso usado = TRUE, la segunda ve el predicado
-- fallar y no actualiza nada: no hace falta SELECT ... FOR UPDATE previo, el
-- propio UPDATE toma el bloqueo de fila y la re-evaluacion cierra la ventana.
--
-- Devuelve TRUE si este codigo (usuario + hash) existia y no estaba usado
-- -- exactamente uno de los llamantes concurrentes lo recibe --, FALSE en
-- cualquier otro caso (no existe, o ya estaba usado).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE. Solo
-- ve el hash SHA-256 del codigo, nunca el codigo en texto plano (se calcula
-- en Java antes de invocar esta funcion, igual que en fn_restablecer_contrasena).
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_consumir_codigo_respaldo_2fa(
    p_id_usuario  BIGINT,
    p_codigo_hash VARCHAR(255)
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_codigo BIGINT;
BEGIN
    IF p_id_usuario IS NULL OR p_codigo_hash IS NULL THEN
        RAISE EXCEPTION 'fn_consumir_codigo_respaldo_2fa: p_id_usuario y p_codigo_hash son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    UPDATE codigos_respaldo_2fa
       SET usado = TRUE
     WHERE id_usuario = p_id_usuario
       AND codigo_hash = p_codigo_hash
       AND usado = FALSE
    RETURNING id_codigo INTO v_id_codigo;

    RETURN v_id_codigo IS NOT NULL;
END;
$$;

COMMENT ON FUNCTION fn_consumir_codigo_respaldo_2fa(BIGINT, VARCHAR)
    IS 'Fase 1 concurrencia - UPDATE atomico que consume un codigo de respaldo 2FA una sola vez, eliminando la actualizacion perdida del patron read-modify-write anterior.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_crear_rol.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_crear_rol
-- Categoria funcional: validaciones cruzadas                    Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §4 — corrige la anomalia A8.
-- =============================================================================
-- Crea un rol personalizado y le asigna sus permisos iniciales, en una unica
-- transaccion.
--
-- Sustituye a RolePermissionServiceImpl.createRole, que comprobaba
-- rolRepository.findByNombreRol(...).isPresent() y luego hacia save() en
-- sentencias separadas -- lectura fantasma no atomica: entre la comprobacion
-- y el insert, otra transaccion podia crear un rol con el mismo nombre.
-- Mitigado en la practica por roles.nombre_rol UNIQUE, pero sin traduccion de
-- error (500 crudo en vez de 409).
--
-- Captura la violacion de unicidad con un bloque EXCEPTION (mismo molde que
-- fn_crear_usuario_admin) en vez de una comprobacion previa: PostgreSQL no
-- ofrece bloqueo de rango bajo READ COMMITTED, asi que la restriccion UNIQUE
-- como predicado es la unica defensa correcta.
--
-- Delega la asignacion de permisos iniciales en fn_sincronizar_permisos_rol
-- (REQ-F-003, ya existente): evita reimplementar la validacion de codigos de
-- permiso y garantiza el mismo comportamiento que syncPermissions.
--
-- Devuelve el id_rol generado. Lanza excepcion si el nombre ya existe, o si
-- algun codigo de permiso inicial es invalido (via fn_sincronizar_permisos_rol).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_crear_rol(
    p_nombre_rol       VARCHAR(50),
    p_descripcion_rol  TEXT,
    p_codigos_permiso  TEXT[]
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_rol         BIGINT;
    v_nombre_rol_norm VARCHAR(50);
BEGIN
    IF p_nombre_rol IS NULL OR btrim(p_nombre_rol) = '' THEN
        RAISE EXCEPTION 'fn_crear_rol: p_nombre_rol es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    v_nombre_rol_norm := UPPER(btrim(p_nombre_rol));

    -- Subtransaccion explicita: el bloque EXCEPTION abre un SAVEPOINT
    -- implicito. Si el INSERT viola uq roles.nombre_rol (fantasma
    -- materializado por otra transaccion concurrente), se hace ROLLBACK TO
    -- SAVEPOINT automatico y se traduce a 409 en vez de un 500 crudo.
    BEGIN
        INSERT INTO roles (nombre_rol, descripcion_rol)
        VALUES (v_nombre_rol_norm, p_descripcion_rol)
        RETURNING id_rol INTO v_id_rol;
    EXCEPTION
        WHEN unique_violation THEN
            RAISE EXCEPTION 'Ya existe un rol con el nombre: %', v_nombre_rol_norm
                USING ERRCODE = '23505';
    END;

    IF p_codigos_permiso IS NOT NULL AND array_length(p_codigos_permiso, 1) > 0 THEN
        PERFORM fn_sincronizar_permisos_rol(v_nombre_rol_norm, p_codigos_permiso);
    END IF;

    RETURN v_id_rol;
END;
$$;

COMMENT ON FUNCTION fn_crear_rol(VARCHAR, TEXT, TEXT[])
    IS 'Fase 3 concurrencia - Crea un rol y asigna sus permisos iniciales atomicamente, capturando unique_violation en vez de una comprobacion findByNombreRol no atomica (A8).';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_crear_usuario_admin.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_crear_usuario_admin
-- Categoria funcional: validaciones cruzadas + inserción multi-tabla   Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §4 — corrige la anomalia A3.
-- =============================================================================
-- Crea un usuario desde el panel administrativo, con su(s) rol(es) y el
-- perfil de creador si corresponde, en una unica transaccion.
--
-- Sustituye a AdminUserServiceImpl.createUser, que comprobaba
-- existsByCorreo(...) y luego hacia save() en sentencias separadas -- una
-- comprobacion "existe?" no atomica respecto a la insercion (lectura
-- fantasma): entre ambas, otra transaccion podia insertar el mismo correo.
-- PostgreSQL no ofrece bloqueo de rango bajo READ COMMITTED ni REPEATABLE READ
-- (no se puede "bloquear un correo que aun no existe"), asi que la unica
-- defensa correcta es la restriccion UNIQUE usuarios.correo como predicado,
-- capturada aqui con un bloque EXCEPTION en vez de una comprobacion previa.
-- En el peor caso el correo duplicado terminaba en un 500 crudo en vez del
-- 409 CONFLICT esperado (el fantasma SI quedaba bloqueado por el UNIQUE, pero
-- sin traduccion de error).
--
-- Delega la asignacion de roles (y el alta perezosa de perfiles_creadores) en
-- fn_sincronizar_roles_usuario (Fase 1, docs/basedatos/PLAN-CONCURRENCIA-SP.md
-- §3): evita reimplementar esa logica y garantiza el mismo comportamiento que
-- assignRoles/updateUser para el caso "rol CREADOR incluido".
--
-- Devuelve el id_usuario generado. Lanza excepcion si el correo ya existe, si
-- el pais no existe, o si algun rol solicitado no existe (via
-- fn_sincronizar_roles_usuario).
--
-- Seguridad: parametros formales tipados (la contrasena llega ya hasheada con
-- BCrypt, calculada en Java); sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_crear_usuario_admin(
    p_nombres          VARCHAR(100),
    p_apellidos        VARCHAR(100),
    p_correo           VARCHAR(150),
    p_contrasena_hash  VARCHAR(255),
    p_fecha_nacimiento DATE,
    p_id_pais          BIGINT,
    p_estado_cuenta    BOOLEAN,
    p_nombres_rol      TEXT[]
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_usuario BIGINT;
BEGIN
    IF p_correo IS NULL OR p_contrasena_hash IS NULL THEN
        RAISE EXCEPTION 'fn_crear_usuario_admin: p_correo y p_contrasena_hash son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    IF p_id_pais IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pais WHERE id_pais = p_id_pais) THEN
        RAISE EXCEPTION 'Pais no encontrado' USING ERRCODE = '23503';
    END IF;

    -- Subtransaccion explicita: el bloque EXCEPTION abre un SAVEPOINT
    -- implicito. Si el INSERT viola uq usuarios.correo (fantasma
    -- materializado por otra transaccion concurrente), se hace ROLLBACK TO
    -- SAVEPOINT automatico y se traduce a 409 en vez de un 500 crudo.
    BEGIN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash,
                               fecha_nacimiento, id_pais, estado_cuenta)
        VALUES (p_nombres, p_apellidos, p_correo, p_contrasena_hash,
                p_fecha_nacimiento, p_id_pais, COALESCE(p_estado_cuenta, TRUE))
        RETURNING id_usuario INTO v_id_usuario;
    EXCEPTION
        WHEN unique_violation THEN
            RAISE EXCEPTION 'El correo ya esta registrado: %', p_correo
                USING ERRCODE = '23505';
    END;

    PERFORM fn_sincronizar_roles_usuario(
        v_id_usuario,
        COALESCE(NULLIF(p_nombres_rol, ARRAY[]::TEXT[]), ARRAY['CLIENTE']));

    RETURN v_id_usuario;
END;
$$;

COMMENT ON FUNCTION fn_crear_usuario_admin(VARCHAR, VARCHAR, VARCHAR, VARCHAR, DATE, BIGINT, BOOLEAN, TEXT[])
    IS 'Fase 3 concurrencia - Crea un usuario administrativo con sus roles en una transaccion atomica, capturando unique_violation en vez de una comprobacion existsByCorreo no atomica (A3).';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_desactivar_2fa.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_desactivar_2fa
-- Categoria funcional: validaciones cruzadas + escritura multi-tabla   Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §7 — corrige la anomalia A4.
-- =============================================================================
-- Desactiva el 2FA de un usuario y purga sus codigos de respaldo, en una
-- unica transaccion. Contraparte de fn_configurar_2fa; unifica el codigo que
-- antes estaba DUPLICADO entre dos sitios que hacian exactamente lo mismo:
--   - TwoFactorServiceImpl.disable2Fa (con el codigo/TOTP ya validado)
--   - AdminUserServiceImpl.updateUser, rama dosFactoresHabilitado = false
--     (el administrador fuerza la desactivacion sin validar codigo)
-- Ambos hacian: findByUsuarioIdUsuario + set esta_habilitado=false + save +
-- deleteByUsuarioIdUsuario en Java, sin atomicidad entre el UPDATE y el DELETE.
--
-- Es intencionalmente IDEMPOTENTE y silenciosa si el usuario no tiene 2FA
-- configurado: devuelve FALSE en vez de lanzar excepcion, porque el caso de
-- uso administrativo (forzar 2FA=false) es legitimo aunque el usuario nunca
-- lo haya configurado -- exactamente el comportamiento que ya tenia el
-- `ifPresent(...)` de AdminUserServiceImpl.updateUser.
--
-- Devuelve TRUE si habia un registro de 2FA y quedo desactivado, FALSE si el
-- usuario no tenia 2FA configurado (no-op).
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_desactivar_2fa(
    p_id_usuario BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_afectadas INTEGER;
BEGIN
    IF p_id_usuario IS NULL THEN
        RAISE EXCEPTION 'fn_desactivar_2fa: p_id_usuario es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    UPDATE autenticacion_dos_factores
       SET esta_habilitado = FALSE
     WHERE id_usuario = p_id_usuario;

    GET DIAGNOSTICS v_afectadas = ROW_COUNT;

    -- Purga de codigos de respaldo en la misma transaccion que el UPDATE: no
    -- hay ventana en la que el 2FA aparezca desactivado pero sus codigos de
    -- respaldo sigan siendo validos (o viceversa).
    DELETE FROM codigos_respaldo_2fa WHERE id_usuario = p_id_usuario;

    RETURN v_afectadas > 0;
END;
$$;

COMMENT ON FUNCTION fn_desactivar_2fa(BIGINT)
    IS 'Fase 3 concurrencia - Desactiva 2FA y purga codigos de respaldo atomicamente; idempotente si el usuario no tenia 2FA configurado. Unifica el codigo duplicado entre TwoFactorServiceImpl.disable2Fa y AdminUserServiceImpl.updateUser.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_eliminar_rol.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_eliminar_rol
-- Categoria funcional: validaciones cruzadas                          Requisito: REQ-F-004
-- =============================================================================
-- Elimina un rol personalizado tras comprobar, dentro de la misma transaccion,
-- dos condiciones de negocio: que no sea uno de los roles base protegidos del
-- sistema, y que no tenga usuarios activos asignados (usuario_roles).
-- Sustituye a RolePermissionServiceImpl.deleteRole, que cargaba el rol
-- completo, validaba en Java contra un Set<String> de roles protegidos y
-- consultaba usuarioRolRepository.existsByRolIdRol antes de delete().
--
-- Por que en el motor: la comprobacion "sin usuarios asignados" debe ser
-- atomica frente al DELETE que la sigue -- si un UsuarioRol se insertara entre
-- la validacion y el borrado, se perderia la asociacion silenciosamente. Al
-- resolverlo como una unica sentencia dentro de la funcion, la validacion y el
-- borrado comparten la misma transaccion.
--
-- Devuelve TRUE si elimino el rol. Lanza excepcion si el rol no existe, si es
-- un rol base protegido, o si tiene usuarios activos asignados.
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_eliminar_rol(
    p_id_rol BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_nombre_rol VARCHAR(50);
    v_usuarios_asignados INTEGER;
    v_roles_protegidos TEXT[] := ARRAY['ADMIN', 'CLIENTE', 'CREADOR', 'MODERADOR', 'SOPORTE', 'AUDITOR_FINANCIERO'];
BEGIN
    IF p_id_rol IS NULL THEN
        RAISE EXCEPTION 'fn_eliminar_rol: p_id_rol es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    SELECT nombre_rol INTO v_nombre_rol FROM roles WHERE id_rol = p_id_rol;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Rol no encontrado con ID: %', p_id_rol
            USING ERRCODE = 'P0002';
    END IF;

    IF UPPER(v_nombre_rol) = ANY (v_roles_protegidos) THEN
        RAISE EXCEPTION 'No se puede eliminar un rol base del sistema: %', v_nombre_rol
            USING ERRCODE = '23514';
    END IF;

    SELECT COUNT(*) INTO v_usuarios_asignados
      FROM usuario_roles
     WHERE id_rol = p_id_rol;

    IF v_usuarios_asignados > 0 THEN
        RAISE EXCEPTION 'No se puede eliminar el rol porque tiene usuarios activos asignados: %', v_nombre_rol
            USING ERRCODE = '23514';
    END IF;

    DELETE FROM rol_permisos WHERE id_rol = p_id_rol;
    DELETE FROM roles WHERE id_rol = p_id_rol;

    RETURN TRUE;
END;
$$;

COMMENT ON FUNCTION fn_eliminar_rol(BIGINT)
    IS 'REQ-F-004 - Validacion cruzada: elimina un rol personalizado solo si no es un rol base protegido y no tiene usuarios asignados.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_generar_codigo_pedido.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_generar_codigo_pedido
-- Categoria funcional: generacion de codigos secuenciales   Requisito: REQ-F-018
-- =============================================================================
-- Genera y asigna el codigo publico de un pedido con el formato
--
--     ART-<AAAA>-<NNNNNN>        p. ej.  ART-2026-000042
--
-- donde <AAAA> es el ano de creacion del pedido y <NNNNNN> un correlativo de
-- seis digitos tomado de la secuencia seq_codigo_pedido.
--
-- Por que una secuencia y no MAX(codigo)+1: la secuencia entrega valores
-- unicos sin bloquear la tabla y sin sufrir condiciones de carrera cuando dos
-- pedidos se crean a la vez. Es la razon por la que esta operacion no puede
-- resolverse con un contador en Java: dos instancias del backend generarian el
-- mismo numero. La contrapartida aceptada es que los huecos en la secuencia son
-- posibles tras un rollback; el codigo es un identificador, no un contador de
-- pedidos, y su unicidad la garantiza ademas la restriccion UNIQUE de la tabla.
--
-- La rutina es idempotente: si el pedido ya tiene codigo asignado lo devuelve
-- sin consumir un valor nuevo de la secuencia.
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE. El operador
-- || construye el texto del codigo a partir de valores ya tipados, no SQL.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_generar_codigo_pedido(
    p_id_pedido BIGINT
)
RETURNS VARCHAR(20)
LANGUAGE plpgsql
AS $$
DECLARE
    v_codigo_existente VARCHAR(20);
    v_anio             TEXT;
    v_correlativo      BIGINT;
    v_codigo           VARCHAR(20);
BEGIN
    IF p_id_pedido IS NULL THEN
        RAISE EXCEPTION 'fn_generar_codigo_pedido: p_id_pedido es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    SELECT p.codigo_pedido,
           to_char(COALESCE(p.fecha_inicio, CURRENT_TIMESTAMP), 'YYYY')
      INTO v_codigo_existente, v_anio
      FROM pedidos p
     WHERE p.id_pedido = p_id_pedido
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El pedido % no existe', p_id_pedido
            USING ERRCODE = '23503';
    END IF;

    -- Idempotencia: no se consume secuencia si el codigo ya esta asignado.
    IF v_codigo_existente IS NOT NULL THEN
        RETURN v_codigo_existente;
    END IF;

    v_correlativo := nextval('seq_codigo_pedido');
    v_codigo      := 'ART-' || v_anio || '-' || lpad(v_correlativo::TEXT, 6, '0');

    UPDATE pedidos
       SET codigo_pedido = v_codigo
     WHERE id_pedido = p_id_pedido;

    RETURN v_codigo;
END;
$$;

COMMENT ON FUNCTION fn_generar_codigo_pedido(BIGINT)
    IS 'REQ-F-018 - Generacion de codigo secuencial ART-AAAA-NNNNNN para un pedido. Idempotente: devuelve el codigo ya asignado si existe.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_guardar_pais.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_guardar_pais
-- Categoria funcional: validaciones cruzadas                    Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §4 — corrige la anomalia A9.
-- =============================================================================
-- Crea (p_id_pais NULL) o renombra (p_id_pais con valor) un pais, validando
-- la unicidad del nombre de forma atomica respecto a la escritura.
--
-- Sustituye la parte de escritura de PaisServiceImpl.createPais y .updatePais,
-- que comprobaban paisRepository.findByNombrePais(...) y luego hacian save()
-- en sentencias separadas -- lectura fantasma no atomica: entre la
-- comprobacion y el insert/update, otra transaccion podia tomar el mismo
-- nombre. Mitigado en la practica por pais.nombre_pais UNIQUE, pero sin
-- traduccion de error (ExcepcionRecursoDuplicado nunca se lanzaba realmente
-- por una condicion de carrera; solo por la lectura previa, que era la parte
-- no atomica).
--
-- Una sola rutina cubre ambos casos porque comparten la misma tecnica: ni
-- crear ni renombrar pueden usar SELECT ... FOR UPDATE para "bloquear" el
-- nombre en conflicto (esa fila, si existe, pertenece a OTRO pais, no al que
-- se esta creando o editando); la unica defensa correcta en ambos casos es la
-- restriccion UNIQUE como predicado, capturada con un bloque EXCEPTION.
-- Renombrar un pais a su propio nombre actual no dispara la restriccion (es
-- la misma fila, mismo valor), preservando el comportamiento previo de
-- permitir un "no-op" de nombre.
--
-- Devuelve el id_pais afectado. Lanza excepcion si el nombre ya pertenece a
-- otro pais, o (al renombrar) si el id no existe.
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_guardar_pais(
    p_id_pais     BIGINT,
    p_nombre_pais VARCHAR(100)
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_pais      BIGINT;
    v_nombre_norm  VARCHAR(100);
BEGIN
    IF p_nombre_pais IS NULL OR btrim(p_nombre_pais) = '' THEN
        RAISE EXCEPTION 'fn_guardar_pais: p_nombre_pais es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    v_nombre_norm := btrim(p_nombre_pais);

    -- Subtransaccion explicita: el bloque EXCEPTION abre un SAVEPOINT
    -- implicito. Si el INSERT/UPDATE viola uq pais.nombre_pais (fantasma
    -- materializado por otra transaccion concurrente), se hace ROLLBACK TO
    -- SAVEPOINT automatico y se traduce a 409 en vez de dejar pasar el
    -- duplicado o fallar con un error generico.
    BEGIN
        IF p_id_pais IS NULL THEN
            INSERT INTO pais (nombre_pais)
            VALUES (v_nombre_norm)
            RETURNING id_pais INTO v_id_pais;
        ELSE
            UPDATE pais
               SET nombre_pais = v_nombre_norm
             WHERE id_pais = p_id_pais
            RETURNING id_pais INTO v_id_pais;

            IF NOT FOUND THEN
                RAISE EXCEPTION 'Pais no encontrado con ID: %', p_id_pais
                    USING ERRCODE = 'P0002';
            END IF;
        END IF;
    EXCEPTION
        WHEN unique_violation THEN
            RAISE EXCEPTION 'Ya existe un pais registrado con el nombre: %', v_nombre_norm
                USING ERRCODE = '23505';
    END;

    RETURN v_id_pais;
END;
$$;

COMMENT ON FUNCTION fn_guardar_pais(BIGINT, VARCHAR)
    IS 'Fase 3 concurrencia - Crea o renombra un pais capturando unique_violation en vez de una comprobacion findByNombrePais no atomica (A9).';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_liberar_fondos_escrow.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_liberar_fondos_escrow
-- Categoria funcional: validaciones cruzadas        Requisito: REQ-F-021
-- =============================================================================
-- Libera los fondos retenidos de un pedido bajo el patron escrow, pero solo
-- despues de comprobar, dentro de la misma transaccion y con el registro
-- bloqueado, las cinco condiciones de negocio que lo autorizan:
--
--   1. El pedido tiene contrato formalizado y pago de garantia asociado.
--   2. Los fondos estan en estado 'Retenido' (no liberados ni reembolsados).
--   3. El contrato esta firmado por ambas partes (cliente y creador).
--   4. Existe al menos un entregable final cargado para el pedido.
--   5. No queda ningun ticket de revision abierto.
--
-- Por que en el motor y no en PagoServicioImpl: las cinco lecturas y las dos
-- escrituras deben ser atomicas frente a otra liberacion concurrente del mismo
-- pedido. La fila de pagos_garantia se toma con SELECT ... FOR UPDATE, de modo
-- que dos llamadas simultaneas se serializan y la segunda observa el estado ya
-- cambiado por la primera. Resolverlo en Java exigiria un bloqueo pesimista
-- explicito y varias idas y vueltas a la base.
--
-- Efectos: actualiza pagos_garantia.estado_fondos a 'Liberado', marca los
-- entregables del pedido como liberados y registra la transaccion contable.
--
-- Devuelve TRUE si libero los fondos, FALSE si no habia nada que liberar
-- (idempotencia: una segunda llamada sobre el mismo pedido devuelve FALSE).
-- Lanza excepcion cuando alguna precondicion de negocio no se cumple, para que
-- la capa de servicio pueda distinguir "ya estaba liberado" de "no se puede".
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_liberar_fondos_escrow(
    p_id_pedido BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_pago         BIGINT;
    v_monto           NUMERIC(10,2);
    v_estado_fondos   VARCHAR(50);
    v_firma_cliente   VARCHAR(255);
    v_firma_creador   VARCHAR(255);
    v_entregables     INTEGER;
    v_tickets_abiertos INTEGER;
BEGIN
    IF p_id_pedido IS NULL THEN
        RAISE EXCEPTION 'fn_liberar_fondos_escrow: p_id_pedido es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    -- (1) Contrato + pago de garantia. FOR UPDATE serializa liberaciones
    -- concurrentes sobre el mismo pedido.
    SELECT pg.id_pago, pg.monto_retenido, pg.estado_fondos,
           c.hash_firma_cliente, c.hash_firma_creador
      INTO v_id_pago, v_monto, v_estado_fondos, v_firma_cliente, v_firma_creador
      FROM pagos_garantia pg
      JOIN contratos c ON c.id_contrato = pg.id_contrato
     WHERE c.id_pedido = p_id_pedido
       FOR UPDATE OF pg;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El pedido % no tiene contrato con pago de garantia asociado', p_id_pedido
            USING ERRCODE = '23503';
    END IF;

    -- (2) Idempotencia: si ya no estan retenidos, no hay nada que hacer.
    IF v_estado_fondos IS DISTINCT FROM 'Retenido' THEN
        RETURN FALSE;
    END IF;

    -- (3) Firma de ambas partes.
    IF v_firma_cliente IS NULL OR v_firma_creador IS NULL THEN
        RAISE EXCEPTION 'El contrato del pedido % no esta firmado por ambas partes', p_id_pedido
            USING ERRCODE = '23514';
    END IF;

    -- (4) Entregable final cargado.
    SELECT COUNT(*) INTO v_entregables
      FROM entregables_finales ef
     WHERE ef.id_pedido = p_id_pedido;

    IF v_entregables = 0 THEN
        RAISE EXCEPTION 'El pedido % no tiene entregables finales cargados', p_id_pedido
            USING ERRCODE = '23514';
    END IF;

    -- (5) Sin revisiones abiertas.
    SELECT COUNT(*) INTO v_tickets_abiertos
      FROM tickets_revision tr
     WHERE tr.id_pedido = p_id_pedido
       AND tr.estado_ticket = 'Abierto';

    IF v_tickets_abiertos > 0 THEN
        RAISE EXCEPTION 'El pedido % tiene % ticket(s) de revision abiertos', p_id_pedido, v_tickets_abiertos
            USING ERRCODE = '23514';
    END IF;

    -- Precondiciones satisfechas: se aplican los tres efectos.
    UPDATE pagos_garantia
       SET estado_fondos = 'Liberado'
     WHERE id_pago = v_id_pago;

    UPDATE entregables_finales
       SET esta_liberado = TRUE
     WHERE id_pedido = p_id_pedido;

    INSERT INTO transacciones_pago (id_pago, tipo_transaccion, monto, fecha_ejecucion)
    VALUES (v_id_pago, 'LIBERACION', v_monto, CURRENT_TIMESTAMP);

    RETURN TRUE;
END;
$$;

COMMENT ON FUNCTION fn_liberar_fondos_escrow(BIGINT)
    IS 'REQ-F-021 - Validacion cruzada: libera los fondos escrow de un pedido tras verificar contrato firmado, entregable cargado y ausencia de revisiones abiertas. TRUE si libero, FALSE si ya estaba liberado.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_permisos_efectivos_usuario.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_permisos_efectivos_usuario
-- Categoria funcional: consultas multi-tabla                    Requisito: REQ-NF (rendimiento)
-- Fase 2 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §8.
-- =============================================================================
-- Resuelve, en una sola llamada, todo lo que CustomUserDetailsService.loadUserByUsername
-- necesita para autenticar una peticion: datos basicos de la cuenta y el
-- conjunto completo de authorities de Spring Security (roles con prefijo
-- ROLE_ + permisos), ya deduplicado.
--
-- Por que en el motor: loadUserByUsername se ejecuta EN CADA peticion
-- autenticada (via JwtAuthenticationFilter), y hacia: findByCorreo (1) +
-- findByUsuarioIdUsuario en usuario_roles (1) + por cada rol, el acceso a
-- Rol.permisos (FetchType.EAGER) resuelto con un SELECT propio (N) -- entre 4
-- y 8 consultas por peticion segun cuantos roles tenga el usuario, con el N+1
-- clasico. Aqui se resuelve con dos subconsultas (UNION, deduplicadas por
-- DISTINCT dentro del jsonb_agg) sobre usuario_roles/roles/rol_permisos/permisos.
--
-- STABLE (no LANGUAGE plpgsql con side effects, no escribe nada): dentro de
-- una misma llamada a la funcion, PostgreSQL evalua todas las subconsultas
-- sobre el MISMO snapshot, asi que roles y permisos siempre son coherentes
-- entre si -- a diferencia de las 2-3 consultas independientes que sustituye,
-- que bajo READ COMMITTED podian ver una version de los roles y otra de los
-- permisos si una sincronizacion (fn_sincronizar_roles_usuario,
-- fn_sincronizar_permisos_rol) se colaba justo entremedias.
--
-- La contrasena_hash SI viaja en el JSONB (a diferencia de fn_resolver_estado_login,
-- que no la necesita): loadUserByUsername construye un UserDetails completo, y
-- el AuthenticationManager de Spring Security compara el hash BCrypt fuera del
-- motor. El valor nunca se loguea ni se expone en ninguna respuesta HTTP.
--
-- Devuelve NULL si el correo no existe (la capa Java lo traduce a
-- UsernameNotFoundException, igual que antes).
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_permisos_efectivos_usuario(
    p_correo VARCHAR(150)
)
RETURNS JSONB
LANGUAGE sql
STABLE
AS $$
    SELECT jsonb_build_object(
               'idUsuario', u.id_usuario,
               'correo', u.correo,
               'contrasenaHash', u.contrasena_hash,
               'estadoCuenta', u.estado_cuenta,
               'authorities', COALESCE(
                   (SELECT jsonb_agg(DISTINCT a.autoridad)
                      FROM (
                            SELECT 'ROLE_' || UPPER(r.nombre_rol) AS autoridad
                              FROM usuario_roles ur
                              JOIN roles r ON r.id_rol = ur.id_rol
                             WHERE ur.id_usuario = u.id_usuario
                            UNION
                            SELECT UPPER(p.nombre_permiso)
                              FROM usuario_roles ur
                              JOIN rol_permisos rp ON rp.id_rol = ur.id_rol
                              JOIN permisos p ON p.id_permiso = rp.id_permiso
                             WHERE ur.id_usuario = u.id_usuario
                           ) a),
                   '[]'::jsonb)
           )
      FROM usuarios u
     WHERE u.correo = p_correo;
$$;

COMMENT ON FUNCTION fn_permisos_efectivos_usuario(VARCHAR)
    IS 'Fase 2 rendimiento - Resuelve usuario + authorities (roles ROLE_* y permisos) en una sola llamada STABLE, sustituyendo el N+1 de CustomUserDetailsService en cada peticion autenticada.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_registrar_infraccion.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_registrar_infraccion
-- Categoria funcional: calculos agregados + validacion cruzada     Requisito: REQ-F-015
-- =============================================================================
-- Registra una infraccion de mensaje (RF-15: intento de compartir datos de
-- contacto fuera del chat de la plataforma) y, en la misma transaccion,
-- cuenta cuantas infracciones acumula el usuario en la ventana movil de los
-- ultimos 30 dias; si alcanza 3 o mas, suspende la cuenta automaticamente.
-- Sustituye a InfraccionServiceImpl.registrarInfraccion/suspenderCuenta, que
-- hacia un INSERT, un COUNT y un UPDATE condicional como tres llamadas
-- independientes al repositorio.
--
-- Por que en el motor: el conteo y la suspension deben ser consistentes con
-- el INSERT que las precede -- si dos infracciones del mismo usuario llegan
-- casi al mismo tiempo (dos mensajes filtrados en paralelo), cada llamada
-- debe ver el efecto de la anterior. La fila de usuarios se toma con
-- FOR UPDATE antes de decidir la suspension para serializar esa carrera.
--
-- El filtrado de patrones (deteccion de telefonos/correos/redes en el texto
-- del mensaje) permanece en Java (MensajeFilterService): es logica de texto,
-- no de datos, y no pertenece al motor.
--
-- Devuelve JSONB: { idInfraccion, totalInfraccionesPeriodo, cuentaSuspendida }.
-- cuentaSuspendida es TRUE solo en la llamada que cruza el umbral (no se
-- repite el efecto de notificacion en llamadas posteriores porque la cuenta
-- ya estara con estado_cuenta = FALSE y no se vuelve a marcar suspendida).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_registrar_infraccion(
    p_id_usuario        BIGINT,
    p_id_pedido         BIGINT,
    p_mensaje_original  TEXT,
    p_patron_detectado  VARCHAR(50)
)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_infraccion       BIGINT;
    v_total_periodo       INTEGER;
    v_estado_cuenta       BOOLEAN;
    v_cuenta_suspendida   BOOLEAN := FALSE;
    v_max_infracciones    CONSTANT INTEGER := 3;
    v_periodo_dias        CONSTANT INTEGER := 30;
BEGIN
    IF p_id_usuario IS NULL THEN
        RAISE EXCEPTION 'fn_registrar_infraccion: p_id_usuario es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = p_id_usuario) THEN
        RAISE EXCEPTION 'Usuario no encontrado: %', p_id_usuario
            USING ERRCODE = 'P0002';
    END IF;

    INSERT INTO infracciones_mensaje (id_usuario, id_pedido, mensaje_original, patron_detectado, fecha_infraccion)
    VALUES (p_id_usuario, p_id_pedido, p_mensaje_original, p_patron_detectado, CURRENT_TIMESTAMP)
    RETURNING id_infraccion INTO v_id_infraccion;

    SELECT COUNT(*) INTO v_total_periodo
      FROM infracciones_mensaje
     WHERE id_usuario = p_id_usuario
       AND fecha_infraccion > CURRENT_TIMESTAMP - (v_periodo_dias || ' days')::interval;

    IF v_total_periodo >= v_max_infracciones THEN
        SELECT estado_cuenta INTO v_estado_cuenta
          FROM usuarios
         WHERE id_usuario = p_id_usuario
           FOR UPDATE;

        IF v_estado_cuenta IS TRUE THEN
            UPDATE usuarios SET estado_cuenta = FALSE WHERE id_usuario = p_id_usuario;
            v_cuenta_suspendida := TRUE;
        END IF;
    END IF;

    RETURN jsonb_build_object(
        'idInfraccion', v_id_infraccion,
        'totalInfraccionesPeriodo', v_total_periodo,
        'cuentaSuspendida', v_cuenta_suspendida
    );
END;
$$;

COMMENT ON FUNCTION fn_registrar_infraccion(BIGINT, BIGINT, TEXT, VARCHAR)
    IS 'REQ-F-015 - Calculo agregado + validacion cruzada: registra una infraccion de mensaje, cuenta las del usuario en 30 dias y suspende la cuenta automaticamente al llegar a 3.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_registrar_usuario.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_registrar_usuario
-- Categoria funcional: validaciones cruzadas + insercion multi-tabla   Requisito: REQ-F-001
-- =============================================================================
-- Registra un nuevo usuario de la plataforma en una unica transaccion atomica:
-- valida que el correo no exista, valida mayoria de edad (RNF-12, >= 18 anios),
-- valida que el rol solicitado sea uno de los permitidos en auto-registro
-- (CLIENTE o CREADOR), inserta la fila en usuarios, la asociacion en
-- usuario_roles y, si el rol es CREADOR, el perfil de creador inicial.
--
-- Por que en el motor y no en AuthServiceImpl.register: son cuatro lecturas/
-- escrituras (existsByCorreo, findByNombreRol, insert usuario, insert
-- usuario_roles, insert perfil opcional) que deben ser atomicas: si el perfil
-- de creador fallara tras crear el usuario, quedaria una cuenta sin perfil.
-- Resolverlo en el motor evita el patron de multiples idas y vueltas y las
-- inconsistencias parciales que un rollback incompleto en Java podria dejar.
--
-- El hash de la contrasena se calcula en Java (BCrypt vive fuera del motor de
-- datos) y llega ya cifrado como parametro; la funcion nunca ve la contrasena
-- en texto plano.
--
-- Devuelve el id_usuario generado. Lanza excepcion si el correo ya existe, si
-- la fecha de nacimiento no cumple la mayoria de edad, o si el rol solicitado
-- no existe o no esta permitido en auto-registro.
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_registrar_usuario(
    p_nombres           VARCHAR(100),
    p_apellidos         VARCHAR(100),
    p_correo            VARCHAR(150),
    p_contrasena_hash   VARCHAR(255),
    p_fecha_nacimiento  DATE,
    p_nombre_rol        VARCHAR(50)
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_usuario BIGINT;
    v_id_rol     BIGINT;
    v_nombre_rol VARCHAR(50) := UPPER(p_nombre_rol);
BEGIN
    IF p_correo IS NULL OR p_contrasena_hash IS NULL OR p_fecha_nacimiento IS NULL THEN
        RAISE EXCEPTION 'fn_registrar_usuario: correo, contrasena y fecha de nacimiento son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    IF EXISTS (SELECT 1 FROM usuarios WHERE correo = p_correo) THEN
        RAISE EXCEPTION 'El correo % ya esta registrado en la plataforma', p_correo
            USING ERRCODE = '23505';
    END IF;

    -- RNF-12: mayoria de edad (>= 18 anios cumplidos a la fecha actual).
    IF p_fecha_nacimiento > (CURRENT_DATE - INTERVAL '18 years')::date THEN
        RAISE EXCEPTION 'Debes tener al menos 18 anios para registrarte en ARTISYNC (RNF-12)'
            USING ERRCODE = '23514';
    END IF;

    IF v_nombre_rol NOT IN ('CLIENTE', 'CREADOR') THEN
        RAISE EXCEPTION 'Rol no permitido en registro. Solo se permiten CLIENTE o CREADOR: %', p_nombre_rol
            USING ERRCODE = '23514';
    END IF;

    SELECT id_rol INTO v_id_rol FROM roles WHERE nombre_rol = v_nombre_rol;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El rol especificado no existe en el sistema: %', v_nombre_rol
            USING ERRCODE = '23503';
    END IF;

    INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, fecha_nacimiento, estado_cuenta, fecha_registro)
    VALUES (p_nombres, p_apellidos, p_correo, p_contrasena_hash, p_fecha_nacimiento, TRUE, CURRENT_TIMESTAMP)
    RETURNING id_usuario INTO v_id_usuario;

    INSERT INTO usuario_roles (id_usuario, id_rol)
    VALUES (v_id_usuario, v_id_rol);

    IF v_nombre_rol = 'CREADOR' THEN
        INSERT INTO perfiles_creadores (id_usuario, biografia)
        VALUES (v_id_usuario, 'Hola! Soy un creador en ARTISYNC.');
    END IF;

    RETURN v_id_usuario;
END;
$$;

COMMENT ON FUNCTION fn_registrar_usuario(VARCHAR, VARCHAR, VARCHAR, VARCHAR, DATE, VARCHAR)
    IS 'REQ-F-001 - Insercion multi-tabla: registra usuario + usuario_roles + perfil de creador opcional, validando correo unico, mayoria de edad y rol permitido.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_reporte_comisiones_creador.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_reporte_comisiones_creador
-- Categoria funcional: reportes                     Requisito: REQ-NF-013
-- =============================================================================
-- Produce el reporte financiero de un creador en una ventana temporal: importe
-- bruto liberado, comision de la plataforma, importe neto, numero de pedidos y
-- desglose por transaccion. Recorre la cadena completa del patron escrow:
--   transacciones_pago -> pagos_garantia -> contratos -> pedidos -> servicios
--
-- Sustituye a la consulta JPQL de tres JOIN de
--   repository/legal/TransaccionPagoRepository.findByCreadorPerfilId
-- que devolvia entidades crudas y obligaba a agregar en Java.
--
-- La comision de la plataforma se parametriza (p_tasa_comision) en lugar de
-- fijarse en la rutina, para que el reporte pueda recalcularse historicamente
-- si la tasa cambia sin necesidad de versionar una nueva funcion.
--
-- Contrato de tipo de retorno: JSONB (ver db/procs/README.md).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_reporte_comisiones_creador(
    p_id_perfil     BIGINT,
    p_fecha_desde   TIMESTAMP     DEFAULT NULL,
    p_fecha_hasta   TIMESTAMP     DEFAULT NULL,
    p_tasa_comision NUMERIC(5,4)  DEFAULT 0.1000
)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_tasa       NUMERIC(5,4);
    v_bruto      NUMERIC(12,2) := 0;
    v_pedidos    BIGINT        := 0;
    v_operaciones BIGINT       := 0;
    v_detalle    JSONB;
BEGIN
    IF p_id_perfil IS NULL THEN
        RAISE EXCEPTION 'fn_reporte_comisiones_creador: p_id_perfil es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    -- La tasa se acota al rango [0,1]: una tasa fuera de rango produciria un
    -- neto negativo o superior al bruto, y el reporte alimenta el capitulo de
    -- evaluacion empirica del documento academico.
    v_tasa := LEAST(GREATEST(COALESCE(p_tasa_comision, 0.1000), 0::NUMERIC), 1::NUMERIC);

    WITH movimientos AS (
        SELECT t.id_transaccion,
               t.tipo_transaccion,
               t.monto,
               t.fecha_ejecucion,
               p.id_pedido,
               s.titulo_servicio
          FROM transacciones_pago t
          JOIN pagos_garantia pg ON pg.id_pago     = t.id_pago
          JOIN contratos      c  ON c.id_contrato  = pg.id_contrato
          JOIN pedidos        p  ON p.id_pedido    = c.id_pedido
          JOIN servicios      s  ON s.id_servicio  = p.id_servicio
         WHERE s.id_perfil = p_id_perfil
           AND (p_fecha_desde IS NULL OR t.fecha_ejecucion >= p_fecha_desde)
           AND (p_fecha_hasta IS NULL OR t.fecha_ejecucion <= p_fecha_hasta)
    )
    SELECT COALESCE(SUM(m.monto), 0),
           COUNT(DISTINCT m.id_pedido),
           COUNT(*),
           COALESCE(
               jsonb_agg(
                   jsonb_build_object(
                       'idTransaccion',  m.id_transaccion,
                       'idPedido',       m.id_pedido,
                       'servicio',       m.titulo_servicio,
                       'tipo',           m.tipo_transaccion,
                       'monto',          m.monto,
                       'fechaEjecucion', to_char(m.fecha_ejecucion, 'YYYY-MM-DD"T"HH24:MI:SS')
                   )
                   ORDER BY m.fecha_ejecucion DESC, m.id_transaccion DESC
               ),
               '[]'::JSONB)
      INTO v_bruto, v_pedidos, v_operaciones, v_detalle
      FROM movimientos m;

    RETURN jsonb_build_object(
        'idPerfil',      p_id_perfil,
        'fechaDesde',    to_char(p_fecha_desde, 'YYYY-MM-DD"T"HH24:MI:SS'),
        'fechaHasta',    to_char(p_fecha_hasta, 'YYYY-MM-DD"T"HH24:MI:SS'),
        'tasaComision',  v_tasa,
        'totalPedidos',  v_pedidos,
        'totalOperaciones', v_operaciones,
        'montoBruto',    ROUND(v_bruto, 2),
        'comision',      ROUND(v_bruto * v_tasa, 2),
        'montoNeto',     ROUND(v_bruto - (v_bruto * v_tasa), 2),
        'detalle',       COALESCE(v_detalle, '[]'::JSONB)
    );
END;
$$;

COMMENT ON FUNCTION fn_reporte_comisiones_creador(BIGINT, TIMESTAMP, TIMESTAMP, NUMERIC)
    IS 'REQ-NF-013 - Reporte financiero por creador (bruto, comision, neto y detalle) sobre la cadena escrow.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_resolver_estado_login.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_resolver_estado_login
-- Categoria funcional: consultas multi-tabla                          Requisito: REQ-F-002
-- =============================================================================
-- Resuelve, en una sola llamada, todo lo que AuthServiceImpl.login/verify2Fa
-- necesitan del usuario tras validar la contrasena con el AuthenticationManager
-- de Spring Security: datos basicos de la cuenta, si el 2FA esta habilitado y
-- la lista de roles asignados (join usuarios - usuario_roles - roles).
--
-- Por que en el motor y no en Java: la version anterior hacia tres consultas
-- independientes (findByCorreo, findByUsuarioIdUsuario en 2FA,
-- findByUsuarioIdUsuario en usuario_roles) en tres idas y vueltas separadas a
-- la base. Aqui se resuelven en una sola sentencia con dos LEFT JOIN y una
-- agregacion de roles, evitando el problema N+1 y garantizando una lectura
-- consistente de los tres estados a la vez.
--
-- No participa en la validacion de la contrasena en si: eso permanece en
-- AuthenticationManager (BCrypt vive fuera del motor de datos). Esta funcion
-- solo se invoca DESPUES de que la autenticacion por contrasena ya tuvo exito.
--
-- Devuelve JSONB con la forma:
--   { idUsuario, correo, nombres, apellidos, estadoCuenta,
--     dosFactoresHabilitado, roles: [ "CLIENTE", ... ] }
-- Devuelve NULL si el correo no existe (la capa de servicio lo traduce a 404).
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_resolver_estado_login(
    p_correo VARCHAR(150)
)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_resultado JSONB;
BEGIN
    IF p_correo IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT jsonb_build_object(
               'idUsuario', u.id_usuario,
               'correo', u.correo,
               'nombres', u.nombres,
               'apellidos', u.apellidos,
               'estadoCuenta', u.estado_cuenta,
               'dosFactoresHabilitado', COALESCE(tf.esta_habilitado, FALSE),
               'roles', COALESCE(
                            (SELECT jsonb_agg(r.nombre_rol ORDER BY r.nombre_rol)
                               FROM usuario_roles ur
                               JOIN roles r ON r.id_rol = ur.id_rol
                              WHERE ur.id_usuario = u.id_usuario),
                            '[]'::jsonb)
           )
      INTO v_resultado
      FROM usuarios u
      LEFT JOIN autenticacion_dos_factores tf ON tf.id_usuario = u.id_usuario
     WHERE u.correo = p_correo;

    RETURN v_resultado;
END;
$$;

COMMENT ON FUNCTION fn_resolver_estado_login(VARCHAR)
    IS 'REQ-F-002 - Consulta multi-tabla: resuelve estado de cuenta, 2FA y roles de un usuario en una sola llamada para el flujo de login.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_restablecer_contrasena.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_restablecer_contrasena
-- Categoria funcional: validaciones cruzadas + escritura multi-tabla  Requisito: REQ-F-005
-- =============================================================================
-- Aplica un restablecimiento de contrasena a partir de un token de
-- recuperacion: valida que el token exista, no haya sido usado y no haya
-- expirado (ventana de 60 minutos desde su generacion), y en tal caso
-- actualiza el hash de la contrasena del usuario y marca el token como usado,
-- en una unica transaccion atomica.
-- Sustituye a AuthServiceImpl.resetPassword, que hacia la busqueda del token,
-- la validacion de expiracion en Java (LocalDateTime.plusMinutes(60)) y dos
-- save() secuenciales (usuario, tokenRecuperacion).
--
-- Por que en el motor: entre la validacion del token y su marcado como usado
-- no debe existir ventana en la que una segunda peticion concurrente con el
-- mismo token pueda colarse y restablecer la contrasena dos veces. La fila del
-- token se toma con FOR UPDATE para serializar restablecimientos concurrentes
-- del mismo token.
--
-- El nuevo hash de contrasena se calcula en Java (BCrypt) y llega ya cifrado;
-- la funcion nunca ve la contrasena en texto plano. El hash del token en si
-- (SHA-256 del valor plano enviado por correo) tambien se calcula en Java
-- antes de invocar la funcion.
--
-- Devuelve el id_usuario cuya contrasena se actualizo. Lanza excepcion si el
-- token no existe, ya fue usado, o expiro.
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_restablecer_contrasena(
    p_hash_token           VARCHAR(255),
    p_nueva_contrasena_hash VARCHAR(255)
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_token   BIGINT;
    v_id_usuario BIGINT;
    v_fecha_generacion TIMESTAMP;
BEGIN
    IF p_hash_token IS NULL OR p_nueva_contrasena_hash IS NULL THEN
        RAISE EXCEPTION 'fn_restablecer_contrasena: hash_token y nueva_contrasena_hash son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    SELECT id_token, id_usuario, fecha_generacion
      INTO v_id_token, v_id_usuario, v_fecha_generacion
      FROM tokens_recuperacion
     WHERE hash_token = p_hash_token
       AND usado = FALSE
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Este enlace ya ha sido utilizado o ha expirado'
            USING ERRCODE = '23514';
    END IF;

    IF v_fecha_generacion + INTERVAL '60 minutes' < CURRENT_TIMESTAMP THEN
        RAISE EXCEPTION 'Este enlace ya ha sido utilizado o ha expirado'
            USING ERRCODE = '23514';
    END IF;

    UPDATE usuarios
       SET contrasena_hash = p_nueva_contrasena_hash
     WHERE id_usuario = v_id_usuario;

    UPDATE tokens_recuperacion
       SET usado = TRUE
     WHERE id_token = v_id_token;

    RETURN v_id_usuario;
END;
$$;

COMMENT ON FUNCTION fn_restablecer_contrasena(VARCHAR, VARCHAR)
    IS 'REQ-F-005 - Validacion cruzada + escritura multi-tabla: valida token de recuperacion (no usado, no expirado) y actualiza usuarios + tokens_recuperacion atomicamente.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_revocar_sesiones_usuario.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_revocar_sesiones_usuario
-- Categoria funcional: actualizaciones masivas                  Requisito: REQ-NF (concurrencia)
-- Fase 1 de docs/basedatos/PLAN-CONCURRENCIA-SP.md — corrige la anomalia A6.
-- =============================================================================
-- Borra todas las sesiones de un usuario y devuelve, en la misma sentencia,
-- el jti y el tiempo de vida restante de cada una que borro.
--
-- Sustituye la parte SQL de SessionRevocationService.revocarSesionesUsuario():
--   (1) List<SesionUsuario> sesiones = findByUsuarioIdUsuario(idUsuario);
--   (2) por cada sesion: revocarJtiEnRedis(...)
--   (3) deleteByUsuarioIdUsuario(idUsuario);
-- son tres sentencias separadas. Bajo READ COMMITTED cada una ve su propio
-- snapshot: una sesion CREADA entre (1) y (3) -- por ejemplo, un login
-- concurrente del mismo usuario justo cuando un administrador lo desactiva --
-- nunca aparece en la lista leida en (1), pero SI la borra el DELETE de (3),
-- porque ese filtra de nuevo por id_usuario sobre el estado mas reciente. El
-- resultado: una sesion se elimina de la base sin haberse revocado nunca en
-- Redis. Su JWT sigue siendo valido hasta que expire por si solo, y ya no
-- queda ninguna fila que permita rastrearlo (lectura no repetible).
--
-- Por que esta forma lo corrige: DELETE ... RETURNING lee y borra en UNA sola
-- sentencia sobre UN solo snapshot. No existe "entre (1) y (3)" porque no hay
-- dos pasos: es imposible que una sesion se cuele sin ser devuelta, porque
-- toda fila que la sentencia borra es, por definicion, una fila que tambien
-- devuelve.
--
-- La escritura en Redis permanece deliberadamente en Java (SessionRevocationService):
-- Redis no participa en la transaccion de PostgreSQL, y un fallo suyo no debe
-- revertir el borrado en la base (ni al reves). Lo que esta funcion garantiza
-- es que Java recibe EXACTAMENTE el conjunto de jti que se borro, ni uno mas
-- ni uno menos.
--
-- segundos_restantes se calcula en el mismo SELECT para que Java no tenga que
-- releer fecha_expiracion por separado; GREATEST(0, ...) evita valores
-- negativos si la sesion ya habia expirado.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_revocar_sesiones_usuario(
    p_id_usuario BIGINT
)
RETURNS TABLE (jti VARCHAR(36), segundos_restantes INTEGER)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_id_usuario IS NULL THEN
        RAISE EXCEPTION 'fn_revocar_sesiones_usuario: p_id_usuario es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    RETURN QUERY
    DELETE FROM sesiones_usuario s
     WHERE s.id_usuario = p_id_usuario
    RETURNING s.jti,
              GREATEST(0, EXTRACT(EPOCH FROM (s.fecha_expiracion - CURRENT_TIMESTAMP))::INTEGER);
END;
$$;

COMMENT ON FUNCTION fn_revocar_sesiones_usuario(BIGINT)
    IS 'Fase 1 concurrencia - DELETE ... RETURNING atomico: lee y borra las sesiones de un usuario en una sola sentencia, eliminando la ventana de lectura no repetible entre leer y borrar por separado.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_seleccionar_ganadores_sorteo.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_seleccionar_ganadores_sorteo
-- Categoria funcional: seleccion aleatoria + actualizacion masiva   Requisito: REQ-F-023
-- =============================================================================
-- Selecciona aleatoriamente a los ganadores de un sorteo entre los
-- participantes que aun no han ganado, y marca en bloque tanto a los
-- participantes ganadores como el sorteo mismo. Es el candidato que el propio
-- ADR-006 (linea 19) identifica por nombre como pendiente de implementacion.
-- Sustituye a SorteoScheduler.ejecutarSorteo, que traia todos los
-- participantes a la aplicacion, hacia Collections.shuffle(new SecureRandom())
-- en Java y actualizaba fila por fila con un save() dentro de un bucle.
--
-- Por que en el motor: la aleatoriedad y la actualizacion masiva deben ser
-- atomicas frente a otra ejecucion concurrente del mismo sorteo -- la fila del
-- sorteo se toma con FOR UPDATE, de modo que dos disparos simultaneos del
-- scheduler sobre el mismo sorteo se serializan y el segundo ve el sorteo ya
-- en estado distinto de 'Activo'. ORDER BY random() LIMIT n resuelve la
-- seleccion y la actualizacion de todos los ganadores en una unica sentencia,
-- en vez de N idas y vueltas a la base.
--
-- La notificacion en tiempo real (WebSocket) permanece en Java: no es
-- responsabilidad del motor de datos. La funcion devuelve el listado de
-- ganadores para que el scheduler los notifique despues de confirmar la
-- transaccion.
--
-- Devuelve JSONB: { idSorteo, tituloSorteo, estado, ganadores: [ { idParticipacion, idUsuario } ] }.
-- Si el sorteo ya no esta 'Activo' (segunda ejecucion concurrente o manual),
-- devuelve el estado actual con ganadores: [] sin volver a sortear
-- (idempotencia). Si no hay participantes, marca el sorteo como
-- 'Finalizado_Sin_Participantes'.
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_seleccionar_ganadores_sorteo(
    p_id_sorteo BIGINT
)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
    v_cantidad_ganadores  INTEGER;
    v_titulo              VARCHAR(150);
    v_estado_actual       VARCHAR(50);
    v_total_participantes INTEGER;
    v_ganadores           JSONB;
BEGIN
    IF p_id_sorteo IS NULL THEN
        RAISE EXCEPTION 'fn_seleccionar_ganadores_sorteo: p_id_sorteo es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    SELECT cantidad_ganadores, titulo_sorteo, estado_sorteo
      INTO v_cantidad_ganadores, v_titulo, v_estado_actual
      FROM sorteos
     WHERE id_sorteo = p_id_sorteo
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Sorteo no encontrado: %', p_id_sorteo
            USING ERRCODE = 'P0002';
    END IF;

    -- Idempotencia: una segunda llamada (ejecucion concurrente del scheduler,
    -- o disparo manual repetido) sobre un sorteo que ya no esta activo no
    -- vuelve a sortear.
    IF v_estado_actual <> 'Activo' THEN
        RETURN jsonb_build_object(
            'idSorteo', p_id_sorteo,
            'tituloSorteo', v_titulo,
            'estado', v_estado_actual,
            'ganadores', '[]'::jsonb
        );
    END IF;

    SELECT COUNT(*) INTO v_total_participantes
      FROM participantes_sorteo
     WHERE id_sorteo = p_id_sorteo
       AND es_ganador = FALSE;

    IF v_total_participantes = 0 THEN
        UPDATE sorteos SET estado_sorteo = 'Finalizado_Sin_Participantes'
         WHERE id_sorteo = p_id_sorteo;

        RETURN jsonb_build_object(
            'idSorteo', p_id_sorteo,
            'tituloSorteo', v_titulo,
            'estado', 'Finalizado_Sin_Participantes',
            'ganadores', '[]'::jsonb
        );
    END IF;

    WITH seleccionados AS (
        SELECT id_participacion, id_usuario
          FROM participantes_sorteo
         WHERE id_sorteo = p_id_sorteo
           AND es_ganador = FALSE
         ORDER BY random()
         LIMIT LEAST(v_cantidad_ganadores, v_total_participantes)
    ),
    actualizados AS (
        UPDATE participantes_sorteo p
           SET es_ganador = TRUE,
               fecha_notificacion_premio = CURRENT_TIMESTAMP
          FROM seleccionados s
         WHERE p.id_participacion = s.id_participacion
        RETURNING p.id_participacion, p.id_usuario
    )
    SELECT jsonb_agg(jsonb_build_object('idParticipacion', id_participacion, 'idUsuario', id_usuario))
      INTO v_ganadores
      FROM actualizados;

    UPDATE sorteos SET estado_sorteo = 'Finalizado'
     WHERE id_sorteo = p_id_sorteo;

    RETURN jsonb_build_object(
        'idSorteo', p_id_sorteo,
        'tituloSorteo', v_titulo,
        'estado', 'Finalizado',
        'ganadores', COALESCE(v_ganadores, '[]'::jsonb)
    );
END;
$$;

COMMENT ON FUNCTION fn_seleccionar_ganadores_sorteo(BIGINT)
    IS 'REQ-F-023 - Seleccion aleatoria + actualizacion masiva: sortea ganadores entre los participantes no ganadores de un sorteo activo y marca en bloque participantes y sorteo.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_sincronizar_permisos_rol.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_sincronizar_permisos_rol
-- Categoria funcional: actualizaciones masivas                        Requisito: REQ-F-003
-- =============================================================================
-- Reemplaza, en una sola transaccion, el conjunto completo de permisos
-- asignados a un rol por el conjunto recibido en p_codigos_permiso.
-- Sustituye a RolePermissionServiceImpl.syncPermissions, que cargaba el rol
-- completo con sus permisos (fetch EAGER de la coleccion @ManyToMany),
-- resolvia cada codigo de permiso con una consulta individual y dejaba que
-- Hibernate calculara el diff de la coleccion al hacer save().
--
-- Por que en el motor y no en Java: el reemplazo de un set completo es una
-- operacion tipicamente DELETE+INSERT sobre la tabla puente rol_permisos; con
-- lote de permisos y necesitando validar cada codigo antes de aplicar nada
-- (todo o nada), es mas seguro y mas barato en round-trips resolverlo como una
-- unica sentencia DELETE seguida de un INSERT ... SELECT que hacerlo fila por
-- fila desde el servicio.
--
-- Valida cada codigo de permiso contra la tabla permisos antes de aplicar el
-- reemplazo: si algun codigo no existe, no se modifica nada (rollback
-- implicito de la transaccion de la funcion).
--
-- Devuelve el numero de permisos finalmente asignados al rol.
-- Lanza excepcion si el rol no existe o si algun codigo de permiso es invalido.
--
-- Seguridad: el arreglo de codigos llega como parametro tipado
-- (TEXT[]); no hay concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_sincronizar_permisos_rol(
    p_nombre_rol       VARCHAR(50),
    p_codigos_permiso  TEXT[]
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_rol           BIGINT;
    v_codigos_normalizados TEXT[];
    v_encontrados      INTEGER;
    v_esperados        INTEGER;
    v_total_asignado   INTEGER;
BEGIN
    IF p_nombre_rol IS NULL THEN
        RAISE EXCEPTION 'fn_sincronizar_permisos_rol: p_nombre_rol es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    SELECT id_rol INTO v_id_rol FROM roles WHERE nombre_rol = UPPER(p_nombre_rol);
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Rol no encontrado: %', p_nombre_rol
            USING ERRCODE = 'P0002';
    END IF;

    IF p_codigos_permiso IS NULL THEN
        v_codigos_normalizados := ARRAY[]::TEXT[];
    ELSE
        SELECT array_agg(DISTINCT UPPER(codigo)) INTO v_codigos_normalizados
          FROM unnest(p_codigos_permiso) AS codigo;
    END IF;

    v_esperados := COALESCE(array_length(v_codigos_normalizados, 1), 0);

    IF v_esperados > 0 THEN
        SELECT COUNT(*) INTO v_encontrados
          FROM permisos
         WHERE nombre_permiso = ANY (v_codigos_normalizados);

        IF v_encontrados <> v_esperados THEN
            RAISE EXCEPTION 'Uno o mas permisos son inexistentes para el rol %', p_nombre_rol
                USING ERRCODE = '23503';
        END IF;
    END IF;

    DELETE FROM rol_permisos WHERE id_rol = v_id_rol;

    INSERT INTO rol_permisos (id_rol, id_permiso)
    SELECT v_id_rol, p.id_permiso
      FROM permisos p
     WHERE p.nombre_permiso = ANY (v_codigos_normalizados);

    GET DIAGNOSTICS v_total_asignado = ROW_COUNT;

    RETURN v_total_asignado;
END;
$$;

COMMENT ON FUNCTION fn_sincronizar_permisos_rol(VARCHAR, TEXT[])
    IS 'REQ-F-003 - Actualizacion masiva: reemplaza atomicamente el conjunto de permisos de un rol (DELETE+INSERT en rol_permisos), validando cada codigo antes de aplicar el cambio.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_sincronizar_roles_usuario.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_sincronizar_roles_usuario
-- Categoria funcional: actualizaciones masivas                  Requisito: REQ-NF (concurrencia)
-- Fase 1 de docs/basedatos/PLAN-CONCURRENCIA-SP.md — corrige la anomalia A2.
-- =============================================================================
-- Reemplaza atomicamente el conjunto completo de roles de un usuario. Gemela
-- de fn_sincronizar_permisos_rol (REQ-F-003), que ya resolvio este mismo
-- patron para roles<->permisos.
--
-- Sustituye a AdminUserServiceImpl.actualizarRoles(): findByUsuarioIdUsuario +
-- deleteAll + flush + POR CADA rol nuevo (findByNombreRol + save + consulta
-- de perfil de creador + save de perfil) -- unos 10 viajes a la base sin
-- ninguna atomicidad entre ellos.
--
-- Dos anomalias que corrige:
--
--   * Lectura fantasma: sin restriccion unica, dos administradores editando
--     el mismo usuario a la vez podian dejar roles duplicados en
--     usuario_roles. Cerrada de forma ESTRUCTURAL por uq_usuario_rol
--     (V17__concurrencia_seguridad.sql) + ON CONFLICT DO NOTHING: el fantasma
--     es imposible aunque otra transaccion se adelante entre el DELETE y el
--     INSERT de esta misma rutina.
--
--   * Actualizacion perdida / estado a medias: SELECT ... FOR UPDATE sobre la
--     fila de usuarios serializa dos sincronizaciones concurrentes del MISMO
--     usuario (el segundo escritor espera y ve el resultado ya confirmado del
--     primero, no un estado intermedio). Ademas, TODOS los roles nuevos se
--     validan ANTES de borrar los antiguos: si un nombre de rol no existe, se
--     aborta sin haber dejado al usuario sin ningun rol (la version en Java
--     borraba primero y podia fallar a mitad del bucle de alta).
--
-- Orden de bloqueo: usuarios -> usuario_roles -> roles (convencion canonica
-- del modulo, ver docs/basedatos/PLAN-CONCURRENCIA-SP.md §0.5.4), evita
-- interbloqueos con fn_sincronizar_permisos_rol y fn_eliminar_rol.
--
-- Devuelve el total de filas de usuario_roles insertadas. Lanza excepcion si
-- el usuario no existe, si el array de roles es nulo/vacio, o si alguno de
-- los nombres de rol no existe en el sistema.
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_sincronizar_roles_usuario(
    p_id_usuario  BIGINT,
    p_nombres_rol TEXT[]
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_existe     BOOLEAN;
    v_nombre_rol TEXT;
    v_total      INTEGER := 0;
BEGIN
    IF p_id_usuario IS NULL THEN
        RAISE EXCEPTION 'fn_sincronizar_roles_usuario: p_id_usuario es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    IF p_nombres_rol IS NULL OR array_length(p_nombres_rol, 1) IS NULL THEN
        RAISE EXCEPTION 'fn_sincronizar_roles_usuario: se requiere al menos un rol'
            USING ERRCODE = '22004';
    END IF;

    -- Bloqueo del agregado raiz: serializa sincronizaciones concurrentes del
    -- mismo usuario y evita que este SELECT vea un conjunto de roles que otra
    -- transaccion cambia justo despues (lectura no repetible).
    SELECT TRUE INTO v_existe
      FROM usuarios
     WHERE id_usuario = p_id_usuario
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario no encontrado con ID: %', p_id_usuario
            USING ERRCODE = 'P0002';
    END IF;

    -- Validacion completa antes de tocar usuario_roles: aborta limpio (todo o
    -- nada) si algun nombre de rol no existe.
    FOREACH v_nombre_rol IN ARRAY p_nombres_rol LOOP
        IF NOT EXISTS (SELECT 1 FROM roles WHERE UPPER(nombre_rol) = UPPER(v_nombre_rol)) THEN
            RAISE EXCEPTION 'El rol especificado no existe en el sistema: %', UPPER(v_nombre_rol)
                USING ERRCODE = '23514';
        END IF;
    END LOOP;

    DELETE FROM usuario_roles WHERE id_usuario = p_id_usuario;

    -- ON CONFLICT DO NOTHING: idempotente aunque el array traiga nombres
    -- repetidos, y ultima linea de defensa estructural contra el fantasma si
    -- alguna otra via insertara sobre esta misma pareja (usuario, rol).
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT p_id_usuario, r.id_rol
      FROM unnest(p_nombres_rol) AS n(nombre)
      JOIN roles r ON UPPER(r.nombre_rol) = UPPER(n.nombre)
    ON CONFLICT (id_usuario, id_rol) DO NOTHING;

    GET DIAGNOSTICS v_total = ROW_COUNT;

    -- Alta perezosa del perfil de creador (mismo criterio que
    -- AdminUserServiceImpl.actualizarRoles ya aplicaba), tambien idempotente.
    IF EXISTS (SELECT 1 FROM unnest(p_nombres_rol) AS n(nombre) WHERE UPPER(n.nombre) = 'CREADOR') THEN
        INSERT INTO perfiles_creadores (id_usuario, biografia)
        SELECT p_id_usuario, 'Hola! Soy un creador en ARTISYNC.'
         WHERE NOT EXISTS (SELECT 1 FROM perfiles_creadores WHERE id_usuario = p_id_usuario);
    END IF;

    RETURN v_total;
END;
$$;

COMMENT ON FUNCTION fn_sincronizar_roles_usuario(BIGINT, TEXT[])
    IS 'Fase 1 concurrencia - Reemplaza atomicamente el conjunto de roles de un usuario (DELETE+INSERT con ON CONFLICT), serializado con SELECT FOR UPDATE sobre usuarios; cierra lectura fantasma y estados a medias.';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/fn_solicitar_recuperacion.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- fn_solicitar_recuperacion
-- Categoria funcional: validaciones cruzadas + escritura multi-tabla   Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §6 — corrige la anomalia A5.
-- =============================================================================
-- Invalida los tokens de recuperacion previos de un usuario e inserta el
-- nuevo, en una unica transaccion.
--
-- Sustituye a AuthServiceImpl.forgotPassword (parte de escritura), que
-- insertaba un TokenRecuperacion mas sin tocar los anteriores: cada solicitud
-- de "olvide mi contrasena" dejaba un token adicional valido durante 60
-- minutos (ventana de fn_restablecer_contrasena, REQ-F-005), de modo que un
-- usuario que pedia varios enlaces de recuperacion acumulaba N tokens
-- utilizables simultaneamente -- superficie de ataque innecesaria si alguno se
-- filtraba (log, proxy, bandeja de entrada comprometida).
--
-- SELECT ... FOR UPDATE sobre la fila de usuarios serializa solicitudes
-- concurrentes del mismo correo (p. ej. un usuario haciendo doble clic en
-- "reenviar enlace"): la segunda espera a que la primera confirme su
-- invalidacion+insercion antes de repetir el mismo patron, en vez de que
-- ambas lean "no hay token que invalidar" y dejen dos tokens validos.
--
-- Preserva DELIBERADAMENTE el comportamiento de "respuesta indistinguible" de
-- forgotPassword: devuelve NULL (no lanza excepcion) si el correo no existe o
-- la cuenta esta inactiva, para que la capa Java responda el mismo mensaje
-- gener ico exista o no la cuenta (no revelar informacion de la cuenta).
--
-- El nuevo hash de token (SHA-256 del valor plano enviado por correo) se
-- calcula en Java antes de invocar esta funcion, igual que en
-- fn_restablecer_contrasena; la funcion nunca ve el token en texto plano.
--
-- Devuelve JSONB {idUsuario, nombres} si la cuenta existe y esta activa, o
-- NULL en caso contrario.
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_solicitar_recuperacion(
    p_correo     VARCHAR(150),
    p_hash_token VARCHAR(255)
)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_usuario BIGINT;
    v_nombres    VARCHAR(100);
BEGIN
    IF p_correo IS NULL OR p_hash_token IS NULL THEN
        RAISE EXCEPTION 'fn_solicitar_recuperacion: p_correo y p_hash_token son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    SELECT id_usuario, nombres INTO v_id_usuario, v_nombres
      FROM usuarios
     WHERE correo = p_correo
       AND estado_cuenta = TRUE
       FOR UPDATE;

    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    -- Invalida cualquier token previo aun vigente antes de insertar el nuevo:
    -- a lo sumo un token de recuperacion vigente por usuario en todo momento.
    UPDATE tokens_recuperacion
       SET usado = TRUE
     WHERE id_usuario = v_id_usuario
       AND usado = FALSE;

    INSERT INTO tokens_recuperacion (id_usuario, hash_token, usado)
    VALUES (v_id_usuario, p_hash_token, FALSE);

    RETURN jsonb_build_object('idUsuario', v_id_usuario, 'nombres', v_nombres);
END;
$$;

COMMENT ON FUNCTION fn_solicitar_recuperacion(VARCHAR, VARCHAR)
    IS 'Fase 3 concurrencia - Invalida tokens de recuperacion previos e inserta el nuevo atomicamente bajo SELECT FOR UPDATE, garantizando a lo sumo un token vigente por usuario. Devuelve NULL si la cuenta no existe (respuesta indistinguible).';


-- ---------------------------------------------------------------------------
-- Origen: db/procs/sp_purgar_datos_seguridad.sql
-- ---------------------------------------------------------------------------
-- =============================================================================
-- sp_purgar_datos_seguridad
-- Categoria funcional: actualizaciones masivas (mantenimiento)   Requisito: REQ-NF (concurrencia)
-- Fase 4 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §7 — corrige la anomalia A10.
-- =============================================================================
-- Purga por lotes los datos de seguridad que hoy crecen sin limite:
-- sesiones_usuario expiradas, tokens_recuperacion muertos y codigos_respaldo_2fa
-- ya consumidos. No existe hoy ninguna tarea de mantenimiento sobre estas tres
-- tablas -- el indice idx_sesiones_usuario_fecha_expiracion que V8 creo para
-- esto no lo usa nadie.
--
-- Es un PROCEDURE, no una FUNCTION, y es la UNICA rutina de db/procs/ que hace
-- COMMIT/ROLLBACK reales (ver docs/basedatos/PLAN-CONCURRENCIA-SP.md §0.3):
-- borrar en una sola transaccion un historial completo de sesiones/tokens
-- mantendria una transaccion de larga duracion que impide a VACUUM recuperar
-- espacio en TODA la base mientras dura. Se confirma un lote a la vez.
--
-- FOR UPDATE SKIP LOCKED es lo que hace la purga compatible con el trafico en
-- vivo: en vez de esperar a una fila que un login/logout concurrente tiene
-- bloqueada, la salta y la recoge en la siguiente ejecucion (se corre a diario,
-- no hay prisa). ORDER BY <pk> ASC antes del LIMIT mantiene el orden de borrado
-- estable entre lotes sucesivos.
--
-- Alcance deliberadamente ACOTADO en codigos_respaldo_2fa: solo se purgan los
-- YA CONSUMIDOS (usado = TRUE). Los no usados de un usuario con 2FA
-- deshabilitado NO se tocan aqui: la tabla no tiene una columna de fecha que
-- distinga un codigo "huerfano" (2FA desactivado hace tiempo) de uno recien
-- generado por fn_configurar_2fa a la espera de que el usuario llame a
-- confirm2Fa -- purgar por esta_habilitado = FALSE borraria codigos de una
-- configuracion de 2FA en curso, todavia sin confirmar, dejando al usuario sin
-- sus codigos de respaldo antes de haber podido guardarlos.
--
-- tokens_recuperacion: se purgan los ya usados y los generados hace mas de 24h
-- (la ventana de validez real es 60 minutos, ver fn_restablecer_contrasena
-- REQ-F-005); 24h da margen de sobra sin acumular tokens muertos indefinidamente.
--
-- No devuelve nada (PROCEDURE). Si una sentencia falla a mitad de un lote (p.
-- ej. deadlock, disco lleno), PostgreSQL aborta automaticamente la
-- transaccion en curso -- sin necesidad de un ROLLBACK explicito -- y los
-- lotes de las tablas anteriores que ya hicieron COMMIT permanecen intactos.
-- El error se propaga tal cual a traves del CALL hasta el llamante Java
-- (SeguridadPurgaScheduler), que ya lo captura y registra sin tumbar el
-- proceso; la siguiente ejecucion programada retoma desde donde quedo.
--
-- A PROPOSITO no hay un bloque EXCEPTION en este procedimiento: PL/pgSQL
-- implementa cada BEGIN...EXCEPTION...END como una subtransaccion respaldada
-- por un SAVEPOINT implicito, y PostgreSQL PROHIBE ejecutar COMMIT/ROLLBACK
-- mientras ese savepoint sigue abierto -- falla con
-- "invalid transaction termination" en el primer COMMIT del primer lote. Es
-- el mismo motivo por el que una FUNCTION no puede hacer COMMIT/ROLLBACK
-- (§0.1 de PLAN-CONCURRENCIA-SP.md), aplicado aqui a un bloque con manejo de
-- excepciones dentro de un PROCEDURE. El propio manual de PostgreSQL resuelve
-- esto separando ambos: la excepcion se atrapa en un bloque interno SIN
-- COMMIT, y el COMMIT ocurre en el bloque externo, fuera de cualquier
-- EXCEPTION -- exactamente la estructura de este archivo (tres LOOP con
-- COMMIT, ninguno envuelto en un manejador de excepciones).
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE PROCEDURE sp_purgar_datos_seguridad(
    p_tamano_lote INTEGER DEFAULT 1000
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_borradas INTEGER;
BEGIN
    IF p_tamano_lote IS NULL OR p_tamano_lote <= 0 THEN
        RAISE EXCEPTION 'sp_purgar_datos_seguridad: p_tamano_lote debe ser un entero positivo'
            USING ERRCODE = '22004';
    END IF;

    -- 1) Sesiones expiradas.
    LOOP
        DELETE FROM sesiones_usuario
         WHERE id_sesion IN (
               SELECT id_sesion
                 FROM sesiones_usuario
                WHERE fecha_expiracion < CURRENT_TIMESTAMP
                ORDER BY id_sesion
                LIMIT p_tamano_lote
                FOR UPDATE SKIP LOCKED);

        GET DIAGNOSTICS v_borradas = ROW_COUNT;
        COMMIT;
        EXIT WHEN v_borradas = 0;
    END LOOP;

    -- 2) Tokens de recuperacion muertos: usados, o con mas de 24h de antiguedad.
    LOOP
        DELETE FROM tokens_recuperacion
         WHERE id_token IN (
               SELECT id_token
                 FROM tokens_recuperacion
                WHERE usado = TRUE
                   OR fecha_generacion < CURRENT_TIMESTAMP - INTERVAL '24 hours'
                ORDER BY id_token
                LIMIT p_tamano_lote
                FOR UPDATE SKIP LOCKED);

        GET DIAGNOSTICS v_borradas = ROW_COUNT;
        COMMIT;
        EXIT WHEN v_borradas = 0;
    END LOOP;

    -- 3) Codigos de respaldo 2FA ya consumidos (ver nota de alcance arriba:
    --    los no usados no se tocan, para no interferir con un setup2Fa en curso).
    LOOP
        DELETE FROM codigos_respaldo_2fa
         WHERE id_codigo IN (
               SELECT id_codigo
                 FROM codigos_respaldo_2fa
                WHERE usado = TRUE
                ORDER BY id_codigo
                LIMIT p_tamano_lote
                FOR UPDATE SKIP LOCKED);

        GET DIAGNOSTICS v_borradas = ROW_COUNT;
        COMMIT;
        EXIT WHEN v_borradas = 0;
    END LOOP;
END;
$$;

COMMENT ON PROCEDURE sp_purgar_datos_seguridad(INTEGER)
    IS 'Fase 4 mantenimiento - Purga por lotes (COMMIT real por lote, FOR UPDATE SKIP LOCKED) sesiones expiradas, tokens de recuperacion muertos y codigos de respaldo 2FA consumidos. Corrige el crecimiento sin limite de A10.';

-- -----------------------------------------------------------------------------
-- Privilegios: igual que sp_registrar_decision_verificacion
-- (V7__verificacion_asistida_ia.sql), el unico otro PROCEDURE del proyecto.
-- ALTER DEFAULT PRIVILEGES ... GRANT EXECUTE ON FUNCTIONS de
-- seed_privilegios.sh cubre las FUNCTION de este directorio, pero un
-- PROCEDURE requiere su propio GRANT EXECUTE ON PROCEDURE explicito. Guardado
-- tras la existencia del rol: seed_privilegios.sh solo lo crea en el primer
-- arranque de un volumen de datos vacio; en una BD restaurada de un dump, en
-- CI o en una instancia gestionada ese rol no existe todavia.
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_app') THEN
        GRANT EXECUTE ON PROCEDURE sp_purgar_datos_seguridad(INTEGER) TO artisync_app;
    END IF;
END
$$;

