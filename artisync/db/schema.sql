-- ==============================================================================
-- ARTISYNC — Esquema consolidado (Bloque B.1 de la guia de la Tercera Entrega)
-- ==============================================================================
--
-- Este archivo consolida, en un solo script idempotente-por-orden, el DDL de las
-- migraciones Flyway V1..V5 tal como existen en
-- Backend/src/main/resources/db/migration/ (la unica fuente de verdad que
-- Spring Boot ejecuta: spring.flyway.locations=classpath:db/migration).
--
-- Se monta en /docker-entrypoint-initdb.d/ para que 'make up' levante el esquema
-- completo sin depender de que el contenedor backend termine de arrancar primero.
-- Flyway, al iniciar el backend, reconoce este esquema via baseline
-- (spring.flyway.baseline-on-migrate=true, spring.flyway.baseline-version=5) y
-- NO vuelve a ejecutar V1..V5 contra el; solo aplicaria V6 en adelante si se
-- agregan nuevas migraciones.
--
-- No editar manualmente: regenerar concatenando el DDL real de V1 (lineas 1-388,
-- sin la seccion de seed) + V2 + V3 + V4 + V5 si esas migraciones cambian.
-- ==============================================================================

-- ── V1__schema_inicial.sql (DDL, sin el seed — ver db/seed.sql) ──
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

-- §2.5 / OBS-AUTO-06: se guarda unicamente el jti (identificador del token),
-- nunca el JWT completo — ver V8__sesiones_usuario_jti.sql para el historial.
CREATE TABLE sesiones_usuario (
    id_sesion BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    jti VARCHAR(36) NOT NULL UNIQUE,
    direccion_ip VARCHAR(45),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL
);
CREATE INDEX idx_sesiones_usuario_id_usuario ON sesiones_usuario(id_usuario);
CREATE INDEX idx_sesiones_usuario_fecha_expiracion ON sesiones_usuario(fecha_expiracion);

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

-- ── V2__ajustes_requisitos_pfc.sql ──
-- ==============================================================================
-- MIGRACIÓN V2: AJUSTES PARA CUMPLIR REQUISITOS OFICIALES DE LA GUÍA PFC
-- ==============================================================================

-- 1. Función PL/pgSQL obligatoria para actualizar actualizado_en en cada UPDATE
CREATE OR REPLACE FUNCTION set_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Añadir columna actualizado_en a la tabla usuarios
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- 3. Trigger obligatorio para la tabla usuarios
DROP TRIGGER IF EXISTS trg_usuarios_actualizado_en ON usuarios;
CREATE TRIGGER trg_usuarios_actualizado_en
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 4. Añadir columna actualizado_en y trigger a la entidad portafolios
ALTER TABLE portafolios ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_portafolios_actualizado_en ON portafolios;
CREATE TRIGGER trg_portafolios_actualizado_en
    BEFORE UPDATE ON portafolios
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- ── V3__catalogo_ajustes.sql ──
-- ==============================================================================
-- MIGRACIÓN V3: AJUSTES EN CATÁLOGO DE SERVICIOS Y AUDITORÍA PFC
-- ==============================================================================

-- 1. Añadir columnas a la tabla servicios según Guía Módulo 3
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS tipo_item VARCHAR(20) NOT NULL DEFAULT 'SERVICIO';
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS estado_publicacion VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS cargo_revision_adicional DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS limite_revisiones_base INT DEFAULT 0;

