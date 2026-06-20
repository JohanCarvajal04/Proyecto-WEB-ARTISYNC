-- ==============================================================================
-- 	PROYECTO ARTISYNC - SCRIPT DE CREACIÓN DE BASE DE DATOS (POSTGRESQL)
-- ==============================================================================

-- MÓDULO 1: SEGURIDAD Y CONTROL DE ACCESO
-- ======================================
CREATE TABLE roles (
    id_rol SERIAL PRIMARY KEY,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE,
    descripcion_rol TEXT
);
CREATE TABLE permisos (
    id_permiso SERIAL PRIMARY KEY,
    nombre_permiso VARCHAR(100) NOT NULL UNIQUE,
    modulo_aplicacion VARCHAR(50)
);
CREATE TABLE rol_permisos (
    id_rol_permiso SERIAL PRIMARY KEY,
    id_rol INT NOT NULL REFERENCES roles(id_rol) ON DELETE CASCADE,
    id_permiso INT NOT NULL REFERENCES permisos(id_permiso) ON DELETE CASCADE
);
CREATE TABLE pais (
    id_pais SERIAL PRIMARY KEY,
    nombre_pais VARCHAR(100) NOT NULL UNIQUE
);
CREATE TABLE usuarios (
    id_usuario SERIAL PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(255) NOT NULL,
    id_pais INT REFERENCES pais(id_pais),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado_cuenta BOOLEAN DEFAULT TRUE
);
CREATE TABLE usuario_roles (
    id_usuario_rol SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_rol INT NOT NULL REFERENCES roles(id_rol) ON DELETE CASCADE
);
CREATE TABLE sesiones_usuario (
    id_sesion SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    token_jwt TEXT NOT NULL,
    direccion_ip VARCHAR(45),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL
);
CREATE TABLE tokens_recuperacion (
    id_token SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    hash_token VARCHAR(255) NOT NULL,
    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usado BOOLEAN DEFAULT FALSE
);
CREATE TABLE autenticacion_dos_factores (
    id_2fa SERIAL PRIMARY KEY,
    id_usuario INT UNIQUE NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    llave_secreta VARCHAR(255) NOT NULL,
    esta_habilitado BOOLEAN DEFAULT FALSE
);
 
