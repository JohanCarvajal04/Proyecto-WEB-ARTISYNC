-- ==============================================================================
-- ARTISYNC — DATOS INICIALES CONSOLIDADOS
-- ==============================================================================
--
-- PROPUESTA. No se ejecuta: spring.flyway.locations apunta a classpath:db/migration.
-- Ver README.md de esta carpeta.
--
-- Reemplaza al seed de V1 más los permisos que hoy llegan sueltos en V10 y V12,
-- y al seed de estados_verificacion de V7. Va separado de V1__esquema.sql
-- porque el catálogo de datos cambia mucho más a menudo que el DDL: así una
-- alta de permiso no obliga a tocar el archivo del esquema.
--
-- Catálogo final: 6 roles y 40 permisos. CONFIGURACION_GESTIONAR (creado por
-- V10 y retirado por V11 al eliminarse la pantalla admin/settings) sencillamente
-- no se crea, con lo que V11 deja de tener razón de existir.
--
-- Todos los INSERT son idempotentes (ON CONFLICT): esta migración se puede
-- reejecutar sobre una base ya sembrada sin duplicar nada.
--
-- Usuario admin de arranque:
--   correo:     admin@artisync.com
--   contrasena: ArtisyncAdmin2026!
--   (BCrypt coste 12, igual que AuthConfig.java: new BCryptPasswordEncoder(12))
-- Es el hash documentado de db/seed.sql, NO el de V1: el original nunca tuvo su
-- contrasena en texto plano documentada en ningun lugar del repositorio.
-- ==============================================================================

-- ==============================================================================
-- SEED DE DATOS INICIALES CONSOLIDADO (ROLES, PERMISOS RBAC Y USUARIO ADMIN)
-- ==============================================================================

-- 0. Insertar Países Iniciales (Catálogo Mundial Completo)
INSERT INTO pais (nombre_pais) VALUES 
    ('Afganistán'), ('Albania'), ('Alemania'), ('Andorra'), ('Angola'), ('Antigua y Barbuda'), ('Arabia Saudita'), ('Argelia'), ('Argentina'), ('Armenia'),
    ('Australia'), ('Austria'), ('Azerbaiyán'), ('Bahamas'), ('Bangladés'), ('Barbados'), ('Baréin'), ('Bélgica'), ('Belice'), ('Benín'),
    ('Bielorrusia'), ('Birmania'), ('Bolivia'), ('Bosnia y Herzegovina'), ('Botsuana'), ('Brasil'), ('Brunéi'), ('Bulgaria'), ('Burkina Faso'), ('Burundi'),
    ('Bután'), ('Cabo Verde'), ('Camboya'), ('Camerún'), ('Canadá'), ('Catar'), ('Chad'), ('Chile'), ('China'), ('Chipre'),
    ('Ciudad del Vaticano'), ('Colombia'), ('Comoras'), ('Corea del Norte'), ('Corea del Sur'), ('Costa de Marfil'), ('Costa Rica'), ('Croacia'), ('Cuba'), ('Dinamarca'),
    ('Dominica'), ('Ecuador'), ('Egipto'), ('El Salvador'), ('Emiratos Árabes Unidos'), ('Eritrea'), ('Eslovaquia'), ('Eslovenia'), ('España'), ('Estados Unidos'),
    ('Estonia'), ('Etiopía'), ('Filipinas'), ('Finlandia'), ('Fiyi'), ('Francia'), ('Gabón'), ('Gambia'), ('Georgia'), ('Ghana'),
    ('Granada'), ('Grecia'), ('Guatemala'), ('Guyana'), ('Guinea'), ('Guinea Ecuatorial'), ('Guinea-Bisáu'), ('Haití'), ('Honduras'), ('Hungría'),
    ('India'), ('Indonesia'), ('Irak'), ('Irán'), ('Irlanda'), ('Islandia'), ('Islas Marshall'), ('Islas Salomón'), ('Israel'), ('Italia'),
    ('Jamaica'), ('Japón'), ('Jordania'), ('Kazajistán'), ('Kenia'), ('Kirguistán'), ('Kiribati'), ('Kuwait'), ('Laos'), ('Lesoto'),
    ('Letonia'), ('Líbano'), ('Liberia'), ('Libia'), ('Liechtenstein'), ('Lituania'), ('Luxemburgo'), ('Macedonia del Norte'), ('Madagascar'), ('Malasia'),
    ('Malaui'), ('Maldivas'), ('Malí'), ('Malta'), ('Marruecos'), ('Mauricio'), ('Mauritania'), ('México'), ('Micronesia'), ('Moldavia'),
    ('Mónaco'), ('Mongolia'), ('Montenegro'), ('Mozambique'), ('Namibia'), ('Nauru'), ('Nepal'), ('Nicaragua'), ('Níger'), ('Nigeria'),
    ('Noruega'), ('Nueva Zelanda'), ('Omán'), ('Países Bajos'), ('Pakistán'), ('Palaos'), ('Palestina'), ('Panamá'), ('Papúa Nueva Guinea'), ('Paraguay'),
    ('Perú'), ('Polonia'), ('Portugal'), ('Puerto Rico'), ('Reino Unido'), ('República Centroafricana'), ('República Checa'), ('República del Congo'), ('República Democrática del Congo'), ('República Dominicana'),
    ('Ruanda'), ('Rumanía'), ('Rusia'), ('Samoa'), ('San Cristóbal y Nieves'), ('San Marino'), ('San Vicente y las Granadinas'), ('Santa Lucía'), ('Santo Tomé y Príncipe'), ('Senegal'),
    ('Serbia'), ('Seychelles'), ('Sierra Leona'), ('Singapur'), ('Siria'), ('Somalia'), ('Sri Lanka'), ('Suazilandia'), ('Sudáfrica'), ('Sudán'),
    ('Sudán del Sur'), ('Suecia'), ('Suiza'), ('Surinam'), ('Tailandia'), ('Tanzania'), ('Tayikistán'), ('Timor Oriental'), ('Togo'), ('Tonga'),
    ('Trinidad y Tobago'), ('Túnez'), ('Turkmenistán'), ('Turquía'), ('Tuvalu'), ('Ucrania'), ('Uganda'), ('Uruguay'), ('Uzbekistán'), ('Vanuatu'),
    ('Venezuela'), ('Vietnam'), ('Yemen'), ('Yibuti'), ('Zambia'), ('Zimbabue')
