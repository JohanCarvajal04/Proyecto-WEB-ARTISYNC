-- ==============================================================================
-- MIGRACIÓN V33: PERMISOS DEL MÓDULO DE RETIROS (PAYOUTS)
-- ==============================================================================
-- El creador solicita el retiro de sus fondos liberados; un auditor financiero
-- revisa la cola y aprueba/rechaza (REQ-F-024, continuación de escrow con PayPal).
--
-- RETIROS_GESTIONAR se asigna a AUDITOR_FINANCIERO y no a ADMIN, siguiendo el
-- mismo criterio que V32__ajuste_permisos_admin_moderador.sql ya estableció
-- para PAGO_AUDITAR/FONDOS_LIBERAR: ADMIN queda restringido a aspectos del
-- sistema, lo financiero es del auditor. Los controladores igual añaden
-- "or hasRole('ADMIN')" como comodín (mismo patrón que
-- PagoGarantiaAuditoriaControlador), así que ADMIN sigue teniendo acceso sin
-- necesitar la fila de permiso.

-- 1. Nuevos permisos, módulo FINANZAS (igual que PAGO_AUDITAR/FONDOS_LIBERAR)
INSERT INTO permisos (nombre_permiso, modulo_aplicacion)
VALUES
    ('RETIROS_SOLICITAR', 'FINANZAS'),
    ('RETIROS_GESTIONAR', 'FINANZAS')
ON CONFLICT (nombre_permiso) DO UPDATE
SET modulo_aplicacion = EXCLUDED.modulo_aplicacion;

-- 2. RETIROS_SOLICITAR -> CREADOR
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'CREADOR'
  AND p.nombre_permiso = 'RETIROS_SOLICITAR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- 3. RETIROS_GESTIONAR -> AUDITOR_FINANCIERO
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'AUDITOR_FINANCIERO'
  AND p.nombre_permiso = 'RETIROS_GESTIONAR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