-- ==========================================
-- MÓDULO 2: PERFILES, VERIFICACIÓN Y PORTAFOLIO
-- ==========================================
CREATE TABLE perfiles_creadores (
    id_perfil SERIAL PRIMARY KEY,
    id_usuario INT UNIQUE NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    biografia TEXT,
    url_red_social VARCHAR(255)
);
CREATE TABLE estados_verificacion (
    id_estado_verificacion SERIAL PRIMARY KEY,
    nombre_estado VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE certificados_ia (
    id_certificado SERIAL PRIMARY KEY,
    id_perfil INT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    id_estado_verificacion INT NOT NULL REFERENCES estados_verificacion(id_estado_verificacion),
    url_documento_s3 VARCHAR(255) NOT NULL,
    puntaje_confianza_ia DECIMAL(5,2),
    fecha_analisis TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE habilidades (
    id_habilidad SERIAL PRIMARY KEY,
    nombre_habilidad VARCHAR(100) NOT NULL UNIQUE
);
CREATE TABLE creador_habilidades (
    id_creador_habilidad SERIAL PRIMARY KEY,
    id_perfil INT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    id_habilidad INT NOT NULL REFERENCES habilidades(id_habilidad) ON DELETE CASCADE,
    nivel_dominio VARCHAR(50) -- Ej: Básico, Intermedio, Experto
);
CREATE TABLE portafolios (
    id_portafolio SERIAL PRIMARY KEY,
    id_perfil INT UNIQUE NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_visitas_acumuladas INT DEFAULT 0,
    es_publico BOOLEAN DEFAULT TRUE,
    color_plantilla VARCHAR(20) DEFAULT '#FFFFFF'
);
CREATE TABLE portafolio_items (
    id_item_portafolio SERIAL PRIMARY KEY,
    id_portafolio INT NOT NULL REFERENCES portafolios(id_portafolio) ON DELETE CASCADE,
    titulo_obra VARCHAR(150) NOT NULL,
    descripcion_obra TEXT,
    url_archivo_multimedia VARCHAR(255) NOT NULL,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
 
-- ==========================================
-- MÓDULO 3: CATÁLOGO DINÁMICO DE SERVICIOS
-- ==========================================
CREATE TABLE categorias (
    id_categoria SERIAL PRIMARY KEY,
    nombre_categoria VARCHAR(100) NOT NULL UNIQUE,
    estado_activa BOOLEAN DEFAULT TRUE
);
CREATE TABLE subcategorias (
    id_subcategoria SERIAL PRIMARY KEY,
    id_categoria INT NOT NULL REFERENCES categorias(id_categoria) ON DELETE CASCADE,
    nombre_subcategoria VARCHAR(100) NOT NULL
);
CREATE TABLE servicios (
    id_servicio SERIAL PRIMARY KEY,
    id_perfil INT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    id_subcategoria INT NOT NULL REFERENCES subcategorias(id_subcategoria),
    titulo_servicio VARCHAR(150) NOT NULL,
    descripcion_detallada TEXT NOT NULL,
    precio_base DECIMAL(10,2) NOT NULL,
    url_miniatura VARCHAR(255)
);
CREATE TABLE atributos_dinamicos (
    id_atributo SERIAL PRIMARY KEY,
    nombre_atributo VARCHAR(100) NOT NULL UNIQUE,
    tipo_dato VARCHAR(50) NOT NULL -- Ej: Texto, Numero, Booleano
);
CREATE TABLE servicio_atributos (
    id_servicio_atributo SERIAL PRIMARY KEY,
    id_servicio INT NOT NULL REFERENCES servicios(id_servicio) ON DELETE CASCADE,
    id_atributo INT NOT NULL REFERENCES atributos_dinamicos(id_atributo) ON DELETE CASCADE,
    valor_asignado VARCHAR(255) NOT NULL
);
CREATE TABLE etiquetas (
    id_etiqueta SERIAL PRIMARY KEY,
    nombre_etiqueta VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE servicio_etiquetas (
    id_servicio_etiqueta SERIAL PRIMARY KEY,
    id_servicio INT NOT NULL REFERENCES servicios(id_servicio) ON DELETE CASCADE,
    id_etiqueta INT NOT NULL REFERENCES etiquetas(id_etiqueta) ON DELETE CASCADE
);
 
-- ==========================================
-- MÓDULO 4: MOTOR DE FLUJOS DE TRABAJO Y PEDIDOS
-- ==========================================
CREATE TABLE flujos_trabajo (
    id_flujo SERIAL PRIMARY KEY,
    nombre_flujo VARCHAR(100) NOT NULL,
    descripcion_flujo TEXT
);
CREATE TABLE etapas_flujo (
    id_etapa SERIAL PRIMARY KEY,
    nombre_etapa VARCHAR(100) NOT NULL UNIQUE
);
CREATE TABLE flujo_etapas_config (
    id_flujo_etapa SERIAL PRIMARY KEY,
    id_flujo INT NOT NULL REFERENCES flujos_trabajo(id_flujo) ON DELETE CASCADE,
    id_etapa INT NOT NULL REFERENCES etapas_flujo(id_etapa) ON DELETE CASCADE,
    numero_orden INT NOT NULL,
    es_etapa_final BOOLEAN DEFAULT FALSE
);
CREATE TABLE pedidos (
    id_pedido SERIAL PRIMARY KEY,
    id_usuario_cliente INT NOT NULL REFERENCES usuarios(id_usuario),
    id_servicio INT NOT NULL REFERENCES servicios(id_servicio),
    id_flujo INT NOT NULL REFERENCES flujos_trabajo(id_flujo),
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_entrega_estimada TIMESTAMP,
    precio_pactado DECIMAL(10,2) NOT NULL
);
CREATE TABLE historial_estados_pedido (
    id_historial_estado SERIAL PRIMARY KEY,
    id_pedido INT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_etapa INT NOT NULL REFERENCES etapas_flujo(id_etapa),
    fecha_transicion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    observacion TEXT
);
CREATE TABLE motivos_rechazo (
    id_motivo SERIAL PRIMARY KEY,
    descripcion_motivo VARCHAR(150) NOT NULL UNIQUE
);
CREATE TABLE tickets_revision (
    id_ticket SERIAL PRIMARY KEY,
    id_pedido INT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_motivo INT NOT NULL REFERENCES motivos_rechazo(id_motivo),
    descripcion_cliente TEXT NOT NULL,
    costo_adicional_generado DECIMAL(10,2) DEFAULT 0.00,
    estado_ticket VARCHAR(50) DEFAULT 'Abierto'
);
 
-- ==========================================
-- MÓDULO 5: LEGAL, ENTREGABLES Y FINANZAS (ESCROW)
-- ==========================================
CREATE TABLE plantillas_contrato (
    id_plantilla SERIAL PRIMARY KEY,
    version_legal VARCHAR(50) NOT NULL UNIQUE,
    cuerpo_html_plantilla TEXT NOT NULL
);
CREATE TABLE contratos (
    id_contrato SERIAL PRIMARY KEY,
    id_pedido INT UNIQUE NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_plantilla INT NOT NULL REFERENCES plantillas_contrato(id_plantilla),
    hash_firma_cliente VARCHAR(255),
    hash_firma_creador VARCHAR(255),
    limite_revisiones INT DEFAULT 0,
    fecha_formalizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    url_documento_pdf VARCHAR(255)
);
CREATE TABLE entregables_finales (
    id_entregable SERIAL PRIMARY KEY,
    id_pedido INT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    url_version_marca_agua VARCHAR(255),
    url_version_limpia VARCHAR(255),
    esta_liberado BOOLEAN DEFAULT FALSE
);
CREATE TABLE pagos_garantia (
    id_pago SERIAL PRIMARY KEY,
    id_contrato INT UNIQUE NOT NULL REFERENCES contratos(id_contrato) ON DELETE CASCADE,
    id_orden_paypal VARCHAR(100),
    monto_retenido DECIMAL(10,2) NOT NULL,
    estado_fondos VARCHAR(50) DEFAULT 'Retenido' -- Retenido, Liberado, Reembolsado
);
CREATE TABLE transacciones_pago (
    id_transaccion SERIAL PRIMARY KEY,
    id_pago INT NOT NULL REFERENCES pagos_garantia(id_pago) ON DELETE CASCADE,
    tipo_transaccion VARCHAR(50) NOT NULL, -- Ingreso, Egreso, Comision
    monto DECIMAL(10,2) NOT NULL,
    fecha_ejecucion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
 
-- ==========================================
-- MÓDULO 6: COMUNICACIÓN Y NOTIFICACIONES
-- ==========================================
CREATE TABLE salas_chat (
    id_sala SERIAL PRIMARY KEY,
    id_pedido INT UNIQUE NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    fecha_apertura TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sala_activa BOOLEAN DEFAULT TRUE
);
CREATE TABLE mensajes (
    id_mensaje SERIAL PRIMARY KEY,
    id_sala INT NOT NULL REFERENCES salas_chat(id_sala) ON DELETE CASCADE,
    id_remitente INT NOT NULL REFERENCES usuarios(id_usuario),
    cuerpo_mensaje TEXT,
    fecha_hora_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    leido BOOLEAN DEFAULT FALSE
);
CREATE TABLE documentos_adjuntos (
    id_adjunto SERIAL PRIMARY KEY,
    id_mensaje INT NOT NULL REFERENCES mensajes(id_mensaje) ON DELETE CASCADE,
    url_archivo VARCHAR(255) NOT NULL,
    tipo_mime VARCHAR(50),
    peso_bytes INT
);
CREATE TABLE tipos_notificacion (
    id_tipo_notificacion SERIAL PRIMARY KEY,
    nombre_evento VARCHAR(100) NOT NULL UNIQUE,
    formato_mensaje TEXT
);
CREATE TABLE notificaciones_sistema (
    id_notificacion SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_tipo_notificacion INT NOT NULL REFERENCES tipos_notificacion(id_tipo_notificacion),
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    esta_leida BOOLEAN DEFAULT FALSE
);
 
-- ==========================================
-- MÓDULO 7: SOCIAL, COMUNIDAD Y SORTEOS
-- ==========================================
CREATE TABLE seguidores (
    id_seguimiento SERIAL PRIMARY KEY,
    id_usuario_seguidor INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_perfil_creador INT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    fecha_seguimiento TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notificaciones_activas BOOLEAN DEFAULT TRUE,
    UNIQUE (id_usuario_seguidor, id_perfil_creador)
);
CREATE TABLE comentarios_portafolio (
    id_comentario SERIAL PRIMARY KEY,
    id_item_portafolio INT NOT NULL REFERENCES portafolio_items(id_item_portafolio) ON DELETE CASCADE,
    id_usuario_autor INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    texto_comentario TEXT NOT NULL,
    fecha_publicacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado_moderacion VARCHAR(50) DEFAULT 'Activo'
);
CREATE TABLE likes_portafolio (
    id_like SERIAL PRIMARY KEY,
    id_item_portafolio INT NOT NULL REFERENCES portafolio_items(id_item_portafolio) ON DELETE CASCADE,
    id_usuario INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    fecha_like TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_item_portafolio, id_usuario)
);
CREATE TABLE resenas_servicios (
    id_resena SERIAL PRIMARY KEY,
    id_pedido INT UNIQUE NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    calificacion_estrellas INT CHECK (calificacion_estrellas BETWEEN 1 AND 5),
    texto_resena TEXT,
    fecha_resena TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE sorteos (
    id_sorteo SERIAL PRIMARY KEY,
    id_perfil_creador INT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    titulo_sorteo VARCHAR(150) NOT NULL,
    descripcion_premios TEXT NOT NULL,
    cantidad_ganadores INT NOT NULL DEFAULT 1,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_cierre TIMESTAMP NOT NULL,
    estado_sorteo VARCHAR(50) DEFAULT 'Activo'
);
CREATE TABLE participantes_sorteo (
    id_participacion SERIAL PRIMARY KEY,
    id_sorteo INT NOT NULL REFERENCES sorteos(id_sorteo) ON DELETE CASCADE,
    id_usuario INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    fecha_inscripcion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    es_ganador BOOLEAN DEFAULT FALSE,
    fecha_notificacion_premio TIMESTAMP,
    UNIQUE (id_sorteo, id_usuario)
);
