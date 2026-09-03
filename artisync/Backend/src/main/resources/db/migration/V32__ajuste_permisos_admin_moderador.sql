-- ==============================================================================
-- MIGRACIÓN V32: AJUSTE DE PERMISOS PARA ADMIN Y MODERADOR
-- ==============================================================================
-- Se retiran permisos operativos y de negocio del rol ADMIN para restringirlo
-- exclusivamente a aspectos del sistema y configuración técnica.
-- Se añaden permisos de gestión operativa al MODERADOR.

-- 1. Retirar permisos operativos y de negocio del rol ADMIN
DELETE FROM rol_permisos
WHERE id_rol = (SELECT id_rol FROM roles WHERE nombre_rol = 'ADMIN')
  AND id_permiso IN (
      SELECT id_permiso FROM permisos WHERE nombre_permiso IN (
          'PANEL_MODERACION_VER',
          'INFRACCION_GESTIONAR',
          'FLUJO_GESTIONAR',
          'FLUJO_MODERAR',
          'PORTAFOLIO_CREAR',
          'PORTAFOLIO_MODERAR',
          'CERTIFICADO_REVISAR',
          'SERVICIO_CREAR',
          'SERVICIO_MODERAR',
          'PEDIDO_CREAR',
          'PEDIDO_GESTIONAR',
          'TICKET_REVISAR',
          'TICKET_RESOLVER',
          'CONTRATO_VER',
          'CONTRATO_FIRMAR',
          'PAGO_AUDITAR',
          'FONDOS_LIBERAR',
          'TRANSACCION_VER',
          'REPORTE_FINANCIERO_EXPORTAR',
          'REPORTE_CONTRATO_EXPORTAR',
          'SALA_VER',
          'MENSAJE_ENVIAR',
          'MENSAJE_MODERAR',
          'NOTIFICACION_ENVIAR',
          'COMENTARIO_MODERAR',
          'SORTEO_CREAR'
      )
  );

-- 2. Añadir permisos de moderación extendida al rol MODERADOR
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'MODERADOR'
  AND p.nombre_permiso IN (
      'INFRACCION_GESTIONAR',
      'FLUJO_MODERAR',
      'TICKET_REVISAR',
      'TICKET_RESOLVER'
  )
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
