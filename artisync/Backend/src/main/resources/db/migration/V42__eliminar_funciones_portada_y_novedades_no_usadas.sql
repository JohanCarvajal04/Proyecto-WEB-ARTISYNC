-- Elimina fn_actualizar_portada_creador y fn_listar_creadores_seguidos_novedades: nunca fueron
-- invocadas desde el backend Java. La funcionalidad equivalente ya vive en SeguidorServicioImpl
-- (actualizarPortadaYTitulo, listarCreadoresSeguidosNovedades), implementada con JPA y cubierta
-- por SeguidorServicioImplTest.
DROP FUNCTION IF EXISTS fn_actualizar_portada_creador(BIGINT, VARCHAR, VARCHAR);
DROP FUNCTION IF EXISTS fn_listar_creadores_seguidos_novedades(BIGINT);
