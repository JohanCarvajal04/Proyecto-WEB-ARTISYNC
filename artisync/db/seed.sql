-- ==============================================================================
-- ARTISYNC — Semilla de datos inicial (Bloque B.1 de la guia)
-- ==============================================================================
--
-- Roles, permisos RBAC y usuario administrador inicial. Fuente: seccion de seed
-- de Backend/src/main/resources/db/migration/V1__schema_inicial.sql (lineas
-- 390-542), con el hash BCrypt del admin regenerado para una contrasena
-- documentada (ver README): el hash original de V1 no tenia contrasena en
-- texto plano documentada en ningun lugar del repositorio.
--
-- Usuario admin de arranque:
--   correo:      admin@artisync.com
--   contrasena:  ArtisyncAdmin2026!
--   (factor de costo BCrypt 12, igual que AuthConfig.java: new BCryptPasswordEncoder(12))
--
-- La cuenta de aplicacion con privilegios minimos (A.2.3) se crea aparte, en
-- seed_privilegios.sh (necesita leer una variable de entorno; los .sql puros
-- montados en /docker-entrypoint-initdb.d/ no tienen acceso al shell).
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

-- 2. Insertar los 35 Permisos Granulares clasificados por Módulo de Aplicación
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
    
    -- SISTEMA / PAISES
    ('PAIS_VER', 'SISTEMA'),
    ('PAIS_CREAR', 'SISTEMA'),
    ('PAIS_EDITAR', 'SISTEMA'),
    ('PAIS_ELIMINAR', 'SISTEMA'),

    -- SISTEMA / ACCESO A PANTALLAS DE ADMINISTRACIÓN (ver V10__permisos_navegacion.sql)
    ('PANEL_MODERACION_VER', 'SISTEMA'),
    ('CONFIGURACION_GESTIONAR', 'SISTEMA'),
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
      'TICKET_REVISAR', 'TICKET_RESOLVER', 'SALA_VER', 'NOTIFICACION_ENVIAR', 'PAIS_VER'
  )
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- 6. Asignar Permisos Específicos al AUDITOR_FINANCIERO
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'AUDITOR_FINANCIERO'
  AND p.nombre_permiso IN (
      'CONTRATO_VER', 'PAGO_AUDITAR', 'FONDOS_LIBERAR', 'TRANSACCION_VER',
      'PAIS_VER', 'ROL_VER'
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