ON CONFLICT (nombre_pais) DO NOTHING;

-- 1. Insertar los 6 Roles Operativos y Administrativos
INSERT INTO roles (nombre_rol, descripcion_rol)
VALUES 
    ('ADMIN', 'Administrador General del Sistema con acceso irrestricto a todos los módulos'),
    ('MODERADOR', 'Responsable de revisar certificados IA, portafolios, comentarios e infracciones'),
    ('SOPORTE', 'Asistencia técnica, consulta de usuarios, sesiones 2FA y resolución de tickets'),
    ('AUDITOR_FINANCIERO', 'Auditoría de contratos, supervisión de pagos Escrow y transacciones'),
    ('CREADOR', 'Artista o creador que ofrece servicios digitales y publica portafolio'),
    ('CLIENTE', 'Comprador que explora el catálogo, contrata servicios y realiza pagos')
ON CONFLICT (nombre_rol) DO UPDATE 
SET descripcion_rol = EXCLUDED.descripcion_rol;

-- 2. Insertar los 40 Permisos Granulares clasificados por Módulo de Aplicación
INSERT INTO permisos (nombre_permiso, modulo_aplicacion)
VALUES 
    -- SEGURIDAD
    ('USUARIO_VER', 'SEGURIDAD'),
    ('USUARIO_CREAR', 'SEGURIDAD'),
    ('USUARIO_EDITAR', 'SEGURIDAD'),
    ('USUARIO_ELIMINAR', 'SEGURIDAD'),
    ('USUARIO_SUSPENDER', 'SEGURIDAD'),
    ('ROL_VER', 'SEGURIDAD'),
    ('ROL_GESTIONAR', 'SEGURIDAD'),
    ('PERMISO_VER', 'SEGURIDAD'),
    ('ROL_ASIGNAR_PERMISO', 'SEGURIDAD'),
    ('SESION_REVOCAR', 'SEGURIDAD'),
    -- Bitácora de auditoría transversal (ver V12__modulo_auditoria.sql)
    ('AUDITORIA_VER', 'SEGURIDAD'),
    ('AUDITORIA_EXPORTAR', 'SEGURIDAD'),

    -- SISTEMA / PAISES
    ('PAIS_VER', 'SISTEMA'),
    ('PAIS_CREAR', 'SISTEMA'),
    ('PAIS_EDITAR', 'SISTEMA'),
    ('PAIS_ELIMINAR', 'SISTEMA'),

    -- SISTEMA / ACCESO A PANTALLAS DE ADMINISTRACIÓN (ver V10__permisos_navegacion.sql)
    -- CONFIGURACION_GESTIONAR se retiró en V11: la pantalla admin/settings que
    -- gobernaba ya no existe (ver V11__retirar_permiso_configuracion.sql).
    ('PANEL_MODERACION_VER', 'SISTEMA'),
    ('INFRACCION_GESTIONAR', 'SISTEMA'),
    ('FLUJO_GESTIONAR', 'SISTEMA'),
    
    -- PORTAFOLIO
    ('PORTAFOLIO_CREAR', 'PORTAFOLIO'),
    ('PORTAFOLIO_MODERAR', 'PORTAFOLIO'),
    ('CERTIFICADO_REVISAR', 'PORTAFOLIO'),
    
    -- CATALOGO
    ('CATEGORIA_GESTIONAR', 'CATALOGO'),
    ('SERVICIO_CREAR', 'CATALOGO'),
    ('SERVICIO_MODERAR', 'CATALOGO'),
    
    -- PEDIDOS
    ('PEDIDO_CREAR', 'PEDIDOS'),
    ('PEDIDO_GESTIONAR', 'PEDIDOS'),
    ('TICKET_REVISAR', 'PEDIDOS'),
    ('TICKET_RESOLVER', 'PEDIDOS'),
    
    -- FINANZAS
    ('CONTRATO_VER', 'FINANZAS'),
    ('CONTRATO_FIRMAR', 'FINANZAS'),
    ('PAGO_AUDITAR', 'FINANZAS'),
    ('FONDOS_LIBERAR', 'FINANZAS'),
    ('TRANSACCION_VER', 'FINANZAS'),
    
    -- COMUNICACION
    ('SALA_VER', 'COMUNICACION'),
    ('MENSAJE_ENVIAR', 'COMUNICACION'),
    ('MENSAJE_MODERAR', 'COMUNICACION'),
    ('NOTIFICACION_ENVIAR', 'COMUNICACION'),
    
    -- SOCIAL
    ('COMENTARIO_MODERAR', 'SOCIAL'),
    ('SORTEO_CREAR', 'SOCIAL')
