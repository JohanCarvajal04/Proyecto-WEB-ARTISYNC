-- ------------------------------------------------------------------------------
-- Motor comun de exportacion de reportes (PDF/CSV/XLSX): permisos de exportar
-- para los reportes de finanzas y contratos. Los permisos de "ver" ya existian
-- (CONTRATO_VER, TRANSACCION_VER, PAGO_AUDITAR, modulo FINANZAS, V1__schema_inicial.sql)
-- pero ninguna pantalla los consumia. Mismo criterio de V15__modulo_auditoria.sql:
-- exportar es sacar datos del sistema en un archivo, es una capacidad mas
-- sensible que solo consultarlos, asi que es un permiso aparte.
-- ------------------------------------------------------------------------------

-- ADMIN: INSERT EXPLICITO. El cross-join de db/seed.sql que da todos los
-- permisos a ADMIN ya corrio como init-script ANTES de que Flyway aplicara
-- esta migracion (mismo motivo documentado en V10 y V15).
INSERT INTO permisos (nombre_permiso, modulo_aplicacion)
VALUES
    ('REPORTE_FINANCIERO_EXPORTAR', 'FINANZAS'),
    ('REPORTE_CONTRATO_EXPORTAR',   'FINANZAS')
ON CONFLICT (nombre_permiso) DO UPDATE
SET modulo_aplicacion = EXCLUDED.modulo_aplicacion;

INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'ADMIN'
  AND p.nombre_permiso IN ('REPORTE_FINANCIERO_EXPORTAR', 'REPORTE_CONTRATO_EXPORTAR')
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- AUDITOR_FINANCIERO ya tenia CONTRATO_VER, PAGO_AUDITAR y TRANSACCION_VER
-- (V1__schema_inicial.sql) sin ninguna pantalla que los usara. Ahora tambien
-- puede exportar los dos reportes nuevos.
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'AUDITOR_FINANCIERO'
  AND p.nombre_permiso IN ('REPORTE_FINANCIERO_EXPORTAR', 'REPORTE_CONTRATO_EXPORTAR')
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
