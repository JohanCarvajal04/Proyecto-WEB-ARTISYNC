-- ===========================================================================
-- R__procedimientos.sql — VERSIÓN CONSOLIDADA (PROPUESTA)
-- ===========================================================================
--
-- Copia de db/migration/R__procedimientos.sql SIN el bloque de DDL inicial.
--
-- El original arrancaba con el contenido de db/procs/V8__estructuras_para_
-- procedimientos.sql: ALTER TABLE pedidos ADD COLUMN codigo_pedido,
-- CREATE SEQUENCE seq_codigo_pedido y cuatro CREATE INDEX. Eso es DDL dentro de
-- una migración REPETIBLE, que Flyway reaplica cada vez que cambia su checksum.
-- Aquí ese DDL vive donde le corresponde: V1__esquema.sql, con las tablas.
-- (El nombre "V8__" de ese archivo tampoco era una versión de Flyway y se
-- confundía con la V8 real, sesiones_usuario_jti.)
--
-- Al adoptar esta carpeta hay que regenerar el archivo real con
-- `make sync-procs` tras retirar db/procs/V8__estructuras_para_procedimientos.sql,
-- para que scripts/sync-procs.sh --check siga pasando en CI.
--
-- Migración REPETIBLE: Flyway la reaplica cada vez que cambia su checksum.
-- Todas las rutinas usan CREATE OR REPLACE, por lo que reaplicarla es inocuo.
--
-- Rutinas incluidas (13):
--   - fn_calificacion_promedio_creador.sql
--   - fn_catalogo_filtrado.sql
--   - fn_cerrar_pedidos_vencidos.sql
--   - fn_eliminar_rol.sql
--   - fn_generar_codigo_pedido.sql
--   - fn_liberar_fondos_escrow.sql
--   - fn_registrar_infraccion.sql
--   - fn_registrar_usuario.sql
--   - fn_reporte_comisiones_creador.sql
--   - fn_resolver_estado_login.sql
--   - fn_restablecer_contrasena.sql
--   - fn_seleccionar_ganadores_sorteo.sql
--   - fn_sincronizar_permisos_rol.sql
-- ===========================================================================

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

