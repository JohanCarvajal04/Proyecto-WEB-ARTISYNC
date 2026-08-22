-- ==============================================================================
-- ARTISYNC — ESQUEMA CONSOLIDADO (PostgreSQL)
-- ==============================================================================
--
-- PROPUESTA. No se ejecuta: spring.flyway.locations apunta a classpath:db/migration.
-- Ver README.md de esta carpeta para el mapa de equivalencias y cómo probarla.
--
-- Este archivo reemplaza a V1..V13 de db/migration/ creando cada tabla YA en su
-- forma final, sin los ALTER que hoy la corrigen a posteriori. Los cuatro casos
-- de "crear y deshacer" del historial actual desaparecen por construcción:
--
--   * portafolios.color_plantilla (V1) que V6 borraba  -> nace opciones_personalizacion
--   * sesiones_usuario.token_jwt   (V1) que V8 borraba  -> nace sólo con jti
--   * permiso CONFIGURACION_GESTIONAR (V10) que V11 borraba -> nunca se crea
--   * pais.estado, categorias.id_flujo, servicios.*, certificados_ia.* ... -> inline
--
-- Además rescata el DDL que hoy vive dentro de la migración REPETIBLE
-- R__procedimientos.sql (pedidos.codigo_pedido, seq_codigo_pedido y 4 índices,
-- procedentes de db/procs/V8__estructuras_para_procedimientos.sql): una
-- repetible se reaplica cada vez que cambia su checksum y no debe llevar DDL.
--
-- Orden del archivo: funciones de infraestructura -> tablas por módulo ->
-- rutinas de verificación -> privilegios.
-- ==============================================================================


-- ==============================================================================
-- 0. FUNCIONES DE INFRAESTRUCTURA
-- ==============================================================================
-- Deben existir antes que los triggers que las referencian. No van a db/procs/:
-- ese catálogo es el de rutinas invocadas desde Java (ADR-006, CATALOGO-SP.md);
-- estas dos son restricciones de integridad de las propias tablas.

-- Origen: V2. Alimenta la columna actualizado_en de las 9 tablas que la llevan.
CREATE OR REPLACE FUNCTION set_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Origen: V12. Hace de auditoria_eventos una tabla de sólo inserción (REQ-NF-013).
CREATE OR REPLACE FUNCTION fn_bloquear_modificacion_auditoria()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'AUDITORIA_INMUTABLE: la bitacora de auditoria es de solo insercion (REQ-NF-013). Operacion % rechazada sobre %',
        TG_OP, TG_TABLE_NAME
        USING ERRCODE = '42501';
END;
$$;


-- ==============================================================================
-- MÓDULO 1: SEGURIDAD Y CONTROL DE ACCESO
-- ==============================================================================

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

