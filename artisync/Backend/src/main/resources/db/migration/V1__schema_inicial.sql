-- ==============================================================================
-- 	PROYECTO ARTISYNC - SCRIPT DE CREACIÓN DE BASE DE DATOS (POSTGRESQL)
--  MIGRACIÓN V1 CONSOLIDADA (INCLUYE ESQUEMA COMPLETO, 2FA, INFRACCIONES Y SEED)
-- ==============================================================================

-- MÓDULO 1: SEGURIDAD Y CONTROL DE ACCESO
-- ======================================
CREATE TABLE roles (
    id_rol BIGSERIAL PRIMARY KEY,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE,
    descripcion_rol TEXT
);

CREATE TABLE permisos (
    id_permiso BIGSERIAL PRIMARY KEY,
    nombre_permiso VARCHAR(100) NOT NULL UNIQUE,
    modulo_aplicacion VARCHAR(50)
);

CREATE TABLE rol_permisos (
    id_rol_permiso BIGSERIAL PRIMARY KEY,
    id_rol BIGINT NOT NULL REFERENCES roles(id_rol) ON DELETE CASCADE,
    id_permiso BIGINT NOT NULL REFERENCES permisos(id_permiso) ON DELETE CASCADE,
    CONSTRAINT uk_rol_permiso UNIQUE (id_rol, id_permiso)
);

CREATE TABLE pais (
    id_pais BIGSERIAL PRIMARY KEY,
    nombre_pais VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(255) NOT NULL,
    id_pais BIGINT REFERENCES pais(id_pais),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado_cuenta BOOLEAN DEFAULT TRUE,
    fecha_nacimiento DATE
);

CREATE TABLE usuario_roles (
    id_usuario_rol BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_rol BIGINT NOT NULL REFERENCES roles(id_rol) ON DELETE CASCADE
);

CREATE TABLE sesiones_usuario (
    id_sesion BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    token_jwt TEXT NOT NULL,
    direccion_ip VARCHAR(45),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL
);

CREATE TABLE tokens_recuperacion (
    id_token BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    hash_token VARCHAR(255) NOT NULL,
    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usado BOOLEAN DEFAULT FALSE
);

CREATE TABLE autenticacion_dos_factores (
    id_2fa BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT UNIQUE NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    llave_secreta VARCHAR(255) NOT NULL,
    esta_habilitado BOOLEAN DEFAULT FALSE
);

