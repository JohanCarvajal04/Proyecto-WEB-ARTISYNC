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
-- seleccion de ganadores en una unica sentencia, en vez de N idas y vueltas a
-- la base.
--
-- REQ-F-023 (V41__sorteo_premios): cada sorteo tiene una lista de premios
-- individuales en premios_sorteo, no un unico texto libre. La cantidad de
-- premios (no sorteos.cantidad_ganadores) es la fuente de verdad de cuantos
-- ganadores se sortean, y cada ganador queda emparejado 1 a 1 con un premio
-- distinto via participantes_sorteo.id_premio -- asi el frontend puede
-- mostrar cada premio con su propio ganador en vez de agruparlos todos.
--
-- La notificacion en tiempo real (WebSocket) permanece en Java: no es
-- responsabilidad del motor de datos. La funcion devuelve el listado de
-- ganadores (con su premio) para que el scheduler los notifique despues de
-- confirmar la transaccion.
--
-- Devuelve JSONB: { idSorteo, tituloSorteo, estado,
--   ganadores: [ { idParticipacion, idUsuario, idPremio, descripcionPremio } ] }.
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
    v_titulo              VARCHAR(150);
    v_estado_actual       VARCHAR(50);
    v_total_participantes INTEGER;
    v_cantidad_premios    INTEGER;
    v_ganadores           JSONB;
BEGIN
    IF p_id_sorteo IS NULL THEN
        RAISE EXCEPTION 'fn_seleccionar_ganadores_sorteo: p_id_sorteo es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    SELECT titulo_sorteo, estado_sorteo
      INTO v_titulo, v_estado_actual
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

    SELECT COUNT(*) INTO v_cantidad_premios
      FROM premios_sorteo
     WHERE id_sorteo = p_id_sorteo;

    IF v_cantidad_premios = 0 THEN
        RAISE EXCEPTION 'El sorteo % no tiene premios configurados', p_id_sorteo
            USING ERRCODE = 'P0001';
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

    -- Empareja cada ganador elegido al azar con un premio distinto: rn de la
    -- seleccion aleatoria contra rn de los premios ordenados por "orden".
    WITH candidatos AS (
        SELECT id_participacion, id_usuario
          FROM participantes_sorteo
         WHERE id_sorteo = p_id_sorteo
           AND es_ganador = FALSE
         ORDER BY random()
         LIMIT LEAST(v_cantidad_premios, v_total_participantes)
    ),
    seleccionados AS (
        SELECT id_participacion, id_usuario,
               ROW_NUMBER() OVER () AS rn
          FROM candidatos
    ),
    premios_ordenados AS (
        SELECT id_premio, descripcion_premio,
               ROW_NUMBER() OVER (ORDER BY orden) AS rn
          FROM premios_sorteo
         WHERE id_sorteo = p_id_sorteo
    ),
    asignaciones AS (
        SELECT s.id_participacion, s.id_usuario, p.id_premio, p.descripcion_premio
          FROM seleccionados s
          JOIN premios_ordenados p ON p.rn = s.rn
    ),
    actualizados AS (
        UPDATE participantes_sorteo pt
           SET es_ganador = TRUE,
               id_premio = a.id_premio,
               fecha_notificacion_premio = CURRENT_TIMESTAMP
          FROM asignaciones a
         WHERE pt.id_participacion = a.id_participacion
        RETURNING pt.id_participacion, pt.id_usuario, a.id_premio, a.descripcion_premio
    )
    SELECT jsonb_agg(jsonb_build_object(
               'idParticipacion', id_participacion,
               'idUsuario', id_usuario,
               'idPremio', id_premio,
               'descripcionPremio', descripcion_premio
           ))
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