-- estado: soft-delete de países (origen V13). Un país referenciado por usuarios
-- no se puede borrar físicamente sin romper la FK, así que se desactiva.
CREATE TABLE pais (
    id_pais BIGSERIAL PRIMARY KEY,
    nombre_pais VARCHAR(100) NOT NULL UNIQUE,
    estado BOOLEAN NOT NULL DEFAULT TRUE
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
    fecha_nacimiento DATE,
    actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_usuarios_actualizado_en
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

CREATE TABLE usuario_roles (
    id_usuario_rol BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_rol BIGINT NOT NULL REFERENCES roles(id_rol) ON DELETE CASCADE
);

-- §2.5 / OBS-AUTO-06 (origen V8): se guarda ÚNICAMENTE el jti, nunca el JWT
-- completo. Guardar el token íntegro convertía cualquier lectura de esta tabla
-- (SQLi, backup filtrado, dump) en la toma de control de todas las sesiones.
--
-- El UNIQUE de la columna crea su propio índice btree: no hace falta un
-- CREATE INDEX aparte. Esto además corrige el defecto actual, donde db/schema.sql
-- ya declaraba el UNIQUE inline y V8 volvía a añadir uq_sesiones_usuario_jti,
-- dejando DOS índices únicos sobre la misma columna en toda base creada por Docker.
CREATE TABLE sesiones_usuario (
    id_sesion BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    jti VARCHAR(36) NOT NULL UNIQUE,
    direccion_ip VARCHAR(45),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL
);

-- Respaldan findByUsuarioIdUsuario / deleteByUsuarioIdUsuario (se ejecutan en
-- cada revocación administrativa) y la purga por expiración.
CREATE INDEX idx_sesiones_usuario_id_usuario
    ON sesiones_usuario (id_usuario);
CREATE INDEX idx_sesiones_usuario_fecha_expiracion
    ON sesiones_usuario (fecha_expiracion);

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


-- ==============================================================================
-- MÓDULO 2: PERFILES, VERIFICACIÓN Y PORTAFOLIO
-- ==============================================================================

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

-- Origen de las columnas de dictamen/decisión: V7 (REQ-F-006, REQ-F-007).
-- La IA asiste al moderador; nunca decide. Por eso el veredicto de la IA
-- (veredicto_ia, razon_ia, fecha_dictamen_ia) está separado de la decisión
-- humana (id_moderador, fecha_decision, nota_moderador) y el estado final sólo
-- lo escribe sp_registrar_decision_verificacion.
CREATE TABLE certificados_ia (
    id_certificado BIGSERIAL PRIMARY KEY,
    id_perfil BIGINT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    id_estado_verificacion BIGINT NOT NULL REFERENCES estados_verificacion(id_estado_verificacion),
    url_documento_s3 VARCHAR(255) NOT NULL,
    puntaje_confianza_ia DECIMAL(5,2),
    fecha_analisis TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tipo_documento VARCHAR(20) NOT NULL DEFAULT 'IDENTIDAD',
    hash_documento VARCHAR(64),
    veredicto_ia VARCHAR(30),
    razon_ia TEXT,
    datos_extraidos_ia TEXT,
    fecha_dictamen_ia TIMESTAMP,
    id_moderador BIGINT REFERENCES usuarios(id_usuario),
    fecha_decision TIMESTAMP,
    nota_moderador TEXT,
    documento_eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_certificados_ia_tipo_documento
        CHECK (tipo_documento IN ('IDENTIDAD', 'CERTIFICADO'))
);

CREATE INDEX idx_certificados_ia_hash ON certificados_ia(hash_documento);
CREATE INDEX idx_certificados_ia_estado ON certificados_ia(id_estado_verificacion);

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

-- opciones_personalizacion (origen V6) sustituye a la columna color_plantilla
-- VARCHAR(20) del esquema original: el portafolio ya no configura un solo color
-- sino la paleta completa (primary, secondary, bg, text, surface).
CREATE TABLE portafolios (
    id_portafolio BIGSERIAL PRIMARY KEY,
    id_perfil BIGINT UNIQUE NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_visitas_acumuladas INT DEFAULT 0,
    es_publico BOOLEAN DEFAULT TRUE,
    opciones_personalizacion JSONB,
    actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_portafolios_actualizado_en
    BEFORE UPDATE ON portafolios
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

CREATE TABLE portafolio_items (
    id_item_portafolio BIGSERIAL PRIMARY KEY,
    id_portafolio BIGINT NOT NULL REFERENCES portafolios(id_portafolio) ON DELETE CASCADE,
    titulo_obra VARCHAR(150) NOT NULL,
    descripcion_obra TEXT,
    url_archivo_multimedia VARCHAR(255) NOT NULL,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- ==============================================================================
-- MÓDULO 3: CATÁLOGO DINÁMICO DE SERVICIOS
-- ==============================================================================

-- id_flujo (origen V9, RF-19): las etapas de un pedido se configuran según la
-- categoría del servicio. Nullable a propósito — una categoría sin flujo cae a
-- un flujo de respaldo en el servicio en vez de impedir crear el pedido.
-- La FK se añade en el Módulo 4, donde nace flujos_trabajo.
CREATE TABLE categorias (
    id_categoria BIGSERIAL PRIMARY KEY,
    nombre_categoria VARCHAR(100) NOT NULL UNIQUE,
    estado_activa BOOLEAN DEFAULT TRUE,
    id_flujo BIGINT,
    actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_categorias_actualizado_en
    BEFORE UPDATE ON categorias
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

CREATE TABLE subcategorias (
    id_subcategoria BIGSERIAL PRIMARY KEY,
    id_categoria BIGINT NOT NULL REFERENCES categorias(id_categoria) ON DELETE CASCADE,
    nombre_subcategoria VARCHAR(100) NOT NULL,
    actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_subcategorias_actualizado_en
    BEFORE UPDATE ON subcategorias
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- tipo_item / estado_publicacion / cargo_revision_adicional /
-- limite_revisiones_base: origen V3 (Guía Módulo 3).
CREATE TABLE servicios (
    id_servicio BIGSERIAL PRIMARY KEY,
    id_perfil BIGINT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    id_subcategoria BIGINT NOT NULL REFERENCES subcategorias(id_subcategoria),
    titulo_servicio VARCHAR(150) NOT NULL,
    descripcion_detallada TEXT NOT NULL,
    precio_base DECIMAL(10,2) NOT NULL,
    url_miniatura VARCHAR(255),
    tipo_item VARCHAR(20) NOT NULL DEFAULT 'SERVICIO',
    estado_publicacion VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    cargo_revision_adicional DECIMAL(10,2) DEFAULT 0.00,
    limite_revisiones_base INT DEFAULT 0,
    actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_servicios_actualizado_en
    BEFORE UPDATE ON servicios
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- Rescatados de R__procedimientos.sql: fn_reporte_comisiones_creador y
-- fn_catalogo_filtrado navegan servicios por perfil y por subcategoría+estado.
CREATE INDEX idx_servicios_perfil
    ON servicios (id_perfil);
CREATE INDEX idx_servicios_subcategoria_estado
    ON servicios (id_subcategoria, estado_publicacion);

CREATE TABLE atributos_dinamicos (
    id_atributo BIGSERIAL PRIMARY KEY,
    nombre_atributo VARCHAR(100) NOT NULL UNIQUE,
    tipo_dato VARCHAR(50) NOT NULL,
    actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_atributos_dinamicos_actualizado_en
    BEFORE UPDATE ON atributos_dinamicos
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

CREATE TABLE servicio_atributos (
    id_servicio_atributo BIGSERIAL PRIMARY KEY,
    id_servicio BIGINT NOT NULL REFERENCES servicios(id_servicio) ON DELETE CASCADE,
    id_atributo BIGINT NOT NULL REFERENCES atributos_dinamicos(id_atributo) ON DELETE CASCADE,
    valor_asignado VARCHAR(255) NOT NULL,
    actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_servicio_atributos_actualizado_en
    BEFORE UPDATE ON servicio_atributos
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

CREATE TABLE etiquetas (
    id_etiqueta BIGSERIAL PRIMARY KEY,
    nombre_etiqueta VARCHAR(50) NOT NULL UNIQUE,
    actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_etiquetas_actualizado_en
    BEFORE UPDATE ON etiquetas
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

CREATE TABLE servicio_etiquetas (
    id_servicio_etiqueta BIGSERIAL PRIMARY KEY,
    id_servicio BIGINT NOT NULL REFERENCES servicios(id_servicio) ON DELETE CASCADE,
    id_etiqueta BIGINT NOT NULL REFERENCES etiquetas(id_etiqueta) ON DELETE CASCADE,
    actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_servicio_etiquetas_actualizado_en
    BEFORE UPDATE ON servicio_etiquetas
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();


-- ==============================================================================
-- MÓDULO 4: MOTOR DE FLUJOS DE TRABAJO Y PEDIDOS
-- ==============================================================================

CREATE TABLE flujos_trabajo (
    id_flujo BIGSERIAL PRIMARY KEY,
    nombre_flujo VARCHAR(100) NOT NULL,
    descripcion_flujo TEXT
);

-- Cierra la relación categoría -> flujo declarada en el Módulo 3. Va aquí y no
-- inline en categorias porque flujos_trabajo se crea después; mantener el orden
-- de módulos importa para la documentación del PFC.
ALTER TABLE categorias
    ADD CONSTRAINT fk_categorias_flujo
    FOREIGN KEY (id_flujo) REFERENCES flujos_trabajo(id_flujo);

-- Acelera la resolución servicio -> subcategoría -> categoría -> flujo al crear
-- un pedido.
CREATE INDEX idx_categorias_id_flujo ON categorias (id_flujo);

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

-- codigo_pedido (REQ-F-018) rescatado de R__procedimientos.sql. Nullable: se
-- asigna de forma perezosa la primera vez que se consulta el pedido. El UNIQUE
-- garantiza la unicidad incluso si la secuencia se reinicia.
CREATE TABLE pedidos (
    id_pedido BIGSERIAL PRIMARY KEY,
    id_usuario_cliente BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    id_servicio BIGINT NOT NULL REFERENCES servicios(id_servicio),
    id_flujo BIGINT NOT NULL REFERENCES flujos_trabajo(id_flujo),
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_entrega_estimada TIMESTAMP,
    precio_pactado DECIMAL(10,2) NOT NULL,
    codigo_pedido VARCHAR(20),
    CONSTRAINT uq_pedidos_codigo_pedido UNIQUE (codigo_pedido)
);

COMMENT ON COLUMN pedidos.codigo_pedido
    IS 'REQ-F-018 - Codigo publico ART-AAAA-NNNNNN. Lo asigna fn_generar_codigo_pedido.';

-- Correlativo del código público. Independiente de id_pedido para que el código
-- no revele el volumen real de pedidos de la plataforma.
CREATE SEQUENCE seq_codigo_pedido
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 1;

COMMENT ON SEQUENCE seq_codigo_pedido
    IS 'Correlativo de fn_generar_codigo_pedido. Ver db/procs/fn_generar_codigo_pedido.sql.';

-- fn_cerrar_pedidos_vencidos filtra por fecha de entrega (rescatado de R__).
CREATE INDEX idx_pedidos_fecha_entrega_estimada
    ON pedidos (fecha_entrega_estimada)
    WHERE fecha_entrega_estimada IS NOT NULL;

CREATE TABLE historial_estados_pedido (
    id_historial_estado BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_etapa BIGINT NOT NULL REFERENCES etapas_flujo(id_etapa),
    fecha_transicion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    observacion TEXT
);

-- Rescatado de R__: fn_cerrar_pedidos_vencidos recorre el historial por pedido.
CREATE INDEX idx_historial_pedido_fecha
    ON historial_estados_pedido (id_pedido, fecha_transicion DESC);

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


-- ==============================================================================
-- MÓDULO 5: LEGAL, ENTREGABLES Y FINANZAS (ESCROW)
-- ==============================================================================

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


-- ==============================================================================
-- MÓDULO 6: COMUNICACIÓN Y NOTIFICACIONES
-- ==============================================================================

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

-- mensaje_original / patron_detectado: origen V4 (RF-15).
CREATE TABLE infracciones_mensaje (
    id_infraccion BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    fecha_infraccion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    mensaje_original TEXT,
    patron_detectado VARCHAR(50)
);

-- ------------------------------------------------------------------------------
-- RF-16: Briefing interactivo (origen V4)
-- ------------------------------------------------------------------------------
CREATE TABLE briefing_plantillas (
    id_briefing_plantilla BIGSERIAL PRIMARY KEY,
    id_perfil             BIGINT       NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    nombre_plantilla      VARCHAR(150) NOT NULL,
    fecha_creacion        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE briefing_preguntas (
    id_pregunta           BIGSERIAL PRIMARY KEY,
    id_briefing_plantilla BIGINT NOT NULL REFERENCES briefing_plantillas(id_briefing_plantilla) ON DELETE CASCADE,
    texto_pregunta        TEXT   NOT NULL,
    numero_orden          INT    NOT NULL,
    CONSTRAINT chk_orden_positivo CHECK (numero_orden > 0)
);

CREATE TABLE briefing_enviados (
    id_briefing_enviado   BIGSERIAL PRIMARY KEY,
    id_pedido             BIGINT    NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_briefing_plantilla BIGINT    NOT NULL REFERENCES briefing_plantillas(id_briefing_plantilla),
    fecha_envio           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completado            BOOLEAN   NOT NULL DEFAULT FALSE
);

-- Respuestas del Cliente, inmutables tras el envío.
CREATE TABLE briefing_respuestas (
    id_respuesta        BIGSERIAL PRIMARY KEY,
    id_briefing_enviado BIGINT    NOT NULL REFERENCES briefing_enviados(id_briefing_enviado) ON DELETE CASCADE,
    id_pregunta         BIGINT    NOT NULL REFERENCES briefing_preguntas(id_pregunta),
    texto_respuesta     TEXT      NOT NULL,
    fecha_respuesta     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_briefing_enviado, id_pregunta)
);


-- ==============================================================================
-- MÓDULO 7: SOCIAL, COMUNIDAD Y SORTEOS
-- ==============================================================================

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

-- requiere_seguidor: origen V5 (RF-23).
CREATE TABLE sorteos (
    id_sorteo BIGSERIAL PRIMARY KEY,
    id_perfil_creador BIGINT NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    titulo_sorteo VARCHAR(150) NOT NULL,
    descripcion_premios TEXT NOT NULL,
    cantidad_ganadores INT NOT NULL DEFAULT 1,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_cierre TIMESTAMP NOT NULL,
    estado_sorteo VARCHAR(50) DEFAULT 'Activo',
    requiere_seguidor BOOLEAN NOT NULL DEFAULT FALSE
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
-- MÓDULO 8: AUDITORÍA TRANSVERSAL (REQ-NF-013) — origen V12
-- ==============================================================================
--
-- auditoria_eventos NO sustituye a historial_estados_pedido: coexisten. El
-- historial es de dominio (lo consume el cliente en su pantalla de seguimiento);
-- esta tabla es forense — inmutable, sobrevive al borrado del pedido, incluye
-- quién y desde qué IP, y registra los intentos FALLIDOS que el historial jamás
-- guarda. El vínculo es entidad_afectada='pedidos' + id_entidad_afectada.
--
-- fecha_evento es TIMESTAMP (no TIMESTAMPTZ) por coherencia con el resto de
-- columnas fecha_* y con el mapeo por defecto de LocalDateTime que
-- spring.jpa.hibernate.ddl-auto=validate comprueba estrictamente.
--
-- id_usuario_actor NO lleva FK a usuarios a propósito: un ON DELETE SET NULL
-- dispararía un UPDATE que el trigger de inmutabilidad bloquearía, haciendo
-- imposible borrar un usuario. correo_actor va desnormalizado y NOT NULL porque
-- siempre hay un valor: el correo tecleado en un login fallido (aunque no exista
-- como usuario) o 'sistema'/'sistema:paypal' para tareas y webhooks.
CREATE TABLE auditoria_eventos (
    id_evento_auditoria BIGSERIAL PRIMARY KEY,
    fecha_evento        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_actor    BIGINT,
    correo_actor        VARCHAR(150) NOT NULL,
    modulo_auditoria    VARCHAR(20)  NOT NULL,
    accion_auditoria    VARCHAR(60)  NOT NULL,
    resultado_evento    VARCHAR(10)  NOT NULL,
    entidad_afectada    VARCHAR(60),
    id_entidad_afectada BIGINT,
    detalle_cambio      JSONB,
    mensaje_error       VARCHAR(500),
    direccion_ip        VARCHAR(45),
    agente_usuario      VARCHAR(255),
    metodo_http         VARCHAR(10),
    ruta_solicitud      VARCHAR(255),
    duracion_ms         INTEGER,
    CONSTRAINT ck_auditoria_resultado
        CHECK (resultado_evento IN ('EXITO', 'FALLIDO', 'DENEGADO')),
    CONSTRAINT ck_auditoria_modulo
        CHECK (modulo_auditoria IN ('SEGURIDAD', 'SISTEMA', 'PORTAFOLIO', 'CATALOGO',
                                     'PEDIDOS', 'FINANZAS', 'COMUNICACION', 'SOCIAL'))
);

COMMENT ON COLUMN auditoria_eventos.detalle_cambio IS
    'JSONB sin indice GIN a proposito: hoy ninguna consulta filtra dentro del '
    'JSON y el coste de mantenerlo en cada INSERT no se justifica. Anadir un '
    'indice GIN aqui si en el futuro se necesita buscar por una clave interna.';

-- Índices con la fecha al final para resolver WHERE y ORDER BY fecha_evento DESC
-- sin un sort adicional.

-- Carga inicial de la pantalla, sin filtros.
CREATE INDEX idx_auditoria_fecha
    ON auditoria_eventos (fecha_evento DESC);

-- "Todo lo que hizo el usuario X" ordenado por fecha.
CREATE INDEX idx_auditoria_actor_fecha
    ON auditoria_eventos (id_usuario_actor, fecha_evento DESC);

-- Filtro por módulo (8 valores, baja selectividad, pero evita el sort).
CREATE INDEX idx_auditoria_modulo_fecha
    ON auditoria_eventos (modulo_auditoria, fecha_evento DESC);

-- Filtro por acción concreta (decenas de valores, buena selectividad).
CREATE INDEX idx_auditoria_accion_fecha
    ON auditoria_eventos (accion_auditoria, fecha_evento DESC);

-- Parcial a propósito: ~99% de las filas serán EXITO, así que un btree completo
-- sobre resultado_evento nunca lo elegiría el planner. El filtro que de verdad
-- se usa es "enséñame sólo lo que falló o se denegó".
CREATE INDEX idx_auditoria_no_exitosos
    ON auditoria_eventos (fecha_evento DESC)
    WHERE resultado_evento <> 'EXITO';

-- Habilita "historial de auditoría de ESTA entidad". Parcial porque muchas
-- acciones (login) no tienen entidad asociada.
CREATE INDEX idx_auditoria_entidad
    ON auditoria_eventos (entidad_afectada, id_entidad_afectada, fecha_evento DESC)
    WHERE entidad_afectada IS NOT NULL;

CREATE TRIGGER trg_auditoria_eventos_inmutable
    BEFORE UPDATE OR DELETE ON auditoria_eventos
    FOR EACH ROW
    EXECUTE FUNCTION fn_bloquear_modificacion_auditoria();

-- PostgreSQL no admite BEFORE TRUNCATE ... FOR EACH ROW: tiene que ser
-- FOR EACH STATEMENT.
CREATE TRIGGER trg_auditoria_eventos_no_truncate
    BEFORE TRUNCATE ON auditoria_eventos
    FOR EACH STATEMENT
    EXECUTE FUNCTION fn_bloquear_modificacion_auditoria();


-- ==============================================================================
-- RUTINAS DE VERIFICACIÓN ASISTIDA (origen V7)
-- ==============================================================================
--
-- Estas dos rutinas siguen aquí y no en db/procs/ + R__procedimientos.sql para
-- no alterar scripts/sync-procs.sh --check ni docs/basedatos/CATALOGO-SP.md en
-- este cambio. Moverlas es una mejora independiente y opcional (ver README).

-- Cola de revisión. tipo_acceso=SP en la matriz de trazabilidad (ADR-006).
CREATE OR REPLACE FUNCTION fn_listar_cola_verificacion(
    p_estado  VARCHAR,
    p_limite  INT,
    p_offset  INT
)
RETURNS TABLE (
    id_certificado        BIGINT,
    id_perfil             BIGINT,
    nombre_creador        VARCHAR,
    tipo_documento        VARCHAR,
    nombre_estado         VARCHAR,
    veredicto_ia          VARCHAR,
    puntaje_confianza_ia  DECIMAL,
    fecha_analisis        TIMESTAMP
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id_certificado,
        c.id_perfil,
        (u.nombres || ' ' || u.apellidos)::VARCHAR,
        c.tipo_documento,
        ev.nombre_estado,
        c.veredicto_ia,
        c.puntaje_confianza_ia,
        c.fecha_analisis
    FROM certificados_ia c
    JOIN perfiles_creadores pc ON pc.id_perfil = c.id_perfil
    JOIN usuarios u ON u.id_usuario = pc.id_usuario
    JOIN estados_verificacion ev ON ev.id_estado_verificacion = c.id_estado_verificacion
    WHERE p_estado IS NULL OR ev.nombre_estado = p_estado
    ORDER BY c.fecha_analisis ASC
    LIMIT p_limite OFFSET p_offset;
END;
$$;

-- Único punto de escritura de id_estado_verificacion. Valida existencia de
-- certificado, estado y moderador antes de escribir (validación cruzada,
-- ADR-006). Exige que el certificado esté en PENDIENTE y sólo marca el documento
-- como eliminado cuando el nuevo estado es terminal (APROBADO/RECHAZADO);
-- REQUIERE_ACLARACION deja el documento intacto porque el flujo vuelve a la cola.
-- El archivo físico lo borra la capa Java tras invocar este procedimiento.
CREATE OR REPLACE PROCEDURE sp_registrar_decision_verificacion(
    p_id_certificado  BIGINT,
    p_id_estado       BIGINT,
    p_id_moderador    BIGINT,
    p_nota            TEXT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nombre_estado_actual VARCHAR;
    v_nombre_estado_nuevo  VARCHAR;
BEGIN
    SELECT ev.nombre_estado INTO v_nombre_estado_actual
    FROM certificados_ia c
    JOIN estados_verificacion ev ON ev.id_estado_verificacion = c.id_estado_verificacion
    WHERE c.id_certificado = p_id_certificado;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Certificado de verificación % no existe', p_id_certificado;
    END IF;

    IF v_nombre_estado_actual <> 'PENDIENTE' THEN
        RAISE EXCEPTION 'El certificado % no está en estado PENDIENTE (estado actual: %); no se puede registrar una decisión sobre él nuevamente.',
            p_id_certificado, v_nombre_estado_actual;
    END IF;

    SELECT nombre_estado INTO v_nombre_estado_nuevo
    FROM estados_verificacion
    WHERE id_estado_verificacion = p_id_estado;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Estado de verificación % no existe', p_id_estado;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = p_id_moderador) THEN
        RAISE EXCEPTION 'Moderador % no existe', p_id_moderador;
    END IF;

    UPDATE certificados_ia
    SET id_estado_verificacion = p_id_estado,
        id_moderador           = p_id_moderador,
        fecha_decision          = CURRENT_TIMESTAMP,
        nota_moderador           = p_nota,
        documento_eliminado      = (v_nombre_estado_nuevo IN ('APROBADO', 'RECHAZADO'))
    WHERE id_certificado = p_id_certificado;
END;
$$;


-- ==============================================================================
-- PRIVILEGIOS DE LA CUENTA DE APLICACIÓN (origen V7 y V12)
-- ==============================================================================
--
-- artisync_app sólo la crea db/seed_privilegios.sh en el primer arranque de un
-- volumen vacío. En una BD restaurada de un dump, en CI o en una instancia
-- gestionada ese rol aún no existe, y un GRANT a un rol inexistente abortaría
-- TODA la migración: de ahí la guarda IF EXISTS.
--
-- El REVOKE sobre auditoria_eventos no es decorativo:
--   (a) seed_privilegios.sh ejecuta ALTER DEFAULT PRIVILEGES ... GRANT
--       SELECT, INSERT, UPDATE, DELETE ON TABLES, y ese default alcanza también
--       a las tablas que Flyway cree DESPUÉS. Sin el REVOKE, artisync_app
--       nacería con UPDATE y DELETE sobre la bitácora y la única defensa sería
--       el trigger.
--   (b) En el despliegue real artisync_app se crea a mano y seed_privilegios.sh
--       no corre: esta migración es lo único que se ejecuta en TODOS los entornos.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_app') THEN
        GRANT EXECUTE ON FUNCTION fn_listar_cola_verificacion(VARCHAR, INT, INT) TO artisync_app;
        GRANT EXECUTE ON PROCEDURE sp_registrar_decision_verificacion(BIGINT, BIGINT, BIGINT, TEXT) TO artisync_app;

        REVOKE ALL ON auditoria_eventos FROM artisync_app;
        GRANT SELECT, INSERT ON auditoria_eventos TO artisync_app;
        GRANT USAGE, SELECT ON SEQUENCE auditoria_eventos_id_evento_auditoria_seq TO artisync_app;
    END IF;
END
$$;