CREATE TABLE codigos_respaldo_2fa (
    id_codigo BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    codigo_hash VARCHAR(255) NOT NULL,
    usado BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE INDEX idx_codigos_respaldo_usuario ON codigos_respaldo_2fa(id_usuario);

-- ==========================================
-- MÓDULO 2: PERFILES, VERIFICACIÓN Y PORTAFOLIO
-- ==========================================
CREATE TABLE perfiles_creadores (
    id_perfil BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT UNIQUE NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    biografia TEXT,
    url_red_social VARCHAR(255)
);

CREATE TABLE estados_verificacion (
    id_estado_verificacion BIGSERIAL PRIMARY KEY,
    nombre_estado VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE certificados_ia (
    id_certificado BIGSERIAL PRIMARY KEY,
    id_perfil BIGINT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    id_estado_verificacion BIGINT NOT NULL REFERENCES estados_verificacion(id_estado_verificacion),
    url_documento_s3 VARCHAR(255) NOT NULL,
    puntaje_confianza_ia DECIMAL(5,2),
    fecha_analisis TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE habilidades (
    id_habilidad BIGSERIAL PRIMARY KEY,
    nombre_habilidad VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE creador_habilidades (
    id_creador_habilidad BIGSERIAL PRIMARY KEY,
    id_perfil BIGINT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    id_habilidad BIGINT NOT NULL REFERENCES habilidades(id_habilidad) ON DELETE CASCADE,
    nivel_dominio VARCHAR(50)
);

CREATE TABLE portafolios (
    id_portafolio BIGSERIAL PRIMARY KEY,
    id_perfil BIGINT UNIQUE NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_visitas_acumuladas INT DEFAULT 0,
    es_publico BOOLEAN DEFAULT TRUE,
    color_plantilla VARCHAR(20) DEFAULT '#FFFFFF'
);

CREATE TABLE portafolio_items (
    id_item_portafolio BIGSERIAL PRIMARY KEY,
    id_portafolio BIGINT NOT NULL REFERENCES portafolios(id_portafolio) ON DELETE CASCADE,
    titulo_obra VARCHAR(150) NOT NULL,
    descripcion_obra TEXT,
    url_archivo_multimedia VARCHAR(255) NOT NULL,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- MÓDULO 3: CATÁLOGO DINÁMICO DE SERVICIOS
-- ==========================================
CREATE TABLE categorias (
    id_categoria BIGSERIAL PRIMARY KEY,
    nombre_categoria VARCHAR(100) NOT NULL UNIQUE,
    estado_activa BOOLEAN DEFAULT TRUE
);

CREATE TABLE subcategorias (
    id_subcategoria BIGSERIAL PRIMARY KEY,
    id_categoria BIGINT NOT NULL REFERENCES categorias(id_categoria) ON DELETE CASCADE,
    nombre_subcategoria VARCHAR(100) NOT NULL
);

CREATE TABLE servicios (
    id_servicio BIGSERIAL PRIMARY KEY,
    id_perfil BIGINT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    id_subcategoria BIGINT NOT NULL REFERENCES subcategorias(id_subcategoria),
    titulo_servicio VARCHAR(150) NOT NULL,
    descripcion_detallada TEXT NOT NULL,
    precio_base DECIMAL(10,2) NOT NULL,
    url_miniatura VARCHAR(255)
);

CREATE TABLE atributos_dinamicos (
    id_atributo BIGSERIAL PRIMARY KEY,
    nombre_atributo VARCHAR(100) NOT NULL UNIQUE,
    tipo_dato VARCHAR(50) NOT NULL
);

CREATE TABLE servicio_atributos (
    id_servicio_atributo BIGSERIAL PRIMARY KEY,
    id_servicio BIGINT NOT NULL REFERENCES servicios(id_servicio) ON DELETE CASCADE,
    id_atributo BIGINT NOT NULL REFERENCES atributos_dinamicos(id_atributo) ON DELETE CASCADE,
    valor_asignado VARCHAR(255) NOT NULL
);

CREATE TABLE etiquetas (
    id_etiqueta BIGSERIAL PRIMARY KEY,
    nombre_etiqueta VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE servicio_etiquetas (
    id_servicio_etiqueta BIGSERIAL PRIMARY KEY,
    id_servicio BIGINT NOT NULL REFERENCES servicios(id_servicio) ON DELETE CASCADE,
    id_etiqueta BIGINT NOT NULL REFERENCES etiquetas(id_etiqueta) ON DELETE CASCADE
);

-- ==========================================
-- MÓDULO 4: MOTOR DE FLUJOS DE TRABAJO Y PEDIDOS
-- ==========================================
CREATE TABLE flujos_trabajo (
    id_flujo BIGSERIAL PRIMARY KEY,
    nombre_flujo VARCHAR(100) NOT NULL,
    descripcion_flujo TEXT
);

CREATE TABLE etapas_flujo (
    id_etapa BIGSERIAL PRIMARY KEY,
    nombre_etapa VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE flujo_etapas_config (
    id_flujo_etapa BIGSERIAL PRIMARY KEY,
    id_flujo BIGINT NOT NULL REFERENCES flujos_trabajo(id_flujo) ON DELETE CASCADE,
    id_etapa BIGINT NOT NULL REFERENCES etapas_flujo(id_etapa) ON DELETE CASCADE,
    numero_orden INT NOT NULL,
    es_etapa_final BOOLEAN DEFAULT FALSE
);

CREATE TABLE pedidos (
    id_pedido BIGSERIAL PRIMARY KEY,
    id_usuario_cliente BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    id_servicio BIGINT NOT NULL REFERENCES servicios(id_servicio),
    id_flujo BIGINT NOT NULL REFERENCES flujos_trabajo(id_flujo),
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_entrega_estimada TIMESTAMP,
    precio_pactado DECIMAL(10,2) NOT NULL
);

CREATE TABLE historial_estados_pedido (
    id_historial_estado BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_etapa BIGINT NOT NULL REFERENCES etapas_flujo(id_etapa),
    fecha_transicion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    observacion TEXT
);

CREATE TABLE motivos_rechazo (
    id_motivo BIGSERIAL PRIMARY KEY,
    descripcion_motivo VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE tickets_revision (
    id_ticket BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_motivo BIGINT NOT NULL REFERENCES motivos_rechazo(id_motivo),
    descripcion_cliente TEXT NOT NULL,
    costo_adicional_generado DECIMAL(10,2) DEFAULT 0.00,
    estado_ticket VARCHAR(50) DEFAULT 'Abierto'
);

-- ==========================================
-- MÓDULO 5: LEGAL, ENTREGABLES Y FINANZAS (ESCROW)
-- ==========================================
CREATE TABLE plantillas_contrato (
    id_plantilla BIGSERIAL PRIMARY KEY,
    version_legal VARCHAR(50) NOT NULL UNIQUE,
    cuerpo_html_plantilla TEXT NOT NULL
);

CREATE TABLE contratos (
    id_contrato BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT UNIQUE NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_plantilla BIGINT NOT NULL REFERENCES plantillas_contrato(id_plantilla),
    hash_firma_cliente VARCHAR(255),
    hash_firma_creador VARCHAR(255),
    limite_revisiones INT DEFAULT 0,
    fecha_formalizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    url_documento_pdf VARCHAR(255)
);

CREATE TABLE entregables_finales (
    id_entregable BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    url_version_marca_agua VARCHAR(255),
    url_version_limpia VARCHAR(255),
    esta_liberado BOOLEAN DEFAULT FALSE
);

CREATE TABLE pagos_garantia (
    id_pago BIGSERIAL PRIMARY KEY,
    id_contrato BIGINT UNIQUE NOT NULL REFERENCES contratos(id_contrato) ON DELETE CASCADE,
    id_orden_paypal VARCHAR(100),
    monto_retenido DECIMAL(10,2) NOT NULL,
    estado_fondos VARCHAR(50) DEFAULT 'Retenido'
);

CREATE TABLE transacciones_pago (
    id_transaccion BIGSERIAL PRIMARY KEY,
    id_pago BIGINT NOT NULL REFERENCES pagos_garantia(id_pago) ON DELETE CASCADE,
    tipo_transaccion VARCHAR(50) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha_ejecucion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- MÓDULO 6: COMUNICACIÓN Y NOTIFICACIONES
-- ==========================================
CREATE TABLE salas_chat (
    id_sala BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT UNIQUE NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    fecha_apertura TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sala_activa BOOLEAN DEFAULT TRUE
);

CREATE TABLE mensajes (
    id_mensaje BIGSERIAL PRIMARY KEY,
    id_sala BIGINT NOT NULL REFERENCES salas_chat(id_sala) ON DELETE CASCADE,
    id_remitente BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    cuerpo_mensaje TEXT,
    fecha_hora_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    leido BOOLEAN DEFAULT FALSE
);

CREATE TABLE documentos_adjuntos (
    id_adjunto BIGSERIAL PRIMARY KEY,
    id_mensaje BIGINT NOT NULL REFERENCES mensajes(id_mensaje) ON DELETE CASCADE,
    url_archivo VARCHAR(255) NOT NULL,
    tipo_mime VARCHAR(50),
    peso_bytes INT
);

CREATE TABLE tipos_notificacion (
    id_tipo_notificacion BIGSERIAL PRIMARY KEY,
    nombre_evento VARCHAR(100) NOT NULL UNIQUE,
    formato_mensaje TEXT
);

CREATE TABLE notificaciones_sistema (
    id_notificacion BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_tipo_notificacion BIGINT NOT NULL REFERENCES tipos_notificacion(id_tipo_notificacion),
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    esta_leida BOOLEAN DEFAULT FALSE
);

CREATE TABLE infracciones_mensaje (
    id_infraccion BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    fecha_infraccion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- MÓDULO 7: SOCIAL, COMUNIDAD Y SORTEOS
-- ==========================================
CREATE TABLE seguidores (
    id_seguimiento BIGSERIAL PRIMARY KEY,
    id_usuario_seguidor BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_perfil_creador BIGINT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    fecha_seguimiento TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notificaciones_activas BOOLEAN DEFAULT TRUE,
    UNIQUE (id_usuario_seguidor, id_perfil_creador)
);

CREATE TABLE comentarios_portafolio (
    id_comentario BIGSERIAL PRIMARY KEY,
    id_item_portafolio BIGINT NOT NULL REFERENCES portafolio_items(id_item_portafolio) ON DELETE CASCADE,
    id_usuario_autor BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    texto_comentario TEXT NOT NULL,
    fecha_publicacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado_moderacion VARCHAR(50) DEFAULT 'Activo'
);

CREATE TABLE likes_portafolio (
    id_like BIGSERIAL PRIMARY KEY,
    id_item_portafolio BIGINT NOT NULL REFERENCES portafolio_items(id_item_portafolio) ON DELETE CASCADE,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    fecha_like TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_item_portafolio, id_usuario)
);

CREATE TABLE resenas_servicios (
    id_resena BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT UNIQUE NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    calificacion_estrellas INT CHECK (calificacion_estrellas BETWEEN 1 AND 5),
    texto_resena TEXT,
    fecha_resena TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sorteos (
    id_sorteo BIGSERIAL PRIMARY KEY,
    id_perfil_creador BIGINT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    titulo_sorteo VARCHAR(150) NOT NULL,
    descripcion_premios TEXT NOT NULL,
    cantidad_ganadores INT NOT NULL DEFAULT 1,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_cierre TIMESTAMP NOT NULL,
    estado_sorteo VARCHAR(50) DEFAULT 'Activo'
);

CREATE TABLE participantes_sorteo (
    id_participacion BIGSERIAL PRIMARY KEY,
    id_sorteo BIGINT NOT NULL REFERENCES sorteos(id_sorteo) ON DELETE CASCADE,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    fecha_inscripcion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    es_ganador BOOLEAN DEFAULT FALSE,
    fecha_notificacion_premio TIMESTAMP,
    UNIQUE (id_sorteo, id_usuario)
);

-- ==============================================================================
-- SEED DE DATOS INICIALES CONSOLIDADO (ROLES, PERMISOS RBAC Y USUARIO ADMIN)
-- ==============================================================================

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
      'PAIS_VER', 'ROL_VER'
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
    '$2a$12$/JZ0KHnWYrbDokk9ocQaE.Ae8i52jidA/PiNbsHXXWK0lrOfHbOVm',
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
