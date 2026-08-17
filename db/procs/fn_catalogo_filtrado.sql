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