ON CONFLICT (nombre_permiso) DO UPDATE 
SET modulo_aplicacion = EXCLUDED.modulo_aplicacion;

-- 3. Asignar TODOS LOS PERMISOS automáticamente al rol ADMIN
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'ADMIN'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- 4. Asignar Permisos Específicos al MODERADOR
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'MODERADOR'
  AND p.nombre_permiso IN (
      'PORTAFOLIO_MODERAR', 'CERTIFICADO_REVISAR', 'CATEGORIA_GESTIONAR',
      'SERVICIO_MODERAR', 'MENSAJE_MODERAR', 'NOTIFICACION_ENVIAR', 'COMENTARIO_MODERAR',
      'PAIS_VER', 'ROL_VER', 'PANEL_MODERACION_VER'
  )
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- 5. Asignar Permisos Específicos a SOPORTE
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'SOPORTE'
  AND p.nombre_permiso IN (
      'USUARIO_VER', 'USUARIO_SUSPENDER', 'ROL_VER', 'PERMISO_VER', 'SESION_REVOCAR',
      'TICKET_REVISAR', 'TICKET_RESOLVER', 'SALA_VER', 'NOTIFICACION_ENVIAR', 'PAIS_VER',
      'AUDITORIA_VER'
  )
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- 6. Asignar Permisos Específicos al AUDITOR_FINANCIERO
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'AUDITOR_FINANCIERO'
  AND p.nombre_permiso IN (
      'CONTRATO_VER', 'PAGO_AUDITAR', 'FONDOS_LIBERAR', 'TRANSACCION_VER',
      'PAIS_VER', 'ROL_VER', 'AUDITORIA_VER', 'AUDITORIA_EXPORTAR'
  )
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- 7. Asignar Permisos Específicos al CREADOR
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'CREADOR'
  AND p.nombre_permiso IN (
      'PORTAFOLIO_CREAR', 'SERVICIO_CREAR', 'PEDIDO_GESTIONAR', 'TICKET_REVISAR',
      'CONTRATO_VER', 'CONTRATO_FIRMAR', 'SALA_VER', 'MENSAJE_ENVIAR', 'SORTEO_CREAR'
  )
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- 8. Asignar Permisos Específicos al CLIENTE
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'CLIENTE'
  AND p.nombre_permiso IN (
      'PEDIDO_CREAR', 'TICKET_REVISAR', 'CONTRATO_VER', 'CONTRATO_FIRMAR',
      'SALA_VER', 'MENSAJE_ENVIAR'
  )
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- 9. Insertar el Usuario Administrador Inicial
INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, fecha_nacimiento, estado_cuenta)
VALUES (
    'Administrador',
    'Artisync',
    'admin@artisync.com',
    '$2a$12$O26tVGE2jZ/6rNDZJYaKyOfDPE0.8E9HIbISLR4nXySuy.nvvycjK',
    '1990-01-01',
    true
)
ON CONFLICT (correo) DO NOTHING;

-- 10. Asignar Rol ADMIN al Usuario Administrador Inicial
INSERT INTO usuario_roles (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM usuarios u, roles r
WHERE u.correo = 'admin@artisync.com' AND r.nombre_rol = 'ADMIN'
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------------------------
-- 11. Estados de verificación (origen V7)
-- ------------------------------------------------------------------------------
-- La tabla estados_verificacion existía desde el esquema original pero nunca se
-- sembró hasta V7. sp_registrar_decision_verificacion exige que el certificado
-- esté en PENDIENTE, así que sin estas cuatro filas el flujo de verificación
-- asistida no arranca.
INSERT INTO estados_verificacion (nombre_estado)
VALUES ('PENDIENTE'), ('APROBADO'), ('RECHAZADO'), ('REQUIERE_ACLARACION')
ON CONFLICT (nombre_estado) DO NOTHING;
