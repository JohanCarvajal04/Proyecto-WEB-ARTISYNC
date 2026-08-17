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