-- 2. Añadir columna actualizado_en y trigger a la tabla servicios (Guía PFC)
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_servicios_actualizado_en ON servicios;
CREATE TRIGGER trg_servicios_actualizado_en
    BEFORE UPDATE ON servicios
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 3. Añadir columna actualizado_en y trigger a la tabla categorias
ALTER TABLE categorias ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_categorias_actualizado_en ON categorias;
CREATE TRIGGER trg_categorias_actualizado_en
    BEFORE UPDATE ON categorias
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 4. Añadir columna actualizado_en y trigger a la tabla subcategorias
ALTER TABLE subcategorias ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_subcategorias_actualizado_en ON subcategorias;
CREATE TRIGGER trg_subcategorias_actualizado_en
    BEFORE UPDATE ON subcategorias
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 5. Añadir columna actualizado_en y trigger a la tabla atributos_dinamicos
ALTER TABLE atributos_dinamicos ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_atributos_dinamicos_actualizado_en ON atributos_dinamicos;
CREATE TRIGGER trg_atributos_dinamicos_actualizado_en
    BEFORE UPDATE ON atributos_dinamicos
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 6. Añadir columna actualizado_en y trigger a la tabla servicio_atributos
ALTER TABLE servicio_atributos ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_servicio_atributos_actualizado_en ON servicio_atributos;
CREATE TRIGGER trg_servicio_atributos_actualizado_en
    BEFORE UPDATE ON servicio_atributos
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 7. Añadir columna actualizado_en y trigger a la tabla etiquetas
ALTER TABLE etiquetas ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_etiquetas_actualizado_en ON etiquetas;
CREATE TRIGGER trg_etiquetas_actualizado_en
    BEFORE UPDATE ON etiquetas
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 8. Añadir columna actualizado_en y trigger a la tabla servicio_etiquetas
ALTER TABLE servicio_etiquetas ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_servicio_etiquetas_actualizado_en ON servicio_etiquetas;
CREATE TRIGGER trg_servicio_etiquetas_actualizado_en
    BEFORE UPDATE ON servicio_etiquetas
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- ── V4__modulo_comunicacion.sql ──
-- =============================================================================
-- V4: Módulo 6 — Comunicación y Notificaciones
-- RF-15: Ampliar infracciones_mensaje con mensaje original y patrón detectado
-- RF-16: Tablas de briefing interactivo
-- =============================================================================

-- -----------------------------------------------------------------------------
-- RF-15: Columnas adicionales en infracciones_mensaje
-- -----------------------------------------------------------------------------
ALTER TABLE infracciones_mensaje
    ADD COLUMN IF NOT EXISTS mensaje_original   TEXT,
    ADD COLUMN IF NOT EXISTS patron_detectado   VARCHAR(50);

-- -----------------------------------------------------------------------------
-- RF-16: Briefing — Plantillas configuradas por el Creador
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS briefing_plantillas (
    id_briefing_plantilla BIGSERIAL PRIMARY KEY,
    id_perfil             BIGINT      NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    nombre_plantilla      VARCHAR(150) NOT NULL,
    fecha_creacion        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- RF-16: Briefing — Preguntas de una plantilla (máximo 10 por plantilla)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS briefing_preguntas (
    id_pregunta           BIGSERIAL PRIMARY KEY,
    id_briefing_plantilla BIGINT  NOT NULL REFERENCES briefing_plantillas(id_briefing_plantilla) ON DELETE CASCADE,
    texto_pregunta        TEXT NOT NULL,
    numero_orden          INT  NOT NULL,
    CONSTRAINT chk_orden_positivo CHECK (numero_orden > 0)
);

-- -----------------------------------------------------------------------------
-- RF-16: Briefing — Formulario enviado a un pedido
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS briefing_enviados (
    id_briefing_enviado   BIGSERIAL PRIMARY KEY,
    id_pedido             BIGINT    NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_briefing_plantilla BIGINT    NOT NULL REFERENCES briefing_plantillas(id_briefing_plantilla),
    fecha_envio           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completado            BOOLEAN   NOT NULL DEFAULT FALSE
);

-- -----------------------------------------------------------------------------
-- RF-16: Briefing — Respuestas del Cliente (inmutables tras envío)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS briefing_respuestas (
    id_respuesta         BIGSERIAL PRIMARY KEY,
    id_briefing_enviado  BIGINT    NOT NULL REFERENCES briefing_enviados(id_briefing_enviado) ON DELETE CASCADE,
    id_pregunta          BIGINT    NOT NULL REFERENCES briefing_preguntas(id_pregunta),
    texto_respuesta      TEXT      NOT NULL,
    fecha_respuesta      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_briefing_enviado, id_pregunta)
);

-- ── V5__modulo_social.sql ──
-- =============================================================================
-- V5: Módulo 7 — Social, Comunidad y Sorteos
-- RF-23: Sorteos configurables con selección automática de ganadores
-- RF-09: Reseñas y calificaciones de servicios (complemento)
-- RNF-13: Exportación de historial de transacciones en CSV
-- =============================================================================

-- -----------------------------------------------------------------------------
-- RF-23: Columna adicional en sorteos (requiere_seguidor)
-- La tabla 'sorteos' y 'participantes_sorteo' ya existen desde V1.
-- Solo se agrega el campo que faltaba para el requisito de seguidor.
-- -----------------------------------------------------------------------------
ALTER TABLE sorteos
    ADD COLUMN IF NOT EXISTS requiere_seguidor BOOLEAN NOT NULL DEFAULT FALSE;
