--
-- PostgreSQL database dump
--

\restrict opQuSbFsbY5COQ33Dq4lhdMGnEsUJivMOE1NNvzSaIgoLhq75hgnUYvnDBhRYVI

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

-- Started on 2026-08-18 23:30:12

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 5 (class 2615 OID 2200)
-- Name: public; Type: SCHEMA; Schema: -; Owner: azure_pg_admin
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO azure_pg_admin;

--
-- TOC entry 352 (class 1255 OID 28927)
-- Name: fn_listar_cola_verificacion(character varying, integer, integer); Type: FUNCTION; Schema: public; Owner: adminuteq
--

CREATE FUNCTION public.fn_listar_cola_verificacion(p_estado character varying, p_limite integer, p_offset integer) RETURNS TABLE(id_certificado bigint, id_perfil bigint, nombre_creador character varying, tipo_documento character varying, nombre_estado character varying, veredicto_ia character varying, puntaje_confianza_ia numeric, fecha_analisis timestamp without time zone)
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


ALTER FUNCTION public.fn_listar_cola_verificacion(p_estado character varying, p_limite integer, p_offset integer) OWNER TO adminuteq;

--
-- TOC entry 325 (class 1255 OID 27847)
-- Name: set_actualizado_en(); Type: FUNCTION; Schema: public; Owner: adminuteq
--

CREATE FUNCTION public.set_actualizado_en() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.actualizado_en = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.set_actualizado_en() OWNER TO adminuteq;

--
-- TOC entry 353 (class 1255 OID 28928)
-- Name: sp_registrar_decision_verificacion(bigint, bigint, bigint, text); Type: PROCEDURE; Schema: public; Owner: adminuteq
--

CREATE PROCEDURE public.sp_registrar_decision_verificacion(IN p_id_certificado bigint, IN p_id_estado bigint, IN p_id_moderador bigint, IN p_nota text)
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


ALTER PROCEDURE public.sp_registrar_decision_verificacion(IN p_id_certificado bigint, IN p_id_estado bigint, IN p_id_moderador bigint, IN p_nota text) OWNER TO adminuteq;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 219 (class 1259 OID 27848)
-- Name: atributos_dinamicos; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.atributos_dinamicos (
    id_atributo bigint NOT NULL,
    nombre_atributo character varying(100) NOT NULL,
    tipo_dato character varying(50) NOT NULL,
    actualizado_en timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.atributos_dinamicos OWNER TO adminuteq;

--
-- TOC entry 220 (class 1259 OID 27855)
-- Name: atributos_dinamicos_id_atributo_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.atributos_dinamicos_id_atributo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.atributos_dinamicos_id_atributo_seq OWNER TO adminuteq;

--
-- TOC entry 4912 (class 0 OID 0)
-- Dependencies: 220
-- Name: atributos_dinamicos_id_atributo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.atributos_dinamicos_id_atributo_seq OWNED BY public.atributos_dinamicos.id_atributo;


--
-- TOC entry 221 (class 1259 OID 27856)
-- Name: autenticacion_dos_factores; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.autenticacion_dos_factores (
    id_2fa bigint NOT NULL,
    id_usuario bigint NOT NULL,
    llave_secreta character varying(255) NOT NULL,
    esta_habilitado boolean DEFAULT false
);


ALTER TABLE public.autenticacion_dos_factores OWNER TO adminuteq;

--
-- TOC entry 222 (class 1259 OID 27863)
-- Name: autenticacion_dos_factores_id_2fa_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.autenticacion_dos_factores_id_2fa_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.autenticacion_dos_factores_id_2fa_seq OWNER TO adminuteq;

--
-- TOC entry 4915 (class 0 OID 0)
-- Dependencies: 222
-- Name: autenticacion_dos_factores_id_2fa_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.autenticacion_dos_factores_id_2fa_seq OWNED BY public.autenticacion_dos_factores.id_2fa;


--
-- TOC entry 223 (class 1259 OID 27864)
-- Name: briefing_enviados; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.briefing_enviados (
    id_briefing_enviado bigint NOT NULL,
    id_pedido bigint NOT NULL,
    id_briefing_plantilla bigint NOT NULL,
    fecha_envio timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completado boolean DEFAULT false NOT NULL
);


ALTER TABLE public.briefing_enviados OWNER TO adminuteq;

--
-- TOC entry 224 (class 1259 OID 27874)
-- Name: briefing_enviados_id_briefing_enviado_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.briefing_enviados_id_briefing_enviado_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.briefing_enviados_id_briefing_enviado_seq OWNER TO adminuteq;

--
-- TOC entry 4918 (class 0 OID 0)
-- Dependencies: 224
-- Name: briefing_enviados_id_briefing_enviado_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.briefing_enviados_id_briefing_enviado_seq OWNED BY public.briefing_enviados.id_briefing_enviado;


--
-- TOC entry 225 (class 1259 OID 27875)
-- Name: briefing_plantillas; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.briefing_plantillas (
    id_briefing_plantilla bigint NOT NULL,
    id_perfil bigint NOT NULL,
    nombre_plantilla character varying(150) NOT NULL,
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.briefing_plantillas OWNER TO adminuteq;

--
-- TOC entry 226 (class 1259 OID 27883)
-- Name: briefing_plantillas_id_briefing_plantilla_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.briefing_plantillas_id_briefing_plantilla_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.briefing_plantillas_id_briefing_plantilla_seq OWNER TO adminuteq;

--
-- TOC entry 4921 (class 0 OID 0)
-- Dependencies: 226
-- Name: briefing_plantillas_id_briefing_plantilla_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.briefing_plantillas_id_briefing_plantilla_seq OWNED BY public.briefing_plantillas.id_briefing_plantilla;


--
-- TOC entry 227 (class 1259 OID 27884)
-- Name: briefing_preguntas; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.briefing_preguntas (
    id_pregunta bigint NOT NULL,
    id_briefing_plantilla bigint NOT NULL,
    texto_pregunta text NOT NULL,
    numero_orden integer NOT NULL,
    CONSTRAINT chk_orden_positivo CHECK ((numero_orden > 0))
);


ALTER TABLE public.briefing_preguntas OWNER TO adminuteq;

--
-- TOC entry 228 (class 1259 OID 27894)
-- Name: briefing_preguntas_id_pregunta_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.briefing_preguntas_id_pregunta_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.briefing_preguntas_id_pregunta_seq OWNER TO adminuteq;

--
-- TOC entry 4924 (class 0 OID 0)
-- Dependencies: 228
-- Name: briefing_preguntas_id_pregunta_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.briefing_preguntas_id_pregunta_seq OWNED BY public.briefing_preguntas.id_pregunta;


--
-- TOC entry 229 (class 1259 OID 27895)
-- Name: briefing_respuestas; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.briefing_respuestas (
    id_respuesta bigint NOT NULL,
    id_briefing_enviado bigint NOT NULL,
    id_pregunta bigint NOT NULL,
    texto_respuesta text NOT NULL,
    fecha_respuesta timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.briefing_respuestas OWNER TO adminuteq;

--
-- TOC entry 230 (class 1259 OID 27906)
-- Name: briefing_respuestas_id_respuesta_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.briefing_respuestas_id_respuesta_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.briefing_respuestas_id_respuesta_seq OWNER TO adminuteq;

--
-- TOC entry 4927 (class 0 OID 0)
-- Dependencies: 230
-- Name: briefing_respuestas_id_respuesta_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.briefing_respuestas_id_respuesta_seq OWNED BY public.briefing_respuestas.id_respuesta;


--
-- TOC entry 231 (class 1259 OID 27907)
-- Name: categorias; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.categorias (
    id_categoria bigint NOT NULL,
    nombre_categoria character varying(100) NOT NULL,
    estado_activa boolean DEFAULT true,
    actualizado_en timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    id_flujo bigint
);


ALTER TABLE public.categorias OWNER TO adminuteq;

--
-- TOC entry 232 (class 1259 OID 27914)
-- Name: categorias_id_categoria_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.categorias_id_categoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.categorias_id_categoria_seq OWNER TO adminuteq;

--
-- TOC entry 4930 (class 0 OID 0)
-- Dependencies: 232
-- Name: categorias_id_categoria_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.categorias_id_categoria_seq OWNED BY public.categorias.id_categoria;


--
-- TOC entry 233 (class 1259 OID 27915)
-- Name: certificados_ia; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.certificados_ia (
    id_certificado bigint NOT NULL,
    id_perfil bigint NOT NULL,
    id_estado_verificacion bigint NOT NULL,
    url_documento_s3 character varying(255) NOT NULL,
    puntaje_confianza_ia numeric(5,2),
    fecha_analisis timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    tipo_documento character varying(20) DEFAULT 'IDENTIDAD'::character varying NOT NULL,
    hash_documento character varying(64),
    veredicto_ia character varying(30),
    razon_ia text,
    datos_extraidos_ia text,
    fecha_dictamen_ia timestamp without time zone,
    id_moderador bigint,
    fecha_decision timestamp without time zone,
    nota_moderador text,
    documento_eliminado boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_certificados_ia_tipo_documento CHECK (((tipo_documento)::text = ANY ((ARRAY['IDENTIDAD'::character varying, 'CERTIFICADO'::character varying])::text[])))
);


ALTER TABLE public.certificados_ia OWNER TO adminuteq;

--
-- TOC entry 234 (class 1259 OID 27923)
-- Name: certificados_ia_id_certificado_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.certificados_ia_id_certificado_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.certificados_ia_id_certificado_seq OWNER TO adminuteq;

--
-- TOC entry 4933 (class 0 OID 0)
-- Dependencies: 234
-- Name: certificados_ia_id_certificado_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.certificados_ia_id_certificado_seq OWNED BY public.certificados_ia.id_certificado;


--
-- TOC entry 235 (class 1259 OID 27924)
-- Name: codigos_respaldo_2fa; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.codigos_respaldo_2fa (
    id_codigo bigint NOT NULL,
    id_usuario bigint NOT NULL,
    codigo_hash character varying(255) NOT NULL,
    usado boolean DEFAULT false NOT NULL
);


ALTER TABLE public.codigos_respaldo_2fa OWNER TO adminuteq;

--
-- TOC entry 236 (class 1259 OID 27932)
-- Name: codigos_respaldo_2fa_id_codigo_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.codigos_respaldo_2fa_id_codigo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.codigos_respaldo_2fa_id_codigo_seq OWNER TO adminuteq;

--
-- TOC entry 4936 (class 0 OID 0)
-- Dependencies: 236
-- Name: codigos_respaldo_2fa_id_codigo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.codigos_respaldo_2fa_id_codigo_seq OWNED BY public.codigos_respaldo_2fa.id_codigo;


--
-- TOC entry 237 (class 1259 OID 27933)
-- Name: comentarios_portafolio; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.comentarios_portafolio (
    id_comentario bigint NOT NULL,
    id_item_portafolio bigint NOT NULL,
    id_usuario_autor bigint NOT NULL,
    texto_comentario text NOT NULL,
    fecha_publicacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    estado_moderacion character varying(50) DEFAULT 'Activo'::character varying
);


ALTER TABLE public.comentarios_portafolio OWNER TO adminuteq;

--
-- TOC entry 238 (class 1259 OID 27944)
-- Name: comentarios_portafolio_id_comentario_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.comentarios_portafolio_id_comentario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.comentarios_portafolio_id_comentario_seq OWNER TO adminuteq;

--
-- TOC entry 4939 (class 0 OID 0)
-- Dependencies: 238
-- Name: comentarios_portafolio_id_comentario_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.comentarios_portafolio_id_comentario_seq OWNED BY public.comentarios_portafolio.id_comentario;


--
-- TOC entry 239 (class 1259 OID 27945)
-- Name: contratos; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.contratos (
    id_contrato bigint NOT NULL,
    id_pedido bigint NOT NULL,
    id_plantilla bigint NOT NULL,
    hash_firma_cliente character varying(255),
    hash_firma_creador character varying(255),
    limite_revisiones integer DEFAULT 0,
    fecha_formalizacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    url_documento_pdf character varying(255)
);


ALTER TABLE public.contratos OWNER TO adminuteq;

--
-- TOC entry 240 (class 1259 OID 27955)
-- Name: contratos_id_contrato_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.contratos_id_contrato_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.contratos_id_contrato_seq OWNER TO adminuteq;

--
-- TOC entry 4942 (class 0 OID 0)
-- Dependencies: 240
-- Name: contratos_id_contrato_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.contratos_id_contrato_seq OWNED BY public.contratos.id_contrato;


--
-- TOC entry 241 (class 1259 OID 27956)
-- Name: creador_habilidades; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.creador_habilidades (
    id_creador_habilidad bigint NOT NULL,
    id_perfil bigint NOT NULL,
    id_habilidad bigint NOT NULL,
    nivel_dominio character varying(50)
);


ALTER TABLE public.creador_habilidades OWNER TO adminuteq;

--
-- TOC entry 242 (class 1259 OID 27962)
-- Name: creador_habilidades_id_creador_habilidad_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.creador_habilidades_id_creador_habilidad_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.creador_habilidades_id_creador_habilidad_seq OWNER TO adminuteq;

--
-- TOC entry 4945 (class 0 OID 0)
-- Dependencies: 242
-- Name: creador_habilidades_id_creador_habilidad_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.creador_habilidades_id_creador_habilidad_seq OWNED BY public.creador_habilidades.id_creador_habilidad;


--
-- TOC entry 243 (class 1259 OID 27963)
-- Name: documentos_adjuntos; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.documentos_adjuntos (
    id_adjunto bigint NOT NULL,
    id_mensaje bigint NOT NULL,
    url_archivo character varying(255) NOT NULL,
    tipo_mime character varying(50),
    peso_bytes integer
);


ALTER TABLE public.documentos_adjuntos OWNER TO adminuteq;

--
-- TOC entry 244 (class 1259 OID 27969)
-- Name: documentos_adjuntos_id_adjunto_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.documentos_adjuntos_id_adjunto_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.documentos_adjuntos_id_adjunto_seq OWNER TO adminuteq;

--
-- TOC entry 4948 (class 0 OID 0)
-- Dependencies: 244
-- Name: documentos_adjuntos_id_adjunto_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.documentos_adjuntos_id_adjunto_seq OWNED BY public.documentos_adjuntos.id_adjunto;


--
-- TOC entry 245 (class 1259 OID 27970)
-- Name: entregables_finales; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.entregables_finales (
    id_entregable bigint NOT NULL,
    id_pedido bigint NOT NULL,
    url_version_marca_agua character varying(255),
    url_version_limpia character varying(255),
    esta_liberado boolean DEFAULT false
);


ALTER TABLE public.entregables_finales OWNER TO adminuteq;

--
-- TOC entry 246 (class 1259 OID 27978)
-- Name: entregables_finales_id_entregable_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.entregables_finales_id_entregable_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.entregables_finales_id_entregable_seq OWNER TO adminuteq;

--
-- TOC entry 4951 (class 0 OID 0)
-- Dependencies: 246
-- Name: entregables_finales_id_entregable_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.entregables_finales_id_entregable_seq OWNED BY public.entregables_finales.id_entregable;


--
-- TOC entry 247 (class 1259 OID 27979)
-- Name: estados_verificacion; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.estados_verificacion (
    id_estado_verificacion bigint NOT NULL,
    nombre_estado character varying(50) NOT NULL
);


ALTER TABLE public.estados_verificacion OWNER TO adminuteq;

--
-- TOC entry 248 (class 1259 OID 27984)
-- Name: estados_verificacion_id_estado_verificacion_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.estados_verificacion_id_estado_verificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.estados_verificacion_id_estado_verificacion_seq OWNER TO adminuteq;

--
-- TOC entry 4954 (class 0 OID 0)
-- Dependencies: 248
-- Name: estados_verificacion_id_estado_verificacion_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.estados_verificacion_id_estado_verificacion_seq OWNED BY public.estados_verificacion.id_estado_verificacion;


--
-- TOC entry 249 (class 1259 OID 27985)
-- Name: etapas_flujo; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.etapas_flujo (
    id_etapa bigint NOT NULL,
    nombre_etapa character varying(100) NOT NULL
);


ALTER TABLE public.etapas_flujo OWNER TO adminuteq;

--
-- TOC entry 250 (class 1259 OID 27990)
-- Name: etapas_flujo_id_etapa_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.etapas_flujo_id_etapa_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.etapas_flujo_id_etapa_seq OWNER TO adminuteq;

--
-- TOC entry 4957 (class 0 OID 0)
-- Dependencies: 250
-- Name: etapas_flujo_id_etapa_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.etapas_flujo_id_etapa_seq OWNED BY public.etapas_flujo.id_etapa;


--
-- TOC entry 251 (class 1259 OID 27991)
-- Name: etiquetas; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.etiquetas (
    id_etiqueta bigint NOT NULL,
    nombre_etiqueta character varying(50) NOT NULL,
    actualizado_en timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.etiquetas OWNER TO adminuteq;

--
-- TOC entry 252 (class 1259 OID 27997)
-- Name: etiquetas_id_etiqueta_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.etiquetas_id_etiqueta_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.etiquetas_id_etiqueta_seq OWNER TO adminuteq;

--
-- TOC entry 4960 (class 0 OID 0)
-- Dependencies: 252
-- Name: etiquetas_id_etiqueta_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.etiquetas_id_etiqueta_seq OWNED BY public.etiquetas.id_etiqueta;


--
-- TOC entry 253 (class 1259 OID 27998)
-- Name: flujo_etapas_config; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.flujo_etapas_config (
    id_flujo_etapa bigint NOT NULL,
    id_flujo bigint NOT NULL,
    id_etapa bigint NOT NULL,
    numero_orden integer NOT NULL,
    es_etapa_final boolean DEFAULT false
);


ALTER TABLE public.flujo_etapas_config OWNER TO adminuteq;

--
-- TOC entry 254 (class 1259 OID 28006)
-- Name: flujo_etapas_config_id_flujo_etapa_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.flujo_etapas_config_id_flujo_etapa_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.flujo_etapas_config_id_flujo_etapa_seq OWNER TO adminuteq;

--
-- TOC entry 4963 (class 0 OID 0)
-- Dependencies: 254
-- Name: flujo_etapas_config_id_flujo_etapa_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.flujo_etapas_config_id_flujo_etapa_seq OWNED BY public.flujo_etapas_config.id_flujo_etapa;


--
-- TOC entry 255 (class 1259 OID 28007)
-- Name: flujos_trabajo; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.flujos_trabajo (
    id_flujo bigint NOT NULL,
    nombre_flujo character varying(100) NOT NULL,
    descripcion_flujo text
);


ALTER TABLE public.flujos_trabajo OWNER TO adminuteq;

--
-- TOC entry 256 (class 1259 OID 28014)
-- Name: flujos_trabajo_id_flujo_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.flujos_trabajo_id_flujo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.flujos_trabajo_id_flujo_seq OWNER TO adminuteq;

--
-- TOC entry 4966 (class 0 OID 0)
-- Dependencies: 256
-- Name: flujos_trabajo_id_flujo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.flujos_trabajo_id_flujo_seq OWNED BY public.flujos_trabajo.id_flujo;


--
-- TOC entry 257 (class 1259 OID 28015)
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO adminuteq;

--
-- TOC entry 258 (class 1259 OID 28029)
-- Name: habilidades; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.habilidades (
    id_habilidad bigint NOT NULL,
    nombre_habilidad character varying(100) NOT NULL
);


ALTER TABLE public.habilidades OWNER TO adminuteq;

--
-- TOC entry 259 (class 1259 OID 28034)
-- Name: habilidades_id_habilidad_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.habilidades_id_habilidad_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.habilidades_id_habilidad_seq OWNER TO adminuteq;

--
-- TOC entry 4970 (class 0 OID 0)
-- Dependencies: 259
-- Name: habilidades_id_habilidad_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.habilidades_id_habilidad_seq OWNED BY public.habilidades.id_habilidad;


--
-- TOC entry 260 (class 1259 OID 28035)
-- Name: historial_estados_pedido; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.historial_estados_pedido (
    id_historial_estado bigint NOT NULL,
    id_pedido bigint NOT NULL,
    id_etapa bigint NOT NULL,
    fecha_transicion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    observacion text
);


ALTER TABLE public.historial_estados_pedido OWNER TO adminuteq;

--
-- TOC entry 261 (class 1259 OID 28044)
-- Name: historial_estados_pedido_id_historial_estado_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.historial_estados_pedido_id_historial_estado_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.historial_estados_pedido_id_historial_estado_seq OWNER TO adminuteq;

--
-- TOC entry 4973 (class 0 OID 0)
-- Dependencies: 261
-- Name: historial_estados_pedido_id_historial_estado_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.historial_estados_pedido_id_historial_estado_seq OWNED BY public.historial_estados_pedido.id_historial_estado;


--
-- TOC entry 262 (class 1259 OID 28045)
-- Name: infracciones_mensaje; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.infracciones_mensaje (
    id_infraccion bigint NOT NULL,
    id_usuario bigint NOT NULL,
    id_pedido bigint NOT NULL,
    fecha_infraccion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    mensaje_original text,
    patron_detectado character varying(50)
);


ALTER TABLE public.infracciones_mensaje OWNER TO adminuteq;

--
-- TOC entry 263 (class 1259 OID 28054)
-- Name: infracciones_mensaje_id_infraccion_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.infracciones_mensaje_id_infraccion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.infracciones_mensaje_id_infraccion_seq OWNER TO adminuteq;

--
-- TOC entry 4976 (class 0 OID 0)
-- Dependencies: 263
-- Name: infracciones_mensaje_id_infraccion_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.infracciones_mensaje_id_infraccion_seq OWNED BY public.infracciones_mensaje.id_infraccion;


--
-- TOC entry 264 (class 1259 OID 28055)
-- Name: likes_portafolio; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.likes_portafolio (
    id_like bigint NOT NULL,
    id_item_portafolio bigint NOT NULL,
    id_usuario bigint NOT NULL,
    fecha_like timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.likes_portafolio OWNER TO adminuteq;

--
-- TOC entry 265 (class 1259 OID 28062)
-- Name: likes_portafolio_id_like_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.likes_portafolio_id_like_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.likes_portafolio_id_like_seq OWNER TO adminuteq;

--
-- TOC entry 4979 (class 0 OID 0)
-- Dependencies: 265
-- Name: likes_portafolio_id_like_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.likes_portafolio_id_like_seq OWNED BY public.likes_portafolio.id_like;


--
-- TOC entry 266 (class 1259 OID 28063)
-- Name: mensajes; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.mensajes (
    id_mensaje bigint NOT NULL,
    id_sala bigint NOT NULL,
    id_remitente bigint NOT NULL,
    cuerpo_mensaje text,
    fecha_hora_envio timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    leido boolean DEFAULT false
);


ALTER TABLE public.mensajes OWNER TO adminuteq;

--
-- TOC entry 267 (class 1259 OID 28073)
-- Name: mensajes_id_mensaje_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.mensajes_id_mensaje_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mensajes_id_mensaje_seq OWNER TO adminuteq;

--
-- TOC entry 4982 (class 0 OID 0)
-- Dependencies: 267
-- Name: mensajes_id_mensaje_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.mensajes_id_mensaje_seq OWNED BY public.mensajes.id_mensaje;


--
-- TOC entry 268 (class 1259 OID 28074)
-- Name: motivos_rechazo; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.motivos_rechazo (
    id_motivo bigint NOT NULL,
    descripcion_motivo character varying(150) NOT NULL
);


ALTER TABLE public.motivos_rechazo OWNER TO adminuteq;

--
-- TOC entry 269 (class 1259 OID 28079)
-- Name: motivos_rechazo_id_motivo_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.motivos_rechazo_id_motivo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.motivos_rechazo_id_motivo_seq OWNER TO adminuteq;

--
-- TOC entry 4985 (class 0 OID 0)
-- Dependencies: 269
-- Name: motivos_rechazo_id_motivo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.motivos_rechazo_id_motivo_seq OWNED BY public.motivos_rechazo.id_motivo;


--
-- TOC entry 270 (class 1259 OID 28080)
-- Name: notificaciones_sistema; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.notificaciones_sistema (
    id_notificacion bigint NOT NULL,
    id_usuario bigint NOT NULL,
    id_tipo_notificacion bigint NOT NULL,
    fecha_emision timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    esta_leida boolean DEFAULT false
);


ALTER TABLE public.notificaciones_sistema OWNER TO adminuteq;

--
-- TOC entry 271 (class 1259 OID 28088)
-- Name: notificaciones_sistema_id_notificacion_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.notificaciones_sistema_id_notificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.notificaciones_sistema_id_notificacion_seq OWNER TO adminuteq;

--
-- TOC entry 4988 (class 0 OID 0)
-- Dependencies: 271
-- Name: notificaciones_sistema_id_notificacion_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.notificaciones_sistema_id_notificacion_seq OWNED BY public.notificaciones_sistema.id_notificacion;


--
-- TOC entry 272 (class 1259 OID 28089)
-- Name: pagos_garantia; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.pagos_garantia (
    id_pago bigint NOT NULL,
    id_contrato bigint NOT NULL,
    id_orden_paypal character varying(100),
    monto_retenido numeric(10,2) NOT NULL,
    estado_fondos character varying(50) DEFAULT 'Retenido'::character varying
);


ALTER TABLE public.pagos_garantia OWNER TO adminuteq;

--
-- TOC entry 273 (class 1259 OID 28096)
-- Name: pagos_garantia_id_pago_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.pagos_garantia_id_pago_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pagos_garantia_id_pago_seq OWNER TO adminuteq;

--
-- TOC entry 4991 (class 0 OID 0)
-- Dependencies: 273
-- Name: pagos_garantia_id_pago_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.pagos_garantia_id_pago_seq OWNED BY public.pagos_garantia.id_pago;


--
-- TOC entry 274 (class 1259 OID 28097)
-- Name: pais; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.pais (
    id_pais bigint NOT NULL,
    nombre_pais character varying(100) NOT NULL
);


ALTER TABLE public.pais OWNER TO adminuteq;

--
-- TOC entry 275 (class 1259 OID 28102)
-- Name: pais_id_pais_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.pais_id_pais_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pais_id_pais_seq OWNER TO adminuteq;

--
-- TOC entry 4994 (class 0 OID 0)
-- Dependencies: 275
-- Name: pais_id_pais_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.pais_id_pais_seq OWNED BY public.pais.id_pais;


--
-- TOC entry 276 (class 1259 OID 28103)
-- Name: participantes_sorteo; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.participantes_sorteo (
    id_participacion bigint NOT NULL,
    id_sorteo bigint NOT NULL,
    id_usuario bigint NOT NULL,
    fecha_inscripcion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    es_ganador boolean DEFAULT false,
    fecha_notificacion_premio timestamp without time zone
);


ALTER TABLE public.participantes_sorteo OWNER TO adminuteq;

--
-- TOC entry 277 (class 1259 OID 28111)
-- Name: participantes_sorteo_id_participacion_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.participantes_sorteo_id_participacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.participantes_sorteo_id_participacion_seq OWNER TO adminuteq;

--
-- TOC entry 4997 (class 0 OID 0)
-- Dependencies: 277
-- Name: participantes_sorteo_id_participacion_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.participantes_sorteo_id_participacion_seq OWNED BY public.participantes_sorteo.id_participacion;


--
-- TOC entry 278 (class 1259 OID 28112)
-- Name: pedidos; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.pedidos (
    id_pedido bigint NOT NULL,
    id_usuario_cliente bigint NOT NULL,
    id_servicio bigint NOT NULL,
    id_flujo bigint NOT NULL,
    fecha_inicio timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    fecha_entrega_estimada timestamp without time zone,
    precio_pactado numeric(10,2) NOT NULL
);


ALTER TABLE public.pedidos OWNER TO adminuteq;

--
-- TOC entry 279 (class 1259 OID 28121)
-- Name: pedidos_id_pedido_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.pedidos_id_pedido_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pedidos_id_pedido_seq OWNER TO adminuteq;

--
-- TOC entry 5000 (class 0 OID 0)
-- Dependencies: 279
-- Name: pedidos_id_pedido_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.pedidos_id_pedido_seq OWNED BY public.pedidos.id_pedido;


--
-- TOC entry 280 (class 1259 OID 28122)
-- Name: perfiles_creadores; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.perfiles_creadores (
    id_perfil bigint NOT NULL,
    id_usuario bigint NOT NULL,
    biografia text,
    url_red_social character varying(255)
);


ALTER TABLE public.perfiles_creadores OWNER TO adminuteq;

--
-- TOC entry 281 (class 1259 OID 28129)
-- Name: perfiles_creadores_id_perfil_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.perfiles_creadores_id_perfil_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.perfiles_creadores_id_perfil_seq OWNER TO adminuteq;

--
-- TOC entry 5003 (class 0 OID 0)
-- Dependencies: 281
-- Name: perfiles_creadores_id_perfil_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.perfiles_creadores_id_perfil_seq OWNED BY public.perfiles_creadores.id_perfil;


--
-- TOC entry 282 (class 1259 OID 28130)
-- Name: permisos; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.permisos (
    id_permiso bigint NOT NULL,
    nombre_permiso character varying(100) NOT NULL,
    modulo_aplicacion character varying(50)
);


ALTER TABLE public.permisos OWNER TO adminuteq;

--
-- TOC entry 283 (class 1259 OID 28137)
-- Name: permisos_id_permiso_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.permisos_id_permiso_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.permisos_id_permiso_seq OWNER TO adminuteq;

--
-- TOC entry 5006 (class 0 OID 0)
-- Dependencies: 283
-- Name: permisos_id_permiso_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.permisos_id_permiso_seq OWNED BY public.permisos.id_permiso;


--
-- TOC entry 284 (class 1259 OID 28138)
-- Name: plantillas_contrato; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.plantillas_contrato (
    id_plantilla bigint NOT NULL,
    version_legal character varying(50) NOT NULL,
    cuerpo_html_plantilla text NOT NULL
);


ALTER TABLE public.plantillas_contrato OWNER TO adminuteq;

--
-- TOC entry 285 (class 1259 OID 28146)
-- Name: plantillas_contrato_id_plantilla_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.plantillas_contrato_id_plantilla_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.plantillas_contrato_id_plantilla_seq OWNER TO adminuteq;

--
-- TOC entry 5009 (class 0 OID 0)
-- Dependencies: 285
-- Name: plantillas_contrato_id_plantilla_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.plantillas_contrato_id_plantilla_seq OWNED BY public.plantillas_contrato.id_plantilla;


--
-- TOC entry 286 (class 1259 OID 28147)
-- Name: portafolio_items; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.portafolio_items (
    id_item_portafolio bigint NOT NULL,
    id_portafolio bigint NOT NULL,
    titulo_obra character varying(150) NOT NULL,
    descripcion_obra text,
    url_archivo_multimedia character varying(255) NOT NULL,
    fecha_subida timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.portafolio_items OWNER TO adminuteq;

--
-- TOC entry 287 (class 1259 OID 28157)
-- Name: portafolio_items_id_item_portafolio_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.portafolio_items_id_item_portafolio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.portafolio_items_id_item_portafolio_seq OWNER TO adminuteq;

--
-- TOC entry 5012 (class 0 OID 0)
-- Dependencies: 287
-- Name: portafolio_items_id_item_portafolio_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.portafolio_items_id_item_portafolio_seq OWNED BY public.portafolio_items.id_item_portafolio;


--
-- TOC entry 288 (class 1259 OID 28158)
-- Name: portafolios; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.portafolios (
    id_portafolio bigint NOT NULL,
    id_perfil bigint NOT NULL,
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    total_visitas_acumuladas integer DEFAULT 0,
    es_publico boolean DEFAULT true,
    actualizado_en timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    opciones_personalizacion jsonb
);


ALTER TABLE public.portafolios OWNER TO adminuteq;

--
-- TOC entry 289 (class 1259 OID 28168)
-- Name: portafolios_id_portafolio_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.portafolios_id_portafolio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.portafolios_id_portafolio_seq OWNER TO adminuteq;

--
-- TOC entry 5015 (class 0 OID 0)
-- Dependencies: 289
-- Name: portafolios_id_portafolio_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.portafolios_id_portafolio_seq OWNED BY public.portafolios.id_portafolio;


--
-- TOC entry 290 (class 1259 OID 28169)
-- Name: resenas_servicios; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.resenas_servicios (
    id_resena bigint NOT NULL,
    id_pedido bigint NOT NULL,
    calificacion_estrellas integer,
    texto_resena text,
    fecha_resena timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT resenas_servicios_calificacion_estrellas_check CHECK (((calificacion_estrellas >= 1) AND (calificacion_estrellas <= 5)))
);


ALTER TABLE public.resenas_servicios OWNER TO adminuteq;

--
-- TOC entry 291 (class 1259 OID 28178)
-- Name: resenas_servicios_id_resena_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.resenas_servicios_id_resena_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.resenas_servicios_id_resena_seq OWNER TO adminuteq;

--
-- TOC entry 5018 (class 0 OID 0)
-- Dependencies: 291
-- Name: resenas_servicios_id_resena_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.resenas_servicios_id_resena_seq OWNED BY public.resenas_servicios.id_resena;


--
-- TOC entry 292 (class 1259 OID 28179)
-- Name: rol_permisos; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.rol_permisos (
    id_rol_permiso bigint NOT NULL,
    id_rol bigint NOT NULL,
    id_permiso bigint NOT NULL
);


ALTER TABLE public.rol_permisos OWNER TO adminuteq;

--
-- TOC entry 293 (class 1259 OID 28185)
-- Name: rol_permisos_id_rol_permiso_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.rol_permisos_id_rol_permiso_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.rol_permisos_id_rol_permiso_seq OWNER TO adminuteq;

--
-- TOC entry 5021 (class 0 OID 0)
-- Dependencies: 293
-- Name: rol_permisos_id_rol_permiso_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.rol_permisos_id_rol_permiso_seq OWNED BY public.rol_permisos.id_rol_permiso;


--
-- TOC entry 294 (class 1259 OID 28186)
-- Name: roles; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.roles (
    id_rol bigint NOT NULL,
    nombre_rol character varying(50) NOT NULL,
    descripcion_rol text
);


ALTER TABLE public.roles OWNER TO adminuteq;

--
-- TOC entry 295 (class 1259 OID 28193)
-- Name: roles_id_rol_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.roles_id_rol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.roles_id_rol_seq OWNER TO adminuteq;

--
-- TOC entry 5024 (class 0 OID 0)
-- Dependencies: 295
-- Name: roles_id_rol_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.roles_id_rol_seq OWNED BY public.roles.id_rol;


--
-- TOC entry 296 (class 1259 OID 28194)
-- Name: salas_chat; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.salas_chat (
    id_sala bigint NOT NULL,
    id_pedido bigint NOT NULL,
    fecha_apertura timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    sala_activa boolean DEFAULT true
);


ALTER TABLE public.salas_chat OWNER TO adminuteq;

--
-- TOC entry 297 (class 1259 OID 28201)
-- Name: salas_chat_id_sala_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.salas_chat_id_sala_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.salas_chat_id_sala_seq OWNER TO adminuteq;

--
-- TOC entry 5027 (class 0 OID 0)
-- Dependencies: 297
-- Name: salas_chat_id_sala_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.salas_chat_id_sala_seq OWNED BY public.salas_chat.id_sala;


--
-- TOC entry 298 (class 1259 OID 28202)
-- Name: seguidores; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.seguidores (
    id_seguimiento bigint NOT NULL,
    id_usuario_seguidor bigint NOT NULL,
    id_perfil_creador bigint NOT NULL,
    fecha_seguimiento timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    notificaciones_activas boolean DEFAULT true
);


ALTER TABLE public.seguidores OWNER TO adminuteq;

--
-- TOC entry 299 (class 1259 OID 28210)
-- Name: seguidores_id_seguimiento_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.seguidores_id_seguimiento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.seguidores_id_seguimiento_seq OWNER TO adminuteq;

--
-- TOC entry 5030 (class 0 OID 0)
-- Dependencies: 299
-- Name: seguidores_id_seguimiento_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.seguidores_id_seguimiento_seq OWNED BY public.seguidores.id_seguimiento;


--
-- TOC entry 300 (class 1259 OID 28211)
-- Name: servicio_atributos; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.servicio_atributos (
    id_servicio_atributo bigint NOT NULL,
    id_servicio bigint NOT NULL,
    id_atributo bigint NOT NULL,
    valor_asignado character varying(255) NOT NULL,
    actualizado_en timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.servicio_atributos OWNER TO adminuteq;

--
-- TOC entry 301 (class 1259 OID 28219)
-- Name: servicio_atributos_id_servicio_atributo_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.servicio_atributos_id_servicio_atributo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.servicio_atributos_id_servicio_atributo_seq OWNER TO adminuteq;

--
-- TOC entry 5033 (class 0 OID 0)
-- Dependencies: 301
-- Name: servicio_atributos_id_servicio_atributo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.servicio_atributos_id_servicio_atributo_seq OWNED BY public.servicio_atributos.id_servicio_atributo;


--
-- TOC entry 302 (class 1259 OID 28220)
-- Name: servicio_etiquetas; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.servicio_etiquetas (
    id_servicio_etiqueta bigint NOT NULL,
    id_servicio bigint NOT NULL,
    id_etiqueta bigint NOT NULL,
    actualizado_en timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.servicio_etiquetas OWNER TO adminuteq;

--
-- TOC entry 303 (class 1259 OID 28227)
-- Name: servicio_etiquetas_id_servicio_etiqueta_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.servicio_etiquetas_id_servicio_etiqueta_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.servicio_etiquetas_id_servicio_etiqueta_seq OWNER TO adminuteq;

--
-- TOC entry 5036 (class 0 OID 0)
-- Dependencies: 303
-- Name: servicio_etiquetas_id_servicio_etiqueta_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.servicio_etiquetas_id_servicio_etiqueta_seq OWNED BY public.servicio_etiquetas.id_servicio_etiqueta;


--
-- TOC entry 304 (class 1259 OID 28228)
-- Name: servicios; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.servicios (
    id_servicio bigint NOT NULL,
    id_perfil bigint NOT NULL,
    id_subcategoria bigint NOT NULL,
    titulo_servicio character varying(150) NOT NULL,
    descripcion_detallada text NOT NULL,
    precio_base numeric(10,2) NOT NULL,
    url_miniatura character varying(255),
    tipo_item character varying(20) DEFAULT 'SERVICIO'::character varying NOT NULL,
    estado_publicacion character varying(20) DEFAULT 'ACTIVO'::character varying NOT NULL,
    cargo_revision_adicional numeric(10,2) DEFAULT 0.00,
    limite_revisiones_base integer DEFAULT 0,
    actualizado_en timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.servicios OWNER TO adminuteq;

--
-- TOC entry 305 (class 1259 OID 28246)
-- Name: servicios_id_servicio_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.servicios_id_servicio_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.servicios_id_servicio_seq OWNER TO adminuteq;

--
-- TOC entry 5039 (class 0 OID 0)
-- Dependencies: 305
-- Name: servicios_id_servicio_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.servicios_id_servicio_seq OWNED BY public.servicios.id_servicio;


--
-- TOC entry 306 (class 1259 OID 28247)
-- Name: sesiones_usuario; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.sesiones_usuario (
    id_sesion bigint NOT NULL,
    id_usuario bigint NOT NULL,
    direccion_ip character varying(45),
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion timestamp without time zone NOT NULL,
    jti character varying(36) NOT NULL
);


ALTER TABLE public.sesiones_usuario OWNER TO adminuteq;

--
-- TOC entry 307 (class 1259 OID 28257)
-- Name: sesiones_usuario_id_sesion_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.sesiones_usuario_id_sesion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sesiones_usuario_id_sesion_seq OWNER TO adminuteq;

--
-- TOC entry 5042 (class 0 OID 0)
-- Dependencies: 307
-- Name: sesiones_usuario_id_sesion_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.sesiones_usuario_id_sesion_seq OWNED BY public.sesiones_usuario.id_sesion;


--
-- TOC entry 308 (class 1259 OID 28258)
-- Name: sorteos; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.sorteos (
    id_sorteo bigint NOT NULL,
    id_perfil_creador bigint NOT NULL,
    titulo_sorteo character varying(150) NOT NULL,
    descripcion_premios text NOT NULL,
    cantidad_ganadores integer DEFAULT 1 NOT NULL,
    fecha_inicio timestamp without time zone NOT NULL,
    fecha_cierre timestamp without time zone NOT NULL,
    estado_sorteo character varying(50) DEFAULT 'Activo'::character varying,
    requiere_seguidor boolean DEFAULT false NOT NULL
);


ALTER TABLE public.sorteos OWNER TO adminuteq;

--
-- TOC entry 309 (class 1259 OID 28274)
-- Name: sorteos_id_sorteo_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.sorteos_id_sorteo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sorteos_id_sorteo_seq OWNER TO adminuteq;

--
-- TOC entry 5045 (class 0 OID 0)
-- Dependencies: 309
-- Name: sorteos_id_sorteo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.sorteos_id_sorteo_seq OWNED BY public.sorteos.id_sorteo;


--
-- TOC entry 310 (class 1259 OID 28275)
-- Name: subcategorias; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.subcategorias (
    id_subcategoria bigint NOT NULL,
    id_categoria bigint NOT NULL,
    nombre_subcategoria character varying(100) NOT NULL,
    actualizado_en timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.subcategorias OWNER TO adminuteq;

--
-- TOC entry 311 (class 1259 OID 28282)
-- Name: subcategorias_id_subcategoria_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.subcategorias_id_subcategoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.subcategorias_id_subcategoria_seq OWNER TO adminuteq;

--
-- TOC entry 5048 (class 0 OID 0)
-- Dependencies: 311
-- Name: subcategorias_id_subcategoria_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.subcategorias_id_subcategoria_seq OWNED BY public.subcategorias.id_subcategoria;


--
-- TOC entry 312 (class 1259 OID 28283)
-- Name: tickets_revision; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.tickets_revision (
    id_ticket bigint NOT NULL,
    id_pedido bigint NOT NULL,
    id_motivo bigint NOT NULL,
    descripcion_cliente text NOT NULL,
    costo_adicional_generado numeric(10,2) DEFAULT 0.00,
    estado_ticket character varying(50) DEFAULT 'Abierto'::character varying
);


ALTER TABLE public.tickets_revision OWNER TO adminuteq;

--
-- TOC entry 313 (class 1259 OID 28294)
-- Name: tickets_revision_id_ticket_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.tickets_revision_id_ticket_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tickets_revision_id_ticket_seq OWNER TO adminuteq;

--
-- TOC entry 5051 (class 0 OID 0)
-- Dependencies: 313
-- Name: tickets_revision_id_ticket_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.tickets_revision_id_ticket_seq OWNED BY public.tickets_revision.id_ticket;


--
-- TOC entry 314 (class 1259 OID 28295)
-- Name: tipos_notificacion; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.tipos_notificacion (
    id_tipo_notificacion bigint NOT NULL,
    nombre_evento character varying(100) NOT NULL,
    formato_mensaje text
);


ALTER TABLE public.tipos_notificacion OWNER TO adminuteq;

--
-- TOC entry 315 (class 1259 OID 28302)
-- Name: tipos_notificacion_id_tipo_notificacion_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.tipos_notificacion_id_tipo_notificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tipos_notificacion_id_tipo_notificacion_seq OWNER TO adminuteq;

--
-- TOC entry 5054 (class 0 OID 0)
-- Dependencies: 315
-- Name: tipos_notificacion_id_tipo_notificacion_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.tipos_notificacion_id_tipo_notificacion_seq OWNED BY public.tipos_notificacion.id_tipo_notificacion;


--
-- TOC entry 316 (class 1259 OID 28303)
-- Name: tokens_recuperacion; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.tokens_recuperacion (
    id_token bigint NOT NULL,
    id_usuario bigint NOT NULL,
    hash_token character varying(255) NOT NULL,
    fecha_generacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    usado boolean DEFAULT false
);


ALTER TABLE public.tokens_recuperacion OWNER TO adminuteq;

--
-- TOC entry 317 (class 1259 OID 28311)
-- Name: tokens_recuperacion_id_token_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.tokens_recuperacion_id_token_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tokens_recuperacion_id_token_seq OWNER TO adminuteq;

--
-- TOC entry 5057 (class 0 OID 0)
-- Dependencies: 317
-- Name: tokens_recuperacion_id_token_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.tokens_recuperacion_id_token_seq OWNED BY public.tokens_recuperacion.id_token;


--
-- TOC entry 318 (class 1259 OID 28312)
-- Name: transacciones_pago; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.transacciones_pago (
    id_transaccion bigint NOT NULL,
    id_pago bigint NOT NULL,
    tipo_transaccion character varying(50) NOT NULL,
    monto numeric(10,2) NOT NULL,
    fecha_ejecucion timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.transacciones_pago OWNER TO adminuteq;

--
-- TOC entry 319 (class 1259 OID 28320)
-- Name: transacciones_pago_id_transaccion_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.transacciones_pago_id_transaccion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.transacciones_pago_id_transaccion_seq OWNER TO adminuteq;

--
-- TOC entry 5060 (class 0 OID 0)
-- Dependencies: 319
-- Name: transacciones_pago_id_transaccion_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.transacciones_pago_id_transaccion_seq OWNED BY public.transacciones_pago.id_transaccion;


--
-- TOC entry 320 (class 1259 OID 28321)
-- Name: usuario_roles; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.usuario_roles (
    id_usuario_rol bigint NOT NULL,
    id_usuario bigint NOT NULL,
    id_rol bigint NOT NULL
);


ALTER TABLE public.usuario_roles OWNER TO adminuteq;

--
-- TOC entry 321 (class 1259 OID 28327)
-- Name: usuario_roles_id_usuario_rol_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.usuario_roles_id_usuario_rol_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.usuario_roles_id_usuario_rol_seq OWNER TO adminuteq;

--
-- TOC entry 5063 (class 0 OID 0)
-- Dependencies: 321
-- Name: usuario_roles_id_usuario_rol_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.usuario_roles_id_usuario_rol_seq OWNED BY public.usuario_roles.id_usuario_rol;


--
-- TOC entry 322 (class 1259 OID 28328)
-- Name: usuarios; Type: TABLE; Schema: public; Owner: adminuteq
--

CREATE TABLE public.usuarios (
    id_usuario bigint NOT NULL,
    nombres character varying(100) NOT NULL,
    apellidos character varying(100) NOT NULL,
    correo character varying(150) NOT NULL,
    contrasena_hash character varying(255) NOT NULL,
    id_pais bigint,
    fecha_registro timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    estado_cuenta boolean DEFAULT true,
    fecha_nacimiento date,
    actualizado_en timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.usuarios OWNER TO adminuteq;

--
-- TOC entry 323 (class 1259 OID 28341)
-- Name: usuarios_id_usuario_seq; Type: SEQUENCE; Schema: public; Owner: adminuteq
--

CREATE SEQUENCE public.usuarios_id_usuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.usuarios_id_usuario_seq OWNER TO adminuteq;

--
-- TOC entry 5066 (class 0 OID 0)
-- Dependencies: 323
-- Name: usuarios_id_usuario_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: adminuteq
--

ALTER SEQUENCE public.usuarios_id_usuario_seq OWNED BY public.usuarios.id_usuario;


--
-- TOC entry 4222 (class 2604 OID 28342)
-- Name: atributos_dinamicos id_atributo; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.atributos_dinamicos ALTER COLUMN id_atributo SET DEFAULT nextval('public.atributos_dinamicos_id_atributo_seq'::regclass);


--
-- TOC entry 4224 (class 2604 OID 28343)
-- Name: autenticacion_dos_factores id_2fa; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.autenticacion_dos_factores ALTER COLUMN id_2fa SET DEFAULT nextval('public.autenticacion_dos_factores_id_2fa_seq'::regclass);


--
-- TOC entry 4226 (class 2604 OID 28344)
-- Name: briefing_enviados id_briefing_enviado; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_enviados ALTER COLUMN id_briefing_enviado SET DEFAULT nextval('public.briefing_enviados_id_briefing_enviado_seq'::regclass);


--
-- TOC entry 4229 (class 2604 OID 28345)
-- Name: briefing_plantillas id_briefing_plantilla; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_plantillas ALTER COLUMN id_briefing_plantilla SET DEFAULT nextval('public.briefing_plantillas_id_briefing_plantilla_seq'::regclass);


--
-- TOC entry 4231 (class 2604 OID 28346)
-- Name: briefing_preguntas id_pregunta; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_preguntas ALTER COLUMN id_pregunta SET DEFAULT nextval('public.briefing_preguntas_id_pregunta_seq'::regclass);


--
-- TOC entry 4232 (class 2604 OID 28347)
-- Name: briefing_respuestas id_respuesta; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_respuestas ALTER COLUMN id_respuesta SET DEFAULT nextval('public.briefing_respuestas_id_respuesta_seq'::regclass);


--
-- TOC entry 4234 (class 2604 OID 28348)
-- Name: categorias id_categoria; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.categorias ALTER COLUMN id_categoria SET DEFAULT nextval('public.categorias_id_categoria_seq'::regclass);


--
-- TOC entry 4237 (class 2604 OID 28349)
-- Name: certificados_ia id_certificado; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.certificados_ia ALTER COLUMN id_certificado SET DEFAULT nextval('public.certificados_ia_id_certificado_seq'::regclass);


--
-- TOC entry 4241 (class 2604 OID 28350)
-- Name: codigos_respaldo_2fa id_codigo; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.codigos_respaldo_2fa ALTER COLUMN id_codigo SET DEFAULT nextval('public.codigos_respaldo_2fa_id_codigo_seq'::regclass);


--
-- TOC entry 4243 (class 2604 OID 28351)
-- Name: comentarios_portafolio id_comentario; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.comentarios_portafolio ALTER COLUMN id_comentario SET DEFAULT nextval('public.comentarios_portafolio_id_comentario_seq'::regclass);


--
-- TOC entry 4246 (class 2604 OID 28352)
-- Name: contratos id_contrato; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.contratos ALTER COLUMN id_contrato SET DEFAULT nextval('public.contratos_id_contrato_seq'::regclass);


--
-- TOC entry 4249 (class 2604 OID 28353)
-- Name: creador_habilidades id_creador_habilidad; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.creador_habilidades ALTER COLUMN id_creador_habilidad SET DEFAULT nextval('public.creador_habilidades_id_creador_habilidad_seq'::regclass);


--
-- TOC entry 4250 (class 2604 OID 28354)
-- Name: documentos_adjuntos id_adjunto; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.documentos_adjuntos ALTER COLUMN id_adjunto SET DEFAULT nextval('public.documentos_adjuntos_id_adjunto_seq'::regclass);


--
-- TOC entry 4251 (class 2604 OID 28355)
-- Name: entregables_finales id_entregable; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.entregables_finales ALTER COLUMN id_entregable SET DEFAULT nextval('public.entregables_finales_id_entregable_seq'::regclass);


--
-- TOC entry 4253 (class 2604 OID 28356)
-- Name: estados_verificacion id_estado_verificacion; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.estados_verificacion ALTER COLUMN id_estado_verificacion SET DEFAULT nextval('public.estados_verificacion_id_estado_verificacion_seq'::regclass);


--
-- TOC entry 4254 (class 2604 OID 28357)
-- Name: etapas_flujo id_etapa; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.etapas_flujo ALTER COLUMN id_etapa SET DEFAULT nextval('public.etapas_flujo_id_etapa_seq'::regclass);


--
-- TOC entry 4255 (class 2604 OID 28358)
-- Name: etiquetas id_etiqueta; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.etiquetas ALTER COLUMN id_etiqueta SET DEFAULT nextval('public.etiquetas_id_etiqueta_seq'::regclass);


--
-- TOC entry 4257 (class 2604 OID 28359)
-- Name: flujo_etapas_config id_flujo_etapa; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.flujo_etapas_config ALTER COLUMN id_flujo_etapa SET DEFAULT nextval('public.flujo_etapas_config_id_flujo_etapa_seq'::regclass);


--
-- TOC entry 4259 (class 2604 OID 28360)
-- Name: flujos_trabajo id_flujo; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.flujos_trabajo ALTER COLUMN id_flujo SET DEFAULT nextval('public.flujos_trabajo_id_flujo_seq'::regclass);


--
-- TOC entry 4261 (class 2604 OID 28361)
-- Name: habilidades id_habilidad; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.habilidades ALTER COLUMN id_habilidad SET DEFAULT nextval('public.habilidades_id_habilidad_seq'::regclass);


--
-- TOC entry 4262 (class 2604 OID 28362)
-- Name: historial_estados_pedido id_historial_estado; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.historial_estados_pedido ALTER COLUMN id_historial_estado SET DEFAULT nextval('public.historial_estados_pedido_id_historial_estado_seq'::regclass);


--
-- TOC entry 4264 (class 2604 OID 28363)
-- Name: infracciones_mensaje id_infraccion; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.infracciones_mensaje ALTER COLUMN id_infraccion SET DEFAULT nextval('public.infracciones_mensaje_id_infraccion_seq'::regclass);


--
-- TOC entry 4266 (class 2604 OID 28364)
-- Name: likes_portafolio id_like; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.likes_portafolio ALTER COLUMN id_like SET DEFAULT nextval('public.likes_portafolio_id_like_seq'::regclass);


--
-- TOC entry 4268 (class 2604 OID 28365)
-- Name: mensajes id_mensaje; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.mensajes ALTER COLUMN id_mensaje SET DEFAULT nextval('public.mensajes_id_mensaje_seq'::regclass);


--
-- TOC entry 4271 (class 2604 OID 28366)
-- Name: motivos_rechazo id_motivo; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.motivos_rechazo ALTER COLUMN id_motivo SET DEFAULT nextval('public.motivos_rechazo_id_motivo_seq'::regclass);


--
-- TOC entry 4272 (class 2604 OID 28367)
-- Name: notificaciones_sistema id_notificacion; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.notificaciones_sistema ALTER COLUMN id_notificacion SET DEFAULT nextval('public.notificaciones_sistema_id_notificacion_seq'::regclass);


--
-- TOC entry 4275 (class 2604 OID 28368)
-- Name: pagos_garantia id_pago; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pagos_garantia ALTER COLUMN id_pago SET DEFAULT nextval('public.pagos_garantia_id_pago_seq'::regclass);


--
-- TOC entry 4277 (class 2604 OID 28369)
-- Name: pais id_pais; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pais ALTER COLUMN id_pais SET DEFAULT nextval('public.pais_id_pais_seq'::regclass);


--
-- TOC entry 4278 (class 2604 OID 28370)
-- Name: participantes_sorteo id_participacion; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.participantes_sorteo ALTER COLUMN id_participacion SET DEFAULT nextval('public.participantes_sorteo_id_participacion_seq'::regclass);


--
-- TOC entry 4281 (class 2604 OID 28371)
-- Name: pedidos id_pedido; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pedidos ALTER COLUMN id_pedido SET DEFAULT nextval('public.pedidos_id_pedido_seq'::regclass);


--
-- TOC entry 4283 (class 2604 OID 28372)
-- Name: perfiles_creadores id_perfil; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.perfiles_creadores ALTER COLUMN id_perfil SET DEFAULT nextval('public.perfiles_creadores_id_perfil_seq'::regclass);


--
-- TOC entry 4284 (class 2604 OID 28373)
-- Name: permisos id_permiso; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.permisos ALTER COLUMN id_permiso SET DEFAULT nextval('public.permisos_id_permiso_seq'::regclass);


--
-- TOC entry 4285 (class 2604 OID 28374)
-- Name: plantillas_contrato id_plantilla; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.plantillas_contrato ALTER COLUMN id_plantilla SET DEFAULT nextval('public.plantillas_contrato_id_plantilla_seq'::regclass);


--
-- TOC entry 4286 (class 2604 OID 28375)
-- Name: portafolio_items id_item_portafolio; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.portafolio_items ALTER COLUMN id_item_portafolio SET DEFAULT nextval('public.portafolio_items_id_item_portafolio_seq'::regclass);


--
-- TOC entry 4288 (class 2604 OID 28376)
-- Name: portafolios id_portafolio; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.portafolios ALTER COLUMN id_portafolio SET DEFAULT nextval('public.portafolios_id_portafolio_seq'::regclass);


--
-- TOC entry 4293 (class 2604 OID 28377)
-- Name: resenas_servicios id_resena; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.resenas_servicios ALTER COLUMN id_resena SET DEFAULT nextval('public.resenas_servicios_id_resena_seq'::regclass);


--
-- TOC entry 4295 (class 2604 OID 28378)
-- Name: rol_permisos id_rol_permiso; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.rol_permisos ALTER COLUMN id_rol_permiso SET DEFAULT nextval('public.rol_permisos_id_rol_permiso_seq'::regclass);


--
-- TOC entry 4296 (class 2604 OID 28379)
-- Name: roles id_rol; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.roles ALTER COLUMN id_rol SET DEFAULT nextval('public.roles_id_rol_seq'::regclass);


--
-- TOC entry 4297 (class 2604 OID 28380)
-- Name: salas_chat id_sala; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.salas_chat ALTER COLUMN id_sala SET DEFAULT nextval('public.salas_chat_id_sala_seq'::regclass);


--
-- TOC entry 4300 (class 2604 OID 28381)
-- Name: seguidores id_seguimiento; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.seguidores ALTER COLUMN id_seguimiento SET DEFAULT nextval('public.seguidores_id_seguimiento_seq'::regclass);


--
-- TOC entry 4303 (class 2604 OID 28382)
-- Name: servicio_atributos id_servicio_atributo; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicio_atributos ALTER COLUMN id_servicio_atributo SET DEFAULT nextval('public.servicio_atributos_id_servicio_atributo_seq'::regclass);


--
-- TOC entry 4305 (class 2604 OID 28383)
-- Name: servicio_etiquetas id_servicio_etiqueta; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicio_etiquetas ALTER COLUMN id_servicio_etiqueta SET DEFAULT nextval('public.servicio_etiquetas_id_servicio_etiqueta_seq'::regclass);


--
-- TOC entry 4307 (class 2604 OID 28384)
-- Name: servicios id_servicio; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicios ALTER COLUMN id_servicio SET DEFAULT nextval('public.servicios_id_servicio_seq'::regclass);


--
-- TOC entry 4313 (class 2604 OID 28385)
-- Name: sesiones_usuario id_sesion; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.sesiones_usuario ALTER COLUMN id_sesion SET DEFAULT nextval('public.sesiones_usuario_id_sesion_seq'::regclass);


--
-- TOC entry 4315 (class 2604 OID 28386)
-- Name: sorteos id_sorteo; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.sorteos ALTER COLUMN id_sorteo SET DEFAULT nextval('public.sorteos_id_sorteo_seq'::regclass);


--
-- TOC entry 4319 (class 2604 OID 28387)
-- Name: subcategorias id_subcategoria; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.subcategorias ALTER COLUMN id_subcategoria SET DEFAULT nextval('public.subcategorias_id_subcategoria_seq'::regclass);


--
-- TOC entry 4321 (class 2604 OID 28388)
-- Name: tickets_revision id_ticket; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.tickets_revision ALTER COLUMN id_ticket SET DEFAULT nextval('public.tickets_revision_id_ticket_seq'::regclass);


--
-- TOC entry 4324 (class 2604 OID 28389)
-- Name: tipos_notificacion id_tipo_notificacion; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.tipos_notificacion ALTER COLUMN id_tipo_notificacion SET DEFAULT nextval('public.tipos_notificacion_id_tipo_notificacion_seq'::regclass);


--
-- TOC entry 4325 (class 2604 OID 28390)
-- Name: tokens_recuperacion id_token; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.tokens_recuperacion ALTER COLUMN id_token SET DEFAULT nextval('public.tokens_recuperacion_id_token_seq'::regclass);


--
-- TOC entry 4328 (class 2604 OID 28391)
-- Name: transacciones_pago id_transaccion; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.transacciones_pago ALTER COLUMN id_transaccion SET DEFAULT nextval('public.transacciones_pago_id_transaccion_seq'::regclass);


--
-- TOC entry 4330 (class 2604 OID 28392)
-- Name: usuario_roles id_usuario_rol; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.usuario_roles ALTER COLUMN id_usuario_rol SET DEFAULT nextval('public.usuario_roles_id_usuario_rol_seq'::regclass);


--
-- TOC entry 4331 (class 2604 OID 28393)
-- Name: usuarios id_usuario; Type: DEFAULT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.usuarios ALTER COLUMN id_usuario SET DEFAULT nextval('public.usuarios_id_usuario_seq'::regclass);


--
-- TOC entry 4722 (class 0 OID 27848)
-- Dependencies: 219
-- Data for Name: atributos_dinamicos; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.atributos_dinamicos (id_atributo, nombre_atributo, tipo_dato, actualizado_en) FROM stdin;
1	Color Principal	TEXTO	2026-08-02 03:11:17.845077+00
2	Formato de Entrega	NUMERO	2026-08-02 03:11:17.845077+00
3	Tiempo de Entrega	BOOLEANO	2026-08-02 03:11:17.845077+00
4	Número de Revisiones	FECHA	2026-08-02 03:11:17.845077+00
5	Resolución	LISTA	2026-08-02 03:11:17.845077+00
6	Tamaño de Archivo	TEXTO	2026-08-02 03:11:17.845077+00
7	Estilo Artístico	NUMERO	2026-08-02 03:11:17.845077+00
8	Software Utilizado	BOOLEANO	2026-08-02 03:11:17.845077+00
9	Duración del Video	FECHA	2026-08-02 03:11:17.845077+00
10	Número de Palabras	LISTA	2026-08-02 03:11:17.845077+00
11	Idioma	TEXTO	2026-08-02 03:11:17.845077+00
12	Licencia de Uso	NUMERO	2026-08-02 03:11:17.845077+00
13	Tipo de Archivo Fuente	BOOLEANO	2026-08-02 03:11:17.845077+00
14	Paleta de Colores	FECHA	2026-08-02 03:11:17.845077+00
15	Orientación	LISTA	2026-08-02 03:11:17.845077+00
16	Tamaño de Impresión	TEXTO	2026-08-02 03:11:17.845077+00
17	Tipo de Papel	NUMERO	2026-08-02 03:11:17.845077+00
18	Acabado	BOOLEANO	2026-08-02 03:11:17.845077+00
19	Cantidad de Conceptos	FECHA	2026-08-02 03:11:17.845077+00
20	Número de Páginas	LISTA	2026-08-02 03:11:17.845077+00
21	Duración de Audio	TEXTO	2026-08-02 03:11:17.845077+00
22	Formato de Audio	NUMERO	2026-08-02 03:11:17.845077+00
23	Tipo de Voz	BOOLEANO	2026-08-02 03:11:17.845077+00
24	Género Musical	FECHA	2026-08-02 03:11:17.845077+00
25	Tempo	LISTA	2026-08-02 03:11:17.845077+00
26	Tipo de Animación	TEXTO	2026-08-02 03:11:17.845077+00
27	Cuadros por Segundo	NUMERO	2026-08-02 03:11:17.845077+00
28	Resolución de Video	BOOLEANO	2026-08-02 03:11:17.845077+00
29	Codec de Video	FECHA	2026-08-02 03:11:17.845077+00
30	Tipo de Licencia Musical	LISTA	2026-08-02 03:11:17.845077+00
31	Nivel de Experiencia Requerido	TEXTO	2026-08-02 03:11:17.845077+00
32	Plataforma Objetivo	NUMERO	2026-08-02 03:11:17.845077+00
33	Sistema Operativo	BOOLEANO	2026-08-02 03:11:17.845077+00
34	Framework Utilizado	FECHA	2026-08-02 03:11:17.845077+00
35	Lenguaje de Programación	LISTA	2026-08-02 03:11:17.845077+00
36	Base de Datos	TEXTO	2026-08-02 03:11:17.845077+00
37	Tipo de API	NUMERO	2026-08-02 03:11:17.845077+00
38	Nivel de Seguridad	BOOLEANO	2026-08-02 03:11:17.845077+00
39	Tipo de Diseño	FECHA	2026-08-02 03:11:17.845077+00
40	Estilo Tipográfico	LISTA	2026-08-02 03:11:17.845077+00
41	Tamaño de Fuente	TEXTO	2026-08-02 03:11:17.845077+00
42	Formato de Logo	NUMERO	2026-08-02 03:11:17.845077+00
43	Variaciones de Logo	BOOLEANO	2026-08-02 03:11:17.845077+00
44	Tipo de Empaque	FECHA	2026-08-02 03:11:17.845077+00
45	Material	LISTA	2026-08-02 03:11:17.845077+00
46	Dimensiones Físicas	TEXTO	2026-08-02 03:11:17.845077+00
47	Tipo de Encuadernación	NUMERO	2026-08-02 03:11:17.845077+00
48	Cantidad de Fotos	BOOLEANO	2026-08-02 03:11:17.845077+00
49	Tipo de Retoque	FECHA	2026-08-02 03:11:17.845077+00
50	Formato de Entrega Final	LISTA	2026-08-02 03:11:17.845077+00
\.


--
-- TOC entry 4724 (class 0 OID 27856)
-- Dependencies: 221
-- Data for Name: autenticacion_dos_factores; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.autenticacion_dos_factores (id_2fa, id_usuario, llave_secreta, esta_habilitado) FROM stdin;
1	1	EE41FAAB785400350BCBF77E4D4EEC09	t
2	2	F54E95C7972DD35612290839A809E542	t
3	3	7D56E942902390E2019F2D9968A16589	f
4	4	B929269717301342A8C43EE5AB71879C	t
5	5	F7581718A43448523EC951A52CE8E9A7	t
6	6	C18DE73C3EFEFEE6785CECB3B79A344D	f
7	7	850C6AD38130EE8C533CF45237987C8C	t
8	8	D3B45C6D89F9A000F92595643F3D8E29	t
9	9	764292F28AC3711F176781C9DC01FB52	f
10	10	AB916079D45FD16AA4258D5EFABBCA39	t
11	11	EB11D161F3AE4352779ADF111CEBE825	t
12	12	9F0EE9014A468A30C6EA2025427CB46E	f
13	13	E9607B40C7C17ECBD68B56F7DDE55F25	t
14	14	DDC2076FDC19F7E8069CE6A0ADCC5F51	t
15	15	C54BB1824A6022AD60981644D75BC435	f
16	16	96562E4FE5C7B7F129A7659A42A34EEC	t
17	17	97DE3CF0B67D35E1D1AA3664CF7F6EC3	t
18	18	E3DA2F1596FBDFBC417831628D6C06C8	f
19	19	4F6246D524CB1FC7911C69064BED3ACF	t
20	20	D3FD87DD302F77370631B084D25BD7E8	t
21	21	AF13E9F196EE27C15A7C7B7D49BB56E9	f
22	22	DD5829825CEF77320564EEC4BEFA7421	t
23	23	B469D843F9BA43F38B4AB09AE731296C	t
24	24	63EEA70B112C6BC1B952375F41A4358D	f
25	25	85EC38517FB9C955FA936C03140EF847	t
26	26	E505C8D456C516A9DF80997908A0E336	t
27	27	2368B48AA59C543D311B33CA3F097BD9	f
28	28	4565985A4F6F3F9AF6EBA4C902822490	t
29	29	7FF0FAA6699A76A55D102FA42FE12ED6	t
30	30	41879401835D81FBD7561DC0596D3048	f
31	31	BBEEE8630F3C6BBF7DE41B4F75206969	t
32	32	92E5CA3A0464FD5F4072E79C1C38159C	t
33	33	40346CDDF0D2C280ADDE1F10241E8B06	f
34	34	CA3FEEDFA26F8CA55008E197E5D94C6C	t
35	35	2CDE5349FDA7EDE81F54DFA6EFAFCEF7	t
36	36	C3248B43372FBF9866B0CE695D71D808	f
37	37	3B8188E12AE3F8BCE5FB62AB6A5DE811	t
38	38	BB16B1091499BA466402B2F19FC00974	t
39	39	4AE9B5CE35258EEB164D2FDE28BED877	f
40	40	51B21501763356E9CC3AED92AC0AE1A8	t
41	41	1637DA02ABDA93B4264E0ACD02A912FE	t
42	42	F6FB6A44C1FD629C48DBB37C222D6BB8	f
43	43	34906795717D18F303CAA5D093AAFAC1	t
44	44	AE8D372F66FA1AB6FDB1096EA674172B	t
45	45	F63DABEC04E5C78E427ECBB26921910B	f
46	46	D7190973AC53C6F7163501A9BE0A7939	t
47	47	CC025FAE50BEB3F3A75B7FC73EA03710	t
48	48	42E4E7C585021629B8FE29490F43F80F	f
49	49	EAA317162CDDC950813E519D695A3D73	t
50	50	0D7ED7D81ADC4346FC5E58BF3D1C4801	t
51	61	A47FV3VGV2XK5URKCME72M62TTZXBD2G	f
\.


--
-- TOC entry 4726 (class 0 OID 27864)
-- Dependencies: 223
-- Data for Name: briefing_enviados; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.briefing_enviados (id_briefing_enviado, id_pedido, id_briefing_plantilla, fecha_envio, completado) FROM stdin;
1	1	1	2026-07-31 22:11:17.845077	t
2	2	2	2026-07-30 22:11:17.845077	t
3	3	3	2026-07-29 22:11:17.845077	f
4	4	4	2026-07-28 22:11:17.845077	t
5	5	5	2026-07-27 22:11:17.845077	t
6	6	6	2026-07-26 22:11:17.845077	f
7	7	7	2026-07-25 22:11:17.845077	t
8	8	8	2026-07-24 22:11:17.845077	t
9	9	9	2026-07-23 22:11:17.845077	f
10	10	10	2026-07-22 22:11:17.845077	t
11	11	11	2026-07-21 22:11:17.845077	t
12	12	12	2026-07-20 22:11:17.845077	f
13	13	13	2026-07-19 22:11:17.845077	t
14	14	14	2026-07-18 22:11:17.845077	t
15	15	15	2026-07-17 22:11:17.845077	f
16	16	16	2026-07-16 22:11:17.845077	t
17	17	17	2026-07-15 22:11:17.845077	t
18	18	18	2026-07-14 22:11:17.845077	f
19	19	19	2026-07-13 22:11:17.845077	t
20	20	20	2026-07-12 22:11:17.845077	t
21	21	21	2026-07-11 22:11:17.845077	f
22	22	22	2026-07-10 22:11:17.845077	t
23	23	23	2026-07-09 22:11:17.845077	t
24	24	24	2026-07-08 22:11:17.845077	f
25	25	25	2026-07-07 22:11:17.845077	t
26	26	26	2026-07-06 22:11:17.845077	t
27	27	27	2026-07-05 22:11:17.845077	f
28	28	28	2026-07-04 22:11:17.845077	t
29	29	29	2026-07-03 22:11:17.845077	t
30	30	30	2026-07-02 22:11:17.845077	f
31	31	31	2026-07-01 22:11:17.845077	t
32	32	32	2026-06-30 22:11:17.845077	t
33	33	33	2026-06-29 22:11:17.845077	f
34	34	34	2026-06-28 22:11:17.845077	t
35	35	35	2026-06-27 22:11:17.845077	t
36	36	36	2026-06-26 22:11:17.845077	f
37	37	37	2026-06-25 22:11:17.845077	t
38	38	38	2026-06-24 22:11:17.845077	t
39	39	39	2026-06-23 22:11:17.845077	f
40	40	40	2026-06-22 22:11:17.845077	t
41	41	41	2026-06-21 22:11:17.845077	t
42	42	42	2026-06-20 22:11:17.845077	f
43	43	43	2026-06-19 22:11:17.845077	t
44	44	44	2026-06-18 22:11:17.845077	t
45	45	45	2026-06-17 22:11:17.845077	f
46	46	46	2026-06-16 22:11:17.845077	t
47	47	47	2026-06-15 22:11:17.845077	t
48	48	48	2026-06-14 22:11:17.845077	f
49	49	49	2026-06-13 22:11:17.845077	t
50	50	50	2026-06-12 22:11:17.845077	t
51	51	51	2026-06-11 22:11:17.845077	f
52	52	52	2026-06-10 22:11:17.845077	t
53	53	53	2026-06-09 22:11:17.845077	t
54	54	54	2026-06-08 22:11:17.845077	f
55	55	55	2026-06-07 22:11:17.845077	t
56	56	1	2026-06-06 22:11:17.845077	t
57	57	2	2026-06-05 22:11:17.845077	f
58	58	3	2026-06-04 22:11:17.845077	t
59	59	4	2026-06-03 22:11:17.845077	t
60	60	5	2026-06-02 22:11:17.845077	f
\.


--
-- TOC entry 4728 (class 0 OID 27875)
-- Dependencies: 225
-- Data for Name: briefing_plantillas; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.briefing_plantillas (id_briefing_plantilla, id_perfil, nombre_plantilla, fecha_creacion) FROM stdin;
1	1	Plantilla de Briefing 1	2026-07-31 22:11:17.845077
2	2	Plantilla de Briefing 2	2026-07-30 22:11:17.845077
3	3	Plantilla de Briefing 3	2026-07-29 22:11:17.845077
4	4	Plantilla de Briefing 4	2026-07-28 22:11:17.845077
5	5	Plantilla de Briefing 5	2026-07-27 22:11:17.845077
6	6	Plantilla de Briefing 6	2026-07-26 22:11:17.845077
7	7	Plantilla de Briefing 7	2026-07-25 22:11:17.845077
8	8	Plantilla de Briefing 8	2026-07-24 22:11:17.845077
9	9	Plantilla de Briefing 9	2026-07-23 22:11:17.845077
10	10	Plantilla de Briefing 10	2026-07-22 22:11:17.845077
11	11	Plantilla de Briefing 11	2026-07-21 22:11:17.845077
12	12	Plantilla de Briefing 12	2026-07-20 22:11:17.845077
13	13	Plantilla de Briefing 13	2026-07-19 22:11:17.845077
14	14	Plantilla de Briefing 14	2026-07-18 22:11:17.845077
15	15	Plantilla de Briefing 15	2026-07-17 22:11:17.845077
16	16	Plantilla de Briefing 16	2026-07-16 22:11:17.845077
17	17	Plantilla de Briefing 17	2026-07-15 22:11:17.845077
18	18	Plantilla de Briefing 18	2026-07-14 22:11:17.845077
19	19	Plantilla de Briefing 19	2026-07-13 22:11:17.845077
20	20	Plantilla de Briefing 20	2026-07-12 22:11:17.845077
21	21	Plantilla de Briefing 21	2026-07-11 22:11:17.845077
22	22	Plantilla de Briefing 22	2026-07-10 22:11:17.845077
23	23	Plantilla de Briefing 23	2026-07-09 22:11:17.845077
24	24	Plantilla de Briefing 24	2026-07-08 22:11:17.845077
25	25	Plantilla de Briefing 25	2026-07-07 22:11:17.845077
26	26	Plantilla de Briefing 26	2026-07-06 22:11:17.845077
27	27	Plantilla de Briefing 27	2026-07-05 22:11:17.845077
28	28	Plantilla de Briefing 28	2026-07-04 22:11:17.845077
29	29	Plantilla de Briefing 29	2026-07-03 22:11:17.845077
30	30	Plantilla de Briefing 30	2026-07-02 22:11:17.845077
31	31	Plantilla de Briefing 31	2026-07-01 22:11:17.845077
32	32	Plantilla de Briefing 32	2026-06-30 22:11:17.845077
33	33	Plantilla de Briefing 33	2026-06-29 22:11:17.845077
34	34	Plantilla de Briefing 34	2026-06-28 22:11:17.845077
35	35	Plantilla de Briefing 35	2026-06-27 22:11:17.845077
36	36	Plantilla de Briefing 36	2026-06-26 22:11:17.845077
37	37	Plantilla de Briefing 37	2026-06-25 22:11:17.845077
38	38	Plantilla de Briefing 38	2026-06-24 22:11:17.845077
39	39	Plantilla de Briefing 39	2026-06-23 22:11:17.845077
40	40	Plantilla de Briefing 40	2026-06-22 22:11:17.845077
41	41	Plantilla de Briefing 41	2026-06-21 22:11:17.845077
42	42	Plantilla de Briefing 42	2026-06-20 22:11:17.845077
43	43	Plantilla de Briefing 43	2026-06-19 22:11:17.845077
44	44	Plantilla de Briefing 44	2026-06-18 22:11:17.845077
45	45	Plantilla de Briefing 45	2026-06-17 22:11:17.845077
46	46	Plantilla de Briefing 46	2026-06-16 22:11:17.845077
47	47	Plantilla de Briefing 47	2026-06-15 22:11:17.845077
48	48	Plantilla de Briefing 48	2026-06-14 22:11:17.845077
49	49	Plantilla de Briefing 49	2026-06-13 22:11:17.845077
50	50	Plantilla de Briefing 50	2026-06-12 22:11:17.845077
51	1	Plantilla de Briefing 51	2026-06-11 22:11:17.845077
52	2	Plantilla de Briefing 52	2026-06-10 22:11:17.845077
53	3	Plantilla de Briefing 53	2026-06-09 22:11:17.845077
54	4	Plantilla de Briefing 54	2026-06-08 22:11:17.845077
55	5	Plantilla de Briefing 55	2026-06-07 22:11:17.845077
\.


--
-- TOC entry 4730 (class 0 OID 27884)
-- Dependencies: 227
-- Data for Name: briefing_preguntas; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.briefing_preguntas (id_pregunta, id_briefing_plantilla, texto_pregunta, numero_orden) FROM stdin;
1	1	¿Cuáles son los requisitos específicos para el proyecto (pregunta 1)?	1
2	2	¿Cuáles son los requisitos específicos para el proyecto (pregunta 2)?	2
3	3	¿Cuáles son los requisitos específicos para el proyecto (pregunta 3)?	3
4	4	¿Cuáles son los requisitos específicos para el proyecto (pregunta 4)?	4
5	5	¿Cuáles son los requisitos específicos para el proyecto (pregunta 5)?	5
6	6	¿Cuáles son los requisitos específicos para el proyecto (pregunta 6)?	6
7	7	¿Cuáles son los requisitos específicos para el proyecto (pregunta 7)?	7
8	8	¿Cuáles son los requisitos específicos para el proyecto (pregunta 8)?	8
9	9	¿Cuáles son los requisitos específicos para el proyecto (pregunta 9)?	9
10	10	¿Cuáles son los requisitos específicos para el proyecto (pregunta 10)?	10
11	11	¿Cuáles son los requisitos específicos para el proyecto (pregunta 11)?	1
12	12	¿Cuáles son los requisitos específicos para el proyecto (pregunta 12)?	2
13	13	¿Cuáles son los requisitos específicos para el proyecto (pregunta 13)?	3
14	14	¿Cuáles son los requisitos específicos para el proyecto (pregunta 14)?	4
15	15	¿Cuáles son los requisitos específicos para el proyecto (pregunta 15)?	5
16	16	¿Cuáles son los requisitos específicos para el proyecto (pregunta 16)?	6
17	17	¿Cuáles son los requisitos específicos para el proyecto (pregunta 17)?	7
18	18	¿Cuáles son los requisitos específicos para el proyecto (pregunta 18)?	8
19	19	¿Cuáles son los requisitos específicos para el proyecto (pregunta 19)?	9
20	20	¿Cuáles son los requisitos específicos para el proyecto (pregunta 20)?	10
21	21	¿Cuáles son los requisitos específicos para el proyecto (pregunta 21)?	1
22	22	¿Cuáles son los requisitos específicos para el proyecto (pregunta 22)?	2
23	23	¿Cuáles son los requisitos específicos para el proyecto (pregunta 23)?	3
24	24	¿Cuáles son los requisitos específicos para el proyecto (pregunta 24)?	4
25	25	¿Cuáles son los requisitos específicos para el proyecto (pregunta 25)?	5
26	26	¿Cuáles son los requisitos específicos para el proyecto (pregunta 26)?	6
27	27	¿Cuáles son los requisitos específicos para el proyecto (pregunta 27)?	7
28	28	¿Cuáles son los requisitos específicos para el proyecto (pregunta 28)?	8
29	29	¿Cuáles son los requisitos específicos para el proyecto (pregunta 29)?	9
30	30	¿Cuáles son los requisitos específicos para el proyecto (pregunta 30)?	10
31	31	¿Cuáles son los requisitos específicos para el proyecto (pregunta 31)?	1
32	32	¿Cuáles son los requisitos específicos para el proyecto (pregunta 32)?	2
33	33	¿Cuáles son los requisitos específicos para el proyecto (pregunta 33)?	3
34	34	¿Cuáles son los requisitos específicos para el proyecto (pregunta 34)?	4
35	35	¿Cuáles son los requisitos específicos para el proyecto (pregunta 35)?	5
36	36	¿Cuáles son los requisitos específicos para el proyecto (pregunta 36)?	6
37	37	¿Cuáles son los requisitos específicos para el proyecto (pregunta 37)?	7
38	38	¿Cuáles son los requisitos específicos para el proyecto (pregunta 38)?	8
39	39	¿Cuáles son los requisitos específicos para el proyecto (pregunta 39)?	9
40	40	¿Cuáles son los requisitos específicos para el proyecto (pregunta 40)?	10
41	41	¿Cuáles son los requisitos específicos para el proyecto (pregunta 41)?	1
42	42	¿Cuáles son los requisitos específicos para el proyecto (pregunta 42)?	2
43	43	¿Cuáles son los requisitos específicos para el proyecto (pregunta 43)?	3
44	44	¿Cuáles son los requisitos específicos para el proyecto (pregunta 44)?	4
45	45	¿Cuáles son los requisitos específicos para el proyecto (pregunta 45)?	5
46	46	¿Cuáles son los requisitos específicos para el proyecto (pregunta 46)?	6
47	47	¿Cuáles son los requisitos específicos para el proyecto (pregunta 47)?	7
48	48	¿Cuáles son los requisitos específicos para el proyecto (pregunta 48)?	8
49	49	¿Cuáles son los requisitos específicos para el proyecto (pregunta 49)?	9
50	50	¿Cuáles son los requisitos específicos para el proyecto (pregunta 50)?	10
51	51	¿Cuáles son los requisitos específicos para el proyecto (pregunta 51)?	1
52	52	¿Cuáles son los requisitos específicos para el proyecto (pregunta 52)?	2
53	53	¿Cuáles son los requisitos específicos para el proyecto (pregunta 53)?	3
54	54	¿Cuáles son los requisitos específicos para el proyecto (pregunta 54)?	4
55	55	¿Cuáles son los requisitos específicos para el proyecto (pregunta 55)?	5
56	1	¿Cuáles son los requisitos específicos para el proyecto (pregunta 56)?	6
57	2	¿Cuáles son los requisitos específicos para el proyecto (pregunta 57)?	7
58	3	¿Cuáles son los requisitos específicos para el proyecto (pregunta 58)?	8
59	4	¿Cuáles son los requisitos específicos para el proyecto (pregunta 59)?	9
60	5	¿Cuáles son los requisitos específicos para el proyecto (pregunta 60)?	10
61	6	¿Cuáles son los requisitos específicos para el proyecto (pregunta 61)?	1
62	7	¿Cuáles son los requisitos específicos para el proyecto (pregunta 62)?	2
63	8	¿Cuáles son los requisitos específicos para el proyecto (pregunta 63)?	3
64	9	¿Cuáles son los requisitos específicos para el proyecto (pregunta 64)?	4
65	10	¿Cuáles son los requisitos específicos para el proyecto (pregunta 65)?	5
66	11	¿Cuáles son los requisitos específicos para el proyecto (pregunta 66)?	6
67	12	¿Cuáles son los requisitos específicos para el proyecto (pregunta 67)?	7
68	13	¿Cuáles son los requisitos específicos para el proyecto (pregunta 68)?	8
69	14	¿Cuáles son los requisitos específicos para el proyecto (pregunta 69)?	9
70	15	¿Cuáles son los requisitos específicos para el proyecto (pregunta 70)?	10
71	16	¿Cuáles son los requisitos específicos para el proyecto (pregunta 71)?	1
72	17	¿Cuáles son los requisitos específicos para el proyecto (pregunta 72)?	2
73	18	¿Cuáles son los requisitos específicos para el proyecto (pregunta 73)?	3
74	19	¿Cuáles son los requisitos específicos para el proyecto (pregunta 74)?	4
75	20	¿Cuáles son los requisitos específicos para el proyecto (pregunta 75)?	5
76	21	¿Cuáles son los requisitos específicos para el proyecto (pregunta 76)?	6
77	22	¿Cuáles son los requisitos específicos para el proyecto (pregunta 77)?	7
78	23	¿Cuáles son los requisitos específicos para el proyecto (pregunta 78)?	8
79	24	¿Cuáles son los requisitos específicos para el proyecto (pregunta 79)?	9
80	25	¿Cuáles son los requisitos específicos para el proyecto (pregunta 80)?	10
\.


--
-- TOC entry 4732 (class 0 OID 27895)
-- Dependencies: 229
-- Data for Name: briefing_respuestas; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.briefing_respuestas (id_respuesta, id_briefing_enviado, id_pregunta, texto_respuesta, fecha_respuesta) FROM stdin;
1	1	1	Respuesta detallada del cliente número 1 al briefing.	2026-07-31 22:11:17.845077
2	2	1	Respuesta detallada del cliente número 2 al briefing.	2026-07-30 22:11:17.845077
3	3	1	Respuesta detallada del cliente número 3 al briefing.	2026-07-29 22:11:17.845077
4	4	1	Respuesta detallada del cliente número 4 al briefing.	2026-07-28 22:11:17.845077
5	5	1	Respuesta detallada del cliente número 5 al briefing.	2026-07-27 22:11:17.845077
6	6	1	Respuesta detallada del cliente número 6 al briefing.	2026-07-26 22:11:17.845077
7	7	1	Respuesta detallada del cliente número 7 al briefing.	2026-07-25 22:11:17.845077
8	8	1	Respuesta detallada del cliente número 8 al briefing.	2026-07-24 22:11:17.845077
9	9	1	Respuesta detallada del cliente número 9 al briefing.	2026-07-23 22:11:17.845077
10	10	1	Respuesta detallada del cliente número 10 al briefing.	2026-07-22 22:11:17.845077
11	11	1	Respuesta detallada del cliente número 11 al briefing.	2026-07-21 22:11:17.845077
12	12	1	Respuesta detallada del cliente número 12 al briefing.	2026-07-20 22:11:17.845077
13	13	1	Respuesta detallada del cliente número 13 al briefing.	2026-07-19 22:11:17.845077
14	14	1	Respuesta detallada del cliente número 14 al briefing.	2026-07-18 22:11:17.845077
15	15	1	Respuesta detallada del cliente número 15 al briefing.	2026-07-17 22:11:17.845077
16	16	1	Respuesta detallada del cliente número 16 al briefing.	2026-07-16 22:11:17.845077
17	17	1	Respuesta detallada del cliente número 17 al briefing.	2026-07-15 22:11:17.845077
18	18	1	Respuesta detallada del cliente número 18 al briefing.	2026-07-14 22:11:17.845077
19	19	1	Respuesta detallada del cliente número 19 al briefing.	2026-07-13 22:11:17.845077
20	20	1	Respuesta detallada del cliente número 20 al briefing.	2026-07-12 22:11:17.845077
21	21	1	Respuesta detallada del cliente número 21 al briefing.	2026-07-11 22:11:17.845077
22	22	1	Respuesta detallada del cliente número 22 al briefing.	2026-07-10 22:11:17.845077
23	23	1	Respuesta detallada del cliente número 23 al briefing.	2026-07-09 22:11:17.845077
24	24	1	Respuesta detallada del cliente número 24 al briefing.	2026-07-08 22:11:17.845077
25	25	1	Respuesta detallada del cliente número 25 al briefing.	2026-07-07 22:11:17.845077
26	26	1	Respuesta detallada del cliente número 26 al briefing.	2026-07-06 22:11:17.845077
27	27	1	Respuesta detallada del cliente número 27 al briefing.	2026-07-05 22:11:17.845077
28	28	1	Respuesta detallada del cliente número 28 al briefing.	2026-07-04 22:11:17.845077
29	29	1	Respuesta detallada del cliente número 29 al briefing.	2026-07-03 22:11:17.845077
30	30	1	Respuesta detallada del cliente número 30 al briefing.	2026-07-02 22:11:17.845077
31	31	1	Respuesta detallada del cliente número 31 al briefing.	2026-07-01 22:11:17.845077
32	32	1	Respuesta detallada del cliente número 32 al briefing.	2026-06-30 22:11:17.845077
33	33	1	Respuesta detallada del cliente número 33 al briefing.	2026-06-29 22:11:17.845077
34	34	1	Respuesta detallada del cliente número 34 al briefing.	2026-06-28 22:11:17.845077
35	35	1	Respuesta detallada del cliente número 35 al briefing.	2026-06-27 22:11:17.845077
36	36	1	Respuesta detallada del cliente número 36 al briefing.	2026-06-26 22:11:17.845077
37	37	1	Respuesta detallada del cliente número 37 al briefing.	2026-06-25 22:11:17.845077
38	38	1	Respuesta detallada del cliente número 38 al briefing.	2026-06-24 22:11:17.845077
39	39	1	Respuesta detallada del cliente número 39 al briefing.	2026-06-23 22:11:17.845077
40	40	1	Respuesta detallada del cliente número 40 al briefing.	2026-06-22 22:11:17.845077
41	41	1	Respuesta detallada del cliente número 41 al briefing.	2026-06-21 22:11:17.845077
42	42	1	Respuesta detallada del cliente número 42 al briefing.	2026-06-20 22:11:17.845077
43	43	1	Respuesta detallada del cliente número 43 al briefing.	2026-06-19 22:11:17.845077
44	44	1	Respuesta detallada del cliente número 44 al briefing.	2026-06-18 22:11:17.845077
45	45	1	Respuesta detallada del cliente número 45 al briefing.	2026-06-17 22:11:17.845077
46	46	1	Respuesta detallada del cliente número 46 al briefing.	2026-06-16 22:11:17.845077
47	47	1	Respuesta detallada del cliente número 47 al briefing.	2026-06-15 22:11:17.845077
48	48	1	Respuesta detallada del cliente número 48 al briefing.	2026-06-14 22:11:17.845077
49	49	1	Respuesta detallada del cliente número 49 al briefing.	2026-06-13 22:11:17.845077
50	50	1	Respuesta detallada del cliente número 50 al briefing.	2026-06-12 22:11:17.845077
51	51	1	Respuesta detallada del cliente número 51 al briefing.	2026-06-11 22:11:17.845077
52	52	1	Respuesta detallada del cliente número 52 al briefing.	2026-06-10 22:11:17.845077
53	53	1	Respuesta detallada del cliente número 53 al briefing.	2026-06-09 22:11:17.845077
54	54	1	Respuesta detallada del cliente número 54 al briefing.	2026-06-08 22:11:17.845077
55	55	1	Respuesta detallada del cliente número 55 al briefing.	2026-06-07 22:11:17.845077
56	56	1	Respuesta detallada del cliente número 56 al briefing.	2026-06-06 22:11:17.845077
57	57	1	Respuesta detallada del cliente número 57 al briefing.	2026-06-05 22:11:17.845077
58	58	1	Respuesta detallada del cliente número 58 al briefing.	2026-06-04 22:11:17.845077
59	59	1	Respuesta detallada del cliente número 59 al briefing.	2026-06-03 22:11:17.845077
60	60	1	Respuesta detallada del cliente número 60 al briefing.	2026-06-02 22:11:17.845077
61	1	2	Respuesta detallada del cliente número 61 al briefing.	2026-06-01 22:11:17.845077
62	2	2	Respuesta detallada del cliente número 62 al briefing.	2026-05-31 22:11:17.845077
63	3	2	Respuesta detallada del cliente número 63 al briefing.	2026-05-30 22:11:17.845077
64	4	2	Respuesta detallada del cliente número 64 al briefing.	2026-05-29 22:11:17.845077
65	5	2	Respuesta detallada del cliente número 65 al briefing.	2026-05-28 22:11:17.845077
66	6	2	Respuesta detallada del cliente número 66 al briefing.	2026-05-27 22:11:17.845077
67	7	2	Respuesta detallada del cliente número 67 al briefing.	2026-05-26 22:11:17.845077
68	8	2	Respuesta detallada del cliente número 68 al briefing.	2026-05-25 22:11:17.845077
69	9	2	Respuesta detallada del cliente número 69 al briefing.	2026-05-24 22:11:17.845077
70	10	2	Respuesta detallada del cliente número 70 al briefing.	2026-05-23 22:11:17.845077
71	11	2	Respuesta detallada del cliente número 71 al briefing.	2026-05-22 22:11:17.845077
72	12	2	Respuesta detallada del cliente número 72 al briefing.	2026-05-21 22:11:17.845077
73	13	2	Respuesta detallada del cliente número 73 al briefing.	2026-05-20 22:11:17.845077
74	14	2	Respuesta detallada del cliente número 74 al briefing.	2026-05-19 22:11:17.845077
75	15	2	Respuesta detallada del cliente número 75 al briefing.	2026-05-18 22:11:17.845077
76	16	2	Respuesta detallada del cliente número 76 al briefing.	2026-05-17 22:11:17.845077
77	17	2	Respuesta detallada del cliente número 77 al briefing.	2026-05-16 22:11:17.845077
78	18	2	Respuesta detallada del cliente número 78 al briefing.	2026-05-15 22:11:17.845077
79	19	2	Respuesta detallada del cliente número 79 al briefing.	2026-05-14 22:11:17.845077
80	20	2	Respuesta detallada del cliente número 80 al briefing.	2026-05-13 22:11:17.845077
\.


--
-- TOC entry 4734 (class 0 OID 27907)
-- Dependencies: 231
-- Data for Name: categorias; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.categorias (id_categoria, nombre_categoria, estado_activa, actualizado_en, id_flujo) FROM stdin;
1	Diseño Gráfico	t	2026-08-16 16:32:27.13782+00	1
2	Ilustración	t	2026-08-16 16:32:27.13782+00	1
3	Marketing Digital	t	2026-08-16 16:32:27.13782+00	1
4	Escritura y Traducción	t	2026-08-16 16:32:27.13782+00	1
5	Video y Animación	t	2026-08-16 16:32:27.13782+00	1
6	Música y Audio	t	2026-08-16 16:32:27.13782+00	1
7	Programación y Tecnología	t	2026-08-16 16:32:27.13782+00	1
8	Negocios	t	2026-08-16 16:32:27.13782+00	1
9	Estilo de Vida	f	2026-08-16 16:32:27.13782+00	1
10	Fotografía	t	2026-08-16 16:32:27.13782+00	1
11	Diseño de Moda	t	2026-08-16 16:32:27.13782+00	1
12	Arquitectura	t	2026-08-16 16:32:27.13782+00	1
13	Diseño Industrial	t	2026-08-16 16:32:27.13782+00	1
14	Consultoría	t	2026-08-16 16:32:27.13782+00	1
15	Educación y Cursos	t	2026-08-16 16:32:27.13782+00	1
16	Diseño Web	t	2026-08-16 16:32:27.13782+00	1
17	Desarrollo de Apps	t	2026-08-16 16:32:27.13782+00	1
18	Inteligencia Artificial	f	2026-08-16 16:32:27.13782+00	1
19	Diseño de Videojuegos	t	2026-08-16 16:32:27.13782+00	1
20	Diseño de Interiores	t	2026-08-16 16:32:27.13782+00	1
21	Publicidad	t	2026-08-16 16:32:27.13782+00	1
22	Redes Sociales	t	2026-08-16 16:32:27.13782+00	1
23	Branding	t	2026-08-16 16:32:27.13782+00	1
24	Diseño de Empaques	t	2026-08-16 16:32:27.13782+00	1
25	Diseño Editorial	t	2026-08-16 16:32:27.13782+00	1
26	Diseño de Joyas	t	2026-08-16 16:32:27.13782+00	1
27	Arte Digital	f	2026-08-16 16:32:27.13782+00	1
28	Escultura	t	2026-08-16 16:32:27.13782+00	1
29	Pintura	t	2026-08-16 16:32:27.13782+00	1
30	Caligrafía	t	2026-08-16 16:32:27.13782+00	1
31	Diseño de Tatuajes	t	2026-08-16 16:32:27.13782+00	1
32	Producción Musical	t	2026-08-16 16:32:27.13782+00	1
33	Locución y Doblaje	t	2026-08-16 16:32:27.13782+00	1
34	Diseño de Presentaciones	t	2026-08-16 16:32:27.13782+00	1
35	SEO y SEM	t	2026-08-16 16:32:27.13782+00	1
36	Comercio Electrónico	f	2026-08-16 16:32:27.13782+00	1
37	Diseño de Merchandising	t	2026-08-16 16:32:27.13782+00	1
38	Diseño de Stands	t	2026-08-16 16:32:27.13782+00	1
39	Diseño de Cartelería	t	2026-08-16 16:32:27.13782+00	1
40	Fotografía de Producto	t	2026-08-16 16:32:27.13782+00	1
41	Retoque Fotográfico	t	2026-08-16 16:32:27.13782+00	1
42	Modelado 3D	t	2026-08-16 16:32:27.13782+00	1
43	Realidad Virtual	t	2026-08-16 16:32:27.13782+00	1
44	Diseño de Iconos	t	2026-08-16 16:32:27.13782+00	1
45	Infografías	f	2026-08-16 16:32:27.13782+00	1
46	Storyboards	t	2026-08-16 16:32:27.13782+00	1
47	Motion Graphics	t	2026-08-16 16:32:27.13782+00	1
48	Diseño de Personajes	t	2026-08-16 16:32:27.13782+00	1
49	Concept Art	t	2026-08-16 16:32:27.13782+00	1
50	Rigging y Animación	t	2026-08-16 16:32:27.13782+00	1
\.


--
-- TOC entry 4736 (class 0 OID 27915)
-- Dependencies: 233
-- Data for Name: certificados_ia; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.certificados_ia (id_certificado, id_perfil, id_estado_verificacion, url_documento_s3, puntaje_confianza_ia, fecha_analisis, tipo_documento, hash_documento, veredicto_ia, razon_ia, datos_extraidos_ia, fecha_dictamen_ia, id_moderador, fecha_decision, nota_moderador, documento_eliminado) FROM stdin;
1	1	1	https://s3.example.com/certificados/doc1.pdf	11.70	2026-07-31 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
2	2	2	https://s3.example.com/certificados/doc2.pdf	13.40	2026-07-30 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
3	3	3	https://s3.example.com/certificados/doc3.pdf	15.10	2026-07-29 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
4	4	4	https://s3.example.com/certificados/doc4.pdf	16.80	2026-07-28 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
5	5	5	https://s3.example.com/certificados/doc5.pdf	18.50	2026-07-27 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
6	6	6	https://s3.example.com/certificados/doc6.pdf	20.20	2026-07-26 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
7	7	7	https://s3.example.com/certificados/doc7.pdf	21.90	2026-07-25 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
8	8	8	https://s3.example.com/certificados/doc8.pdf	23.60	2026-07-24 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
9	9	9	https://s3.example.com/certificados/doc9.pdf	25.30	2026-07-23 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
10	10	10	https://s3.example.com/certificados/doc10.pdf	27.00	2026-07-22 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
11	11	11	https://s3.example.com/certificados/doc11.pdf	28.70	2026-07-21 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
12	12	12	https://s3.example.com/certificados/doc12.pdf	30.40	2026-07-20 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
13	13	13	https://s3.example.com/certificados/doc13.pdf	32.10	2026-07-19 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
14	14	14	https://s3.example.com/certificados/doc14.pdf	33.80	2026-07-18 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
15	15	15	https://s3.example.com/certificados/doc15.pdf	35.50	2026-07-17 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
16	16	16	https://s3.example.com/certificados/doc16.pdf	37.20	2026-07-16 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
17	17	17	https://s3.example.com/certificados/doc17.pdf	38.90	2026-07-15 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
18	18	18	https://s3.example.com/certificados/doc18.pdf	40.60	2026-07-14 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
19	19	19	https://s3.example.com/certificados/doc19.pdf	42.30	2026-07-13 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
20	20	20	https://s3.example.com/certificados/doc20.pdf	44.00	2026-07-12 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
21	21	21	https://s3.example.com/certificados/doc21.pdf	45.70	2026-07-11 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
22	22	22	https://s3.example.com/certificados/doc22.pdf	47.40	2026-07-10 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
23	23	23	https://s3.example.com/certificados/doc23.pdf	49.10	2026-07-09 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
24	24	24	https://s3.example.com/certificados/doc24.pdf	50.80	2026-07-08 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
25	25	25	https://s3.example.com/certificados/doc25.pdf	52.50	2026-07-07 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
26	26	26	https://s3.example.com/certificados/doc26.pdf	54.20	2026-07-06 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
27	27	27	https://s3.example.com/certificados/doc27.pdf	55.90	2026-07-05 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
28	28	28	https://s3.example.com/certificados/doc28.pdf	57.60	2026-07-04 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
29	29	29	https://s3.example.com/certificados/doc29.pdf	59.30	2026-07-03 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
30	30	30	https://s3.example.com/certificados/doc30.pdf	61.00	2026-07-02 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
31	31	31	https://s3.example.com/certificados/doc31.pdf	62.70	2026-07-01 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
32	32	32	https://s3.example.com/certificados/doc32.pdf	64.40	2026-06-30 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
33	33	33	https://s3.example.com/certificados/doc33.pdf	66.10	2026-06-29 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
34	34	34	https://s3.example.com/certificados/doc34.pdf	67.80	2026-06-28 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
35	35	35	https://s3.example.com/certificados/doc35.pdf	69.50	2026-06-27 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
36	36	36	https://s3.example.com/certificados/doc36.pdf	71.20	2026-06-26 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
37	37	37	https://s3.example.com/certificados/doc37.pdf	72.90	2026-06-25 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
38	38	38	https://s3.example.com/certificados/doc38.pdf	74.60	2026-06-24 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
39	39	39	https://s3.example.com/certificados/doc39.pdf	76.30	2026-06-23 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
40	40	40	https://s3.example.com/certificados/doc40.pdf	78.00	2026-06-22 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
41	41	41	https://s3.example.com/certificados/doc41.pdf	79.70	2026-06-21 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
42	42	42	https://s3.example.com/certificados/doc42.pdf	81.40	2026-06-20 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
43	43	43	https://s3.example.com/certificados/doc43.pdf	83.10	2026-06-19 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
44	44	44	https://s3.example.com/certificados/doc44.pdf	84.80	2026-06-18 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
45	45	45	https://s3.example.com/certificados/doc45.pdf	86.50	2026-06-17 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
46	46	46	https://s3.example.com/certificados/doc46.pdf	88.20	2026-06-16 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
47	47	47	https://s3.example.com/certificados/doc47.pdf	89.90	2026-06-15 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
48	48	48	https://s3.example.com/certificados/doc48.pdf	91.60	2026-06-14 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
49	49	49	https://s3.example.com/certificados/doc49.pdf	93.30	2026-06-13 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
50	50	50	https://s3.example.com/certificados/doc50.pdf	95.00	2026-06-12 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
51	1	1	https://s3.example.com/certificados/doc51.pdf	96.70	2026-06-11 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
52	2	2	https://s3.example.com/certificados/doc52.pdf	98.40	2026-06-10 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
53	3	3	https://s3.example.com/certificados/doc53.pdf	11.10	2026-06-09 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
54	4	4	https://s3.example.com/certificados/doc54.pdf	12.80	2026-06-08 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
55	5	5	https://s3.example.com/certificados/doc55.pdf	14.50	2026-06-07 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
56	6	6	https://s3.example.com/certificados/doc56.pdf	16.20	2026-06-06 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
57	7	7	https://s3.example.com/certificados/doc57.pdf	17.90	2026-06-05 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
58	8	8	https://s3.example.com/certificados/doc58.pdf	19.60	2026-06-04 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
59	9	9	https://s3.example.com/certificados/doc59.pdf	21.30	2026-06-03 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
60	10	10	https://s3.example.com/certificados/doc60.pdf	23.00	2026-06-02 22:11:17.845077	IDENTIDAD	\N	\N	\N	\N	\N	\N	\N	\N	f
61	52	51	fbee13e1-1f21-4029-b965-c07fbf201254.jpg	0.88	2026-08-17 19:01:47.982391	CERTIFICADO	9f20614eb0d5040720ff3087c84755ad3c36fa753f5210455260fc81f981422b	SUGIERE_APROBAR	\N	{"nombreDetectado":"Usuario de Prueba","tipoDocumentoDetectado":"titulo_universitario","institucionEmisora":"Universidad de Prueba","campoEstudio":"Diseño Gráfico","fechaEmision":"2020-07-15"}	2026-08-17 19:06:48.799059	\N	\N	\N	f
\.


--
-- TOC entry 4738 (class 0 OID 27924)
-- Dependencies: 235
-- Data for Name: codigos_respaldo_2fa; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.codigos_respaldo_2fa (id_codigo, id_usuario, codigo_hash, usado) FROM stdin;
1	1	92997969e7bf8b1010327a95ae32969e	f
2	2	24a6b647f98d86ffe776b28bc4b6798b	f
3	3	5ba71a84d58fa3875947e4f2eba24f1a	f
4	4	cf93fcd675b358454eb63b8e7155a5d4	f
5	5	b78acc5eae1fa74816d7d67e99e45911	t
6	6	13019a7784705476d202006ca00d57a7	f
7	7	1fd05e43daa92c5409de9bfdc145c13c	f
8	8	f7ca9c3151a6d642ac66e69d7f962d7a	f
9	9	c9728fbe25ac2c3c148326562862a3c8	f
10	10	b1b8e9f38f94d94ecab5ba6827aa317c	t
11	11	63b16899e38fa77a2aa62b294c56cae6	f
12	12	d4410e2a8175690886a3ab20293f66e4	f
13	13	0817a5f12e0f3bdc6d93a31350b2bd15	f
14	14	6b20b35a19b52ce576ff92ec4db8eb15	f
15	15	e3f62f879fa6ca6423c35ac9a7a5d2a9	t
16	16	e70efd235924dc9e1e5addee54becd60	f
17	17	51d1c064bd09c930fffd36567041fa7d	f
18	18	c183fdd886d8a946a8572d8ee6f45ea1	f
19	19	65f488d4ff1878d2dc82f511b059c130	f
20	20	e0ebc7ead642e80412bddc00b2438c50	t
21	21	0121dbf6b149400c92510b3703c75fe0	f
22	22	3a1fc02ad813ebef31163409a96a43b4	f
23	23	1ecf7863af3d7e7c71df9a346760ff36	f
24	24	901af302101d94756b37b675bd97b7ce	f
25	25	676499f99a74d23ab8c669bf8c274720	t
26	26	16ad590ac5c2aec7ac1327844762cf3f	f
27	27	24f0856a53765d6e6410a77ec2bf2469	f
28	28	c18e7fc657eb796b587640c33d770acb	f
29	29	14c63b5abce8a60711157e8b4010556a	f
30	30	7e701b904e3fa3213a046029e8fedba8	t
31	31	a8ed612288751301bc11d4d85a4f7a89	f
32	32	e92065a034e5c14095f4241eebac18b4	f
33	33	41bfc4aab6208e7863885bdabefd4f56	f
34	34	a7e16ed9b62739342000250f6299e789	f
35	35	2ac30fce4440f9466bb427894fd388e7	t
36	36	7767d679bd42042b4022e2e7f8856f24	f
37	37	375cfbb1c9501af806d5e024c98dbfe6	f
38	38	46f30ab12b5da4a746cfc443f0697605	f
39	39	050bbe22e7cdbd606e9136a5d0c64579	f
40	40	edaaf3db0c16540097bea2ded6fb88f0	t
41	41	f28b032742d5ff8975cd449e8dc474d8	f
42	42	55ad42bd9357c01bd8cc0992382a3bd8	f
43	43	a48d02f574cffd72e95ab9ccf0693fc3	f
44	44	0698ddac719fef82c453519059f8ce8a	f
45	45	8a1269818320c8f562bd3cbe24ee4023	t
46	46	460ee2093676a13550cb0952771de0b2	f
47	47	7864ffbaf24e1374d835579927696ef1	f
48	48	1f8954f8809ffda78d9c3bc881779ed1	f
49	49	a545a4d8fa1fbda381a1f97ae9cc3e54	f
50	50	e8d43a7c84a8f82ab849d175537acf41	t
51	51	db3fe16664444b39a75e47e62dbad4b9	f
52	52	e5985ce22f98d2645edfd3a24749869a	f
53	53	200732026ac8be3907d0818dbb9de3b4	f
54	54	7b4ce6f021c91efdc5e637e15fb7a3a3	f
55	55	bd15a48d145972341bca03b8804073ed	t
56	56	7c57eb806cd256593ce1663bbcd02024	f
57	57	ed72ce4f2ea3ca4a432142b919a497bf	f
58	58	f7bf3d219cb46022ded4eee3b7526b6b	f
59	59	de9830bb729bce13d7a09cd332d77d67	f
60	60	62eedf63e363659c4370dbccc33e90c8	t
61	1	ba4e5161ecf3d3deaee1580204987851	f
62	2	70c4d21fba4344df5fc66ac169d3eb38	f
63	3	2c1ef5663a259e949e2a748c0992937b	f
64	4	a85812c0ae690d2b1f29cf70bade7235	f
65	5	d777dc511c7cb39489c0a08b8337b9c0	t
66	6	dcc1509374f6210dec691886cd185404	f
67	7	23dbe307219cdd64a68f9859376b8e00	f
68	8	b803102a776d7aae9a811d4c64138bf2	f
69	9	65e4c640c40dc7e158880d6917e1f6f9	f
70	10	5a30d39109f1efa95a99837b0d373b75	t
71	61	05cb64010b67999d2209e4af76c9a7fce80c9d99a1c93590e7b92ae956a15315	f
72	61	5775a3372b1a92130ec1499991ade37561997f79d1b1c7e2339f0ddc21f62063	f
73	61	e18daf51b60d7b61f63aa680659a62ea3d25a38997868ebf1cc561eb8641e9a4	f
74	61	eea4930a1b90c89e48762d2df7296ceaed9d10bc5c1d3d05927ba50d1ae38635	f
75	61	4dae7564a23dfcc2a94dbf5b1714ae6d4bbe2d5c01319e3ea660ba33e3171dfd	f
76	61	0b98d4f260ef5f9b90de8311305c23b02117ed3fad02a92fe03644d2a41e3e1d	f
77	61	f91415ea6cefc20c965993e36dca3f016f1eaffe045fa16374806a4d7d5b7ead	f
78	61	f9d4b18ab8ef5cbaec7cbf38e9a9c710dea1e17059d60c2928b49e8943969050	f
\.


--
-- TOC entry 4740 (class 0 OID 27933)
-- Dependencies: 237
-- Data for Name: comentarios_portafolio; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.comentarios_portafolio (id_comentario, id_item_portafolio, id_usuario_autor, texto_comentario, fecha_publicacion, estado_moderacion) FROM stdin;
1	1	1	Excelente trabajo, muy profesional. Comentario número 1.	2026-07-31 22:11:17.845077	Activo
2	2	2	Excelente trabajo, muy profesional. Comentario número 2.	2026-07-30 22:11:17.845077	Oculto
3	3	3	Excelente trabajo, muy profesional. Comentario número 3.	2026-07-29 22:11:17.845077	Reportado
4	4	4	Excelente trabajo, muy profesional. Comentario número 4.	2026-07-28 22:11:17.845077	Activo
5	5	5	Excelente trabajo, muy profesional. Comentario número 5.	2026-07-27 22:11:17.845077	Oculto
6	6	6	Excelente trabajo, muy profesional. Comentario número 6.	2026-07-26 22:11:17.845077	Reportado
7	7	7	Excelente trabajo, muy profesional. Comentario número 7.	2026-07-25 22:11:17.845077	Activo
8	8	8	Excelente trabajo, muy profesional. Comentario número 8.	2026-07-24 22:11:17.845077	Oculto
9	9	9	Excelente trabajo, muy profesional. Comentario número 9.	2026-07-23 22:11:17.845077	Reportado
10	10	10	Excelente trabajo, muy profesional. Comentario número 10.	2026-07-22 22:11:17.845077	Activo
11	11	11	Excelente trabajo, muy profesional. Comentario número 11.	2026-07-21 22:11:17.845077	Oculto
12	12	12	Excelente trabajo, muy profesional. Comentario número 12.	2026-07-20 22:11:17.845077	Reportado
13	13	13	Excelente trabajo, muy profesional. Comentario número 13.	2026-07-19 22:11:17.845077	Activo
14	14	14	Excelente trabajo, muy profesional. Comentario número 14.	2026-07-18 22:11:17.845077	Oculto
15	15	15	Excelente trabajo, muy profesional. Comentario número 15.	2026-07-17 22:11:17.845077	Reportado
16	16	16	Excelente trabajo, muy profesional. Comentario número 16.	2026-07-16 22:11:17.845077	Activo
17	17	17	Excelente trabajo, muy profesional. Comentario número 17.	2026-07-15 22:11:17.845077	Oculto
18	18	18	Excelente trabajo, muy profesional. Comentario número 18.	2026-07-14 22:11:17.845077	Reportado
19	19	19	Excelente trabajo, muy profesional. Comentario número 19.	2026-07-13 22:11:17.845077	Activo
20	20	20	Excelente trabajo, muy profesional. Comentario número 20.	2026-07-12 22:11:17.845077	Oculto
21	21	21	Excelente trabajo, muy profesional. Comentario número 21.	2026-07-11 22:11:17.845077	Reportado
22	22	22	Excelente trabajo, muy profesional. Comentario número 22.	2026-07-10 22:11:17.845077	Activo
23	23	23	Excelente trabajo, muy profesional. Comentario número 23.	2026-07-09 22:11:17.845077	Oculto
24	24	24	Excelente trabajo, muy profesional. Comentario número 24.	2026-07-08 22:11:17.845077	Reportado
25	25	25	Excelente trabajo, muy profesional. Comentario número 25.	2026-07-07 22:11:17.845077	Activo
26	26	26	Excelente trabajo, muy profesional. Comentario número 26.	2026-07-06 22:11:17.845077	Oculto
27	27	27	Excelente trabajo, muy profesional. Comentario número 27.	2026-07-05 22:11:17.845077	Reportado
28	28	28	Excelente trabajo, muy profesional. Comentario número 28.	2026-07-04 22:11:17.845077	Activo
29	29	29	Excelente trabajo, muy profesional. Comentario número 29.	2026-07-03 22:11:17.845077	Oculto
30	30	30	Excelente trabajo, muy profesional. Comentario número 30.	2026-07-02 22:11:17.845077	Reportado
31	31	31	Excelente trabajo, muy profesional. Comentario número 31.	2026-07-01 22:11:17.845077	Activo
32	32	32	Excelente trabajo, muy profesional. Comentario número 32.	2026-06-30 22:11:17.845077	Oculto
33	33	33	Excelente trabajo, muy profesional. Comentario número 33.	2026-06-29 22:11:17.845077	Reportado
34	34	34	Excelente trabajo, muy profesional. Comentario número 34.	2026-06-28 22:11:17.845077	Activo
35	35	35	Excelente trabajo, muy profesional. Comentario número 35.	2026-06-27 22:11:17.845077	Oculto
36	36	36	Excelente trabajo, muy profesional. Comentario número 36.	2026-06-26 22:11:17.845077	Reportado
37	37	37	Excelente trabajo, muy profesional. Comentario número 37.	2026-06-25 22:11:17.845077	Activo
38	38	38	Excelente trabajo, muy profesional. Comentario número 38.	2026-06-24 22:11:17.845077	Oculto
39	39	39	Excelente trabajo, muy profesional. Comentario número 39.	2026-06-23 22:11:17.845077	Reportado
40	40	40	Excelente trabajo, muy profesional. Comentario número 40.	2026-06-22 22:11:17.845077	Activo
41	41	41	Excelente trabajo, muy profesional. Comentario número 41.	2026-06-21 22:11:17.845077	Oculto
42	42	42	Excelente trabajo, muy profesional. Comentario número 42.	2026-06-20 22:11:17.845077	Reportado
43	43	43	Excelente trabajo, muy profesional. Comentario número 43.	2026-06-19 22:11:17.845077	Activo
44	44	44	Excelente trabajo, muy profesional. Comentario número 44.	2026-06-18 22:11:17.845077	Oculto
45	45	45	Excelente trabajo, muy profesional. Comentario número 45.	2026-06-17 22:11:17.845077	Reportado
46	46	46	Excelente trabajo, muy profesional. Comentario número 46.	2026-06-16 22:11:17.845077	Activo
47	47	47	Excelente trabajo, muy profesional. Comentario número 47.	2026-06-15 22:11:17.845077	Oculto
48	48	48	Excelente trabajo, muy profesional. Comentario número 48.	2026-06-14 22:11:17.845077	Reportado
49	49	49	Excelente trabajo, muy profesional. Comentario número 49.	2026-06-13 22:11:17.845077	Activo
50	50	50	Excelente trabajo, muy profesional. Comentario número 50.	2026-06-12 22:11:17.845077	Oculto
51	51	51	Excelente trabajo, muy profesional. Comentario número 51.	2026-06-11 22:11:17.845077	Reportado
52	52	52	Excelente trabajo, muy profesional. Comentario número 52.	2026-06-10 22:11:17.845077	Activo
53	53	53	Excelente trabajo, muy profesional. Comentario número 53.	2026-06-09 22:11:17.845077	Oculto
54	54	54	Excelente trabajo, muy profesional. Comentario número 54.	2026-06-08 22:11:17.845077	Reportado
55	55	55	Excelente trabajo, muy profesional. Comentario número 55.	2026-06-07 22:11:17.845077	Activo
56	56	56	Excelente trabajo, muy profesional. Comentario número 56.	2026-06-06 22:11:17.845077	Oculto
57	57	57	Excelente trabajo, muy profesional. Comentario número 57.	2026-06-05 22:11:17.845077	Reportado
58	58	58	Excelente trabajo, muy profesional. Comentario número 58.	2026-06-04 22:11:17.845077	Activo
59	59	59	Excelente trabajo, muy profesional. Comentario número 59.	2026-06-03 22:11:17.845077	Oculto
60	60	60	Excelente trabajo, muy profesional. Comentario número 60.	2026-06-02 22:11:17.845077	Reportado
61	61	1	Excelente trabajo, muy profesional. Comentario número 61.	2026-06-01 22:11:17.845077	Activo
62	62	2	Excelente trabajo, muy profesional. Comentario número 62.	2026-05-31 22:11:17.845077	Oculto
63	63	3	Excelente trabajo, muy profesional. Comentario número 63.	2026-05-30 22:11:17.845077	Reportado
64	64	4	Excelente trabajo, muy profesional. Comentario número 64.	2026-05-29 22:11:17.845077	Activo
65	65	5	Excelente trabajo, muy profesional. Comentario número 65.	2026-05-28 22:11:17.845077	Oculto
66	66	6	Excelente trabajo, muy profesional. Comentario número 66.	2026-05-27 22:11:17.845077	Reportado
67	67	7	Excelente trabajo, muy profesional. Comentario número 67.	2026-05-26 22:11:17.845077	Activo
68	68	8	Excelente trabajo, muy profesional. Comentario número 68.	2026-05-25 22:11:17.845077	Oculto
69	69	9	Excelente trabajo, muy profesional. Comentario número 69.	2026-05-24 22:11:17.845077	Reportado
70	70	10	Excelente trabajo, muy profesional. Comentario número 70.	2026-05-23 22:11:17.845077	Activo
\.


--
-- TOC entry 4742 (class 0 OID 27945)
-- Dependencies: 239
-- Data for Name: contratos; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.contratos (id_contrato, id_pedido, id_plantilla, hash_firma_cliente, hash_firma_creador, limite_revisiones, fecha_formalizacion, url_documento_pdf) FROM stdin;
1	1	1	7925c6eac15d3ec67b1561ac4029d151	9d20f8512647d15de29ad4e4261f2441	1	2026-07-31 22:11:17.845077	https://cdn.example.com/contratos/contrato1.pdf
2	2	2	d6d21b015cef9b8795f8d8f3284bad98	b19a9180418c5c7ce8d2cc8b67fce2ec	2	2026-07-30 22:11:17.845077	https://cdn.example.com/contratos/contrato2.pdf
3	3	3	23da4f1416c8ef326aae18b694b18494	c5011400a7f7c59c57d64fdaa3cf75fa	3	2026-07-29 22:11:17.845077	https://cdn.example.com/contratos/contrato3.pdf
4	4	4	6c19d5b8306240bdb571409cd9b159a7	b84416b2fdc5ea4fe39a95de1b422cdb	4	2026-07-28 22:11:17.845077	https://cdn.example.com/contratos/contrato4.pdf
5	5	5	b147cbeb44100eaf364b706f640c733d	56a12ee64989a6d280685e773236fd37	0	2026-07-27 22:11:17.845077	https://cdn.example.com/contratos/contrato5.pdf
6	6	6	c20558af93705c967fdfe38c5ccabfc2	91ba15e994edbdecda5b36ba7cb7284e	1	2026-07-26 22:11:17.845077	https://cdn.example.com/contratos/contrato6.pdf
7	7	7	f580ad1f5ab9268828de95be4ed1dd41	f9028401379c7a8aef22c1a20355b7f3	2	2026-07-25 22:11:17.845077	https://cdn.example.com/contratos/contrato7.pdf
8	8	8	62f0969127caaf722e160e5e910bdc9e	6682124894b9ca7ac6cdfff92869d939	3	2026-07-24 22:11:17.845077	https://cdn.example.com/contratos/contrato8.pdf
9	9	9	bae32bbf8c6fff2c14a7ac5da6f65dee	7f0de4d026498a9dbc71c0fc8fc6f10d	4	2026-07-23 22:11:17.845077	https://cdn.example.com/contratos/contrato9.pdf
10	10	10	9e012c7fbc5b5fa228ca637cbae3b659	2c8ea7c27b1fc9c5ef2005a4b25d3af0	0	2026-07-22 22:11:17.845077	https://cdn.example.com/contratos/contrato10.pdf
11	11	11	cebe681b43e35063051ae7c5b12fc121	10eafb55a706ee3362100d66e8a413f7	1	2026-07-21 22:11:17.845077	https://cdn.example.com/contratos/contrato11.pdf
12	12	12	b6c9a1a6e1b9eb53bec5f29d636fb731	4c19325f5f8f5a0c9d8e99d8ce806ffa	2	2026-07-20 22:11:17.845077	https://cdn.example.com/contratos/contrato12.pdf
13	13	13	0951c32ebf4bf62be3eade02b56b08b0	431fd1f810bd954f4280ccbaf3374608	3	2026-07-19 22:11:17.845077	https://cdn.example.com/contratos/contrato13.pdf
14	14	14	8ed8f1efdb9aa11e679dea96bf26d9d3	22ee0a23e8014585db83642dcee661d2	4	2026-07-18 22:11:17.845077	https://cdn.example.com/contratos/contrato14.pdf
15	15	15	f90eca26c4cfabbf94e557100de4d84f	6c0e94d6a7a44b3dce07c04526920991	0	2026-07-17 22:11:17.845077	https://cdn.example.com/contratos/contrato15.pdf
16	16	16	29706d10c84515a31ccfc267d6f9eeb5	355764ee9425bc00761f7df3baed311e	1	2026-07-16 22:11:17.845077	https://cdn.example.com/contratos/contrato16.pdf
17	17	17	b3dd04e3fb901defc207f005ccbeaf2a	c71c982b9279559548989b8004d642b0	2	2026-07-15 22:11:17.845077	https://cdn.example.com/contratos/contrato17.pdf
18	18	18	9ff9f1da81d6d0c2748ecef15af6ec6f	1eed82659c5694891c3cbe0a86169800	3	2026-07-14 22:11:17.845077	https://cdn.example.com/contratos/contrato18.pdf
19	19	19	d3f6c2076f399b623ffeae1601783bf1	3815e6561fa787a8d49bd53b8fe2ff06	4	2026-07-13 22:11:17.845077	https://cdn.example.com/contratos/contrato19.pdf
20	20	20	9f41b1628a1cdbbda4cfc1391d0fcfc8	1d6dcd97b59bbba9d11e640e952ae47d	0	2026-07-12 22:11:17.845077	https://cdn.example.com/contratos/contrato20.pdf
21	21	21	94bf1ba455ef15a3f232e20810f86b51	14af880b6161262d8a7daa37be72941c	1	2026-07-11 22:11:17.845077	https://cdn.example.com/contratos/contrato21.pdf
22	22	22	41220566669cd2a82cb0c1f8a2080dd0	3298fe78a8ac6bd8ada40b35db75d647	2	2026-07-10 22:11:17.845077	https://cdn.example.com/contratos/contrato22.pdf
23	23	23	ca1cef23fdc5977f021c875fb71ba919	03b753b39909eb470d8046a30f08254c	3	2026-07-09 22:11:17.845077	https://cdn.example.com/contratos/contrato23.pdf
24	24	24	80c458aeae363eaabe3bb76d60c647c8	8e24f1029acf7a7ae7cb424568d4ae80	4	2026-07-08 22:11:17.845077	https://cdn.example.com/contratos/contrato24.pdf
25	25	25	d8109c1008e4c4fd8b4d9b10a2fff001	7f1c97006f4be3fb73c7c45df60b32ef	0	2026-07-07 22:11:17.845077	https://cdn.example.com/contratos/contrato25.pdf
26	26	26	542bf7a6dfb6cc9b1ebd76ad7bba4e5a	edca32f0046ab78cfe01e11fe17c73b1	1	2026-07-06 22:11:17.845077	https://cdn.example.com/contratos/contrato26.pdf
27	27	27	09239ab92be6c277625db6a35d78a5e6	e02c794fd7d445465d51a2def37edfde	2	2026-07-05 22:11:17.845077	https://cdn.example.com/contratos/contrato27.pdf
28	28	28	e787720dcc99efa6beb5dc19f2f9beee	6f77e360ce0df4d6f6e6dd46c461c25b	3	2026-07-04 22:11:17.845077	https://cdn.example.com/contratos/contrato28.pdf
29	29	29	648b6bee3cf5b06da517decb4b3f8170	3c7620f65868601de26ed2adee236b7f	4	2026-07-03 22:11:17.845077	https://cdn.example.com/contratos/contrato29.pdf
30	30	30	a730da845c80b37f302b7e73c8176a5d	babce7c61097a15073819ce7a1e15100	0	2026-07-02 22:11:17.845077	https://cdn.example.com/contratos/contrato30.pdf
31	31	31	77890903f856b28bc54bee9ec9844699	4bf6d5d3462d4ebaf21eaf90072be022	1	2026-07-01 22:11:17.845077	https://cdn.example.com/contratos/contrato31.pdf
32	32	32	d362fe59ba73e9d62ef72ae6239adbc3	787b8a1ccd5bf1151051ce9609a7e058	2	2026-06-30 22:11:17.845077	https://cdn.example.com/contratos/contrato32.pdf
33	33	33	e2dafdf64e06de59520cc1a61c4e2e55	a4be4c61651990d30dc8000717aaa4de	3	2026-06-29 22:11:17.845077	https://cdn.example.com/contratos/contrato33.pdf
34	34	34	89fc9a753419cde54181c9910ff88fe7	b28f6397114262b741e1e7f36551569f	4	2026-06-28 22:11:17.845077	https://cdn.example.com/contratos/contrato34.pdf
35	35	35	e02ca5ac6c1998581ef6d370c49246f9	aeae89e877e5549d3bd8f928e68a41a7	0	2026-06-27 22:11:17.845077	https://cdn.example.com/contratos/contrato35.pdf
36	36	36	79c1f770e4808611606c006ff92012a0	c8d442a68f9820044d200ec11b1104e1	1	2026-06-26 22:11:17.845077	https://cdn.example.com/contratos/contrato36.pdf
37	37	37	124ff893daefae44c7ba0e0d1332e242	7f88477948b44c79e7cbbac21f9c027c	2	2026-06-25 22:11:17.845077	https://cdn.example.com/contratos/contrato37.pdf
38	38	38	9b1545a7ecd61e7b5569044c37e02a00	0639f332a34b1be5232f8bc0ba5693da	3	2026-06-24 22:11:17.845077	https://cdn.example.com/contratos/contrato38.pdf
39	39	39	6c20bfaee15e50f0374f3376dfae0006	da884bb866532a7e59cbaee17a0d24ab	4	2026-06-23 22:11:17.845077	https://cdn.example.com/contratos/contrato39.pdf
40	40	40	6517e0639b860afaab7e179a9b18a826	b1766b137aeaec440294605ad2e49f62	0	2026-06-22 22:11:17.845077	https://cdn.example.com/contratos/contrato40.pdf
41	41	41	13389c3144db548190951ad8d2f74fa7	772745489ee2a33228d0d43f4c1c1cc7	1	2026-06-21 22:11:17.845077	https://cdn.example.com/contratos/contrato41.pdf
42	42	42	b058e426bd45837f05f06e8938492e98	ca8b3935c36addecef3915a87b88a068	2	2026-06-20 22:11:17.845077	https://cdn.example.com/contratos/contrato42.pdf
43	43	43	778c868363811256c3af2f2ab1af4292	40ff63b6c5d0a72cd51369a6d106c978	3	2026-06-19 22:11:17.845077	https://cdn.example.com/contratos/contrato43.pdf
44	44	44	559fe7615b95b7f4e9de855bcfafe0ad	93034ea6629db868a66cd5d6aa6380e2	4	2026-06-18 22:11:17.845077	https://cdn.example.com/contratos/contrato44.pdf
45	45	45	bc9397decf46237fcdc43964d7541e8f	5943d58b7995cde52442e34fa3a75886	0	2026-06-17 22:11:17.845077	https://cdn.example.com/contratos/contrato45.pdf
46	46	46	496a336e525457654052b1a7ab8300ed	1ed184cfd9d9df04da8334d4b83329cc	1	2026-06-16 22:11:17.845077	https://cdn.example.com/contratos/contrato46.pdf
47	47	47	70621ad0c1046fe1d3c247b2bd51886a	1de4be8e9a945a4e63c78e9e2a75489e	2	2026-06-15 22:11:17.845077	https://cdn.example.com/contratos/contrato47.pdf
48	48	48	e19c329f3f9ca10e7de62170f87f318d	0ea0f01159f114bd93c8ae66e27c6fbb	3	2026-06-14 22:11:17.845077	https://cdn.example.com/contratos/contrato48.pdf
49	49	49	6a0686292f8d4c0ee5169c5b52f8c93f	5723997fda4a0a4cc389b63f32ba1232	4	2026-06-13 22:11:17.845077	https://cdn.example.com/contratos/contrato49.pdf
50	50	50	bdf82d5dc756f6655ba3223dba28f403	42e483bd25d1c31cc68aa63a5dffd5c5	0	2026-06-12 22:11:17.845077	https://cdn.example.com/contratos/contrato50.pdf
\.


--
-- TOC entry 4744 (class 0 OID 27956)
-- Dependencies: 241
-- Data for Name: creador_habilidades; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.creador_habilidades (id_creador_habilidad, id_perfil, id_habilidad, nivel_dominio) FROM stdin;
1	1	1	Principiante
2	2	8	Intermedio
3	3	15	Avanzado
4	4	22	Experto
5	5	29	Principiante
6	6	36	Intermedio
7	7	43	Avanzado
8	8	50	Experto
9	9	7	Principiante
10	10	14	Intermedio
11	11	21	Avanzado
12	12	28	Experto
13	13	35	Principiante
14	14	42	Intermedio
15	15	49	Avanzado
16	16	6	Experto
17	17	13	Principiante
18	18	20	Intermedio
19	19	27	Avanzado
20	20	34	Experto
21	21	41	Principiante
22	22	48	Intermedio
23	23	5	Avanzado
24	24	12	Experto
25	25	19	Principiante
26	26	26	Intermedio
27	27	33	Avanzado
28	28	40	Experto
29	29	47	Principiante
30	30	4	Intermedio
31	31	11	Avanzado
32	32	18	Experto
33	33	25	Principiante
34	34	32	Intermedio
35	35	39	Avanzado
36	36	46	Experto
37	37	3	Principiante
38	38	10	Intermedio
39	39	17	Avanzado
40	40	24	Experto
41	41	31	Principiante
42	42	38	Intermedio
43	43	45	Avanzado
44	44	2	Experto
45	45	9	Principiante
46	46	16	Intermedio
47	47	23	Avanzado
48	48	30	Experto
49	49	37	Principiante
50	50	44	Intermedio
51	1	1	Avanzado
52	2	8	Experto
53	3	15	Principiante
54	4	22	Intermedio
55	5	29	Avanzado
56	6	36	Experto
57	7	43	Principiante
58	8	50	Intermedio
59	9	7	Avanzado
60	10	14	Experto
61	11	21	Principiante
62	12	28	Intermedio
63	13	35	Avanzado
64	14	42	Experto
65	15	49	Principiante
66	16	6	Intermedio
67	17	13	Avanzado
68	18	20	Experto
69	19	27	Principiante
70	20	34	Intermedio
\.


--
-- TOC entry 4746 (class 0 OID 27963)
-- Dependencies: 243
-- Data for Name: documentos_adjuntos; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.documentos_adjuntos (id_adjunto, id_mensaje, url_archivo, tipo_mime, peso_bytes) FROM stdin;
1	1	https://cdn.example.com/adjuntos/archivo1	image/png	2048
2	2	https://cdn.example.com/adjuntos/archivo2	image/jpeg	3072
3	3	https://cdn.example.com/adjuntos/archivo3	application/pdf	4096
4	4	https://cdn.example.com/adjuntos/archivo4	video/mp4	5120
5	5	https://cdn.example.com/adjuntos/archivo5	image/svg+xml	6144
6	6	https://cdn.example.com/adjuntos/archivo6	image/png	7168
7	7	https://cdn.example.com/adjuntos/archivo7	image/jpeg	8192
8	8	https://cdn.example.com/adjuntos/archivo8	application/pdf	9216
9	9	https://cdn.example.com/adjuntos/archivo9	video/mp4	10240
10	10	https://cdn.example.com/adjuntos/archivo10	image/svg+xml	11264
11	11	https://cdn.example.com/adjuntos/archivo11	image/png	12288
12	12	https://cdn.example.com/adjuntos/archivo12	image/jpeg	13312
13	13	https://cdn.example.com/adjuntos/archivo13	application/pdf	14336
14	14	https://cdn.example.com/adjuntos/archivo14	video/mp4	15360
15	15	https://cdn.example.com/adjuntos/archivo15	image/svg+xml	16384
16	16	https://cdn.example.com/adjuntos/archivo16	image/png	17408
17	17	https://cdn.example.com/adjuntos/archivo17	image/jpeg	18432
18	18	https://cdn.example.com/adjuntos/archivo18	application/pdf	19456
19	19	https://cdn.example.com/adjuntos/archivo19	video/mp4	20480
20	20	https://cdn.example.com/adjuntos/archivo20	image/svg+xml	21504
21	21	https://cdn.example.com/adjuntos/archivo21	image/png	22528
22	22	https://cdn.example.com/adjuntos/archivo22	image/jpeg	23552
23	23	https://cdn.example.com/adjuntos/archivo23	application/pdf	24576
24	24	https://cdn.example.com/adjuntos/archivo24	video/mp4	25600
25	25	https://cdn.example.com/adjuntos/archivo25	image/svg+xml	26624
26	26	https://cdn.example.com/adjuntos/archivo26	image/png	27648
27	27	https://cdn.example.com/adjuntos/archivo27	image/jpeg	28672
28	28	https://cdn.example.com/adjuntos/archivo28	application/pdf	29696
29	29	https://cdn.example.com/adjuntos/archivo29	video/mp4	30720
30	30	https://cdn.example.com/adjuntos/archivo30	image/svg+xml	31744
31	31	https://cdn.example.com/adjuntos/archivo31	image/png	32768
32	32	https://cdn.example.com/adjuntos/archivo32	image/jpeg	33792
33	33	https://cdn.example.com/adjuntos/archivo33	application/pdf	34816
34	34	https://cdn.example.com/adjuntos/archivo34	video/mp4	35840
35	35	https://cdn.example.com/adjuntos/archivo35	image/svg+xml	36864
36	36	https://cdn.example.com/adjuntos/archivo36	image/png	37888
37	37	https://cdn.example.com/adjuntos/archivo37	image/jpeg	38912
38	38	https://cdn.example.com/adjuntos/archivo38	application/pdf	39936
39	39	https://cdn.example.com/adjuntos/archivo39	video/mp4	40960
40	40	https://cdn.example.com/adjuntos/archivo40	image/svg+xml	41984
41	41	https://cdn.example.com/adjuntos/archivo41	image/png	43008
42	42	https://cdn.example.com/adjuntos/archivo42	image/jpeg	44032
43	43	https://cdn.example.com/adjuntos/archivo43	application/pdf	45056
44	44	https://cdn.example.com/adjuntos/archivo44	video/mp4	46080
45	45	https://cdn.example.com/adjuntos/archivo45	image/svg+xml	47104
46	46	https://cdn.example.com/adjuntos/archivo46	image/png	48128
47	47	https://cdn.example.com/adjuntos/archivo47	image/jpeg	49152
48	48	https://cdn.example.com/adjuntos/archivo48	application/pdf	50176
49	49	https://cdn.example.com/adjuntos/archivo49	video/mp4	51200
50	50	https://cdn.example.com/adjuntos/archivo50	image/svg+xml	52224
51	51	https://cdn.example.com/adjuntos/archivo51	image/png	53248
52	52	https://cdn.example.com/adjuntos/archivo52	image/jpeg	54272
53	53	https://cdn.example.com/adjuntos/archivo53	application/pdf	55296
54	54	https://cdn.example.com/adjuntos/archivo54	video/mp4	56320
55	55	https://cdn.example.com/adjuntos/archivo55	image/svg+xml	57344
56	56	https://cdn.example.com/adjuntos/archivo56	image/png	58368
57	57	https://cdn.example.com/adjuntos/archivo57	image/jpeg	59392
58	58	https://cdn.example.com/adjuntos/archivo58	application/pdf	60416
59	59	https://cdn.example.com/adjuntos/archivo59	video/mp4	61440
60	60	https://cdn.example.com/adjuntos/archivo60	image/svg+xml	62464
\.


--
-- TOC entry 4748 (class 0 OID 27970)
-- Dependencies: 245
-- Data for Name: entregables_finales; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.entregables_finales (id_entregable, id_pedido, url_version_marca_agua, url_version_limpia, esta_liberado) FROM stdin;
1	1	https://cdn.example.com/entregables/marca_agua1.jpg	https://cdn.example.com/entregables/limpia1.jpg	t
2	2	https://cdn.example.com/entregables/marca_agua2.jpg	https://cdn.example.com/entregables/limpia2.jpg	t
3	3	https://cdn.example.com/entregables/marca_agua3.jpg	https://cdn.example.com/entregables/limpia3.jpg	f
4	4	https://cdn.example.com/entregables/marca_agua4.jpg	https://cdn.example.com/entregables/limpia4.jpg	t
5	5	https://cdn.example.com/entregables/marca_agua5.jpg	https://cdn.example.com/entregables/limpia5.jpg	t
6	6	https://cdn.example.com/entregables/marca_agua6.jpg	https://cdn.example.com/entregables/limpia6.jpg	f
7	7	https://cdn.example.com/entregables/marca_agua7.jpg	https://cdn.example.com/entregables/limpia7.jpg	t
8	8	https://cdn.example.com/entregables/marca_agua8.jpg	https://cdn.example.com/entregables/limpia8.jpg	t
9	9	https://cdn.example.com/entregables/marca_agua9.jpg	https://cdn.example.com/entregables/limpia9.jpg	f
10	10	https://cdn.example.com/entregables/marca_agua10.jpg	https://cdn.example.com/entregables/limpia10.jpg	t
11	11	https://cdn.example.com/entregables/marca_agua11.jpg	https://cdn.example.com/entregables/limpia11.jpg	t
12	12	https://cdn.example.com/entregables/marca_agua12.jpg	https://cdn.example.com/entregables/limpia12.jpg	f
13	13	https://cdn.example.com/entregables/marca_agua13.jpg	https://cdn.example.com/entregables/limpia13.jpg	t
14	14	https://cdn.example.com/entregables/marca_agua14.jpg	https://cdn.example.com/entregables/limpia14.jpg	t
15	15	https://cdn.example.com/entregables/marca_agua15.jpg	https://cdn.example.com/entregables/limpia15.jpg	f
16	16	https://cdn.example.com/entregables/marca_agua16.jpg	https://cdn.example.com/entregables/limpia16.jpg	t
17	17	https://cdn.example.com/entregables/marca_agua17.jpg	https://cdn.example.com/entregables/limpia17.jpg	t
18	18	https://cdn.example.com/entregables/marca_agua18.jpg	https://cdn.example.com/entregables/limpia18.jpg	f
19	19	https://cdn.example.com/entregables/marca_agua19.jpg	https://cdn.example.com/entregables/limpia19.jpg	t
20	20	https://cdn.example.com/entregables/marca_agua20.jpg	https://cdn.example.com/entregables/limpia20.jpg	t
21	21	https://cdn.example.com/entregables/marca_agua21.jpg	https://cdn.example.com/entregables/limpia21.jpg	f
22	22	https://cdn.example.com/entregables/marca_agua22.jpg	https://cdn.example.com/entregables/limpia22.jpg	t
23	23	https://cdn.example.com/entregables/marca_agua23.jpg	https://cdn.example.com/entregables/limpia23.jpg	t
24	24	https://cdn.example.com/entregables/marca_agua24.jpg	https://cdn.example.com/entregables/limpia24.jpg	f
25	25	https://cdn.example.com/entregables/marca_agua25.jpg	https://cdn.example.com/entregables/limpia25.jpg	t
26	26	https://cdn.example.com/entregables/marca_agua26.jpg	https://cdn.example.com/entregables/limpia26.jpg	t
27	27	https://cdn.example.com/entregables/marca_agua27.jpg	https://cdn.example.com/entregables/limpia27.jpg	f
28	28	https://cdn.example.com/entregables/marca_agua28.jpg	https://cdn.example.com/entregables/limpia28.jpg	t
29	29	https://cdn.example.com/entregables/marca_agua29.jpg	https://cdn.example.com/entregables/limpia29.jpg	t
30	30	https://cdn.example.com/entregables/marca_agua30.jpg	https://cdn.example.com/entregables/limpia30.jpg	f
31	31	https://cdn.example.com/entregables/marca_agua31.jpg	https://cdn.example.com/entregables/limpia31.jpg	t
32	32	https://cdn.example.com/entregables/marca_agua32.jpg	https://cdn.example.com/entregables/limpia32.jpg	t
33	33	https://cdn.example.com/entregables/marca_agua33.jpg	https://cdn.example.com/entregables/limpia33.jpg	f
34	34	https://cdn.example.com/entregables/marca_agua34.jpg	https://cdn.example.com/entregables/limpia34.jpg	t
35	35	https://cdn.example.com/entregables/marca_agua35.jpg	https://cdn.example.com/entregables/limpia35.jpg	t
36	36	https://cdn.example.com/entregables/marca_agua36.jpg	https://cdn.example.com/entregables/limpia36.jpg	f
37	37	https://cdn.example.com/entregables/marca_agua37.jpg	https://cdn.example.com/entregables/limpia37.jpg	t
38	38	https://cdn.example.com/entregables/marca_agua38.jpg	https://cdn.example.com/entregables/limpia38.jpg	t
39	39	https://cdn.example.com/entregables/marca_agua39.jpg	https://cdn.example.com/entregables/limpia39.jpg	f
40	40	https://cdn.example.com/entregables/marca_agua40.jpg	https://cdn.example.com/entregables/limpia40.jpg	t
41	41	https://cdn.example.com/entregables/marca_agua41.jpg	https://cdn.example.com/entregables/limpia41.jpg	t
42	42	https://cdn.example.com/entregables/marca_agua42.jpg	https://cdn.example.com/entregables/limpia42.jpg	f
43	43	https://cdn.example.com/entregables/marca_agua43.jpg	https://cdn.example.com/entregables/limpia43.jpg	t
44	44	https://cdn.example.com/entregables/marca_agua44.jpg	https://cdn.example.com/entregables/limpia44.jpg	t
45	45	https://cdn.example.com/entregables/marca_agua45.jpg	https://cdn.example.com/entregables/limpia45.jpg	f
46	46	https://cdn.example.com/entregables/marca_agua46.jpg	https://cdn.example.com/entregables/limpia46.jpg	t
47	47	https://cdn.example.com/entregables/marca_agua47.jpg	https://cdn.example.com/entregables/limpia47.jpg	t
48	48	https://cdn.example.com/entregables/marca_agua48.jpg	https://cdn.example.com/entregables/limpia48.jpg	f
49	49	https://cdn.example.com/entregables/marca_agua49.jpg	https://cdn.example.com/entregables/limpia49.jpg	t
50	50	https://cdn.example.com/entregables/marca_agua50.jpg	https://cdn.example.com/entregables/limpia50.jpg	t
51	51	https://cdn.example.com/entregables/marca_agua51.jpg	https://cdn.example.com/entregables/limpia51.jpg	f
52	52	https://cdn.example.com/entregables/marca_agua52.jpg	https://cdn.example.com/entregables/limpia52.jpg	t
53	53	https://cdn.example.com/entregables/marca_agua53.jpg	https://cdn.example.com/entregables/limpia53.jpg	t
54	54	https://cdn.example.com/entregables/marca_agua54.jpg	https://cdn.example.com/entregables/limpia54.jpg	f
55	55	https://cdn.example.com/entregables/marca_agua55.jpg	https://cdn.example.com/entregables/limpia55.jpg	t
56	56	https://cdn.example.com/entregables/marca_agua56.jpg	https://cdn.example.com/entregables/limpia56.jpg	t
57	57	https://cdn.example.com/entregables/marca_agua57.jpg	https://cdn.example.com/entregables/limpia57.jpg	f
58	58	https://cdn.example.com/entregables/marca_agua58.jpg	https://cdn.example.com/entregables/limpia58.jpg	t
59	59	https://cdn.example.com/entregables/marca_agua59.jpg	https://cdn.example.com/entregables/limpia59.jpg	t
60	60	https://cdn.example.com/entregables/marca_agua60.jpg	https://cdn.example.com/entregables/limpia60.jpg	f
\.


--
-- TOC entry 4750 (class 0 OID 27979)
-- Dependencies: 247
-- Data for Name: estados_verificacion; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.estados_verificacion (id_estado_verificacion, nombre_estado) FROM stdin;
1	Pendiente v1
2	En Revisión v2
3	Aprobado v3
4	Rechazado v4
5	Requiere Corrección v5
6	En Cola v6
7	Analizando v7
8	Verificado v8
9	Observado v9
10	Suspendido v10
11	Pendiente v11
12	En Revisión v12
13	Aprobado v13
14	Rechazado v14
15	Requiere Corrección v15
16	En Cola v16
17	Analizando v17
18	Verificado v18
19	Observado v19
20	Suspendido v20
21	Pendiente v21
22	En Revisión v22
23	Aprobado v23
24	Rechazado v24
25	Requiere Corrección v25
26	En Cola v26
27	Analizando v27
28	Verificado v28
29	Observado v29
30	Suspendido v30
31	Pendiente v31
32	En Revisión v32
33	Aprobado v33
34	Rechazado v34
35	Requiere Corrección v35
36	En Cola v36
37	Analizando v37
38	Verificado v38
39	Observado v39
40	Suspendido v40
41	Pendiente v41
42	En Revisión v42
43	Aprobado v43
44	Rechazado v44
45	Requiere Corrección v45
46	En Cola v46
47	Analizando v47
48	Verificado v48
49	Observado v49
50	Suspendido v50
51	PENDIENTE
52	APROBADO
53	RECHAZADO
54	REQUIERE_ACLARACION
\.


--
-- TOC entry 4752 (class 0 OID 27985)
-- Dependencies: 249
-- Data for Name: etapas_flujo; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.etapas_flujo (id_etapa, nombre_etapa) FROM stdin;
1	Solicitud Recibida v1
2	Briefing Pendiente v2
3	Briefing Completado v3
4	En Diseño v4
5	En Revisión Interna v5
6	Enviado a Cliente v6
7	En Revisión Cliente v7
8	Cambios Solicitados v8
9	Aprobado por Cliente v9
10	Pago Liberado v10
11	Entrega Final v11
12	Cerrado v12
13	Cancelado v13
14	En Disputa v14
15	Reembolsado v15
16	Solicitud Recibida v16
17	Briefing Pendiente v17
18	Briefing Completado v18
19	En Diseño v19
20	En Revisión Interna v20
21	Enviado a Cliente v21
22	En Revisión Cliente v22
23	Cambios Solicitados v23
24	Aprobado por Cliente v24
25	Pago Liberado v25
26	Entrega Final v26
27	Cerrado v27
28	Cancelado v28
29	En Disputa v29
30	Reembolsado v30
31	Solicitud Recibida v31
32	Briefing Pendiente v32
33	Briefing Completado v33
34	En Diseño v34
35	En Revisión Interna v35
36	Enviado a Cliente v36
37	En Revisión Cliente v37
38	Cambios Solicitados v38
39	Aprobado por Cliente v39
40	Pago Liberado v40
41	Entrega Final v41
42	Cerrado v42
43	Cancelado v43
44	En Disputa v44
45	Reembolsado v45
46	Solicitud Recibida v46
47	Briefing Pendiente v47
48	Briefing Completado v48
49	En Diseño v49
50	En Revisión Interna v50
\.


--
-- TOC entry 4754 (class 0 OID 27991)
-- Dependencies: 251
-- Data for Name: etiquetas; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.etiquetas (id_etiqueta, nombre_etiqueta, actualizado_en) FROM stdin;
1	Minimalista	2026-08-02 03:11:17.845077+00
2	Moderno	2026-08-02 03:11:17.845077+00
3	Vintage	2026-08-02 03:11:17.845077+00
4	Colorido	2026-08-02 03:11:17.845077+00
5	Elegante	2026-08-02 03:11:17.845077+00
6	Corporativo	2026-08-02 03:11:17.845077+00
7	Creativo	2026-08-02 03:11:17.845077+00
8	Profesional	2026-08-02 03:11:17.845077+00
9	Divertido	2026-08-02 03:11:17.845077+00
10	Artístico	2026-08-02 03:11:17.845077+00
11	Abstracto	2026-08-02 03:11:17.845077+00
12	Realista	2026-08-02 03:11:17.845077+00
13	Geométrico	2026-08-02 03:11:17.845077+00
14	Orgánico	2026-08-02 03:11:17.845077+00
15	Retro	2026-08-02 03:11:17.845077+00
16	Futurista	2026-08-02 03:11:17.845077+00
17	Urbano	2026-08-02 03:11:17.845077+00
18	Natural	2026-08-02 03:11:17.845077+00
19	Lujoso	2026-08-02 03:11:17.845077+00
20	Económico	2026-08-02 03:11:17.845077+00
21	Rápido	2026-08-02 03:11:17.845077+00
22	Premium	2026-08-02 03:11:17.845077+00
23	Personalizado	2026-08-02 03:11:17.845077+00
24	Exclusivo	2026-08-02 03:11:17.845077+00
25	Tendencia	2026-08-02 03:11:17.845077+00
26	Clásico	2026-08-02 03:11:17.845077+00
27	Innovador	2026-08-02 03:11:17.845077+00
28	Sostenible	2026-08-02 03:11:17.845077+00
29	Digital	2026-08-02 03:11:17.845077+00
30	Monocromático	2026-08-02 03:11:17.845077+00
31	Vibrante	2026-08-02 03:11:17.845077+00
32	Sutil	2026-08-02 03:11:17.845077+00
33	Audaz	2026-08-02 03:11:17.845077+00
34	Sofisticado	2026-08-02 03:11:17.845077+00
35	Juvenil	2026-08-02 03:11:17.845077+00
36	Femenino	2026-08-02 03:11:17.845077+00
37	Masculino	2026-08-02 03:11:17.845077+00
38	Infantil	2026-08-02 03:11:17.845077+00
39	Tecnológico	2026-08-02 03:11:17.845077+00
40	Artesanal	2026-08-02 03:11:17.845077+00
41	Editorial	2026-08-02 03:11:17.845077+00
42	Comercial	2026-08-02 03:11:17.845077+00
43	Institucional	2026-08-02 03:11:17.845077+00
44	Deportivo	2026-08-02 03:11:17.845077+00
45	Gastronómico	2026-08-02 03:11:17.845077+00
46	Musical	2026-08-02 03:11:17.845077+00
47	Cinematográfico	2026-08-02 03:11:17.845077+00
48	Literario	2026-08-02 03:11:17.845077+00
49	Científico	2026-08-02 03:11:17.845077+00
50	Espacial	2026-08-02 03:11:17.845077+00
\.


--
-- TOC entry 4756 (class 0 OID 27998)
-- Dependencies: 253
-- Data for Name: flujo_etapas_config; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.flujo_etapas_config (id_flujo_etapa, id_flujo, id_etapa, numero_orden, es_etapa_final) FROM stdin;
1	1	1	1	f
2	2	1	1	f
3	3	1	1	f
4	4	1	1	f
5	5	1	1	f
6	6	1	1	f
7	7	1	1	f
8	8	1	1	f
9	9	1	1	f
10	10	1	1	f
11	11	1	1	f
12	12	1	1	f
13	13	1	1	f
14	14	1	1	f
15	15	1	1	f
16	16	1	1	f
17	17	1	1	f
18	18	1	1	f
19	19	1	1	f
20	20	1	1	f
21	21	1	1	f
22	22	1	1	f
23	23	1	1	f
24	24	1	1	f
25	25	1	1	f
26	26	1	1	f
27	27	1	1	f
28	28	1	1	f
29	29	1	1	f
30	30	1	1	f
31	31	1	1	f
32	32	1	1	f
33	33	1	1	f
34	34	1	1	f
35	35	1	1	f
36	36	1	1	f
37	37	1	1	f
38	38	1	1	f
39	39	1	1	f
40	40	1	1	f
41	41	1	1	f
42	42	1	1	f
43	43	1	1	f
44	44	1	1	f
45	45	1	1	f
46	46	1	1	f
47	47	1	1	f
48	48	1	1	f
49	49	1	1	f
50	50	1	1	f
51	1	2	2	t
52	2	2	2	t
53	3	2	2	t
54	4	2	2	t
55	5	2	2	t
56	6	2	2	t
57	7	2	2	t
58	8	2	2	t
59	9	2	2	t
60	10	2	2	t
61	11	2	2	t
62	12	2	2	t
63	13	2	2	t
64	14	2	2	t
65	15	2	2	t
66	16	2	2	t
67	17	2	2	t
68	18	2	2	t
69	19	2	2	t
70	20	2	2	t
71	21	2	2	t
72	22	2	2	t
73	23	2	2	t
74	24	2	2	t
75	25	2	2	t
76	26	2	2	t
77	27	2	2	t
78	28	2	2	t
79	29	2	2	t
80	30	2	2	t
81	31	2	2	t
82	32	2	2	t
83	33	2	2	t
84	34	2	2	t
85	35	2	2	t
86	36	2	2	t
87	37	2	2	t
88	38	2	2	t
89	39	2	2	t
90	40	2	2	t
91	41	2	2	t
92	42	2	2	t
93	43	2	2	t
94	44	2	2	t
95	45	2	2	t
96	46	2	2	t
97	47	2	2	t
98	48	2	2	t
99	49	2	2	t
100	50	2	2	t
\.


--
-- TOC entry 4758 (class 0 OID 28007)
-- Dependencies: 255
-- Data for Name: flujos_trabajo; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.flujos_trabajo (id_flujo, nombre_flujo, descripcion_flujo) FROM stdin;
1	Flujo Estándar v1	Descripción del flujo de trabajo número 1
2	Flujo Express v2	Descripción del flujo de trabajo número 2
3	Flujo Premium v3	Descripción del flujo de trabajo número 3
4	Flujo con Revisiones Ilimitadas v4	Descripción del flujo de trabajo número 4
5	Flujo Simplificado v5	Descripción del flujo de trabajo número 5
6	Flujo para Productos Digitales v6	Descripción del flujo de trabajo número 6
7	Flujo para Servicios Personalizados v7	Descripción del flujo de trabajo número 7
8	Flujo con Briefing Obligatorio v8	Descripción del flujo de trabajo número 8
9	Flujo Corporativo v9	Descripción del flujo de trabajo número 9
10	Flujo para Sorteos v10	Descripción del flujo de trabajo número 10
11	Flujo Estándar v11	Descripción del flujo de trabajo número 11
12	Flujo Express v12	Descripción del flujo de trabajo número 12
13	Flujo Premium v13	Descripción del flujo de trabajo número 13
14	Flujo con Revisiones Ilimitadas v14	Descripción del flujo de trabajo número 14
15	Flujo Simplificado v15	Descripción del flujo de trabajo número 15
16	Flujo para Productos Digitales v16	Descripción del flujo de trabajo número 16
17	Flujo para Servicios Personalizados v17	Descripción del flujo de trabajo número 17
18	Flujo con Briefing Obligatorio v18	Descripción del flujo de trabajo número 18
19	Flujo Corporativo v19	Descripción del flujo de trabajo número 19
20	Flujo para Sorteos v20	Descripción del flujo de trabajo número 20
21	Flujo Estándar v21	Descripción del flujo de trabajo número 21
22	Flujo Express v22	Descripción del flujo de trabajo número 22
23	Flujo Premium v23	Descripción del flujo de trabajo número 23
24	Flujo con Revisiones Ilimitadas v24	Descripción del flujo de trabajo número 24
25	Flujo Simplificado v25	Descripción del flujo de trabajo número 25
26	Flujo para Productos Digitales v26	Descripción del flujo de trabajo número 26
27	Flujo para Servicios Personalizados v27	Descripción del flujo de trabajo número 27
28	Flujo con Briefing Obligatorio v28	Descripción del flujo de trabajo número 28
29	Flujo Corporativo v29	Descripción del flujo de trabajo número 29
30	Flujo para Sorteos v30	Descripción del flujo de trabajo número 30
31	Flujo Estándar v31	Descripción del flujo de trabajo número 31
32	Flujo Express v32	Descripción del flujo de trabajo número 32
33	Flujo Premium v33	Descripción del flujo de trabajo número 33
34	Flujo con Revisiones Ilimitadas v34	Descripción del flujo de trabajo número 34
35	Flujo Simplificado v35	Descripción del flujo de trabajo número 35
36	Flujo para Productos Digitales v36	Descripción del flujo de trabajo número 36
37	Flujo para Servicios Personalizados v37	Descripción del flujo de trabajo número 37
38	Flujo con Briefing Obligatorio v38	Descripción del flujo de trabajo número 38
39	Flujo Corporativo v39	Descripción del flujo de trabajo número 39
40	Flujo para Sorteos v40	Descripción del flujo de trabajo número 40
41	Flujo Estándar v41	Descripción del flujo de trabajo número 41
42	Flujo Express v42	Descripción del flujo de trabajo número 42
43	Flujo Premium v43	Descripción del flujo de trabajo número 43
44	Flujo con Revisiones Ilimitadas v44	Descripción del flujo de trabajo número 44
45	Flujo Simplificado v45	Descripción del flujo de trabajo número 45
46	Flujo para Productos Digitales v46	Descripción del flujo de trabajo número 46
47	Flujo para Servicios Personalizados v47	Descripción del flujo de trabajo número 47
48	Flujo con Briefing Obligatorio v48	Descripción del flujo de trabajo número 48
49	Flujo Corporativo v49	Descripción del flujo de trabajo número 49
50	Flujo para Sorteos v50	Descripción del flujo de trabajo número 50
\.


--
-- TOC entry 4760 (class 0 OID 28015)
-- Dependencies: 257
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	schema inicial	SQL	V1__schema_inicial.sql	-2095363830	postgres	2026-07-30 16:55:38.614382	443	t
2	2	ajustes requisitos pfc	SQL	V2__ajustes_requisitos_pfc.sql	1985837939	postgres	2026-07-30 16:55:39.11657	1362	t
3	3	catalogo ajustes	SQL	V3__catalogo_ajustes.sql	-711324738	postgres	2026-07-30 16:55:40.49964	27	t
4	4	modulo comunicacion	SQL	V4__modulo_comunicacion.sql	2133235572	postgres	2026-07-30 16:55:40.5427	42	t
5	5	modulo social	SQL	V5__modulo_social.sql	-2132714275	postgres	2026-07-30 16:55:40.597549	6	t
6	6	personalizacion colores portafolio	SQL	V6__personalizacion_colores_portafolio.sql	1532893593	adminuteq	2026-08-16 16:32:18.043713	889	t
7	7	verificacion asistida ia	SQL	V7__verificacion_asistida_ia.sql	1724780102	adminuteq	2026-08-16 16:32:20.60379	1634	t
8	8	sesiones usuario jti	SQL	V8__sesiones_usuario_jti.sql	906553284	adminuteq	2026-08-16 16:32:23.754841	1373	t
9	9	flujo por categoria	SQL	V9__flujo_por_categoria.sql	1212401835	adminuteq	2026-08-16 16:32:26.634879	854	t
10	10	seed paises	SQL	V10__seed_paises.sql	2106564618	adminuteq	2026-08-16 16:32:29.008774	625	t
\.


--
-- TOC entry 4761 (class 0 OID 28029)
-- Dependencies: 258
-- Data for Name: habilidades; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.habilidades (id_habilidad, nombre_habilidad) FROM stdin;
1	Ilustración Digital
2	Diseño de Logotipos
3	Edición de Video
4	Animación 2D
5	Animación 3D
6	Copywriting
7	Traducción
8	Fotografía de Producto
9	Retoque Fotográfico
10	Diseño UI/UX
11	Desarrollo Web
12	Composición Musical
13	Locución
14	Diseño de Personajes
15	Modelado 3D
16	Diseño Editorial
17	Branding
18	Diseño de Empaques
19	Diseño de Redes Sociales
20	Motion Graphics
21	Diseño de Videojuegos
22	Storyboard
23	Diseño de Interiores
24	Arquitectura
25	Diseño Industrial
26	Caligrafía
27	Lettering
28	Diseño de Iconos
29	Infografías
30	Diseño de Presentaciones
31	Ghostwriting
32	Corrección de Estilo
33	Diseño de Moda
34	Diseño Textil
35	Diseño de Joyas
36	Escultura Digital
37	Pintura Digital
38	Concept Art
39	Diseño de Tatuajes
40	Rigging 3D
41	Renderizado Arquitectónico
42	Diseño de Interfaces Móviles
43	SEO Writing
44	Guionismo
45	Doblaje
46	Mezcla de Audio
47	Producción Musical
48	Diseño de Stands
49	Cartelería
50	Diseño de Merchandising
\.


--
-- TOC entry 4763 (class 0 OID 28035)
-- Dependencies: 260
-- Data for Name: historial_estados_pedido; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.historial_estados_pedido (id_historial_estado, id_pedido, id_etapa, fecha_transicion, observacion) FROM stdin;
1	1	1	2026-07-31 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 1
2	2	2	2026-07-30 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 2
3	3	3	2026-07-29 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 3
4	4	4	2026-07-28 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 4
5	5	5	2026-07-27 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 5
6	6	6	2026-07-26 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 6
7	7	7	2026-07-25 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 7
8	8	8	2026-07-24 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 8
9	9	9	2026-07-23 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 9
10	10	10	2026-07-22 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 10
11	11	11	2026-07-21 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 11
12	12	12	2026-07-20 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 12
13	13	13	2026-07-19 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 13
14	14	14	2026-07-18 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 14
15	15	15	2026-07-17 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 15
16	16	16	2026-07-16 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 16
17	17	17	2026-07-15 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 17
18	18	18	2026-07-14 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 18
19	19	19	2026-07-13 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 19
20	20	20	2026-07-12 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 20
21	21	21	2026-07-11 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 21
22	22	22	2026-07-10 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 22
23	23	23	2026-07-09 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 23
24	24	24	2026-07-08 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 24
25	25	25	2026-07-07 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 25
26	26	26	2026-07-06 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 26
27	27	27	2026-07-05 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 27
28	28	28	2026-07-04 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 28
29	29	29	2026-07-03 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 29
30	30	30	2026-07-02 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 30
31	31	31	2026-07-01 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 31
32	32	32	2026-06-30 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 32
33	33	33	2026-06-29 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 33
34	34	34	2026-06-28 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 34
35	35	35	2026-06-27 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 35
36	36	36	2026-06-26 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 36
37	37	37	2026-06-25 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 37
38	38	38	2026-06-24 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 38
39	39	39	2026-06-23 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 39
40	40	40	2026-06-22 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 40
41	41	41	2026-06-21 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 41
42	42	42	2026-06-20 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 42
43	43	43	2026-06-19 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 43
44	44	44	2026-06-18 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 44
45	45	45	2026-06-17 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 45
46	46	46	2026-06-16 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 46
47	47	47	2026-06-15 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 47
48	48	48	2026-06-14 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 48
49	49	49	2026-06-13 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 49
50	50	50	2026-06-12 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 50
51	51	1	2026-06-11 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 51
52	52	2	2026-06-10 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 52
53	53	3	2026-06-09 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 53
54	54	4	2026-06-08 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 54
55	55	5	2026-06-07 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 55
56	56	6	2026-06-06 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 56
57	57	7	2026-06-05 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 57
58	58	8	2026-06-04 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 58
59	59	9	2026-06-03 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 59
60	60	10	2026-06-02 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 60
61	1	11	2026-06-01 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 61
62	2	12	2026-05-31 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 62
63	3	13	2026-05-30 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 63
64	4	14	2026-05-29 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 64
65	5	15	2026-05-28 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 65
66	6	16	2026-05-27 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 66
67	7	17	2026-05-26 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 67
68	8	18	2026-05-25 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 68
69	9	19	2026-05-24 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 69
70	10	20	2026-05-23 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 70
71	11	21	2026-05-22 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 71
72	12	22	2026-05-21 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 72
73	13	23	2026-05-20 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 73
74	14	24	2026-05-19 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 74
75	15	25	2026-05-18 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 75
76	16	26	2026-05-17 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 76
77	17	27	2026-05-16 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 77
78	18	28	2026-05-15 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 78
79	19	29	2026-05-14 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 79
80	20	30	2026-05-13 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 80
81	21	31	2026-05-12 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 81
82	22	32	2026-05-11 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 82
83	23	33	2026-05-10 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 83
84	24	34	2026-05-09 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 84
85	25	35	2026-05-08 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 85
86	26	36	2026-05-07 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 86
87	27	37	2026-05-06 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 87
88	28	38	2026-05-05 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 88
89	29	39	2026-05-04 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 89
90	30	40	2026-05-03 22:11:17.845077	Transición registrada automáticamente para el pedido, evento 90
\.


--
-- TOC entry 4765 (class 0 OID 28045)
-- Dependencies: 262
-- Data for Name: infracciones_mensaje; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.infracciones_mensaje (id_infraccion, id_usuario, id_pedido, fecha_infraccion, mensaje_original, patron_detectado) FROM stdin;
1	1	1	2026-07-31 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 1	Email
2	2	2	2026-07-30 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 2	Teléfono
3	3	3	2026-07-29 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 3	URL Externa
4	4	4	2026-07-28 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 4	Usuario Redes Sociales
5	5	5	2026-07-27 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 5	Email
6	6	6	2026-07-26 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 6	Teléfono
7	7	7	2026-07-25 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 7	URL Externa
8	8	8	2026-07-24 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 8	Usuario Redes Sociales
9	9	9	2026-07-23 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 9	Email
10	10	10	2026-07-22 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 10	Teléfono
11	11	11	2026-07-21 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 11	URL Externa
12	12	12	2026-07-20 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 12	Usuario Redes Sociales
13	13	13	2026-07-19 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 13	Email
14	14	14	2026-07-18 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 14	Teléfono
15	15	15	2026-07-17 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 15	URL Externa
16	16	16	2026-07-16 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 16	Usuario Redes Sociales
17	17	17	2026-07-15 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 17	Email
18	18	18	2026-07-14 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 18	Teléfono
19	19	19	2026-07-13 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 19	URL Externa
20	20	20	2026-07-12 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 20	Usuario Redes Sociales
21	21	21	2026-07-11 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 21	Email
22	22	22	2026-07-10 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 22	Teléfono
23	23	23	2026-07-09 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 23	URL Externa
24	24	24	2026-07-08 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 24	Usuario Redes Sociales
25	25	25	2026-07-07 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 25	Email
26	26	26	2026-07-06 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 26	Teléfono
27	27	27	2026-07-05 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 27	URL Externa
28	28	28	2026-07-04 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 28	Usuario Redes Sociales
29	29	29	2026-07-03 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 29	Email
30	30	30	2026-07-02 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 30	Teléfono
31	31	31	2026-07-01 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 31	URL Externa
32	32	32	2026-06-30 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 32	Usuario Redes Sociales
33	33	33	2026-06-29 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 33	Email
34	34	34	2026-06-28 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 34	Teléfono
35	35	35	2026-06-27 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 35	URL Externa
36	36	36	2026-06-26 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 36	Usuario Redes Sociales
37	37	37	2026-06-25 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 37	Email
38	38	38	2026-06-24 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 38	Teléfono
39	39	39	2026-06-23 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 39	URL Externa
40	40	40	2026-06-22 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 40	Usuario Redes Sociales
41	41	41	2026-06-21 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 41	Email
42	42	42	2026-06-20 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 42	Teléfono
43	43	43	2026-06-19 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 43	URL Externa
44	44	44	2026-06-18 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 44	Usuario Redes Sociales
45	45	45	2026-06-17 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 45	Email
46	46	46	2026-06-16 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 46	Teléfono
47	47	47	2026-06-15 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 47	URL Externa
48	48	48	2026-06-14 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 48	Usuario Redes Sociales
49	49	49	2026-06-13 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 49	Email
50	50	50	2026-06-12 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 50	Teléfono
51	51	51	2026-06-11 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 51	URL Externa
52	52	52	2026-06-10 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 52	Usuario Redes Sociales
53	53	53	2026-06-09 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 53	Email
54	54	54	2026-06-08 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 54	Teléfono
55	55	55	2026-06-07 22:11:17.845077	Contacto directo detectado fuera de la plataforma, mensaje 55	URL Externa
\.


--
-- TOC entry 4767 (class 0 OID 28055)
-- Dependencies: 264
-- Data for Name: likes_portafolio; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.likes_portafolio (id_like, id_item_portafolio, id_usuario, fecha_like) FROM stdin;
1	1	1	2026-07-31 22:11:17.845077
2	2	1	2026-07-30 22:11:17.845077
3	3	1	2026-07-29 22:11:17.845077
4	4	1	2026-07-28 22:11:17.845077
5	5	1	2026-07-27 22:11:17.845077
6	6	1	2026-07-26 22:11:17.845077
7	7	1	2026-07-25 22:11:17.845077
8	8	1	2026-07-24 22:11:17.845077
9	9	1	2026-07-23 22:11:17.845077
10	10	1	2026-07-22 22:11:17.845077
11	11	1	2026-07-21 22:11:17.845077
12	12	1	2026-07-20 22:11:17.845077
13	13	1	2026-07-19 22:11:17.845077
14	14	1	2026-07-18 22:11:17.845077
15	15	1	2026-07-17 22:11:17.845077
16	16	1	2026-07-16 22:11:17.845077
17	17	1	2026-07-15 22:11:17.845077
18	18	1	2026-07-14 22:11:17.845077
19	19	1	2026-07-13 22:11:17.845077
20	20	1	2026-07-12 22:11:17.845077
21	21	1	2026-07-11 22:11:17.845077
22	22	1	2026-07-10 22:11:17.845077
23	23	1	2026-07-09 22:11:17.845077
24	24	1	2026-07-08 22:11:17.845077
25	25	1	2026-07-07 22:11:17.845077
26	26	1	2026-07-06 22:11:17.845077
27	27	1	2026-07-05 22:11:17.845077
28	28	1	2026-07-04 22:11:17.845077
29	29	1	2026-07-03 22:11:17.845077
30	30	1	2026-07-02 22:11:17.845077
31	31	1	2026-07-01 22:11:17.845077
32	32	1	2026-06-30 22:11:17.845077
33	33	1	2026-06-29 22:11:17.845077
34	34	1	2026-06-28 22:11:17.845077
35	35	1	2026-06-27 22:11:17.845077
36	36	1	2026-06-26 22:11:17.845077
37	37	1	2026-06-25 22:11:17.845077
38	38	1	2026-06-24 22:11:17.845077
39	39	1	2026-06-23 22:11:17.845077
40	40	1	2026-06-22 22:11:17.845077
41	41	1	2026-06-21 22:11:17.845077
42	42	1	2026-06-20 22:11:17.845077
43	43	1	2026-06-19 22:11:17.845077
44	44	1	2026-06-18 22:11:17.845077
45	45	1	2026-06-17 22:11:17.845077
46	46	1	2026-06-16 22:11:17.845077
47	47	1	2026-06-15 22:11:17.845077
48	48	1	2026-06-14 22:11:17.845077
49	49	1	2026-06-13 22:11:17.845077
50	50	1	2026-06-12 22:11:17.845077
51	51	1	2026-06-11 22:11:17.845077
52	52	1	2026-06-10 22:11:17.845077
53	53	1	2026-06-09 22:11:17.845077
54	54	1	2026-06-08 22:11:17.845077
55	55	1	2026-06-07 22:11:17.845077
56	56	1	2026-06-06 22:11:17.845077
57	57	1	2026-06-05 22:11:17.845077
58	58	1	2026-06-04 22:11:17.845077
59	59	1	2026-06-03 22:11:17.845077
60	60	1	2026-06-02 22:11:17.845077
61	61	1	2026-06-01 22:11:17.845077
62	62	1	2026-05-31 22:11:17.845077
63	63	1	2026-05-30 22:11:17.845077
64	64	1	2026-05-29 22:11:17.845077
65	65	1	2026-05-28 22:11:17.845077
66	66	1	2026-05-27 22:11:17.845077
67	67	1	2026-05-26 22:11:17.845077
68	68	1	2026-05-25 22:11:17.845077
69	69	1	2026-05-24 22:11:17.845077
70	70	1	2026-05-23 22:11:17.845077
71	1	2	2026-05-22 22:11:17.845077
72	2	2	2026-05-21 22:11:17.845077
73	3	2	2026-05-20 22:11:17.845077
74	4	2	2026-05-19 22:11:17.845077
75	5	2	2026-05-18 22:11:17.845077
76	6	2	2026-05-17 22:11:17.845077
77	7	2	2026-05-16 22:11:17.845077
78	8	2	2026-05-15 22:11:17.845077
79	9	2	2026-05-14 22:11:17.845077
80	10	2	2026-05-13 22:11:17.845077
\.


--
-- TOC entry 4769 (class 0 OID 28063)
-- Dependencies: 266
-- Data for Name: mensajes; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.mensajes (id_mensaje, id_sala, id_remitente, cuerpo_mensaje, fecha_hora_envio, leido) FROM stdin;
1	1	1	Hola, quería consultar sobre el avance del proyecto. Mensaje 1.	2026-07-31 22:11:17.845077	t
2	2	2	Hola, quería consultar sobre el avance del proyecto. Mensaje 2.	2026-07-30 22:11:17.845077	t
3	3	3	Hola, quería consultar sobre el avance del proyecto. Mensaje 3.	2026-07-29 22:11:17.845077	t
4	4	4	Hola, quería consultar sobre el avance del proyecto. Mensaje 4.	2026-07-28 22:11:17.845077	f
5	5	5	Hola, quería consultar sobre el avance del proyecto. Mensaje 5.	2026-07-27 22:11:17.845077	t
6	6	6	Hola, quería consultar sobre el avance del proyecto. Mensaje 6.	2026-07-26 22:11:17.845077	t
7	7	7	Hola, quería consultar sobre el avance del proyecto. Mensaje 7.	2026-07-25 22:11:17.845077	t
8	8	8	Hola, quería consultar sobre el avance del proyecto. Mensaje 8.	2026-07-24 22:11:17.845077	f
9	9	9	Hola, quería consultar sobre el avance del proyecto. Mensaje 9.	2026-07-23 22:11:17.845077	t
10	10	10	Hola, quería consultar sobre el avance del proyecto. Mensaje 10.	2026-07-22 22:11:17.845077	t
11	11	11	Hola, quería consultar sobre el avance del proyecto. Mensaje 11.	2026-07-21 22:11:17.845077	t
12	12	12	Hola, quería consultar sobre el avance del proyecto. Mensaje 12.	2026-07-20 22:11:17.845077	f
13	13	13	Hola, quería consultar sobre el avance del proyecto. Mensaje 13.	2026-07-19 22:11:17.845077	t
14	14	14	Hola, quería consultar sobre el avance del proyecto. Mensaje 14.	2026-07-18 22:11:17.845077	t
15	15	15	Hola, quería consultar sobre el avance del proyecto. Mensaje 15.	2026-07-17 22:11:17.845077	t
16	16	16	Hola, quería consultar sobre el avance del proyecto. Mensaje 16.	2026-07-16 22:11:17.845077	f
17	17	17	Hola, quería consultar sobre el avance del proyecto. Mensaje 17.	2026-07-15 22:11:17.845077	t
18	18	18	Hola, quería consultar sobre el avance del proyecto. Mensaje 18.	2026-07-14 22:11:17.845077	t
19	19	19	Hola, quería consultar sobre el avance del proyecto. Mensaje 19.	2026-07-13 22:11:17.845077	t
20	20	20	Hola, quería consultar sobre el avance del proyecto. Mensaje 20.	2026-07-12 22:11:17.845077	f
21	21	21	Hola, quería consultar sobre el avance del proyecto. Mensaje 21.	2026-07-11 22:11:17.845077	t
22	22	22	Hola, quería consultar sobre el avance del proyecto. Mensaje 22.	2026-07-10 22:11:17.845077	t
23	23	23	Hola, quería consultar sobre el avance del proyecto. Mensaje 23.	2026-07-09 22:11:17.845077	t
24	24	24	Hola, quería consultar sobre el avance del proyecto. Mensaje 24.	2026-07-08 22:11:17.845077	f
25	25	25	Hola, quería consultar sobre el avance del proyecto. Mensaje 25.	2026-07-07 22:11:17.845077	t
26	26	26	Hola, quería consultar sobre el avance del proyecto. Mensaje 26.	2026-07-06 22:11:17.845077	t
27	27	27	Hola, quería consultar sobre el avance del proyecto. Mensaje 27.	2026-07-05 22:11:17.845077	t
28	28	28	Hola, quería consultar sobre el avance del proyecto. Mensaje 28.	2026-07-04 22:11:17.845077	f
29	29	29	Hola, quería consultar sobre el avance del proyecto. Mensaje 29.	2026-07-03 22:11:17.845077	t
30	30	30	Hola, quería consultar sobre el avance del proyecto. Mensaje 30.	2026-07-02 22:11:17.845077	t
31	31	31	Hola, quería consultar sobre el avance del proyecto. Mensaje 31.	2026-07-01 22:11:17.845077	t
32	32	32	Hola, quería consultar sobre el avance del proyecto. Mensaje 32.	2026-06-30 22:11:17.845077	f
33	33	33	Hola, quería consultar sobre el avance del proyecto. Mensaje 33.	2026-06-29 22:11:17.845077	t
34	34	34	Hola, quería consultar sobre el avance del proyecto. Mensaje 34.	2026-06-28 22:11:17.845077	t
35	35	35	Hola, quería consultar sobre el avance del proyecto. Mensaje 35.	2026-06-27 22:11:17.845077	t
36	36	36	Hola, quería consultar sobre el avance del proyecto. Mensaje 36.	2026-06-26 22:11:17.845077	f
37	37	37	Hola, quería consultar sobre el avance del proyecto. Mensaje 37.	2026-06-25 22:11:17.845077	t
38	38	38	Hola, quería consultar sobre el avance del proyecto. Mensaje 38.	2026-06-24 22:11:17.845077	t
39	39	39	Hola, quería consultar sobre el avance del proyecto. Mensaje 39.	2026-06-23 22:11:17.845077	t
40	40	40	Hola, quería consultar sobre el avance del proyecto. Mensaje 40.	2026-06-22 22:11:17.845077	f
41	41	41	Hola, quería consultar sobre el avance del proyecto. Mensaje 41.	2026-06-21 22:11:17.845077	t
42	42	42	Hola, quería consultar sobre el avance del proyecto. Mensaje 42.	2026-06-20 22:11:17.845077	t
43	43	43	Hola, quería consultar sobre el avance del proyecto. Mensaje 43.	2026-06-19 22:11:17.845077	t
44	44	44	Hola, quería consultar sobre el avance del proyecto. Mensaje 44.	2026-06-18 22:11:17.845077	f
45	45	45	Hola, quería consultar sobre el avance del proyecto. Mensaje 45.	2026-06-17 22:11:17.845077	t
46	46	46	Hola, quería consultar sobre el avance del proyecto. Mensaje 46.	2026-06-16 22:11:17.845077	t
47	47	47	Hola, quería consultar sobre el avance del proyecto. Mensaje 47.	2026-06-15 22:11:17.845077	t
48	48	48	Hola, quería consultar sobre el avance del proyecto. Mensaje 48.	2026-06-14 22:11:17.845077	f
49	49	49	Hola, quería consultar sobre el avance del proyecto. Mensaje 49.	2026-06-13 22:11:17.845077	t
50	50	50	Hola, quería consultar sobre el avance del proyecto. Mensaje 50.	2026-06-12 22:11:17.845077	t
51	1	51	Hola, quería consultar sobre el avance del proyecto. Mensaje 51.	2026-06-11 22:11:17.845077	t
52	2	52	Hola, quería consultar sobre el avance del proyecto. Mensaje 52.	2026-06-10 22:11:17.845077	f
53	3	53	Hola, quería consultar sobre el avance del proyecto. Mensaje 53.	2026-06-09 22:11:17.845077	t
54	4	54	Hola, quería consultar sobre el avance del proyecto. Mensaje 54.	2026-06-08 22:11:17.845077	t
55	5	55	Hola, quería consultar sobre el avance del proyecto. Mensaje 55.	2026-06-07 22:11:17.845077	t
56	6	56	Hola, quería consultar sobre el avance del proyecto. Mensaje 56.	2026-06-06 22:11:17.845077	f
57	7	57	Hola, quería consultar sobre el avance del proyecto. Mensaje 57.	2026-06-05 22:11:17.845077	t
58	8	58	Hola, quería consultar sobre el avance del proyecto. Mensaje 58.	2026-06-04 22:11:17.845077	t
59	9	59	Hola, quería consultar sobre el avance del proyecto. Mensaje 59.	2026-06-03 22:11:17.845077	t
60	10	60	Hola, quería consultar sobre el avance del proyecto. Mensaje 60.	2026-06-02 22:11:17.845077	f
61	11	1	Hola, quería consultar sobre el avance del proyecto. Mensaje 61.	2026-06-01 22:11:17.845077	t
62	12	2	Hola, quería consultar sobre el avance del proyecto. Mensaje 62.	2026-05-31 22:11:17.845077	t
63	13	3	Hola, quería consultar sobre el avance del proyecto. Mensaje 63.	2026-05-30 22:11:17.845077	t
64	14	4	Hola, quería consultar sobre el avance del proyecto. Mensaje 64.	2026-05-29 22:11:17.845077	f
65	15	5	Hola, quería consultar sobre el avance del proyecto. Mensaje 65.	2026-05-28 22:11:17.845077	t
66	16	6	Hola, quería consultar sobre el avance del proyecto. Mensaje 66.	2026-05-27 22:11:17.845077	t
67	17	7	Hola, quería consultar sobre el avance del proyecto. Mensaje 67.	2026-05-26 22:11:17.845077	t
68	18	8	Hola, quería consultar sobre el avance del proyecto. Mensaje 68.	2026-05-25 22:11:17.845077	f
69	19	9	Hola, quería consultar sobre el avance del proyecto. Mensaje 69.	2026-05-24 22:11:17.845077	t
70	20	10	Hola, quería consultar sobre el avance del proyecto. Mensaje 70.	2026-05-23 22:11:17.845077	t
71	21	11	Hola, quería consultar sobre el avance del proyecto. Mensaje 71.	2026-05-22 22:11:17.845077	t
72	22	12	Hola, quería consultar sobre el avance del proyecto. Mensaje 72.	2026-05-21 22:11:17.845077	f
73	23	13	Hola, quería consultar sobre el avance del proyecto. Mensaje 73.	2026-05-20 22:11:17.845077	t
74	24	14	Hola, quería consultar sobre el avance del proyecto. Mensaje 74.	2026-05-19 22:11:17.845077	t
75	25	15	Hola, quería consultar sobre el avance del proyecto. Mensaje 75.	2026-05-18 22:11:17.845077	t
76	26	16	Hola, quería consultar sobre el avance del proyecto. Mensaje 76.	2026-05-17 22:11:17.845077	f
77	27	17	Hola, quería consultar sobre el avance del proyecto. Mensaje 77.	2026-05-16 22:11:17.845077	t
78	28	18	Hola, quería consultar sobre el avance del proyecto. Mensaje 78.	2026-05-15 22:11:17.845077	t
79	29	19	Hola, quería consultar sobre el avance del proyecto. Mensaje 79.	2026-05-14 22:11:17.845077	t
80	30	20	Hola, quería consultar sobre el avance del proyecto. Mensaje 80.	2026-05-13 22:11:17.845077	f
81	31	21	Hola, quería consultar sobre el avance del proyecto. Mensaje 81.	2026-05-12 22:11:17.845077	t
82	32	22	Hola, quería consultar sobre el avance del proyecto. Mensaje 82.	2026-05-11 22:11:17.845077	t
83	33	23	Hola, quería consultar sobre el avance del proyecto. Mensaje 83.	2026-05-10 22:11:17.845077	t
84	34	24	Hola, quería consultar sobre el avance del proyecto. Mensaje 84.	2026-05-09 22:11:17.845077	f
85	35	25	Hola, quería consultar sobre el avance del proyecto. Mensaje 85.	2026-05-08 22:11:17.845077	t
86	36	26	Hola, quería consultar sobre el avance del proyecto. Mensaje 86.	2026-05-07 22:11:17.845077	t
87	37	27	Hola, quería consultar sobre el avance del proyecto. Mensaje 87.	2026-05-06 22:11:17.845077	t
88	38	28	Hola, quería consultar sobre el avance del proyecto. Mensaje 88.	2026-05-05 22:11:17.845077	f
89	39	29	Hola, quería consultar sobre el avance del proyecto. Mensaje 89.	2026-05-04 22:11:17.845077	t
90	40	30	Hola, quería consultar sobre el avance del proyecto. Mensaje 90.	2026-05-03 22:11:17.845077	t
\.


--
-- TOC entry 4771 (class 0 OID 28074)
-- Dependencies: 268
-- Data for Name: motivos_rechazo; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.motivos_rechazo (id_motivo, descripcion_motivo) FROM stdin;
1	Calidad Insuficiente - Caso 1
2	No Cumple el Brief - Caso 2
3	Formato Incorrecto - Caso 3
4	Plagio Detectado - Caso 4
5	Contenido Inapropiado - Caso 5
6	Entrega Tardía - Caso 6
7	Comunicación Deficiente - Caso 7
8	Precio No Acordado - Caso 8
9	Cambios No Autorizados - Caso 9
10	Derechos de Autor - Caso 10
11	Calidad Insuficiente - Caso 11
12	No Cumple el Brief - Caso 12
13	Formato Incorrecto - Caso 13
14	Plagio Detectado - Caso 14
15	Contenido Inapropiado - Caso 15
16	Entrega Tardía - Caso 16
17	Comunicación Deficiente - Caso 17
18	Precio No Acordado - Caso 18
19	Cambios No Autorizados - Caso 19
20	Derechos de Autor - Caso 20
21	Calidad Insuficiente - Caso 21
22	No Cumple el Brief - Caso 22
23	Formato Incorrecto - Caso 23
24	Plagio Detectado - Caso 24
25	Contenido Inapropiado - Caso 25
26	Entrega Tardía - Caso 26
27	Comunicación Deficiente - Caso 27
28	Precio No Acordado - Caso 28
29	Cambios No Autorizados - Caso 29
30	Derechos de Autor - Caso 30
31	Calidad Insuficiente - Caso 31
32	No Cumple el Brief - Caso 32
33	Formato Incorrecto - Caso 33
34	Plagio Detectado - Caso 34
35	Contenido Inapropiado - Caso 35
36	Entrega Tardía - Caso 36
37	Comunicación Deficiente - Caso 37
38	Precio No Acordado - Caso 38
39	Cambios No Autorizados - Caso 39
40	Derechos de Autor - Caso 40
41	Calidad Insuficiente - Caso 41
42	No Cumple el Brief - Caso 42
43	Formato Incorrecto - Caso 43
44	Plagio Detectado - Caso 44
45	Contenido Inapropiado - Caso 45
46	Entrega Tardía - Caso 46
47	Comunicación Deficiente - Caso 47
48	Precio No Acordado - Caso 48
49	Cambios No Autorizados - Caso 49
50	Derechos de Autor - Caso 50
\.


--
-- TOC entry 4773 (class 0 OID 28080)
-- Dependencies: 270
-- Data for Name: notificaciones_sistema; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.notificaciones_sistema (id_notificacion, id_usuario, id_tipo_notificacion, fecha_emision, esta_leida) FROM stdin;
1	1	1	2026-07-31 22:11:17.845077	f
2	2	2	2026-07-30 22:11:17.845077	f
3	3	3	2026-07-29 22:11:17.845077	t
4	4	4	2026-07-28 22:11:17.845077	f
5	5	5	2026-07-27 22:11:17.845077	f
6	6	6	2026-07-26 22:11:17.845077	t
7	7	7	2026-07-25 22:11:17.845077	f
8	8	8	2026-07-24 22:11:17.845077	f
9	9	9	2026-07-23 22:11:17.845077	t
10	10	10	2026-07-22 22:11:17.845077	f
11	11	11	2026-07-21 22:11:17.845077	f
12	12	12	2026-07-20 22:11:17.845077	t
13	13	13	2026-07-19 22:11:17.845077	f
14	14	14	2026-07-18 22:11:17.845077	f
15	15	15	2026-07-17 22:11:17.845077	t
16	16	16	2026-07-16 22:11:17.845077	f
17	17	17	2026-07-15 22:11:17.845077	f
18	18	18	2026-07-14 22:11:17.845077	t
19	19	19	2026-07-13 22:11:17.845077	f
20	20	20	2026-07-12 22:11:17.845077	f
21	21	21	2026-07-11 22:11:17.845077	t
22	22	22	2026-07-10 22:11:17.845077	f
23	23	23	2026-07-09 22:11:17.845077	f
24	24	24	2026-07-08 22:11:17.845077	t
25	25	25	2026-07-07 22:11:17.845077	f
26	26	26	2026-07-06 22:11:17.845077	f
27	27	27	2026-07-05 22:11:17.845077	t
28	28	28	2026-07-04 22:11:17.845077	f
29	29	29	2026-07-03 22:11:17.845077	f
30	30	30	2026-07-02 22:11:17.845077	t
31	31	31	2026-07-01 22:11:17.845077	f
32	32	32	2026-06-30 22:11:17.845077	f
33	33	33	2026-06-29 22:11:17.845077	t
34	34	34	2026-06-28 22:11:17.845077	f
35	35	35	2026-06-27 22:11:17.845077	f
36	36	36	2026-06-26 22:11:17.845077	t
37	37	37	2026-06-25 22:11:17.845077	f
38	38	38	2026-06-24 22:11:17.845077	f
39	39	39	2026-06-23 22:11:17.845077	t
40	40	40	2026-06-22 22:11:17.845077	f
41	41	41	2026-06-21 22:11:17.845077	f
42	42	42	2026-06-20 22:11:17.845077	t
43	43	43	2026-06-19 22:11:17.845077	f
44	44	44	2026-06-18 22:11:17.845077	f
45	45	45	2026-06-17 22:11:17.845077	t
46	46	46	2026-06-16 22:11:17.845077	f
47	47	47	2026-06-15 22:11:17.845077	f
48	48	48	2026-06-14 22:11:17.845077	t
49	49	49	2026-06-13 22:11:17.845077	f
50	50	50	2026-06-12 22:11:17.845077	f
51	51	1	2026-06-11 22:11:17.845077	t
52	52	2	2026-06-10 22:11:17.845077	f
53	53	3	2026-06-09 22:11:17.845077	f
54	54	4	2026-06-08 22:11:17.845077	t
55	55	5	2026-06-07 22:11:17.845077	f
56	56	6	2026-06-06 22:11:17.845077	f
57	57	7	2026-06-05 22:11:17.845077	t
58	58	8	2026-06-04 22:11:17.845077	f
59	59	9	2026-06-03 22:11:17.845077	f
60	60	10	2026-06-02 22:11:17.845077	t
61	1	11	2026-06-01 22:11:17.845077	f
62	2	12	2026-05-31 22:11:17.845077	f
63	3	13	2026-05-30 22:11:17.845077	t
64	4	14	2026-05-29 22:11:17.845077	f
65	5	15	2026-05-28 22:11:17.845077	f
66	6	16	2026-05-27 22:11:17.845077	t
67	7	17	2026-05-26 22:11:17.845077	f
68	8	18	2026-05-25 22:11:17.845077	f
69	9	19	2026-05-24 22:11:17.845077	t
70	10	20	2026-05-23 22:11:17.845077	f
71	11	21	2026-05-22 22:11:17.845077	f
72	12	22	2026-05-21 22:11:17.845077	t
73	13	23	2026-05-20 22:11:17.845077	f
74	14	24	2026-05-19 22:11:17.845077	f
75	15	25	2026-05-18 22:11:17.845077	t
76	16	26	2026-05-17 22:11:17.845077	f
77	17	27	2026-05-16 22:11:17.845077	f
78	18	28	2026-05-15 22:11:17.845077	t
79	19	29	2026-05-14 22:11:17.845077	f
80	20	30	2026-05-13 22:11:17.845077	f
81	1	51	2026-08-17 01:16:30.530233	f
82	2	51	2026-08-17 01:16:30.968774	f
83	2	51	2026-08-17 01:16:31.872345	f
84	1	51	2026-08-17 01:16:32.132259	f
85	2	51	2026-08-17 01:16:32.88005	f
86	1	51	2026-08-17 01:16:33.138252	f
87	1	51	2026-08-17 01:16:33.888549	f
88	2	51	2026-08-17 01:16:34.51154	f
89	1	51	2026-08-17 01:16:34.761982	f
90	1	51	2026-08-17 01:16:35.507951	f
91	1	51	2026-08-17 01:16:36.219371	f
\.


--
-- TOC entry 4775 (class 0 OID 28089)
-- Dependencies: 272
-- Data for Name: pagos_garantia; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.pagos_garantia (id_pago, id_contrato, id_orden_paypal, monto_retenido, estado_fondos) FROM stdin;
1	1	PAYPAL-ORD-100001	51.00	Retenido
2	2	PAYPAL-ORD-100002	82.00	Liberado
3	3	PAYPAL-ORD-100003	113.00	Reembolsado
4	4	PAYPAL-ORD-100004	144.00	En Disputa
5	5	PAYPAL-ORD-100005	175.00	Retenido
6	6	PAYPAL-ORD-100006	206.00	Liberado
7	7	PAYPAL-ORD-100007	237.00	Reembolsado
8	8	PAYPAL-ORD-100008	268.00	En Disputa
9	9	PAYPAL-ORD-100009	299.00	Retenido
10	10	PAYPAL-ORD-100010	330.00	Liberado
11	11	PAYPAL-ORD-100011	361.00	Reembolsado
12	12	PAYPAL-ORD-100012	392.00	En Disputa
13	13	PAYPAL-ORD-100013	423.00	Retenido
14	14	PAYPAL-ORD-100014	454.00	Liberado
15	15	PAYPAL-ORD-100015	485.00	Reembolsado
16	16	PAYPAL-ORD-100016	516.00	En Disputa
17	17	PAYPAL-ORD-100017	547.00	Retenido
18	18	PAYPAL-ORD-100018	578.00	Liberado
19	19	PAYPAL-ORD-100019	609.00	Reembolsado
20	20	PAYPAL-ORD-100020	640.00	En Disputa
21	21	PAYPAL-ORD-100021	671.00	Retenido
22	22	PAYPAL-ORD-100022	702.00	Liberado
23	23	PAYPAL-ORD-100023	733.00	Reembolsado
24	24	PAYPAL-ORD-100024	764.00	En Disputa
25	25	PAYPAL-ORD-100025	795.00	Retenido
26	26	PAYPAL-ORD-100026	826.00	Liberado
27	27	PAYPAL-ORD-100027	857.00	Reembolsado
28	28	PAYPAL-ORD-100028	888.00	En Disputa
29	29	PAYPAL-ORD-100029	919.00	Retenido
30	30	PAYPAL-ORD-100030	950.00	Liberado
31	31	PAYPAL-ORD-100031	981.00	Reembolsado
32	32	PAYPAL-ORD-100032	42.00	En Disputa
33	33	PAYPAL-ORD-100033	73.00	Retenido
34	34	PAYPAL-ORD-100034	104.00	Liberado
35	35	PAYPAL-ORD-100035	135.00	Reembolsado
36	36	PAYPAL-ORD-100036	166.00	En Disputa
37	37	PAYPAL-ORD-100037	197.00	Retenido
38	38	PAYPAL-ORD-100038	228.00	Liberado
39	39	PAYPAL-ORD-100039	259.00	Reembolsado
40	40	PAYPAL-ORD-100040	290.00	En Disputa
41	41	PAYPAL-ORD-100041	321.00	Retenido
42	42	PAYPAL-ORD-100042	352.00	Liberado
43	43	PAYPAL-ORD-100043	383.00	Reembolsado
44	44	PAYPAL-ORD-100044	414.00	En Disputa
45	45	PAYPAL-ORD-100045	445.00	Retenido
46	46	PAYPAL-ORD-100046	476.00	Liberado
47	47	PAYPAL-ORD-100047	507.00	Reembolsado
48	48	PAYPAL-ORD-100048	538.00	En Disputa
49	49	PAYPAL-ORD-100049	569.00	Retenido
50	50	PAYPAL-ORD-100050	600.00	Liberado
\.


--
-- TOC entry 4777 (class 0 OID 28097)
-- Dependencies: 274
-- Data for Name: pais; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.pais (id_pais, nombre_pais) FROM stdin;
1	Argentina
2	México
3	España
4	Colombia
5	Chile
6	Perú
7	Ecuador
8	Venezuela
9	Uruguay
10	Paraguay
11	Bolivia
12	Costa Rica
13	Panamá
14	Guatemala
15	Honduras
16	El Salvador
17	Nicaragua
18	República Dominicana
19	Cuba
20	Puerto Rico
21	Estados Unidos
22	Canadá
23	Brasil
24	Portugal
25	Francia
26	Italia
27	Alemania
28	Reino Unido
29	Países Bajos
30	Bélgica
31	Suiza
32	Austria
33	Suecia
34	Noruega
35	Dinamarca
36	Finlandia
37	Irlanda
38	Polonia
39	República Checa
40	Hungría
41	Grecia
42	Turquía
43	Rusia
44	Japón
45	China
46	Corea del Sur
47	India
48	Australia
49	Nueva Zelanda
50	Sudáfrica
51	Afganistán
52	Albania
54	Andorra
55	Angola
56	Antigua y Barbuda
57	Arabia Saudita
58	Argelia
60	Armenia
63	Azerbaiyán
64	Bahamas
65	Bangladés
66	Barbados
67	Baréin
69	Belice
70	Benín
71	Bielorrusia
72	Birmania
74	Bosnia y Herzegovina
75	Botsuana
77	Brunéi
78	Bulgaria
79	Burkina Faso
80	Burundi
81	Bután
82	Cabo Verde
83	Camboya
84	Camerún
86	Catar
87	Chad
90	Chipre
91	Ciudad del Vaticano
93	Comoras
94	Corea del Norte
96	Costa de Marfil
98	Croacia
101	Dominica
103	Egipto
105	Emiratos Árabes Unidos
106	Eritrea
107	Eslovaquia
108	Eslovenia
111	Estonia
112	Etiopía
113	Filipinas
115	Fiyi
117	Gabón
118	Gambia
119	Georgia
120	Ghana
121	Granada
124	Guyana
125	Guinea
126	Guinea Ecuatorial
127	Guinea-Bisáu
128	Haití
132	Indonesia
133	Irak
134	Irán
136	Islandia
137	Islas Marshall
138	Islas Salomón
139	Israel
141	Jamaica
143	Jordania
144	Kazajistán
145	Kenia
146	Kirguistán
147	Kiribati
148	Kuwait
149	Laos
150	Lesoto
151	Letonia
152	Líbano
153	Liberia
154	Libia
155	Liechtenstein
156	Lituania
157	Luxemburgo
158	Macedonia del Norte
159	Madagascar
160	Malasia
161	Malaui
162	Maldivas
163	Malí
164	Malta
165	Marruecos
166	Mauricio
167	Mauritania
169	Micronesia
170	Moldavia
171	Mónaco
172	Mongolia
173	Montenegro
174	Mozambique
175	Namibia
176	Nauru
177	Nepal
179	Níger
180	Nigeria
183	Omán
185	Pakistán
186	Palaos
187	Palestina
189	Papúa Nueva Guinea
196	República Centroafricana
198	República del Congo
199	República Democrática del Congo
201	Ruanda
202	Rumanía
204	Samoa
205	San Cristóbal y Nieves
206	San Marino
207	San Vicente y las Granadinas
208	Santa Lucía
209	Santo Tomé y Príncipe
210	Senegal
211	Serbia
212	Seychelles
213	Sierra Leona
214	Singapur
215	Siria
216	Somalia
217	Sri Lanka
218	Suazilandia
220	Sudán
221	Sudán del Sur
224	Surinam
225	Tailandia
226	Tanzania
227	Tayikistán
228	Timor Oriental
229	Togo
230	Tonga
231	Trinidad y Tobago
232	Túnez
233	Turkmenistán
235	Tuvalu
236	Ucrania
237	Uganda
239	Uzbekistán
240	Vanuatu
242	Vietnam
243	Yemen
244	Yibuti
245	Zambia
246	Zimbabue
\.


--
-- TOC entry 4779 (class 0 OID 28103)
-- Dependencies: 276
-- Data for Name: participantes_sorteo; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.participantes_sorteo (id_participacion, id_sorteo, id_usuario, fecha_inscripcion, es_ganador, fecha_notificacion_premio) FROM stdin;
2	2	1	2026-07-30 22:11:17.845077	f	\N
3	3	1	2026-07-29 22:11:17.845077	f	\N
5	5	1	2026-07-27 22:11:17.845077	f	\N
6	6	1	2026-07-26 22:11:17.845077	f	\N
8	8	1	2026-07-24 22:11:17.845077	f	\N
9	9	1	2026-07-23 22:11:17.845077	f	\N
11	11	1	2026-07-21 22:11:17.845077	f	\N
12	12	1	2026-07-20 22:11:17.845077	f	\N
14	14	1	2026-07-18 22:11:17.845077	f	\N
15	15	1	2026-07-17 22:11:17.845077	t	2026-07-17 22:11:17.845077
16	16	1	2026-07-16 22:11:17.845077	f	\N
17	17	1	2026-07-15 22:11:17.845077	f	\N
18	18	1	2026-07-14 22:11:17.845077	f	\N
19	19	1	2026-07-13 22:11:17.845077	f	\N
20	20	1	2026-07-12 22:11:17.845077	f	\N
21	21	1	2026-07-11 22:11:17.845077	f	\N
22	22	1	2026-07-10 22:11:17.845077	f	\N
23	23	1	2026-07-09 22:11:17.845077	f	\N
24	24	1	2026-07-08 22:11:17.845077	f	\N
25	25	1	2026-07-07 22:11:17.845077	f	\N
26	26	1	2026-07-06 22:11:17.845077	f	\N
27	27	1	2026-07-05 22:11:17.845077	f	\N
28	28	1	2026-07-04 22:11:17.845077	f	\N
29	29	1	2026-07-03 22:11:17.845077	f	\N
30	30	1	2026-07-02 22:11:17.845077	t	2026-08-01 22:11:17.845077
31	31	1	2026-07-01 22:11:17.845077	f	\N
32	32	1	2026-06-30 22:11:17.845077	f	\N
33	33	1	2026-06-29 22:11:17.845077	f	\N
34	34	1	2026-06-28 22:11:17.845077	f	\N
35	35	1	2026-06-27 22:11:17.845077	f	\N
36	36	1	2026-06-26 22:11:17.845077	f	\N
37	37	1	2026-06-25 22:11:17.845077	f	\N
38	38	1	2026-06-24 22:11:17.845077	f	\N
39	39	1	2026-06-23 22:11:17.845077	f	\N
40	40	1	2026-06-22 22:11:17.845077	f	\N
41	41	1	2026-06-21 22:11:17.845077	f	\N
42	42	1	2026-06-20 22:11:17.845077	f	\N
43	43	1	2026-06-19 22:11:17.845077	f	\N
44	44	1	2026-06-18 22:11:17.845077	f	\N
45	45	1	2026-06-17 22:11:17.845077	t	2026-07-17 22:11:17.845077
47	47	1	2026-06-15 22:11:17.845077	f	\N
48	48	1	2026-06-14 22:11:17.845077	f	\N
50	50	1	2026-06-12 22:11:17.845077	f	\N
52	2	2	2026-06-10 22:11:17.845077	f	\N
53	3	2	2026-06-09 22:11:17.845077	f	\N
55	5	2	2026-06-07 22:11:17.845077	f	\N
56	6	2	2026-06-06 22:11:17.845077	f	\N
58	8	2	2026-06-04 22:11:17.845077	f	\N
59	9	2	2026-06-03 22:11:17.845077	f	\N
60	10	2	2026-06-02 22:11:17.845077	t	2026-08-01 22:11:17.845077
61	11	2	2026-06-01 22:11:17.845077	f	\N
62	12	2	2026-05-31 22:11:17.845077	f	\N
64	14	2	2026-05-29 22:11:17.845077	f	\N
65	15	2	2026-05-28 22:11:17.845077	f	\N
66	16	2	2026-05-27 22:11:17.845077	f	\N
67	17	2	2026-05-26 22:11:17.845077	f	\N
68	18	2	2026-05-25 22:11:17.845077	f	\N
69	19	2	2026-05-24 22:11:17.845077	f	\N
70	20	2	2026-05-23 22:11:17.845077	f	\N
1	1	1	2026-07-31 22:11:17.845077	t	2026-08-17 01:16:30.141111
51	1	2	2026-06-11 22:11:17.845077	t	2026-08-17 01:16:30.141111
4	4	1	2026-07-28 22:11:17.845077	t	2026-08-17 01:16:31.746494
54	4	2	2026-06-08 22:11:17.845077	t	2026-08-17 01:16:31.746494
7	7	1	2026-07-25 22:11:17.845077	t	2026-08-17 01:16:32.751111
57	7	2	2026-06-05 22:11:17.845077	t	2026-08-17 01:16:32.751111
10	10	1	2026-07-22 22:11:17.845077	t	2026-08-17 01:16:33.76117
13	13	1	2026-07-19 22:11:17.845077	t	2026-08-17 01:16:34.382606
63	13	2	2026-05-30 22:11:17.845077	t	2026-08-17 01:16:34.382606
46	46	1	2026-06-16 22:11:17.845077	t	2026-08-17 01:16:35.381418
49	49	1	2026-06-13 22:11:17.845077	t	2026-08-17 01:16:36.036332
\.


--
-- TOC entry 4781 (class 0 OID 28112)
-- Dependencies: 278
-- Data for Name: pedidos; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.pedidos (id_pedido, id_usuario_cliente, id_servicio, id_flujo, fecha_inicio, fecha_entrega_estimada, precio_pactado) FROM stdin;
1	1	1	1	2026-07-31 22:11:17.845077	2026-08-03 22:11:17.845077	51.00
2	2	2	2	2026-07-30 22:11:17.845077	2026-08-04 22:11:17.845077	82.00
3	3	3	3	2026-07-29 22:11:17.845077	2026-08-05 22:11:17.845077	113.00
4	4	4	4	2026-07-28 22:11:17.845077	2026-08-06 22:11:17.845077	144.00
5	5	5	5	2026-07-27 22:11:17.845077	2026-08-07 22:11:17.845077	175.00
6	6	6	6	2026-07-26 22:11:17.845077	2026-08-08 22:11:17.845077	206.00
7	7	7	7	2026-07-25 22:11:17.845077	2026-08-09 22:11:17.845077	237.00
8	8	8	8	2026-07-24 22:11:17.845077	2026-08-10 22:11:17.845077	268.00
9	9	9	9	2026-07-23 22:11:17.845077	2026-08-11 22:11:17.845077	299.00
10	10	10	10	2026-07-22 22:11:17.845077	2026-08-12 22:11:17.845077	330.00
11	11	11	11	2026-07-21 22:11:17.845077	2026-08-13 22:11:17.845077	361.00
12	12	12	12	2026-07-20 22:11:17.845077	2026-08-14 22:11:17.845077	392.00
13	13	13	13	2026-07-19 22:11:17.845077	2026-08-15 22:11:17.845077	423.00
14	14	14	14	2026-07-18 22:11:17.845077	2026-08-16 22:11:17.845077	454.00
15	15	15	15	2026-07-17 22:11:17.845077	2026-08-17 22:11:17.845077	485.00
16	16	16	16	2026-07-16 22:11:17.845077	2026-08-18 22:11:17.845077	516.00
17	17	17	17	2026-07-15 22:11:17.845077	2026-08-19 22:11:17.845077	547.00
18	18	18	18	2026-07-14 22:11:17.845077	2026-08-20 22:11:17.845077	578.00
19	19	19	19	2026-07-13 22:11:17.845077	2026-08-21 22:11:17.845077	609.00
20	20	20	20	2026-07-12 22:11:17.845077	2026-08-02 22:11:17.845077	640.00
21	21	21	21	2026-07-11 22:11:17.845077	2026-08-03 22:11:17.845077	671.00
22	22	22	22	2026-07-10 22:11:17.845077	2026-08-04 22:11:17.845077	702.00
23	23	23	23	2026-07-09 22:11:17.845077	2026-08-05 22:11:17.845077	733.00
24	24	24	24	2026-07-08 22:11:17.845077	2026-08-06 22:11:17.845077	764.00
25	25	25	25	2026-07-07 22:11:17.845077	2026-08-07 22:11:17.845077	795.00
26	26	26	26	2026-07-06 22:11:17.845077	2026-08-08 22:11:17.845077	826.00
27	27	27	27	2026-07-05 22:11:17.845077	2026-08-09 22:11:17.845077	857.00
28	28	28	28	2026-07-04 22:11:17.845077	2026-08-10 22:11:17.845077	888.00
29	29	29	29	2026-07-03 22:11:17.845077	2026-08-11 22:11:17.845077	919.00
30	30	30	30	2026-07-02 22:11:17.845077	2026-08-12 22:11:17.845077	950.00
31	31	31	31	2026-07-01 22:11:17.845077	2026-08-13 22:11:17.845077	981.00
32	32	32	32	2026-06-30 22:11:17.845077	2026-08-14 22:11:17.845077	42.00
33	33	33	33	2026-06-29 22:11:17.845077	2026-08-15 22:11:17.845077	73.00
34	34	34	34	2026-06-28 22:11:17.845077	2026-08-16 22:11:17.845077	104.00
35	35	35	35	2026-06-27 22:11:17.845077	2026-08-17 22:11:17.845077	135.00
36	36	36	36	2026-06-26 22:11:17.845077	2026-08-18 22:11:17.845077	166.00
37	37	37	37	2026-06-25 22:11:17.845077	2026-08-19 22:11:17.845077	197.00
38	38	38	38	2026-06-24 22:11:17.845077	2026-08-20 22:11:17.845077	228.00
39	39	39	39	2026-06-23 22:11:17.845077	2026-08-21 22:11:17.845077	259.00
40	40	40	40	2026-06-22 22:11:17.845077	2026-08-02 22:11:17.845077	290.00
41	41	41	41	2026-06-21 22:11:17.845077	2026-08-03 22:11:17.845077	321.00
42	42	42	42	2026-06-20 22:11:17.845077	2026-08-04 22:11:17.845077	352.00
43	43	43	43	2026-06-19 22:11:17.845077	2026-08-05 22:11:17.845077	383.00
44	44	44	44	2026-06-18 22:11:17.845077	2026-08-06 22:11:17.845077	414.00
45	45	45	45	2026-06-17 22:11:17.845077	2026-08-07 22:11:17.845077	445.00
46	46	46	46	2026-06-16 22:11:17.845077	2026-08-08 22:11:17.845077	476.00
47	47	47	47	2026-06-15 22:11:17.845077	2026-08-09 22:11:17.845077	507.00
48	48	48	48	2026-06-14 22:11:17.845077	2026-08-10 22:11:17.845077	538.00
49	49	49	49	2026-06-13 22:11:17.845077	2026-08-11 22:11:17.845077	569.00
50	50	50	50	2026-06-12 22:11:17.845077	2026-08-12 22:11:17.845077	600.00
51	51	51	1	2026-06-11 22:11:17.845077	2026-08-13 22:11:17.845077	631.00
52	52	52	2	2026-06-10 22:11:17.845077	2026-08-14 22:11:17.845077	662.00
53	53	53	3	2026-06-09 22:11:17.845077	2026-08-15 22:11:17.845077	693.00
54	54	54	4	2026-06-08 22:11:17.845077	2026-08-16 22:11:17.845077	724.00
55	55	55	5	2026-06-07 22:11:17.845077	2026-08-17 22:11:17.845077	755.00
56	56	56	6	2026-06-06 22:11:17.845077	2026-08-18 22:11:17.845077	786.00
57	57	57	7	2026-06-05 22:11:17.845077	2026-08-19 22:11:17.845077	817.00
58	58	58	8	2026-06-04 22:11:17.845077	2026-08-20 22:11:17.845077	848.00
59	59	59	9	2026-06-03 22:11:17.845077	2026-08-21 22:11:17.845077	879.00
60	60	60	10	2026-06-02 22:11:17.845077	2026-08-02 22:11:17.845077	910.00
\.


--
-- TOC entry 4783 (class 0 OID 28122)
-- Dependencies: 280
-- Data for Name: perfiles_creadores; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.perfiles_creadores (id_perfil, id_usuario, biografia, url_red_social) FROM stdin;
1	1	Creador profesional especializado en proyectos creativos. Perfil número 1.	https://redsocial.example.com/creador1
2	2	Creador profesional especializado en proyectos creativos. Perfil número 2.	https://redsocial.example.com/creador2
3	3	Creador profesional especializado en proyectos creativos. Perfil número 3.	https://redsocial.example.com/creador3
4	4	Creador profesional especializado en proyectos creativos. Perfil número 4.	https://redsocial.example.com/creador4
5	5	Creador profesional especializado en proyectos creativos. Perfil número 5.	https://redsocial.example.com/creador5
6	6	Creador profesional especializado en proyectos creativos. Perfil número 6.	https://redsocial.example.com/creador6
7	7	Creador profesional especializado en proyectos creativos. Perfil número 7.	https://redsocial.example.com/creador7
8	8	Creador profesional especializado en proyectos creativos. Perfil número 8.	https://redsocial.example.com/creador8
9	9	Creador profesional especializado en proyectos creativos. Perfil número 9.	https://redsocial.example.com/creador9
10	10	Creador profesional especializado en proyectos creativos. Perfil número 10.	https://redsocial.example.com/creador10
11	11	Creador profesional especializado en proyectos creativos. Perfil número 11.	https://redsocial.example.com/creador11
12	12	Creador profesional especializado en proyectos creativos. Perfil número 12.	https://redsocial.example.com/creador12
13	13	Creador profesional especializado en proyectos creativos. Perfil número 13.	https://redsocial.example.com/creador13
14	14	Creador profesional especializado en proyectos creativos. Perfil número 14.	https://redsocial.example.com/creador14
15	15	Creador profesional especializado en proyectos creativos. Perfil número 15.	https://redsocial.example.com/creador15
16	16	Creador profesional especializado en proyectos creativos. Perfil número 16.	https://redsocial.example.com/creador16
17	17	Creador profesional especializado en proyectos creativos. Perfil número 17.	https://redsocial.example.com/creador17
18	18	Creador profesional especializado en proyectos creativos. Perfil número 18.	https://redsocial.example.com/creador18
19	19	Creador profesional especializado en proyectos creativos. Perfil número 19.	https://redsocial.example.com/creador19
20	20	Creador profesional especializado en proyectos creativos. Perfil número 20.	https://redsocial.example.com/creador20
21	21	Creador profesional especializado en proyectos creativos. Perfil número 21.	https://redsocial.example.com/creador21
22	22	Creador profesional especializado en proyectos creativos. Perfil número 22.	https://redsocial.example.com/creador22
23	23	Creador profesional especializado en proyectos creativos. Perfil número 23.	https://redsocial.example.com/creador23
24	24	Creador profesional especializado en proyectos creativos. Perfil número 24.	https://redsocial.example.com/creador24
25	25	Creador profesional especializado en proyectos creativos. Perfil número 25.	https://redsocial.example.com/creador25
26	26	Creador profesional especializado en proyectos creativos. Perfil número 26.	https://redsocial.example.com/creador26
27	27	Creador profesional especializado en proyectos creativos. Perfil número 27.	https://redsocial.example.com/creador27
28	28	Creador profesional especializado en proyectos creativos. Perfil número 28.	https://redsocial.example.com/creador28
29	29	Creador profesional especializado en proyectos creativos. Perfil número 29.	https://redsocial.example.com/creador29
30	30	Creador profesional especializado en proyectos creativos. Perfil número 30.	https://redsocial.example.com/creador30
31	31	Creador profesional especializado en proyectos creativos. Perfil número 31.	https://redsocial.example.com/creador31
32	32	Creador profesional especializado en proyectos creativos. Perfil número 32.	https://redsocial.example.com/creador32
33	33	Creador profesional especializado en proyectos creativos. Perfil número 33.	https://redsocial.example.com/creador33
34	34	Creador profesional especializado en proyectos creativos. Perfil número 34.	https://redsocial.example.com/creador34
35	35	Creador profesional especializado en proyectos creativos. Perfil número 35.	https://redsocial.example.com/creador35
36	36	Creador profesional especializado en proyectos creativos. Perfil número 36.	https://redsocial.example.com/creador36
37	37	Creador profesional especializado en proyectos creativos. Perfil número 37.	https://redsocial.example.com/creador37
38	38	Creador profesional especializado en proyectos creativos. Perfil número 38.	https://redsocial.example.com/creador38
39	39	Creador profesional especializado en proyectos creativos. Perfil número 39.	https://redsocial.example.com/creador39
40	40	Creador profesional especializado en proyectos creativos. Perfil número 40.	https://redsocial.example.com/creador40
41	41	Creador profesional especializado en proyectos creativos. Perfil número 41.	https://redsocial.example.com/creador41
42	42	Creador profesional especializado en proyectos creativos. Perfil número 42.	https://redsocial.example.com/creador42
43	43	Creador profesional especializado en proyectos creativos. Perfil número 43.	https://redsocial.example.com/creador43
44	44	Creador profesional especializado en proyectos creativos. Perfil número 44.	https://redsocial.example.com/creador44
45	45	Creador profesional especializado en proyectos creativos. Perfil número 45.	https://redsocial.example.com/creador45
46	46	Creador profesional especializado en proyectos creativos. Perfil número 46.	https://redsocial.example.com/creador46
47	47	Creador profesional especializado en proyectos creativos. Perfil número 47.	https://redsocial.example.com/creador47
48	48	Creador profesional especializado en proyectos creativos. Perfil número 48.	https://redsocial.example.com/creador48
49	49	Creador profesional especializado en proyectos creativos. Perfil número 49.	https://redsocial.example.com/creador49
50	50	Creador profesional especializado en proyectos creativos. Perfil número 50.	https://redsocial.example.com/creador50
51	68	prueba	https://instragram.com/ejemplo
52	69	Perfil creado por Administrador	\N
\.


--
-- TOC entry 4785 (class 0 OID 28130)
-- Dependencies: 282
-- Data for Name: permisos; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.permisos (id_permiso, nombre_permiso, modulo_aplicacion) FROM stdin;
1	usuarios.crear	Usuarios
2	usuarios.editar	Roles
3	usuarios.eliminar	Servicios
4	usuarios.ver	Pedidos
5	roles.crear	Pagos
6	roles.editar	Contratos
7	roles.eliminar	Mensajes
8	roles.ver	Portafolio
9	permisos.crear	Sorteos
10	permisos.asignar	Notificaciones
11	servicios.crear	Reportes
12	servicios.editar	Sistema
13	servicios.eliminar	Usuarios
14	servicios.publicar	Roles
15	servicios.pausar	Servicios
16	pedidos.crear	Pedidos
17	pedidos.ver	Pagos
18	pedidos.cancelar	Contratos
19	pedidos.gestionar	Mensajes
20	pagos.ver	Portafolio
21	pagos.liberar	Sorteos
22	pagos.reembolsar	Notificaciones
23	contratos.crear	Reportes
24	contratos.firmar	Sistema
25	contratos.anular	Usuarios
26	mensajes.enviar	Roles
27	mensajes.moderar	Servicios
28	mensajes.eliminar	Pedidos
29	portafolio.crear	Pagos
30	portafolio.editar	Contratos
31	portafolio.eliminar	Mensajes
32	comentarios.moderar	Portafolio
33	resenas.moderar	Sorteos
34	sorteos.crear	Notificaciones
35	sorteos.gestionar	Reportes
36	sorteos.sortear	Sistema
37	notificaciones.enviar	Usuarios
38	notificaciones.gestionar	Roles
39	reportes.ver	Servicios
40	reportes.exportar	Pedidos
41	auditoria.ver	Pagos
42	configuracion.editar	Contratos
43	categorias.gestionar	Mensajes
44	etiquetas.gestionar	Portafolio
45	habilidades.gestionar	Sorteos
46	certificados.aprobar	Notificaciones
47	certificados.rechazar	Reportes
48	tickets.gestionar	Sistema
49	tickets.resolver	Usuarios
50	dashboard.ver	Roles
51	USUARIO_VER	SEGURIDAD
52	USUARIO_CREAR	SEGURIDAD
53	USUARIO_EDITAR	SEGURIDAD
54	USUARIO_ELIMINAR	SEGURIDAD
55	USUARIO_SUSPENDER	SEGURIDAD
56	ROL_VER	SEGURIDAD
57	ROL_GESTIONAR	SEGURIDAD
58	PERMISO_VER	SEGURIDAD
59	ROL_ASIGNAR_PERMISO	SEGURIDAD
60	SESION_REVOCAR	SEGURIDAD
61	PAIS_VER	SISTEMA
62	PAIS_CREAR	SISTEMA
63	PAIS_EDITAR	SISTEMA
64	PAIS_ELIMINAR	SISTEMA
65	PORTAFOLIO_CREAR	PORTAFOLIO
66	PORTAFOLIO_MODERAR	PORTAFOLIO
67	CERTIFICADO_REVISAR	PORTAFOLIO
68	CATEGORIA_GESTIONAR	CATALOGO
69	SERVICIO_CREAR	CATALOGO
70	SERVICIO_MODERAR	CATALOGO
71	PEDIDO_CREAR	PEDIDOS
72	PEDIDO_GESTIONAR	PEDIDOS
73	TICKET_REVISAR	PEDIDOS
74	TICKET_RESOLVER	PEDIDOS
75	CONTRATO_VER	FINANZAS
76	CONTRATO_FIRMAR	FINANZAS
77	PAGO_AUDITAR	FINANZAS
78	FONDOS_LIBERAR	FINANZAS
79	TRANSACCION_VER	FINANZAS
80	SALA_VER	COMUNICACION
81	MENSAJE_ENVIAR	COMUNICACION
82	MENSAJE_MODERAR	COMUNICACION
83	NOTIFICACION_ENVIAR	COMUNICACION
84	COMENTARIO_MODERAR	SOCIAL
85	SORTEO_CREAR	SOCIAL
\.


--
-- TOC entry 4787 (class 0 OID 28138)
-- Dependencies: 284
-- Data for Name: plantillas_contrato; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.plantillas_contrato (id_plantilla, version_legal, cuerpo_html_plantilla) FROM stdin;
1	v1.1-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 1.</p></body></html>
2	v1.2-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 2.</p></body></html>
3	v1.3-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 3.</p></body></html>
4	v1.4-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 4.</p></body></html>
5	v1.5-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 5.</p></body></html>
6	v1.6-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 6.</p></body></html>
7	v1.7-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 7.</p></body></html>
8	v1.8-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 8.</p></body></html>
9	v1.9-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 9.</p></body></html>
10	v1.10-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 10.</p></body></html>
11	v1.11-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 11.</p></body></html>
12	v1.12-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 12.</p></body></html>
13	v1.13-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 13.</p></body></html>
14	v1.14-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 14.</p></body></html>
15	v1.15-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 15.</p></body></html>
16	v1.16-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 16.</p></body></html>
17	v1.17-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 17.</p></body></html>
18	v1.18-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 18.</p></body></html>
19	v1.19-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 19.</p></body></html>
20	v1.20-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 20.</p></body></html>
21	v1.21-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 21.</p></body></html>
22	v1.22-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 22.</p></body></html>
23	v1.23-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 23.</p></body></html>
24	v1.24-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 24.</p></body></html>
25	v1.25-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 25.</p></body></html>
26	v1.26-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 26.</p></body></html>
27	v1.27-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 27.</p></body></html>
28	v1.28-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 28.</p></body></html>
29	v1.29-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 29.</p></body></html>
30	v1.30-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 30.</p></body></html>
31	v1.31-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 31.</p></body></html>
32	v1.32-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 32.</p></body></html>
33	v1.33-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 33.</p></body></html>
34	v1.34-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 34.</p></body></html>
35	v1.35-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 35.</p></body></html>
36	v1.36-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 36.</p></body></html>
37	v1.37-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 37.</p></body></html>
38	v1.38-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 38.</p></body></html>
39	v1.39-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 39.</p></body></html>
40	v1.40-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 40.</p></body></html>
41	v1.41-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 41.</p></body></html>
42	v1.42-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 42.</p></body></html>
43	v1.43-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 43.</p></body></html>
44	v1.44-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 44.</p></body></html>
45	v1.45-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 45.</p></body></html>
46	v1.46-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 46.</p></body></html>
47	v1.47-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 47.</p></body></html>
48	v1.48-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 48.</p></body></html>
49	v1.49-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 49.</p></body></html>
50	v1.50-2024	<html><body><h1>Contrato de Servicio</h1><p>Plantilla legal versión 50.</p></body></html>
\.


--
-- TOC entry 4789 (class 0 OID 28147)
-- Dependencies: 286
-- Data for Name: portafolio_items; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.portafolio_items (id_item_portafolio, id_portafolio, titulo_obra, descripcion_obra, url_archivo_multimedia, fecha_subida) FROM stdin;
1	1	Obra Creativa #1	Descripción de la obra número 1, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra1.jpg	2026-07-31 22:11:17.845077
2	2	Obra Creativa #2	Descripción de la obra número 2, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra2.jpg	2026-07-30 22:11:17.845077
3	3	Obra Creativa #3	Descripción de la obra número 3, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra3.jpg	2026-07-29 22:11:17.845077
4	4	Obra Creativa #4	Descripción de la obra número 4, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra4.jpg	2026-07-28 22:11:17.845077
5	5	Obra Creativa #5	Descripción de la obra número 5, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra5.jpg	2026-07-27 22:11:17.845077
6	6	Obra Creativa #6	Descripción de la obra número 6, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra6.jpg	2026-07-26 22:11:17.845077
7	7	Obra Creativa #7	Descripción de la obra número 7, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra7.jpg	2026-07-25 22:11:17.845077
8	8	Obra Creativa #8	Descripción de la obra número 8, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra8.jpg	2026-07-24 22:11:17.845077
9	9	Obra Creativa #9	Descripción de la obra número 9, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra9.jpg	2026-07-23 22:11:17.845077
10	10	Obra Creativa #10	Descripción de la obra número 10, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra10.jpg	2026-07-22 22:11:17.845077
11	11	Obra Creativa #11	Descripción de la obra número 11, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra11.jpg	2026-07-21 22:11:17.845077
12	12	Obra Creativa #12	Descripción de la obra número 12, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra12.jpg	2026-07-20 22:11:17.845077
13	13	Obra Creativa #13	Descripción de la obra número 13, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra13.jpg	2026-07-19 22:11:17.845077
14	14	Obra Creativa #14	Descripción de la obra número 14, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra14.jpg	2026-07-18 22:11:17.845077
15	15	Obra Creativa #15	Descripción de la obra número 15, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra15.jpg	2026-07-17 22:11:17.845077
16	16	Obra Creativa #16	Descripción de la obra número 16, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra16.jpg	2026-07-16 22:11:17.845077
17	17	Obra Creativa #17	Descripción de la obra número 17, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra17.jpg	2026-07-15 22:11:17.845077
18	18	Obra Creativa #18	Descripción de la obra número 18, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra18.jpg	2026-07-14 22:11:17.845077
19	19	Obra Creativa #19	Descripción de la obra número 19, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra19.jpg	2026-07-13 22:11:17.845077
20	20	Obra Creativa #20	Descripción de la obra número 20, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra20.jpg	2026-07-12 22:11:17.845077
21	21	Obra Creativa #21	Descripción de la obra número 21, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra21.jpg	2026-07-11 22:11:17.845077
22	22	Obra Creativa #22	Descripción de la obra número 22, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra22.jpg	2026-07-10 22:11:17.845077
23	23	Obra Creativa #23	Descripción de la obra número 23, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra23.jpg	2026-07-09 22:11:17.845077
24	24	Obra Creativa #24	Descripción de la obra número 24, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra24.jpg	2026-07-08 22:11:17.845077
25	25	Obra Creativa #25	Descripción de la obra número 25, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra25.jpg	2026-07-07 22:11:17.845077
26	26	Obra Creativa #26	Descripción de la obra número 26, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra26.jpg	2026-07-06 22:11:17.845077
27	27	Obra Creativa #27	Descripción de la obra número 27, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra27.jpg	2026-07-05 22:11:17.845077
28	28	Obra Creativa #28	Descripción de la obra número 28, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra28.jpg	2026-07-04 22:11:17.845077
29	29	Obra Creativa #29	Descripción de la obra número 29, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra29.jpg	2026-07-03 22:11:17.845077
30	30	Obra Creativa #30	Descripción de la obra número 30, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra30.jpg	2026-07-02 22:11:17.845077
31	31	Obra Creativa #31	Descripción de la obra número 31, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra31.jpg	2026-07-01 22:11:17.845077
32	32	Obra Creativa #32	Descripción de la obra número 32, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra32.jpg	2026-06-30 22:11:17.845077
33	33	Obra Creativa #33	Descripción de la obra número 33, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra33.jpg	2026-06-29 22:11:17.845077
34	34	Obra Creativa #34	Descripción de la obra número 34, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra34.jpg	2026-06-28 22:11:17.845077
35	35	Obra Creativa #35	Descripción de la obra número 35, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra35.jpg	2026-06-27 22:11:17.845077
36	36	Obra Creativa #36	Descripción de la obra número 36, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra36.jpg	2026-06-26 22:11:17.845077
37	37	Obra Creativa #37	Descripción de la obra número 37, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra37.jpg	2026-06-25 22:11:17.845077
38	38	Obra Creativa #38	Descripción de la obra número 38, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra38.jpg	2026-06-24 22:11:17.845077
39	39	Obra Creativa #39	Descripción de la obra número 39, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra39.jpg	2026-06-23 22:11:17.845077
40	40	Obra Creativa #40	Descripción de la obra número 40, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra40.jpg	2026-06-22 22:11:17.845077
41	41	Obra Creativa #41	Descripción de la obra número 41, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra41.jpg	2026-06-21 22:11:17.845077
42	42	Obra Creativa #42	Descripción de la obra número 42, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra42.jpg	2026-06-20 22:11:17.845077
43	43	Obra Creativa #43	Descripción de la obra número 43, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra43.jpg	2026-06-19 22:11:17.845077
44	44	Obra Creativa #44	Descripción de la obra número 44, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra44.jpg	2026-06-18 22:11:17.845077
45	45	Obra Creativa #45	Descripción de la obra número 45, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra45.jpg	2026-06-17 22:11:17.845077
46	46	Obra Creativa #46	Descripción de la obra número 46, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra46.jpg	2026-06-16 22:11:17.845077
47	47	Obra Creativa #47	Descripción de la obra número 47, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra47.jpg	2026-06-15 22:11:17.845077
48	48	Obra Creativa #48	Descripción de la obra número 48, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra48.jpg	2026-06-14 22:11:17.845077
49	49	Obra Creativa #49	Descripción de la obra número 49, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra49.jpg	2026-06-13 22:11:17.845077
50	50	Obra Creativa #50	Descripción de la obra número 50, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra50.jpg	2026-06-12 22:11:17.845077
51	1	Obra Creativa #51	Descripción de la obra número 51, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra51.jpg	2026-06-11 22:11:17.845077
52	2	Obra Creativa #52	Descripción de la obra número 52, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra52.jpg	2026-06-10 22:11:17.845077
53	3	Obra Creativa #53	Descripción de la obra número 53, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra53.jpg	2026-06-09 22:11:17.845077
54	4	Obra Creativa #54	Descripción de la obra número 54, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra54.jpg	2026-06-08 22:11:17.845077
55	5	Obra Creativa #55	Descripción de la obra número 55, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra55.jpg	2026-06-07 22:11:17.845077
56	6	Obra Creativa #56	Descripción de la obra número 56, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra56.jpg	2026-06-06 22:11:17.845077
57	7	Obra Creativa #57	Descripción de la obra número 57, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra57.jpg	2026-06-05 22:11:17.845077
58	8	Obra Creativa #58	Descripción de la obra número 58, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra58.jpg	2026-06-04 22:11:17.845077
59	9	Obra Creativa #59	Descripción de la obra número 59, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra59.jpg	2026-06-03 22:11:17.845077
60	10	Obra Creativa #60	Descripción de la obra número 60, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra60.jpg	2026-06-02 22:11:17.845077
61	11	Obra Creativa #61	Descripción de la obra número 61, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra61.jpg	2026-06-01 22:11:17.845077
62	12	Obra Creativa #62	Descripción de la obra número 62, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra62.jpg	2026-05-31 22:11:17.845077
63	13	Obra Creativa #63	Descripción de la obra número 63, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra63.jpg	2026-05-30 22:11:17.845077
64	14	Obra Creativa #64	Descripción de la obra número 64, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra64.jpg	2026-05-29 22:11:17.845077
65	15	Obra Creativa #65	Descripción de la obra número 65, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra65.jpg	2026-05-28 22:11:17.845077
66	16	Obra Creativa #66	Descripción de la obra número 66, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra66.jpg	2026-05-27 22:11:17.845077
67	17	Obra Creativa #67	Descripción de la obra número 67, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra67.jpg	2026-05-26 22:11:17.845077
68	18	Obra Creativa #68	Descripción de la obra número 68, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra68.jpg	2026-05-25 22:11:17.845077
69	19	Obra Creativa #69	Descripción de la obra número 69, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra69.jpg	2026-05-24 22:11:17.845077
70	20	Obra Creativa #70	Descripción de la obra número 70, realizada como pieza destacada del portafolio.	https://cdn.example.com/portafolio/obra70.jpg	2026-05-23 22:11:17.845077
71	51	Prueba	prueba	portafolio/b6dcf63c-e985-457d-b24a-b90405296638.png	2026-08-17 19:01:12.292167
\.


--
-- TOC entry 4791 (class 0 OID 28158)
-- Dependencies: 288
-- Data for Name: portafolios; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.portafolios (id_portafolio, id_perfil, fecha_creacion, total_visitas_acumuladas, es_publico, actualizado_en, opciones_personalizacion) FROM stdin;
1	1	2026-07-31 22:11:17.845077	37	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#FFFFFF", "surface": "#ffffff", "secondary": "#6c757d"}
2	2	2026-07-30 22:11:17.845077	74	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#000000", "surface": "#ffffff", "secondary": "#6c757d"}
3	3	2026-07-29 22:11:17.845077	111	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#3498DB", "surface": "#ffffff", "secondary": "#6c757d"}
4	4	2026-07-28 22:11:17.845077	148	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#E74C3C", "surface": "#ffffff", "secondary": "#6c757d"}
5	5	2026-07-27 22:11:17.845077	185	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#2ECC71", "surface": "#ffffff", "secondary": "#6c757d"}
6	6	2026-07-26 22:11:17.845077	222	f	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#F39C12", "surface": "#ffffff", "secondary": "#6c757d"}
7	7	2026-07-25 22:11:17.845077	259	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#9B59B6", "surface": "#ffffff", "secondary": "#6c757d"}
8	8	2026-07-24 22:11:17.845077	296	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#1ABC9C", "surface": "#ffffff", "secondary": "#6c757d"}
9	9	2026-07-23 22:11:17.845077	333	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#FFFFFF", "surface": "#ffffff", "secondary": "#6c757d"}
10	10	2026-07-22 22:11:17.845077	370	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#000000", "surface": "#ffffff", "secondary": "#6c757d"}
11	11	2026-07-21 22:11:17.845077	407	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#3498DB", "surface": "#ffffff", "secondary": "#6c757d"}
12	12	2026-07-20 22:11:17.845077	444	f	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#E74C3C", "surface": "#ffffff", "secondary": "#6c757d"}
13	13	2026-07-19 22:11:17.845077	481	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#2ECC71", "surface": "#ffffff", "secondary": "#6c757d"}
14	14	2026-07-18 22:11:17.845077	518	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#F39C12", "surface": "#ffffff", "secondary": "#6c757d"}
15	15	2026-07-17 22:11:17.845077	555	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#9B59B6", "surface": "#ffffff", "secondary": "#6c757d"}
16	16	2026-07-16 22:11:17.845077	592	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#1ABC9C", "surface": "#ffffff", "secondary": "#6c757d"}
17	17	2026-07-15 22:11:17.845077	629	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#FFFFFF", "surface": "#ffffff", "secondary": "#6c757d"}
18	18	2026-07-14 22:11:17.845077	666	f	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#000000", "surface": "#ffffff", "secondary": "#6c757d"}
19	19	2026-07-13 22:11:17.845077	703	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#3498DB", "surface": "#ffffff", "secondary": "#6c757d"}
20	20	2026-07-12 22:11:17.845077	740	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#E74C3C", "surface": "#ffffff", "secondary": "#6c757d"}
21	21	2026-07-11 22:11:17.845077	777	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#2ECC71", "surface": "#ffffff", "secondary": "#6c757d"}
22	22	2026-07-10 22:11:17.845077	814	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#F39C12", "surface": "#ffffff", "secondary": "#6c757d"}
23	23	2026-07-09 22:11:17.845077	851	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#9B59B6", "surface": "#ffffff", "secondary": "#6c757d"}
24	24	2026-07-08 22:11:17.845077	888	f	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#1ABC9C", "surface": "#ffffff", "secondary": "#6c757d"}
25	25	2026-07-07 22:11:17.845077	925	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#FFFFFF", "surface": "#ffffff", "secondary": "#6c757d"}
26	26	2026-07-06 22:11:17.845077	962	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#000000", "surface": "#ffffff", "secondary": "#6c757d"}
27	27	2026-07-05 22:11:17.845077	999	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#3498DB", "surface": "#ffffff", "secondary": "#6c757d"}
28	28	2026-07-04 22:11:17.845077	1036	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#E74C3C", "surface": "#ffffff", "secondary": "#6c757d"}
29	29	2026-07-03 22:11:17.845077	1073	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#2ECC71", "surface": "#ffffff", "secondary": "#6c757d"}
30	30	2026-07-02 22:11:17.845077	1110	f	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#F39C12", "surface": "#ffffff", "secondary": "#6c757d"}
31	31	2026-07-01 22:11:17.845077	1147	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#9B59B6", "surface": "#ffffff", "secondary": "#6c757d"}
32	32	2026-06-30 22:11:17.845077	1184	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#1ABC9C", "surface": "#ffffff", "secondary": "#6c757d"}
33	33	2026-06-29 22:11:17.845077	1221	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#FFFFFF", "surface": "#ffffff", "secondary": "#6c757d"}
34	34	2026-06-28 22:11:17.845077	1258	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#000000", "surface": "#ffffff", "secondary": "#6c757d"}
35	35	2026-06-27 22:11:17.845077	1295	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#3498DB", "surface": "#ffffff", "secondary": "#6c757d"}
36	36	2026-06-26 22:11:17.845077	1332	f	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#E74C3C", "surface": "#ffffff", "secondary": "#6c757d"}
37	37	2026-06-25 22:11:17.845077	1369	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#2ECC71", "surface": "#ffffff", "secondary": "#6c757d"}
38	38	2026-06-24 22:11:17.845077	1406	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#F39C12", "surface": "#ffffff", "secondary": "#6c757d"}
39	39	2026-06-23 22:11:17.845077	1443	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#9B59B6", "surface": "#ffffff", "secondary": "#6c757d"}
40	40	2026-06-22 22:11:17.845077	1480	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#1ABC9C", "surface": "#ffffff", "secondary": "#6c757d"}
41	41	2026-06-21 22:11:17.845077	1517	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#FFFFFF", "surface": "#ffffff", "secondary": "#6c757d"}
42	42	2026-06-20 22:11:17.845077	1554	f	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#000000", "surface": "#ffffff", "secondary": "#6c757d"}
43	43	2026-06-19 22:11:17.845077	1591	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#3498DB", "surface": "#ffffff", "secondary": "#6c757d"}
44	44	2026-06-18 22:11:17.845077	1628	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#E74C3C", "surface": "#ffffff", "secondary": "#6c757d"}
45	45	2026-06-17 22:11:17.845077	1665	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#2ECC71", "surface": "#ffffff", "secondary": "#6c757d"}
46	46	2026-06-16 22:11:17.845077	1702	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#F39C12", "surface": "#ffffff", "secondary": "#6c757d"}
47	47	2026-06-15 22:11:17.845077	1739	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#9B59B6", "surface": "#ffffff", "secondary": "#6c757d"}
48	48	2026-06-14 22:11:17.845077	1776	f	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#1ABC9C", "surface": "#ffffff", "secondary": "#6c757d"}
49	49	2026-06-13 22:11:17.845077	1813	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#FFFFFF", "surface": "#ffffff", "secondary": "#6c757d"}
50	50	2026-06-12 22:11:17.845077	1850	t	2026-08-16 16:32:18.584613+00	{"bg": "#f8f9fa", "text": "#212529", "primary": "#000000", "surface": "#ffffff", "secondary": "#6c757d"}
51	52	2026-08-17 19:00:24.452017	0	t	2026-08-17 19:00:24.611195+00	{"bg": "#EFF2F7", "text": "#1E293B", "primary": "#0F9B8E", "surface": "#FFFFFF", "secondary": "#203A43"}
\.


--
-- TOC entry 4793 (class 0 OID 28169)
-- Dependencies: 290
-- Data for Name: resenas_servicios; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.resenas_servicios (id_resena, id_pedido, calificacion_estrellas, texto_resena, fecha_resena) FROM stdin;
1	1	2	Muy buen servicio, cumplió con lo prometido. Reseña número 1.	2026-07-31 22:11:17.845077
2	2	3	Muy buen servicio, cumplió con lo prometido. Reseña número 2.	2026-07-30 22:11:17.845077
3	3	4	Muy buen servicio, cumplió con lo prometido. Reseña número 3.	2026-07-29 22:11:17.845077
4	4	5	Muy buen servicio, cumplió con lo prometido. Reseña número 4.	2026-07-28 22:11:17.845077
5	5	1	Muy buen servicio, cumplió con lo prometido. Reseña número 5.	2026-07-27 22:11:17.845077
6	6	2	Muy buen servicio, cumplió con lo prometido. Reseña número 6.	2026-07-26 22:11:17.845077
7	7	3	Muy buen servicio, cumplió con lo prometido. Reseña número 7.	2026-07-25 22:11:17.845077
8	8	4	Muy buen servicio, cumplió con lo prometido. Reseña número 8.	2026-07-24 22:11:17.845077
9	9	5	Muy buen servicio, cumplió con lo prometido. Reseña número 9.	2026-07-23 22:11:17.845077
10	10	1	Muy buen servicio, cumplió con lo prometido. Reseña número 10.	2026-07-22 22:11:17.845077
11	11	2	Muy buen servicio, cumplió con lo prometido. Reseña número 11.	2026-07-21 22:11:17.845077
12	12	3	Muy buen servicio, cumplió con lo prometido. Reseña número 12.	2026-07-20 22:11:17.845077
13	13	4	Muy buen servicio, cumplió con lo prometido. Reseña número 13.	2026-07-19 22:11:17.845077
14	14	5	Muy buen servicio, cumplió con lo prometido. Reseña número 14.	2026-07-18 22:11:17.845077
15	15	1	Muy buen servicio, cumplió con lo prometido. Reseña número 15.	2026-07-17 22:11:17.845077
16	16	2	Muy buen servicio, cumplió con lo prometido. Reseña número 16.	2026-07-16 22:11:17.845077
17	17	3	Muy buen servicio, cumplió con lo prometido. Reseña número 17.	2026-07-15 22:11:17.845077
18	18	4	Muy buen servicio, cumplió con lo prometido. Reseña número 18.	2026-07-14 22:11:17.845077
19	19	5	Muy buen servicio, cumplió con lo prometido. Reseña número 19.	2026-07-13 22:11:17.845077
20	20	1	Muy buen servicio, cumplió con lo prometido. Reseña número 20.	2026-07-12 22:11:17.845077
21	21	2	Muy buen servicio, cumplió con lo prometido. Reseña número 21.	2026-07-11 22:11:17.845077
22	22	3	Muy buen servicio, cumplió con lo prometido. Reseña número 22.	2026-07-10 22:11:17.845077
23	23	4	Muy buen servicio, cumplió con lo prometido. Reseña número 23.	2026-07-09 22:11:17.845077
24	24	5	Muy buen servicio, cumplió con lo prometido. Reseña número 24.	2026-07-08 22:11:17.845077
25	25	1	Muy buen servicio, cumplió con lo prometido. Reseña número 25.	2026-07-07 22:11:17.845077
26	26	2	Muy buen servicio, cumplió con lo prometido. Reseña número 26.	2026-07-06 22:11:17.845077
27	27	3	Muy buen servicio, cumplió con lo prometido. Reseña número 27.	2026-07-05 22:11:17.845077
28	28	4	Muy buen servicio, cumplió con lo prometido. Reseña número 28.	2026-07-04 22:11:17.845077
29	29	5	Muy buen servicio, cumplió con lo prometido. Reseña número 29.	2026-07-03 22:11:17.845077
30	30	1	Muy buen servicio, cumplió con lo prometido. Reseña número 30.	2026-07-02 22:11:17.845077
31	31	2	Muy buen servicio, cumplió con lo prometido. Reseña número 31.	2026-07-01 22:11:17.845077
32	32	3	Muy buen servicio, cumplió con lo prometido. Reseña número 32.	2026-06-30 22:11:17.845077
33	33	4	Muy buen servicio, cumplió con lo prometido. Reseña número 33.	2026-06-29 22:11:17.845077
34	34	5	Muy buen servicio, cumplió con lo prometido. Reseña número 34.	2026-06-28 22:11:17.845077
35	35	1	Muy buen servicio, cumplió con lo prometido. Reseña número 35.	2026-06-27 22:11:17.845077
36	36	2	Muy buen servicio, cumplió con lo prometido. Reseña número 36.	2026-06-26 22:11:17.845077
37	37	3	Muy buen servicio, cumplió con lo prometido. Reseña número 37.	2026-06-25 22:11:17.845077
38	38	4	Muy buen servicio, cumplió con lo prometido. Reseña número 38.	2026-06-24 22:11:17.845077
39	39	5	Muy buen servicio, cumplió con lo prometido. Reseña número 39.	2026-06-23 22:11:17.845077
40	40	1	Muy buen servicio, cumplió con lo prometido. Reseña número 40.	2026-06-22 22:11:17.845077
41	41	2	Muy buen servicio, cumplió con lo prometido. Reseña número 41.	2026-06-21 22:11:17.845077
42	42	3	Muy buen servicio, cumplió con lo prometido. Reseña número 42.	2026-06-20 22:11:17.845077
43	43	4	Muy buen servicio, cumplió con lo prometido. Reseña número 43.	2026-06-19 22:11:17.845077
44	44	5	Muy buen servicio, cumplió con lo prometido. Reseña número 44.	2026-06-18 22:11:17.845077
45	45	1	Muy buen servicio, cumplió con lo prometido. Reseña número 45.	2026-06-17 22:11:17.845077
46	46	2	Muy buen servicio, cumplió con lo prometido. Reseña número 46.	2026-06-16 22:11:17.845077
47	47	3	Muy buen servicio, cumplió con lo prometido. Reseña número 47.	2026-06-15 22:11:17.845077
48	48	4	Muy buen servicio, cumplió con lo prometido. Reseña número 48.	2026-06-14 22:11:17.845077
49	49	5	Muy buen servicio, cumplió con lo prometido. Reseña número 49.	2026-06-13 22:11:17.845077
50	50	1	Muy buen servicio, cumplió con lo prometido. Reseña número 50.	2026-06-12 22:11:17.845077
\.


--
-- TOC entry 4795 (class 0 OID 28179)
-- Dependencies: 292
-- Data for Name: rol_permisos; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.rol_permisos (id_rol_permiso, id_rol, id_permiso) FROM stdin;
1	1	1
2	2	1
3	3	1
4	4	1
5	5	1
6	6	1
7	7	1
8	8	1
9	9	1
10	10	1
11	11	1
12	12	1
13	13	1
14	14	1
15	15	1
16	16	1
17	17	1
18	18	1
19	19	1
20	20	1
21	21	1
22	22	1
23	23	1
24	24	1
25	25	1
26	26	1
27	27	1
28	28	1
29	29	1
30	30	1
31	31	1
32	32	1
33	33	1
34	34	1
35	35	1
36	36	1
37	37	1
38	38	1
39	39	1
40	40	1
41	41	1
42	42	1
43	43	1
44	44	1
45	45	1
46	46	1
47	47	1
48	48	1
49	49	1
50	50	1
51	1	2
52	2	2
53	3	2
54	4	2
55	5	2
56	6	2
57	7	2
58	8	2
59	9	2
60	10	2
63	1	3
64	1	4
65	1	5
66	1	6
67	1	7
68	1	8
69	1	9
70	1	10
71	1	11
72	1	12
73	1	13
74	1	14
75	1	15
76	1	16
77	1	17
78	1	18
79	1	19
80	1	20
81	1	21
82	1	22
83	1	23
84	1	24
85	1	25
86	1	26
87	1	27
88	1	28
89	1	29
90	1	30
91	1	31
92	1	32
93	1	33
94	1	34
95	1	35
96	1	36
97	1	37
98	1	38
99	1	39
100	1	40
101	1	41
102	1	42
103	1	43
104	1	44
105	1	45
106	1	46
107	1	47
108	1	48
109	1	49
110	1	50
111	1	51
112	1	52
113	1	53
114	1	54
115	1	55
116	1	56
117	1	57
118	1	58
119	1	59
120	1	60
121	1	61
122	1	62
123	1	63
124	1	64
125	1	65
126	1	66
127	1	67
128	1	68
129	1	69
130	1	70
131	1	71
132	1	72
133	1	73
134	1	74
135	1	75
136	1	76
137	1	77
138	1	78
139	1	79
140	1	80
141	1	81
142	1	82
143	1	83
144	1	84
145	1	85
146	52	56
147	52	61
148	52	66
149	52	67
150	52	68
151	52	70
152	52	82
153	52	83
154	52	84
155	53	51
156	53	55
157	53	56
158	53	58
159	53	60
160	53	61
161	53	73
162	53	74
163	53	80
164	53	83
165	54	56
166	54	61
167	54	75
168	54	77
169	54	78
170	54	79
171	5	65
172	5	69
173	5	72
174	5	73
175	5	75
176	5	76
177	5	80
178	5	81
179	5	85
180	3	71
181	3	73
182	3	75
183	3	76
184	3	80
185	3	81
\.


--
-- TOC entry 4797 (class 0 OID 28186)
-- Dependencies: 294
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.roles (id_rol, nombre_rol, descripcion_rol) FROM stdin;
2	Super Administrador	Descripción del rol número 2
4	Cliente VIP	Descripción del rol número 4
6	Creador Verificado	Descripción del rol número 6
7	Moderador de Contenido	Descripción del rol número 7
8	Moderador de Chat	Descripción del rol número 8
9	Soporte Nivel 1	Descripción del rol número 9
10	Soporte Nivel 2	Descripción del rol número 10
11	Editor	Descripción del rol número 11
12	Supervisor	Descripción del rol número 12
13	Auditor	Descripción del rol número 13
14	Finanzas	Descripción del rol número 14
15	Marketing	Descripción del rol número 15
16	Ventas	Descripción del rol número 16
17	Legal	Descripción del rol número 17
18	Recursos Humanos	Descripción del rol número 18
19	Desarrollador	Descripción del rol número 19
20	QA Tester	Descripción del rol número 20
21	DevOps	Descripción del rol número 21
22	Analista de Datos	Descripción del rol número 22
23	Community Manager	Descripción del rol número 23
24	Diseñador Interno	Descripción del rol número 24
25	Gestor de Contenido	Descripción del rol número 25
26	Gestor de Pagos	Descripción del rol número 26
27	Gestor de Disputas	Descripción del rol número 27
28	Embajador de Marca	Descripción del rol número 28
29	Socio Estratégico	Descripción del rol número 29
30	Freelancer Premium	Descripción del rol número 30
31	Freelancer Básico	Descripción del rol número 31
32	Invitado	Descripción del rol número 32
33	Colaborador Externo	Descripción del rol número 33
34	Revisor de Certificados	Descripción del rol número 34
35	Gestor de Sorteos	Descripción del rol número 35
36	Gestor de Notificaciones	Descripción del rol número 36
37	Administrador de Roles	Descripción del rol número 37
38	Administrador de Permisos	Descripción del rol número 38
39	Administrador de Categorías	Descripción del rol número 39
40	Administrador de Pagos	Descripción del rol número 40
41	Analista de Fraude	Descripción del rol número 41
42	Especialista en SEO	Descripción del rol número 42
43	Especialista en Redes	Descripción del rol número 43
44	Gestor de Comunidad	Descripción del rol número 44
45	Coordinador de Proyectos	Descripción del rol número 45
46	Gerente Regional	Descripción del rol número 46
47	Gerente General	Descripción del rol número 47
48	Director de Operaciones	Descripción del rol número 48
49	Director Ejecutivo	Descripción del rol número 49
50	Consultor Externo	Descripción del rol número 50
1	ADMIN	Administrador General del Sistema con acceso irrestricto a todos los módulos
52	MODERADOR	Responsable de revisar certificados IA, portafolios, comentarios e infracciones
53	SOPORTE	Asistencia técnica, consulta de usuarios, sesiones 2FA y resolución de tickets
54	AUDITOR_FINANCIERO	Auditoría de contratos, supervisión de pagos Escrow y transacciones
5	CREADOR	Artista o creador que ofrece servicios digitales y publica portafolio
3	CLIENTE	Comprador que explora el catálogo, contrata servicios y realiza pagos
57	PRUEBAS	
\.


--
-- TOC entry 4799 (class 0 OID 28194)
-- Dependencies: 296
-- Data for Name: salas_chat; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.salas_chat (id_sala, id_pedido, fecha_apertura, sala_activa) FROM stdin;
1	1	2026-07-31 22:11:17.845077	t
2	2	2026-07-30 22:11:17.845077	t
3	3	2026-07-29 22:11:17.845077	t
4	4	2026-07-28 22:11:17.845077	t
5	5	2026-07-27 22:11:17.845077	f
6	6	2026-07-26 22:11:17.845077	t
7	7	2026-07-25 22:11:17.845077	t
8	8	2026-07-24 22:11:17.845077	t
9	9	2026-07-23 22:11:17.845077	t
10	10	2026-07-22 22:11:17.845077	f
11	11	2026-07-21 22:11:17.845077	t
12	12	2026-07-20 22:11:17.845077	t
13	13	2026-07-19 22:11:17.845077	t
14	14	2026-07-18 22:11:17.845077	t
15	15	2026-07-17 22:11:17.845077	f
16	16	2026-07-16 22:11:17.845077	t
17	17	2026-07-15 22:11:17.845077	t
18	18	2026-07-14 22:11:17.845077	t
19	19	2026-07-13 22:11:17.845077	t
20	20	2026-07-12 22:11:17.845077	f
21	21	2026-07-11 22:11:17.845077	t
22	22	2026-07-10 22:11:17.845077	t
23	23	2026-07-09 22:11:17.845077	t
24	24	2026-07-08 22:11:17.845077	t
25	25	2026-07-07 22:11:17.845077	f
26	26	2026-07-06 22:11:17.845077	t
27	27	2026-07-05 22:11:17.845077	t
28	28	2026-07-04 22:11:17.845077	t
29	29	2026-07-03 22:11:17.845077	t
30	30	2026-07-02 22:11:17.845077	f
31	31	2026-07-01 22:11:17.845077	t
32	32	2026-06-30 22:11:17.845077	t
33	33	2026-06-29 22:11:17.845077	t
34	34	2026-06-28 22:11:17.845077	t
35	35	2026-06-27 22:11:17.845077	f
36	36	2026-06-26 22:11:17.845077	t
37	37	2026-06-25 22:11:17.845077	t
38	38	2026-06-24 22:11:17.845077	t
39	39	2026-06-23 22:11:17.845077	t
40	40	2026-06-22 22:11:17.845077	f
41	41	2026-06-21 22:11:17.845077	t
42	42	2026-06-20 22:11:17.845077	t
43	43	2026-06-19 22:11:17.845077	t
44	44	2026-06-18 22:11:17.845077	t
45	45	2026-06-17 22:11:17.845077	f
46	46	2026-06-16 22:11:17.845077	t
47	47	2026-06-15 22:11:17.845077	t
48	48	2026-06-14 22:11:17.845077	t
49	49	2026-06-13 22:11:17.845077	t
50	50	2026-06-12 22:11:17.845077	f
\.


--
-- TOC entry 4801 (class 0 OID 28202)
-- Dependencies: 298
-- Data for Name: seguidores; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.seguidores (id_seguimiento, id_usuario_seguidor, id_perfil_creador, fecha_seguimiento, notificaciones_activas) FROM stdin;
1	1	1	2026-07-31 22:11:17.845077	t
2	2	1	2026-07-30 22:11:17.845077	t
3	3	1	2026-07-29 22:11:17.845077	t
4	4	1	2026-07-28 22:11:17.845077	f
5	5	1	2026-07-27 22:11:17.845077	t
6	6	1	2026-07-26 22:11:17.845077	t
7	7	1	2026-07-25 22:11:17.845077	t
8	8	1	2026-07-24 22:11:17.845077	f
9	9	1	2026-07-23 22:11:17.845077	t
10	10	1	2026-07-22 22:11:17.845077	t
11	11	1	2026-07-21 22:11:17.845077	t
12	12	1	2026-07-20 22:11:17.845077	f
13	13	1	2026-07-19 22:11:17.845077	t
14	14	1	2026-07-18 22:11:17.845077	t
15	15	1	2026-07-17 22:11:17.845077	t
16	16	1	2026-07-16 22:11:17.845077	f
17	17	1	2026-07-15 22:11:17.845077	t
18	18	1	2026-07-14 22:11:17.845077	t
19	19	1	2026-07-13 22:11:17.845077	t
20	20	1	2026-07-12 22:11:17.845077	f
21	21	1	2026-07-11 22:11:17.845077	t
22	22	1	2026-07-10 22:11:17.845077	t
23	23	1	2026-07-09 22:11:17.845077	t
24	24	1	2026-07-08 22:11:17.845077	f
25	25	1	2026-07-07 22:11:17.845077	t
26	26	1	2026-07-06 22:11:17.845077	t
27	27	1	2026-07-05 22:11:17.845077	t
28	28	1	2026-07-04 22:11:17.845077	f
29	29	1	2026-07-03 22:11:17.845077	t
30	30	1	2026-07-02 22:11:17.845077	t
31	31	1	2026-07-01 22:11:17.845077	t
32	32	1	2026-06-30 22:11:17.845077	f
33	33	1	2026-06-29 22:11:17.845077	t
34	34	1	2026-06-28 22:11:17.845077	t
35	35	1	2026-06-27 22:11:17.845077	t
36	36	1	2026-06-26 22:11:17.845077	f
37	37	1	2026-06-25 22:11:17.845077	t
38	38	1	2026-06-24 22:11:17.845077	t
39	39	1	2026-06-23 22:11:17.845077	t
40	40	1	2026-06-22 22:11:17.845077	f
41	41	1	2026-06-21 22:11:17.845077	t
42	42	1	2026-06-20 22:11:17.845077	t
43	43	1	2026-06-19 22:11:17.845077	t
44	44	1	2026-06-18 22:11:17.845077	f
45	45	1	2026-06-17 22:11:17.845077	t
46	46	1	2026-06-16 22:11:17.845077	t
47	47	1	2026-06-15 22:11:17.845077	t
48	48	1	2026-06-14 22:11:17.845077	f
49	49	1	2026-06-13 22:11:17.845077	t
50	50	1	2026-06-12 22:11:17.845077	t
51	51	1	2026-06-11 22:11:17.845077	t
52	52	1	2026-06-10 22:11:17.845077	f
53	53	1	2026-06-09 22:11:17.845077	t
54	54	1	2026-06-08 22:11:17.845077	t
55	55	1	2026-06-07 22:11:17.845077	t
56	56	1	2026-06-06 22:11:17.845077	f
57	57	1	2026-06-05 22:11:17.845077	t
58	58	1	2026-06-04 22:11:17.845077	t
59	59	1	2026-06-03 22:11:17.845077	t
60	60	1	2026-06-02 22:11:17.845077	f
61	1	2	2026-06-01 22:11:17.845077	t
62	2	2	2026-05-31 22:11:17.845077	t
63	3	2	2026-05-30 22:11:17.845077	t
64	4	2	2026-05-29 22:11:17.845077	f
65	5	2	2026-05-28 22:11:17.845077	t
66	6	2	2026-05-27 22:11:17.845077	t
67	7	2	2026-05-26 22:11:17.845077	t
68	8	2	2026-05-25 22:11:17.845077	f
69	9	2	2026-05-24 22:11:17.845077	t
70	10	2	2026-05-23 22:11:17.845077	t
\.


--
-- TOC entry 4803 (class 0 OID 28211)
-- Dependencies: 300
-- Data for Name: servicio_atributos; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.servicio_atributos (id_servicio_atributo, id_servicio, id_atributo, valor_asignado, actualizado_en) FROM stdin;
1	1	1	Valor asignado 1	2026-08-02 03:11:17.845077+00
2	2	6	Valor asignado 2	2026-08-02 03:11:17.845077+00
3	3	11	Valor asignado 3	2026-08-02 03:11:17.845077+00
4	4	16	Valor asignado 4	2026-08-02 03:11:17.845077+00
5	5	21	Valor asignado 5	2026-08-02 03:11:17.845077+00
6	6	26	Valor asignado 6	2026-08-02 03:11:17.845077+00
7	7	31	Valor asignado 7	2026-08-02 03:11:17.845077+00
8	8	36	Valor asignado 8	2026-08-02 03:11:17.845077+00
9	9	41	Valor asignado 9	2026-08-02 03:11:17.845077+00
10	10	46	Valor asignado 10	2026-08-02 03:11:17.845077+00
11	11	1	Valor asignado 11	2026-08-02 03:11:17.845077+00
12	12	6	Valor asignado 12	2026-08-02 03:11:17.845077+00
13	13	11	Valor asignado 13	2026-08-02 03:11:17.845077+00
14	14	16	Valor asignado 14	2026-08-02 03:11:17.845077+00
15	15	21	Valor asignado 15	2026-08-02 03:11:17.845077+00
16	16	26	Valor asignado 16	2026-08-02 03:11:17.845077+00
17	17	31	Valor asignado 17	2026-08-02 03:11:17.845077+00
18	18	36	Valor asignado 18	2026-08-02 03:11:17.845077+00
19	19	41	Valor asignado 19	2026-08-02 03:11:17.845077+00
20	20	46	Valor asignado 20	2026-08-02 03:11:17.845077+00
21	21	1	Valor asignado 21	2026-08-02 03:11:17.845077+00
22	22	6	Valor asignado 22	2026-08-02 03:11:17.845077+00
23	23	11	Valor asignado 23	2026-08-02 03:11:17.845077+00
24	24	16	Valor asignado 24	2026-08-02 03:11:17.845077+00
25	25	21	Valor asignado 25	2026-08-02 03:11:17.845077+00
26	26	26	Valor asignado 26	2026-08-02 03:11:17.845077+00
27	27	31	Valor asignado 27	2026-08-02 03:11:17.845077+00
28	28	36	Valor asignado 28	2026-08-02 03:11:17.845077+00
29	29	41	Valor asignado 29	2026-08-02 03:11:17.845077+00
30	30	46	Valor asignado 30	2026-08-02 03:11:17.845077+00
31	31	1	Valor asignado 31	2026-08-02 03:11:17.845077+00
32	32	6	Valor asignado 32	2026-08-02 03:11:17.845077+00
33	33	11	Valor asignado 33	2026-08-02 03:11:17.845077+00
34	34	16	Valor asignado 34	2026-08-02 03:11:17.845077+00
35	35	21	Valor asignado 35	2026-08-02 03:11:17.845077+00
36	36	26	Valor asignado 36	2026-08-02 03:11:17.845077+00
37	37	31	Valor asignado 37	2026-08-02 03:11:17.845077+00
38	38	36	Valor asignado 38	2026-08-02 03:11:17.845077+00
39	39	41	Valor asignado 39	2026-08-02 03:11:17.845077+00
40	40	46	Valor asignado 40	2026-08-02 03:11:17.845077+00
41	41	1	Valor asignado 41	2026-08-02 03:11:17.845077+00
42	42	6	Valor asignado 42	2026-08-02 03:11:17.845077+00
43	43	11	Valor asignado 43	2026-08-02 03:11:17.845077+00
44	44	16	Valor asignado 44	2026-08-02 03:11:17.845077+00
45	45	21	Valor asignado 45	2026-08-02 03:11:17.845077+00
46	46	26	Valor asignado 46	2026-08-02 03:11:17.845077+00
47	47	31	Valor asignado 47	2026-08-02 03:11:17.845077+00
48	48	36	Valor asignado 48	2026-08-02 03:11:17.845077+00
49	49	41	Valor asignado 49	2026-08-02 03:11:17.845077+00
50	50	46	Valor asignado 50	2026-08-02 03:11:17.845077+00
51	51	1	Valor asignado 51	2026-08-02 03:11:17.845077+00
52	52	6	Valor asignado 52	2026-08-02 03:11:17.845077+00
53	53	11	Valor asignado 53	2026-08-02 03:11:17.845077+00
54	54	16	Valor asignado 54	2026-08-02 03:11:17.845077+00
55	55	21	Valor asignado 55	2026-08-02 03:11:17.845077+00
56	56	26	Valor asignado 56	2026-08-02 03:11:17.845077+00
57	57	31	Valor asignado 57	2026-08-02 03:11:17.845077+00
58	58	36	Valor asignado 58	2026-08-02 03:11:17.845077+00
59	59	41	Valor asignado 59	2026-08-02 03:11:17.845077+00
60	60	46	Valor asignado 60	2026-08-02 03:11:17.845077+00
61	1	1	Valor asignado 61	2026-08-02 03:11:17.845077+00
62	2	6	Valor asignado 62	2026-08-02 03:11:17.845077+00
63	3	11	Valor asignado 63	2026-08-02 03:11:17.845077+00
64	4	16	Valor asignado 64	2026-08-02 03:11:17.845077+00
65	5	21	Valor asignado 65	2026-08-02 03:11:17.845077+00
66	6	26	Valor asignado 66	2026-08-02 03:11:17.845077+00
67	7	31	Valor asignado 67	2026-08-02 03:11:17.845077+00
68	8	36	Valor asignado 68	2026-08-02 03:11:17.845077+00
69	9	41	Valor asignado 69	2026-08-02 03:11:17.845077+00
70	10	46	Valor asignado 70	2026-08-02 03:11:17.845077+00
\.


--
-- TOC entry 4805 (class 0 OID 28220)
-- Dependencies: 302
-- Data for Name: servicio_etiquetas; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.servicio_etiquetas (id_servicio_etiqueta, id_servicio, id_etiqueta, actualizado_en) FROM stdin;
1	1	1	2026-08-02 03:11:17.845077+00
2	2	4	2026-08-02 03:11:17.845077+00
3	3	7	2026-08-02 03:11:17.845077+00
4	4	10	2026-08-02 03:11:17.845077+00
5	5	13	2026-08-02 03:11:17.845077+00
6	6	16	2026-08-02 03:11:17.845077+00
7	7	19	2026-08-02 03:11:17.845077+00
8	8	22	2026-08-02 03:11:17.845077+00
9	9	25	2026-08-02 03:11:17.845077+00
10	10	28	2026-08-02 03:11:17.845077+00
11	11	31	2026-08-02 03:11:17.845077+00
12	12	34	2026-08-02 03:11:17.845077+00
13	13	37	2026-08-02 03:11:17.845077+00
14	14	40	2026-08-02 03:11:17.845077+00
15	15	43	2026-08-02 03:11:17.845077+00
16	16	46	2026-08-02 03:11:17.845077+00
17	17	49	2026-08-02 03:11:17.845077+00
18	18	2	2026-08-02 03:11:17.845077+00
19	19	5	2026-08-02 03:11:17.845077+00
20	20	8	2026-08-02 03:11:17.845077+00
21	21	11	2026-08-02 03:11:17.845077+00
22	22	14	2026-08-02 03:11:17.845077+00
23	23	17	2026-08-02 03:11:17.845077+00
24	24	20	2026-08-02 03:11:17.845077+00
25	25	23	2026-08-02 03:11:17.845077+00
26	26	26	2026-08-02 03:11:17.845077+00
27	27	29	2026-08-02 03:11:17.845077+00
28	28	32	2026-08-02 03:11:17.845077+00
29	29	35	2026-08-02 03:11:17.845077+00
30	30	38	2026-08-02 03:11:17.845077+00
31	31	41	2026-08-02 03:11:17.845077+00
32	32	44	2026-08-02 03:11:17.845077+00
33	33	47	2026-08-02 03:11:17.845077+00
34	34	50	2026-08-02 03:11:17.845077+00
35	35	3	2026-08-02 03:11:17.845077+00
36	36	6	2026-08-02 03:11:17.845077+00
37	37	9	2026-08-02 03:11:17.845077+00
38	38	12	2026-08-02 03:11:17.845077+00
39	39	15	2026-08-02 03:11:17.845077+00
40	40	18	2026-08-02 03:11:17.845077+00
41	41	21	2026-08-02 03:11:17.845077+00
42	42	24	2026-08-02 03:11:17.845077+00
43	43	27	2026-08-02 03:11:17.845077+00
44	44	30	2026-08-02 03:11:17.845077+00
45	45	33	2026-08-02 03:11:17.845077+00
46	46	36	2026-08-02 03:11:17.845077+00
47	47	39	2026-08-02 03:11:17.845077+00
48	48	42	2026-08-02 03:11:17.845077+00
49	49	45	2026-08-02 03:11:17.845077+00
50	50	48	2026-08-02 03:11:17.845077+00
51	51	1	2026-08-02 03:11:17.845077+00
52	52	4	2026-08-02 03:11:17.845077+00
53	53	7	2026-08-02 03:11:17.845077+00
54	54	10	2026-08-02 03:11:17.845077+00
55	55	13	2026-08-02 03:11:17.845077+00
56	56	16	2026-08-02 03:11:17.845077+00
57	57	19	2026-08-02 03:11:17.845077+00
58	58	22	2026-08-02 03:11:17.845077+00
59	59	25	2026-08-02 03:11:17.845077+00
60	60	28	2026-08-02 03:11:17.845077+00
61	1	31	2026-08-02 03:11:17.845077+00
62	2	34	2026-08-02 03:11:17.845077+00
63	3	37	2026-08-02 03:11:17.845077+00
64	4	40	2026-08-02 03:11:17.845077+00
65	5	43	2026-08-02 03:11:17.845077+00
66	6	46	2026-08-02 03:11:17.845077+00
67	7	49	2026-08-02 03:11:17.845077+00
68	8	2	2026-08-02 03:11:17.845077+00
69	9	5	2026-08-02 03:11:17.845077+00
70	10	8	2026-08-02 03:11:17.845077+00
\.


--
-- TOC entry 4807 (class 0 OID 28228)
-- Dependencies: 304
-- Data for Name: servicios; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.servicios (id_servicio, id_perfil, id_subcategoria, titulo_servicio, descripcion_detallada, precio_base, url_miniatura, tipo_item, estado_publicacion, cargo_revision_adicional, limite_revisiones_base, actualizado_en) FROM stdin;
1	1	1	Diseño de Logotipo Profesional #1	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 1.	38.00	https://cdn.example.com/miniaturas/servicio1.jpg	SERVICIO	ACTIVO	6.00	1	2026-08-02 03:11:17.845077+00
2	2	2	Ilustración Personalizada #2	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 2.	61.00	https://cdn.example.com/miniaturas/servicio2.jpg	PRODUCTO	PAUSADO	7.00	2	2026-08-02 03:11:17.845077+00
3	3	3	Edición de Video Promocional #3	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 3.	84.00	https://cdn.example.com/miniaturas/servicio3.jpg	SERVICIO	BORRADOR	8.00	3	2026-08-02 03:11:17.845077+00
4	4	4	Animación 2D a Medida #4	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 4.	107.00	https://cdn.example.com/miniaturas/servicio4.jpg	PRODUCTO	ACTIVO	9.00	4	2026-08-02 03:11:17.845077+00
5	5	5	Redacción de Contenido SEO #5	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 5.	130.00	https://cdn.example.com/miniaturas/servicio5.jpg	SERVICIO	PAUSADO	10.00	0	2026-08-02 03:11:17.845077+00
6	6	6	Traducción Profesional #6	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 6.	153.00	https://cdn.example.com/miniaturas/servicio6.jpg	PRODUCTO	BORRADOR	11.00	1	2026-08-02 03:11:17.845077+00
7	7	7	Diseño de Portada de Libro #7	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 7.	176.00	https://cdn.example.com/miniaturas/servicio7.jpg	SERVICIO	ACTIVO	12.00	2	2026-08-02 03:11:17.845077+00
8	8	8	Diseño de Interfaz de App #8	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 8.	199.00	https://cdn.example.com/miniaturas/servicio8.jpg	PRODUCTO	PAUSADO	13.00	3	2026-08-02 03:11:17.845077+00
9	9	9	Composición Musical Original #9	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 9.	222.00	https://cdn.example.com/miniaturas/servicio9.jpg	SERVICIO	BORRADOR	14.00	4	2026-08-02 03:11:17.845077+00
10	10	10	Diseño de Empaque de Producto #10	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 10.	245.00	https://cdn.example.com/miniaturas/servicio10.jpg	PRODUCTO	ACTIVO	15.00	0	2026-08-02 03:11:17.845077+00
11	11	11	Diseño de Logotipo Profesional #11	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 11.	268.00	https://cdn.example.com/miniaturas/servicio11.jpg	SERVICIO	PAUSADO	16.00	1	2026-08-02 03:11:17.845077+00
12	12	12	Ilustración Personalizada #12	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 12.	291.00	https://cdn.example.com/miniaturas/servicio12.jpg	PRODUCTO	BORRADOR	17.00	2	2026-08-02 03:11:17.845077+00
13	13	13	Edición de Video Promocional #13	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 13.	314.00	https://cdn.example.com/miniaturas/servicio13.jpg	SERVICIO	ACTIVO	18.00	3	2026-08-02 03:11:17.845077+00
14	14	14	Animación 2D a Medida #14	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 14.	337.00	https://cdn.example.com/miniaturas/servicio14.jpg	PRODUCTO	PAUSADO	19.00	4	2026-08-02 03:11:17.845077+00
15	15	15	Redacción de Contenido SEO #15	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 15.	360.00	https://cdn.example.com/miniaturas/servicio15.jpg	SERVICIO	BORRADOR	20.00	0	2026-08-02 03:11:17.845077+00
16	16	16	Traducción Profesional #16	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 16.	383.00	https://cdn.example.com/miniaturas/servicio16.jpg	PRODUCTO	ACTIVO	21.00	1	2026-08-02 03:11:17.845077+00
17	17	17	Diseño de Portada de Libro #17	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 17.	406.00	https://cdn.example.com/miniaturas/servicio17.jpg	SERVICIO	PAUSADO	22.00	2	2026-08-02 03:11:17.845077+00
18	18	18	Diseño de Interfaz de App #18	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 18.	429.00	https://cdn.example.com/miniaturas/servicio18.jpg	PRODUCTO	BORRADOR	23.00	3	2026-08-02 03:11:17.845077+00
19	19	19	Composición Musical Original #19	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 19.	452.00	https://cdn.example.com/miniaturas/servicio19.jpg	SERVICIO	ACTIVO	24.00	4	2026-08-02 03:11:17.845077+00
20	20	20	Diseño de Empaque de Producto #20	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 20.	475.00	https://cdn.example.com/miniaturas/servicio20.jpg	PRODUCTO	PAUSADO	25.00	0	2026-08-02 03:11:17.845077+00
21	21	21	Diseño de Logotipo Profesional #21	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 21.	498.00	https://cdn.example.com/miniaturas/servicio21.jpg	SERVICIO	BORRADOR	26.00	1	2026-08-02 03:11:17.845077+00
22	22	22	Ilustración Personalizada #22	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 22.	521.00	https://cdn.example.com/miniaturas/servicio22.jpg	PRODUCTO	ACTIVO	27.00	2	2026-08-02 03:11:17.845077+00
23	23	23	Edición de Video Promocional #23	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 23.	544.00	https://cdn.example.com/miniaturas/servicio23.jpg	SERVICIO	PAUSADO	28.00	3	2026-08-02 03:11:17.845077+00
24	24	24	Animación 2D a Medida #24	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 24.	567.00	https://cdn.example.com/miniaturas/servicio24.jpg	PRODUCTO	BORRADOR	29.00	4	2026-08-02 03:11:17.845077+00
25	25	25	Redacción de Contenido SEO #25	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 25.	590.00	https://cdn.example.com/miniaturas/servicio25.jpg	SERVICIO	ACTIVO	30.00	0	2026-08-02 03:11:17.845077+00
26	26	26	Traducción Profesional #26	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 26.	613.00	https://cdn.example.com/miniaturas/servicio26.jpg	PRODUCTO	PAUSADO	31.00	1	2026-08-02 03:11:17.845077+00
27	27	27	Diseño de Portada de Libro #27	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 27.	636.00	https://cdn.example.com/miniaturas/servicio27.jpg	SERVICIO	BORRADOR	32.00	2	2026-08-02 03:11:17.845077+00
28	28	28	Diseño de Interfaz de App #28	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 28.	659.00	https://cdn.example.com/miniaturas/servicio28.jpg	PRODUCTO	ACTIVO	33.00	3	2026-08-02 03:11:17.845077+00
29	29	29	Composición Musical Original #29	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 29.	682.00	https://cdn.example.com/miniaturas/servicio29.jpg	SERVICIO	PAUSADO	34.00	4	2026-08-02 03:11:17.845077+00
30	30	30	Diseño de Empaque de Producto #30	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 30.	705.00	https://cdn.example.com/miniaturas/servicio30.jpg	PRODUCTO	BORRADOR	35.00	0	2026-08-02 03:11:17.845077+00
31	31	31	Diseño de Logotipo Profesional #31	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 31.	728.00	https://cdn.example.com/miniaturas/servicio31.jpg	SERVICIO	ACTIVO	36.00	1	2026-08-02 03:11:17.845077+00
32	32	32	Ilustración Personalizada #32	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 32.	751.00	https://cdn.example.com/miniaturas/servicio32.jpg	PRODUCTO	PAUSADO	37.00	2	2026-08-02 03:11:17.845077+00
33	33	33	Edición de Video Promocional #33	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 33.	774.00	https://cdn.example.com/miniaturas/servicio33.jpg	SERVICIO	BORRADOR	38.00	3	2026-08-02 03:11:17.845077+00
34	34	34	Animación 2D a Medida #34	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 34.	797.00	https://cdn.example.com/miniaturas/servicio34.jpg	PRODUCTO	ACTIVO	39.00	4	2026-08-02 03:11:17.845077+00
35	35	35	Redacción de Contenido SEO #35	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 35.	820.00	https://cdn.example.com/miniaturas/servicio35.jpg	SERVICIO	PAUSADO	40.00	0	2026-08-02 03:11:17.845077+00
36	36	36	Traducción Profesional #36	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 36.	843.00	https://cdn.example.com/miniaturas/servicio36.jpg	PRODUCTO	BORRADOR	41.00	1	2026-08-02 03:11:17.845077+00
37	37	37	Diseño de Portada de Libro #37	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 37.	866.00	https://cdn.example.com/miniaturas/servicio37.jpg	SERVICIO	ACTIVO	42.00	2	2026-08-02 03:11:17.845077+00
38	38	38	Diseño de Interfaz de App #38	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 38.	889.00	https://cdn.example.com/miniaturas/servicio38.jpg	PRODUCTO	PAUSADO	43.00	3	2026-08-02 03:11:17.845077+00
39	39	39	Composición Musical Original #39	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 39.	912.00	https://cdn.example.com/miniaturas/servicio39.jpg	SERVICIO	BORRADOR	44.00	4	2026-08-02 03:11:17.845077+00
40	40	40	Diseño de Empaque de Producto #40	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 40.	935.00	https://cdn.example.com/miniaturas/servicio40.jpg	PRODUCTO	ACTIVO	5.00	0	2026-08-02 03:11:17.845077+00
41	41	41	Diseño de Logotipo Profesional #41	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 41.	958.00	https://cdn.example.com/miniaturas/servicio41.jpg	SERVICIO	PAUSADO	6.00	1	2026-08-02 03:11:17.845077+00
42	42	42	Ilustración Personalizada #42	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 42.	981.00	https://cdn.example.com/miniaturas/servicio42.jpg	PRODUCTO	BORRADOR	7.00	2	2026-08-02 03:11:17.845077+00
43	43	43	Edición de Video Promocional #43	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 43.	19.00	https://cdn.example.com/miniaturas/servicio43.jpg	SERVICIO	ACTIVO	8.00	3	2026-08-02 03:11:17.845077+00
44	44	44	Animación 2D a Medida #44	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 44.	42.00	https://cdn.example.com/miniaturas/servicio44.jpg	PRODUCTO	PAUSADO	9.00	4	2026-08-02 03:11:17.845077+00
45	45	45	Redacción de Contenido SEO #45	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 45.	65.00	https://cdn.example.com/miniaturas/servicio45.jpg	SERVICIO	BORRADOR	10.00	0	2026-08-02 03:11:17.845077+00
46	46	46	Traducción Profesional #46	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 46.	88.00	https://cdn.example.com/miniaturas/servicio46.jpg	PRODUCTO	ACTIVO	11.00	1	2026-08-02 03:11:17.845077+00
47	47	47	Diseño de Portada de Libro #47	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 47.	111.00	https://cdn.example.com/miniaturas/servicio47.jpg	SERVICIO	PAUSADO	12.00	2	2026-08-02 03:11:17.845077+00
48	48	48	Diseño de Interfaz de App #48	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 48.	134.00	https://cdn.example.com/miniaturas/servicio48.jpg	PRODUCTO	BORRADOR	13.00	3	2026-08-02 03:11:17.845077+00
49	49	49	Composición Musical Original #49	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 49.	157.00	https://cdn.example.com/miniaturas/servicio49.jpg	SERVICIO	ACTIVO	14.00	4	2026-08-02 03:11:17.845077+00
50	50	50	Diseño de Empaque de Producto #50	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 50.	180.00	https://cdn.example.com/miniaturas/servicio50.jpg	PRODUCTO	PAUSADO	15.00	0	2026-08-02 03:11:17.845077+00
51	1	51	Diseño de Logotipo Profesional #51	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 51.	203.00	https://cdn.example.com/miniaturas/servicio51.jpg	SERVICIO	BORRADOR	16.00	1	2026-08-02 03:11:17.845077+00
52	2	52	Ilustración Personalizada #52	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 52.	226.00	https://cdn.example.com/miniaturas/servicio52.jpg	PRODUCTO	ACTIVO	17.00	2	2026-08-02 03:11:17.845077+00
53	3	53	Edición de Video Promocional #53	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 53.	249.00	https://cdn.example.com/miniaturas/servicio53.jpg	SERVICIO	PAUSADO	18.00	3	2026-08-02 03:11:17.845077+00
54	4	54	Animación 2D a Medida #54	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 54.	272.00	https://cdn.example.com/miniaturas/servicio54.jpg	PRODUCTO	BORRADOR	19.00	4	2026-08-02 03:11:17.845077+00
55	5	55	Redacción de Contenido SEO #55	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 55.	295.00	https://cdn.example.com/miniaturas/servicio55.jpg	SERVICIO	ACTIVO	20.00	0	2026-08-02 03:11:17.845077+00
56	6	56	Traducción Profesional #56	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 56.	318.00	https://cdn.example.com/miniaturas/servicio56.jpg	PRODUCTO	PAUSADO	21.00	1	2026-08-02 03:11:17.845077+00
57	7	57	Diseño de Portada de Libro #57	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 57.	341.00	https://cdn.example.com/miniaturas/servicio57.jpg	SERVICIO	BORRADOR	22.00	2	2026-08-02 03:11:17.845077+00
58	8	58	Diseño de Interfaz de App #58	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 58.	364.00	https://cdn.example.com/miniaturas/servicio58.jpg	PRODUCTO	ACTIVO	23.00	3	2026-08-02 03:11:17.845077+00
59	9	59	Composición Musical Original #59	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 59.	387.00	https://cdn.example.com/miniaturas/servicio59.jpg	SERVICIO	PAUSADO	24.00	4	2026-08-02 03:11:17.845077+00
60	10	60	Diseño de Empaque de Producto #60	Servicio profesional de alta calidad, entregado con revisiones incluidas y comunicación directa con el cliente. Proyecto número 60.	410.00	https://cdn.example.com/miniaturas/servicio60.jpg	PRODUCTO	BORRADOR	25.00	0	2026-08-02 03:11:17.845077+00
\.


--
-- TOC entry 4809 (class 0 OID 28247)
-- Dependencies: 306
-- Data for Name: sesiones_usuario; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.sesiones_usuario (id_sesion, id_usuario, direccion_ip, fecha_creacion, fecha_expiracion, jti) FROM stdin;
73	61	172.18.0.1	2026-08-17 01:20:51.315461	2026-08-18 01:20:51.314744	06126595-2e06-4773-a7d8-00ffaa5f3cec
74	61	172.18.0.1	2026-08-17 01:20:51.439526	2026-08-24 01:20:51.438719	25129eed-c694-4345-86fc-861cb575dff6
77	61	172.18.0.1	2026-08-17 01:39:06.280661	2026-08-18 01:39:06.279896	0896c688-44d9-4424-af50-f1a44998b60d
79	61	172.18.0.1	2026-08-17 01:39:23.746539	2026-08-18 01:39:23.745601	48b79355-122a-42d6-85b0-917aedeb54fd
80	61	172.18.0.1	2026-08-17 01:39:23.882132	2026-08-24 01:39:23.880988	9997137e-253b-4591-b8f6-c506a4e07128
85	61	172.18.0.1	2026-08-17 03:28:36.517888	2026-08-18 03:28:36.50743	1ae041f7-b388-471a-a988-9204a61c4c10
87	67	172.18.0.1	2026-08-17 03:36:24.601005	2026-08-18 03:36:24.597504	f2fe8597-d028-4989-a5b3-d36cdd5347b1
89	68	172.18.0.1	2026-08-17 03:37:57.970285	2026-08-18 03:37:57.966642	82cff9b8-d2f6-41c9-8f4e-22f8ff253fd0
90	68	172.18.0.1	2026-08-17 03:37:58.259111	2026-08-24 03:37:58.257738	5ba80d60-59ff-41aa-b69f-3124217c8686
91	67	172.18.0.1	2026-08-17 04:05:45.679465	2026-08-18 04:05:45.678136	2d8d53fe-5864-4139-a942-9d881e39a32e
92	67	172.18.0.1	2026-08-17 04:05:46.030489	2026-08-24 04:05:46.029385	b85add31-6737-4cea-88a9-77e915c1f789
93	67	172.18.0.1	2026-08-17 04:05:48.51454	2026-08-18 04:05:48.511877	46172772-26db-4e7e-8956-ae9438e52887
95	61	172.18.0.1	2026-08-17 06:29:10.961782	2026-08-18 06:29:10.859528	289513fc-4e6a-4cd0-aa1f-14894c033b68
107	69	172.18.0.1	2026-08-17 13:05:54.998221	2026-08-18 13:05:54.996769	8945394e-54ee-4b47-bc11-e67171a2d9d0
109	69	172.18.0.1	2026-08-17 13:33:57.13595	2026-08-18 13:33:57.133876	719de815-5a5f-4da3-9514-e90f7222fadb
111	69	172.18.0.1	2026-08-17 14:29:04.0271	2026-08-18 14:29:04.016794	91d95bad-22d1-4bf5-ac98-62b9adff105b
121	70	172.18.0.1	2026-08-17 18:14:20.038353	2026-08-18 18:14:20.037357	479154f5-22f0-4ecf-b947-72a06a4d1166
122	70	172.18.0.1	2026-08-17 18:14:20.39796	2026-08-24 18:14:20.39703	9f9006a2-befc-4087-b48d-e3a8f2b6eae3
128	61	172.18.0.1	2026-08-17 18:28:25.325746	2026-08-24 18:28:25.32441	a9e0ffb4-6a95-4caa-b326-52d1f2e13429
129	61	172.18.0.1	2026-08-17 18:29:59.360185	2026-08-18 18:29:59.35925	ad6c6f91-e1fa-48fa-a2a8-a64283c8885a
141	73	172.18.0.1	2026-08-17 19:05:32.623685	2026-08-18 19:05:32.622502	ffcc422c-62e6-42b2-978b-e8b019a3cfa0
145	61	172.18.0.1	2026-08-17 19:14:34.89075	2026-08-18 19:14:34.88874	87092811-e597-44f8-844a-141893cd4c23
146	61	172.18.0.1	2026-08-17 19:14:35.05561	2026-08-24 19:14:35.05487	85915a05-bf7c-420a-95af-1aadc7de345f
\.


--
-- TOC entry 4811 (class 0 OID 28258)
-- Dependencies: 308
-- Data for Name: sorteos; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.sorteos (id_sorteo, id_perfil_creador, titulo_sorteo, descripcion_premios, cantidad_ganadores, fecha_inicio, fecha_cierre, estado_sorteo, requiere_seguidor) FROM stdin;
2	2	Sorteo Especial #2	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 2.	3	2026-07-30 22:11:17.845077	2026-08-04 22:11:17.845077	Cerrado	t
3	3	Sorteo Especial #3	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 3.	1	2026-07-29 22:11:17.845077	2026-08-05 22:11:17.845077	Cancelado	f
5	5	Sorteo Especial #5	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 5.	3	2026-07-27 22:11:17.845077	2026-08-07 22:11:17.845077	Cerrado	f
6	6	Sorteo Especial #6	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 6.	1	2026-07-26 22:11:17.845077	2026-08-08 22:11:17.845077	Cancelado	t
8	8	Sorteo Especial #8	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 8.	3	2026-07-24 22:11:17.845077	2026-08-10 22:11:17.845077	Cerrado	t
9	9	Sorteo Especial #9	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 9.	1	2026-07-23 22:11:17.845077	2026-08-11 22:11:17.845077	Cancelado	f
11	11	Sorteo Especial #11	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 11.	3	2026-07-21 22:11:17.845077	2026-08-13 22:11:17.845077	Cerrado	f
12	12	Sorteo Especial #12	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 12.	1	2026-07-20 22:11:17.845077	2026-08-14 22:11:17.845077	Cancelado	t
14	14	Sorteo Especial #14	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 14.	3	2026-07-18 22:11:17.845077	2026-08-16 22:11:17.845077	Cerrado	t
15	15	Sorteo Especial #15	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 15.	1	2026-07-17 22:11:17.845077	2026-08-17 22:11:17.845077	Cancelado	f
16	16	Sorteo Especial #16	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 16.	2	2026-07-16 22:11:17.845077	2026-08-18 22:11:17.845077	Activo	t
17	17	Sorteo Especial #17	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 17.	3	2026-07-15 22:11:17.845077	2026-08-19 22:11:17.845077	Cerrado	f
18	18	Sorteo Especial #18	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 18.	1	2026-07-14 22:11:17.845077	2026-08-20 22:11:17.845077	Cancelado	t
19	19	Sorteo Especial #19	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 19.	2	2026-07-13 22:11:17.845077	2026-08-21 22:11:17.845077	Activo	f
20	20	Sorteo Especial #20	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 20.	3	2026-07-12 22:11:17.845077	2026-08-22 22:11:17.845077	Cerrado	t
21	21	Sorteo Especial #21	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 21.	1	2026-07-11 22:11:17.845077	2026-08-23 22:11:17.845077	Cancelado	f
22	22	Sorteo Especial #22	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 22.	2	2026-07-10 22:11:17.845077	2026-08-24 22:11:17.845077	Activo	t
23	23	Sorteo Especial #23	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 23.	3	2026-07-09 22:11:17.845077	2026-08-25 22:11:17.845077	Cerrado	f
24	24	Sorteo Especial #24	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 24.	1	2026-07-08 22:11:17.845077	2026-08-26 22:11:17.845077	Cancelado	t
25	25	Sorteo Especial #25	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 25.	2	2026-07-07 22:11:17.845077	2026-08-27 22:11:17.845077	Activo	f
26	26	Sorteo Especial #26	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 26.	3	2026-07-06 22:11:17.845077	2026-08-28 22:11:17.845077	Cerrado	t
27	27	Sorteo Especial #27	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 27.	1	2026-07-05 22:11:17.845077	2026-08-29 22:11:17.845077	Cancelado	f
28	28	Sorteo Especial #28	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 28.	2	2026-07-04 22:11:17.845077	2026-08-30 22:11:17.845077	Activo	t
29	29	Sorteo Especial #29	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 29.	3	2026-07-03 22:11:17.845077	2026-08-31 22:11:17.845077	Cerrado	f
30	30	Sorteo Especial #30	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 30.	1	2026-07-02 22:11:17.845077	2026-09-01 22:11:17.845077	Cancelado	t
31	31	Sorteo Especial #31	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 31.	2	2026-07-01 22:11:17.845077	2026-09-02 22:11:17.845077	Activo	f
32	32	Sorteo Especial #32	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 32.	3	2026-06-30 22:11:17.845077	2026-09-03 22:11:17.845077	Cerrado	t
33	33	Sorteo Especial #33	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 33.	1	2026-06-29 22:11:17.845077	2026-09-04 22:11:17.845077	Cancelado	f
34	34	Sorteo Especial #34	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 34.	2	2026-06-28 22:11:17.845077	2026-09-05 22:11:17.845077	Activo	t
35	35	Sorteo Especial #35	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 35.	3	2026-06-27 22:11:17.845077	2026-09-06 22:11:17.845077	Cerrado	f
36	36	Sorteo Especial #36	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 36.	1	2026-06-26 22:11:17.845077	2026-09-07 22:11:17.845077	Cancelado	t
37	37	Sorteo Especial #37	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 37.	2	2026-06-25 22:11:17.845077	2026-09-08 22:11:17.845077	Activo	f
38	38	Sorteo Especial #38	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 38.	3	2026-06-24 22:11:17.845077	2026-09-09 22:11:17.845077	Cerrado	t
39	39	Sorteo Especial #39	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 39.	1	2026-06-23 22:11:17.845077	2026-09-10 22:11:17.845077	Cancelado	f
40	40	Sorteo Especial #40	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 40.	2	2026-06-22 22:11:17.845077	2026-09-11 22:11:17.845077	Activo	t
41	41	Sorteo Especial #41	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 41.	3	2026-06-21 22:11:17.845077	2026-09-12 22:11:17.845077	Cerrado	f
42	42	Sorteo Especial #42	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 42.	1	2026-06-20 22:11:17.845077	2026-09-13 22:11:17.845077	Cancelado	t
43	43	Sorteo Especial #43	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 43.	2	2026-06-19 22:11:17.845077	2026-09-14 22:11:17.845077	Activo	f
44	44	Sorteo Especial #44	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 44.	3	2026-06-18 22:11:17.845077	2026-09-15 22:11:17.845077	Cerrado	t
45	45	Sorteo Especial #45	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 45.	1	2026-06-17 22:11:17.845077	2026-08-02 22:11:17.845077	Cancelado	f
47	47	Sorteo Especial #47	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 47.	3	2026-06-15 22:11:17.845077	2026-08-04 22:11:17.845077	Cerrado	f
48	48	Sorteo Especial #48	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 48.	1	2026-06-14 22:11:17.845077	2026-08-05 22:11:17.845077	Cancelado	t
50	50	Sorteo Especial #50	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 50.	3	2026-06-12 22:11:17.845077	2026-08-07 22:11:17.845077	Cerrado	t
1	1	Sorteo Especial #1	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 1.	2	2026-07-31 22:11:17.845077	2026-08-03 22:11:17.845077	Finalizado	f
4	4	Sorteo Especial #4	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 4.	2	2026-07-28 22:11:17.845077	2026-08-06 22:11:17.845077	Finalizado	t
7	7	Sorteo Especial #7	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 7.	2	2026-07-25 22:11:17.845077	2026-08-09 22:11:17.845077	Finalizado	f
10	10	Sorteo Especial #10	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 10.	2	2026-07-22 22:11:17.845077	2026-08-12 22:11:17.845077	Finalizado	t
13	13	Sorteo Especial #13	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 13.	2	2026-07-19 22:11:17.845077	2026-08-15 22:11:17.845077	Finalizado	f
46	46	Sorteo Especial #46	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 46.	2	2026-06-16 22:11:17.845077	2026-08-03 22:11:17.845077	Finalizado	t
49	49	Sorteo Especial #49	Sorteo de un pack de productos digitales y asesoría personalizada para el ganador número 49.	2	2026-06-13 22:11:17.845077	2026-08-06 22:11:17.845077	Finalizado	f
\.


--
-- TOC entry 4813 (class 0 OID 28275)
-- Dependencies: 310
-- Data for Name: subcategorias; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.subcategorias (id_subcategoria, id_categoria, nombre_subcategoria, actualizado_en) FROM stdin;
1	1	Subcategoría 1 - Básico	2026-08-02 03:11:17.845077+00
2	2	Subcategoría 2 - Intermedio	2026-08-02 03:11:17.845077+00
3	3	Subcategoría 3 - Avanzado	2026-08-02 03:11:17.845077+00
4	4	Subcategoría 4 - Profesional	2026-08-02 03:11:17.845077+00
5	5	Subcategoría 5 - Especializado	2026-08-02 03:11:17.845077+00
6	6	Subcategoría 6 - Básico	2026-08-02 03:11:17.845077+00
7	7	Subcategoría 7 - Intermedio	2026-08-02 03:11:17.845077+00
8	8	Subcategoría 8 - Avanzado	2026-08-02 03:11:17.845077+00
9	9	Subcategoría 9 - Profesional	2026-08-02 03:11:17.845077+00
10	10	Subcategoría 10 - Especializado	2026-08-02 03:11:17.845077+00
11	11	Subcategoría 11 - Básico	2026-08-02 03:11:17.845077+00
12	12	Subcategoría 12 - Intermedio	2026-08-02 03:11:17.845077+00
13	13	Subcategoría 13 - Avanzado	2026-08-02 03:11:17.845077+00
14	14	Subcategoría 14 - Profesional	2026-08-02 03:11:17.845077+00
15	15	Subcategoría 15 - Especializado	2026-08-02 03:11:17.845077+00
16	16	Subcategoría 16 - Básico	2026-08-02 03:11:17.845077+00
17	17	Subcategoría 17 - Intermedio	2026-08-02 03:11:17.845077+00
18	18	Subcategoría 18 - Avanzado	2026-08-02 03:11:17.845077+00
19	19	Subcategoría 19 - Profesional	2026-08-02 03:11:17.845077+00
20	20	Subcategoría 20 - Especializado	2026-08-02 03:11:17.845077+00
21	21	Subcategoría 21 - Básico	2026-08-02 03:11:17.845077+00
22	22	Subcategoría 22 - Intermedio	2026-08-02 03:11:17.845077+00
23	23	Subcategoría 23 - Avanzado	2026-08-02 03:11:17.845077+00
24	24	Subcategoría 24 - Profesional	2026-08-02 03:11:17.845077+00
25	25	Subcategoría 25 - Especializado	2026-08-02 03:11:17.845077+00
26	26	Subcategoría 26 - Básico	2026-08-02 03:11:17.845077+00
27	27	Subcategoría 27 - Intermedio	2026-08-02 03:11:17.845077+00
28	28	Subcategoría 28 - Avanzado	2026-08-02 03:11:17.845077+00
29	29	Subcategoría 29 - Profesional	2026-08-02 03:11:17.845077+00
30	30	Subcategoría 30 - Especializado	2026-08-02 03:11:17.845077+00
31	31	Subcategoría 31 - Básico	2026-08-02 03:11:17.845077+00
32	32	Subcategoría 32 - Intermedio	2026-08-02 03:11:17.845077+00
33	33	Subcategoría 33 - Avanzado	2026-08-02 03:11:17.845077+00
34	34	Subcategoría 34 - Profesional	2026-08-02 03:11:17.845077+00
35	35	Subcategoría 35 - Especializado	2026-08-02 03:11:17.845077+00
36	36	Subcategoría 36 - Básico	2026-08-02 03:11:17.845077+00
37	37	Subcategoría 37 - Intermedio	2026-08-02 03:11:17.845077+00
38	38	Subcategoría 38 - Avanzado	2026-08-02 03:11:17.845077+00
39	39	Subcategoría 39 - Profesional	2026-08-02 03:11:17.845077+00
40	40	Subcategoría 40 - Especializado	2026-08-02 03:11:17.845077+00
41	41	Subcategoría 41 - Básico	2026-08-02 03:11:17.845077+00
42	42	Subcategoría 42 - Intermedio	2026-08-02 03:11:17.845077+00
43	43	Subcategoría 43 - Avanzado	2026-08-02 03:11:17.845077+00
44	44	Subcategoría 44 - Profesional	2026-08-02 03:11:17.845077+00
45	45	Subcategoría 45 - Especializado	2026-08-02 03:11:17.845077+00
46	46	Subcategoría 46 - Básico	2026-08-02 03:11:17.845077+00
47	47	Subcategoría 47 - Intermedio	2026-08-02 03:11:17.845077+00
48	48	Subcategoría 48 - Avanzado	2026-08-02 03:11:17.845077+00
49	49	Subcategoría 49 - Profesional	2026-08-02 03:11:17.845077+00
50	50	Subcategoría 50 - Especializado	2026-08-02 03:11:17.845077+00
51	1	Subcategoría 51 - Básico	2026-08-02 03:11:17.845077+00
52	2	Subcategoría 52 - Intermedio	2026-08-02 03:11:17.845077+00
53	3	Subcategoría 53 - Avanzado	2026-08-02 03:11:17.845077+00
54	4	Subcategoría 54 - Profesional	2026-08-02 03:11:17.845077+00
55	5	Subcategoría 55 - Especializado	2026-08-02 03:11:17.845077+00
56	6	Subcategoría 56 - Básico	2026-08-02 03:11:17.845077+00
57	7	Subcategoría 57 - Intermedio	2026-08-02 03:11:17.845077+00
58	8	Subcategoría 58 - Avanzado	2026-08-02 03:11:17.845077+00
59	9	Subcategoría 59 - Profesional	2026-08-02 03:11:17.845077+00
60	10	Subcategoría 60 - Especializado	2026-08-02 03:11:17.845077+00
\.


--
-- TOC entry 4815 (class 0 OID 28283)
-- Dependencies: 312
-- Data for Name: tickets_revision; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.tickets_revision (id_ticket, id_pedido, id_motivo, descripcion_cliente, costo_adicional_generado, estado_ticket) FROM stdin;
1	1	1	El cliente solicita ajustes adicionales en la entrega. Ticket número 1.	1.00	Abierto
2	2	2	El cliente solicita ajustes adicionales en la entrega. Ticket número 2.	2.00	En Proceso
3	3	3	El cliente solicita ajustes adicionales en la entrega. Ticket número 3.	3.00	Resuelto
4	4	4	El cliente solicita ajustes adicionales en la entrega. Ticket número 4.	4.00	Cerrado
5	5	5	El cliente solicita ajustes adicionales en la entrega. Ticket número 5.	5.00	Abierto
6	6	6	El cliente solicita ajustes adicionales en la entrega. Ticket número 6.	6.00	En Proceso
7	7	7	El cliente solicita ajustes adicionales en la entrega. Ticket número 7.	7.00	Resuelto
8	8	8	El cliente solicita ajustes adicionales en la entrega. Ticket número 8.	8.00	Cerrado
9	9	9	El cliente solicita ajustes adicionales en la entrega. Ticket número 9.	9.00	Abierto
10	10	10	El cliente solicita ajustes adicionales en la entrega. Ticket número 10.	10.00	En Proceso
11	11	11	El cliente solicita ajustes adicionales en la entrega. Ticket número 11.	11.00	Resuelto
12	12	12	El cliente solicita ajustes adicionales en la entrega. Ticket número 12.	12.00	Cerrado
13	13	13	El cliente solicita ajustes adicionales en la entrega. Ticket número 13.	13.00	Abierto
14	14	14	El cliente solicita ajustes adicionales en la entrega. Ticket número 14.	14.00	En Proceso
15	15	15	El cliente solicita ajustes adicionales en la entrega. Ticket número 15.	15.00	Resuelto
16	16	16	El cliente solicita ajustes adicionales en la entrega. Ticket número 16.	16.00	Cerrado
17	17	17	El cliente solicita ajustes adicionales en la entrega. Ticket número 17.	17.00	Abierto
18	18	18	El cliente solicita ajustes adicionales en la entrega. Ticket número 18.	18.00	En Proceso
19	19	19	El cliente solicita ajustes adicionales en la entrega. Ticket número 19.	19.00	Resuelto
20	20	20	El cliente solicita ajustes adicionales en la entrega. Ticket número 20.	20.00	Cerrado
21	21	21	El cliente solicita ajustes adicionales en la entrega. Ticket número 21.	21.00	Abierto
22	22	22	El cliente solicita ajustes adicionales en la entrega. Ticket número 22.	22.00	En Proceso
23	23	23	El cliente solicita ajustes adicionales en la entrega. Ticket número 23.	23.00	Resuelto
24	24	24	El cliente solicita ajustes adicionales en la entrega. Ticket número 24.	24.00	Cerrado
25	25	25	El cliente solicita ajustes adicionales en la entrega. Ticket número 25.	25.00	Abierto
26	26	26	El cliente solicita ajustes adicionales en la entrega. Ticket número 26.	26.00	En Proceso
27	27	27	El cliente solicita ajustes adicionales en la entrega. Ticket número 27.	27.00	Resuelto
28	28	28	El cliente solicita ajustes adicionales en la entrega. Ticket número 28.	28.00	Cerrado
29	29	29	El cliente solicita ajustes adicionales en la entrega. Ticket número 29.	29.00	Abierto
30	30	30	El cliente solicita ajustes adicionales en la entrega. Ticket número 30.	30.00	En Proceso
31	31	31	El cliente solicita ajustes adicionales en la entrega. Ticket número 31.	31.00	Resuelto
32	32	32	El cliente solicita ajustes adicionales en la entrega. Ticket número 32.	32.00	Cerrado
33	33	33	El cliente solicita ajustes adicionales en la entrega. Ticket número 33.	33.00	Abierto
34	34	34	El cliente solicita ajustes adicionales en la entrega. Ticket número 34.	34.00	En Proceso
35	35	35	El cliente solicita ajustes adicionales en la entrega. Ticket número 35.	35.00	Resuelto
36	36	36	El cliente solicita ajustes adicionales en la entrega. Ticket número 36.	36.00	Cerrado
37	37	37	El cliente solicita ajustes adicionales en la entrega. Ticket número 37.	37.00	Abierto
38	38	38	El cliente solicita ajustes adicionales en la entrega. Ticket número 38.	38.00	En Proceso
39	39	39	El cliente solicita ajustes adicionales en la entrega. Ticket número 39.	39.00	Resuelto
40	40	40	El cliente solicita ajustes adicionales en la entrega. Ticket número 40.	40.00	Cerrado
41	41	41	El cliente solicita ajustes adicionales en la entrega. Ticket número 41.	41.00	Abierto
42	42	42	El cliente solicita ajustes adicionales en la entrega. Ticket número 42.	42.00	En Proceso
43	43	43	El cliente solicita ajustes adicionales en la entrega. Ticket número 43.	43.00	Resuelto
44	44	44	El cliente solicita ajustes adicionales en la entrega. Ticket número 44.	44.00	Cerrado
45	45	45	El cliente solicita ajustes adicionales en la entrega. Ticket número 45.	45.00	Abierto
46	46	46	El cliente solicita ajustes adicionales en la entrega. Ticket número 46.	46.00	En Proceso
47	47	47	El cliente solicita ajustes adicionales en la entrega. Ticket número 47.	47.00	Resuelto
48	48	48	El cliente solicita ajustes adicionales en la entrega. Ticket número 48.	48.00	Cerrado
49	49	49	El cliente solicita ajustes adicionales en la entrega. Ticket número 49.	49.00	Abierto
50	50	50	El cliente solicita ajustes adicionales en la entrega. Ticket número 50.	0.00	En Proceso
51	51	1	El cliente solicita ajustes adicionales en la entrega. Ticket número 51.	1.00	Resuelto
52	52	2	El cliente solicita ajustes adicionales en la entrega. Ticket número 52.	2.00	Cerrado
53	53	3	El cliente solicita ajustes adicionales en la entrega. Ticket número 53.	3.00	Abierto
54	54	4	El cliente solicita ajustes adicionales en la entrega. Ticket número 54.	4.00	En Proceso
55	55	5	El cliente solicita ajustes adicionales en la entrega. Ticket número 55.	5.00	Resuelto
56	56	6	El cliente solicita ajustes adicionales en la entrega. Ticket número 56.	6.00	Cerrado
57	57	7	El cliente solicita ajustes adicionales en la entrega. Ticket número 57.	7.00	Abierto
58	58	8	El cliente solicita ajustes adicionales en la entrega. Ticket número 58.	8.00	En Proceso
59	59	9	El cliente solicita ajustes adicionales en la entrega. Ticket número 59.	9.00	Resuelto
60	60	10	El cliente solicita ajustes adicionales en la entrega. Ticket número 60.	10.00	Cerrado
\.


--
-- TOC entry 4817 (class 0 OID 28295)
-- Dependencies: 314
-- Data for Name: tipos_notificacion; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.tipos_notificacion (id_tipo_notificacion, nombre_evento, formato_mensaje) FROM stdin;
1	Nuevo Pedido 1	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
2	Pedido Cancelado 2	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
3	Mensaje Nuevo 3	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
4	Pago Recibido 4	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
5	Pago Liberado 5	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
6	Entrega Realizada 6	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
7	Revisión Solicitada 7	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
8	Contrato Firmado 8	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
9	Nuevo Seguidor 9	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
10	Nuevo Like 10	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
11	Nuevo Comentario 11	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
12	Sorteo Ganado 12	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
13	Sorteo Iniciado 13	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
14	Cuenta Verificada 14	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
15	Certificado Aprobado 15	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
16	Ticket Abierto 16	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
17	Ticket Resuelto 17	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
18	Recordatorio de Entrega 18	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
19	Reseña Recibida 19	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
20	Actualización de Sistema 20	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
21	Nuevo Pedido 21	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
22	Pedido Cancelado 22	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
23	Mensaje Nuevo 23	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
24	Pago Recibido 24	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
25	Pago Liberado 25	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
26	Entrega Realizada 26	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
27	Revisión Solicitada 27	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
28	Contrato Firmado 28	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
29	Nuevo Seguidor 29	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
30	Nuevo Like 30	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
31	Nuevo Comentario 31	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
32	Sorteo Ganado 32	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
33	Sorteo Iniciado 33	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
34	Cuenta Verificada 34	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
35	Certificado Aprobado 35	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
36	Ticket Abierto 36	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
37	Ticket Resuelto 37	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
38	Recordatorio de Entrega 38	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
39	Reseña Recibida 39	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
40	Actualización de Sistema 40	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
41	Nuevo Pedido 41	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
42	Pedido Cancelado 42	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
43	Mensaje Nuevo 43	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
44	Pago Recibido 44	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
45	Pago Liberado 45	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
46	Entrega Realizada 46	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
47	Revisión Solicitada 47	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
48	Contrato Firmado 48	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
49	Nuevo Seguidor 49	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
50	Nuevo Like 50	Se ha generado el evento {evento} para el usuario {usuario} el {fecha}
51	SORTEO_GANADOR	¡Felicidades! Has ganado el sorteo: Sorteo Especial #1
\.


--
-- TOC entry 4819 (class 0 OID 28303)
-- Dependencies: 316
-- Data for Name: tokens_recuperacion; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.tokens_recuperacion (id_token, id_usuario, hash_token, fecha_generacion, usado) FROM stdin;
1	1	bc4fb96df4b7444ae58b6b0100d1f3a8	2026-07-31 22:11:17.845077	f
2	2	7502198569d6c502da22b58167c73cf4	2026-07-30 22:11:17.845077	f
3	3	2bd9e5127fc4f05486f7e34d2e5f644c	2026-07-29 22:11:17.845077	f
4	4	e8acfdd34dbaa9128b6ac70a7f7f2db5	2026-07-28 22:11:17.845077	t
5	5	fa20935c65e08c5ea12f9aa3d66cf348	2026-07-27 22:11:17.845077	f
6	6	fb71ca718adf0f6f60ada9ab98772971	2026-07-26 22:11:17.845077	f
7	7	28a25f0d49da825f0787f37847121e2a	2026-07-25 22:11:17.845077	f
8	8	4961b3b75fccb0157061bc30ee6b2175	2026-07-24 22:11:17.845077	t
9	9	9bb6ede969fbb3182a9e84f968e7055a	2026-07-23 22:11:17.845077	f
10	10	de657b8b6b52f70f0022a8eb4744230a	2026-07-22 22:11:17.845077	f
11	11	91a93af01d217778a89ea2844d69e49b	2026-07-21 22:11:17.845077	f
12	12	aa84cafa2e83f2e1ce77a7bc4a91fab0	2026-07-20 22:11:17.845077	t
13	13	51fd3f04d56edb7f1feb9c6f923b2854	2026-07-19 22:11:17.845077	f
14	14	e7b22f98f4adf4aa124efae87965024c	2026-07-18 22:11:17.845077	f
15	15	002c0d2cba4445aaedb7ad258f1628ef	2026-07-17 22:11:17.845077	f
16	16	33baacb946da249ea2e560173fb54d15	2026-07-16 22:11:17.845077	t
17	17	07601ffccf9b0f38194d92e2efd1159b	2026-07-15 22:11:17.845077	f
18	18	3f577767fa312b56007657b8033f8fc9	2026-07-14 22:11:17.845077	f
19	19	99c46ff6f1e05470e21724fec3950700	2026-07-13 22:11:17.845077	f
20	20	708edce9d1f07d6857e6bf7a5673d296	2026-07-12 22:11:17.845077	t
21	21	74aaa62c016b94861fd3ffdf505566e5	2026-07-11 22:11:17.845077	f
22	22	7f4cdbd3a22b0f55ff31b2ff125c5ecc	2026-07-10 22:11:17.845077	f
23	23	8a08150cf3b5394ece0215bf854ee7c0	2026-07-09 22:11:17.845077	f
24	24	a8957fa66b883432f3c8784d7db2c86f	2026-07-08 22:11:17.845077	t
25	25	03bb12560bb9050d87bb55a57ca284a9	2026-07-07 22:11:17.845077	f
26	26	2cf596c4b69407efc14f0e317a1632b1	2026-07-06 22:11:17.845077	f
27	27	32281aeefee03318f9c8460f7303d3ce	2026-07-05 22:11:17.845077	f
28	28	a588658f92ac42069ce5bbf8ec1c20e5	2026-07-04 22:11:17.845077	t
29	29	8313cab9a669325a2eb3cc474ab82f40	2026-07-03 22:11:17.845077	f
30	30	3771e531b9b7e08ee4cf286a5df21952	2026-07-02 22:11:17.845077	f
31	31	018377ee9b4a11083a2dddd45659d539	2026-07-01 22:11:17.845077	f
32	32	c07128acc81ea8c8d36c0687cf757ed0	2026-06-30 22:11:17.845077	t
33	33	79f11eba4bc4fd674680894bdf75d6e0	2026-06-29 22:11:17.845077	f
34	34	b13bcf003c2b9067bd9638295813a7d0	2026-06-28 22:11:17.845077	f
35	35	d19f8da576f35c28b7883b01ac9ebc7a	2026-06-27 22:11:17.845077	f
36	36	ae5fdb560285206d49bc2a44fa7289dd	2026-06-26 22:11:17.845077	t
37	37	bc160d2bda1ff01a8fb5b055d3e8e8de	2026-06-25 22:11:17.845077	f
38	38	b8bfc1ef41446ba911088c03e4f0d63a	2026-06-24 22:11:17.845077	f
39	39	51ca4c491da216dde2d970c0fe90f445	2026-06-23 22:11:17.845077	f
40	40	4aa4464ee7b5e42c5d1e388c4bc72e63	2026-06-22 22:11:17.845077	t
41	41	4b8619636b648c89f7f88573c93b1249	2026-06-21 22:11:17.845077	f
42	42	a513a9b3c1b32124f30816d58cad41aa	2026-06-20 22:11:17.845077	f
43	43	70708991d205ca0917d45faded496933	2026-06-19 22:11:17.845077	f
44	44	4a1f4a4d72f2cb54590d107f2beb1cab	2026-06-18 22:11:17.845077	t
45	45	3a5213417f856fcc0fabe453fc2533b7	2026-06-17 22:11:17.845077	f
46	46	70d441c9e588727eaa6abd46c01ee878	2026-06-16 22:11:17.845077	f
47	47	5b0c1655fed07017d8da9f128dd900b4	2026-06-15 22:11:17.845077	f
48	48	d421708435912c9b6ef17698a060313a	2026-06-14 22:11:17.845077	t
49	49	fb3fc351fe63c4efae3323898708f253	2026-06-13 22:11:17.845077	f
50	50	b74c50da6285ca1112683375145d821c	2026-06-12 22:11:17.845077	f
51	51	a797a71d68855eabdfd488b8c6d6cfac	2026-06-11 22:11:17.845077	f
52	52	f3673349a56c52eacda291e6d7e26a8c	2026-06-10 22:11:17.845077	t
53	53	0771f36955d06e27429e18f4a5fb589d	2026-06-09 22:11:17.845077	f
54	54	2919e312db71455167c4ece850c44bb0	2026-06-08 22:11:17.845077	f
55	55	d051e2cca5f745e9135f5d9f16c04a98	2026-06-07 22:11:17.845077	f
56	56	6fd4af65cfbc89115f1161f04ee227b8	2026-06-06 22:11:17.845077	t
57	57	52362e8bec5affcf00fb4164814aee76	2026-06-05 22:11:17.845077	f
58	58	9439d5b84824ff62df96c8b168398546	2026-06-04 22:11:17.845077	f
59	59	cef216c31d3c189edd04a02398924968	2026-06-03 22:11:17.845077	f
60	60	e8e2a555ae473df39506bbc77bd0875c	2026-06-02 22:11:17.845077	t
\.


--
-- TOC entry 4821 (class 0 OID 28312)
-- Dependencies: 318
-- Data for Name: transacciones_pago; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.transacciones_pago (id_transaccion, id_pago, tipo_transaccion, monto, fecha_ejecucion) FROM stdin;
1	1	Retencion	27.00	2026-07-31 22:11:17.845077
2	2	Liberacion	44.00	2026-07-30 22:11:17.845077
3	3	Reembolso	61.00	2026-07-29 22:11:17.845077
4	4	Comision	78.00	2026-07-28 22:11:17.845077
5	5	Retencion	95.00	2026-07-27 22:11:17.845077
6	6	Liberacion	112.00	2026-07-26 22:11:17.845077
7	7	Reembolso	129.00	2026-07-25 22:11:17.845077
8	8	Comision	146.00	2026-07-24 22:11:17.845077
9	9	Retencion	163.00	2026-07-23 22:11:17.845077
10	10	Liberacion	180.00	2026-07-22 22:11:17.845077
11	11	Reembolso	197.00	2026-07-21 22:11:17.845077
12	12	Comision	214.00	2026-07-20 22:11:17.845077
13	13	Retencion	231.00	2026-07-19 22:11:17.845077
14	14	Liberacion	248.00	2026-07-18 22:11:17.845077
15	15	Reembolso	265.00	2026-07-17 22:11:17.845077
16	16	Comision	282.00	2026-07-16 22:11:17.845077
17	17	Retencion	299.00	2026-07-15 22:11:17.845077
18	18	Liberacion	316.00	2026-07-14 22:11:17.845077
19	19	Reembolso	333.00	2026-07-13 22:11:17.845077
20	20	Comision	350.00	2026-07-12 22:11:17.845077
21	21	Retencion	367.00	2026-07-11 22:11:17.845077
22	22	Liberacion	384.00	2026-07-10 22:11:17.845077
23	23	Reembolso	401.00	2026-07-09 22:11:17.845077
24	24	Comision	418.00	2026-07-08 22:11:17.845077
25	25	Retencion	435.00	2026-07-07 22:11:17.845077
26	26	Liberacion	452.00	2026-07-06 22:11:17.845077
27	27	Reembolso	469.00	2026-07-05 22:11:17.845077
28	28	Comision	486.00	2026-07-04 22:11:17.845077
29	29	Retencion	503.00	2026-07-03 22:11:17.845077
30	30	Liberacion	520.00	2026-07-02 22:11:17.845077
31	31	Reembolso	537.00	2026-07-01 22:11:17.845077
32	32	Comision	554.00	2026-06-30 22:11:17.845077
33	33	Retencion	571.00	2026-06-29 22:11:17.845077
34	34	Liberacion	588.00	2026-06-28 22:11:17.845077
35	35	Reembolso	605.00	2026-06-27 22:11:17.845077
36	36	Comision	622.00	2026-06-26 22:11:17.845077
37	37	Retencion	639.00	2026-06-25 22:11:17.845077
38	38	Liberacion	656.00	2026-06-24 22:11:17.845077
39	39	Reembolso	673.00	2026-06-23 22:11:17.845077
40	40	Comision	690.00	2026-06-22 22:11:17.845077
41	41	Retencion	707.00	2026-06-21 22:11:17.845077
42	42	Liberacion	724.00	2026-06-20 22:11:17.845077
43	43	Reembolso	741.00	2026-06-19 22:11:17.845077
44	44	Comision	758.00	2026-06-18 22:11:17.845077
45	45	Retencion	775.00	2026-06-17 22:11:17.845077
46	46	Liberacion	792.00	2026-06-16 22:11:17.845077
47	47	Reembolso	809.00	2026-06-15 22:11:17.845077
48	48	Comision	826.00	2026-06-14 22:11:17.845077
49	49	Retencion	843.00	2026-06-13 22:11:17.845077
50	50	Liberacion	860.00	2026-06-12 22:11:17.845077
51	1	Reembolso	877.00	2026-06-11 22:11:17.845077
52	2	Comision	894.00	2026-06-10 22:11:17.845077
53	3	Retencion	911.00	2026-06-09 22:11:17.845077
54	4	Liberacion	928.00	2026-06-08 22:11:17.845077
55	5	Reembolso	945.00	2026-06-07 22:11:17.845077
56	6	Comision	962.00	2026-06-06 22:11:17.845077
57	7	Retencion	979.00	2026-06-05 22:11:17.845077
58	8	Liberacion	11.00	2026-06-04 22:11:17.845077
59	9	Reembolso	28.00	2026-06-03 22:11:17.845077
60	10	Comision	45.00	2026-06-02 22:11:17.845077
61	11	Retencion	62.00	2026-06-01 22:11:17.845077
62	12	Liberacion	79.00	2026-05-31 22:11:17.845077
63	13	Reembolso	96.00	2026-05-30 22:11:17.845077
64	14	Comision	113.00	2026-05-29 22:11:17.845077
65	15	Retencion	130.00	2026-05-28 22:11:17.845077
66	16	Liberacion	147.00	2026-05-27 22:11:17.845077
67	17	Reembolso	164.00	2026-05-26 22:11:17.845077
68	18	Comision	181.00	2026-05-25 22:11:17.845077
69	19	Retencion	198.00	2026-05-24 22:11:17.845077
70	20	Liberacion	215.00	2026-05-23 22:11:17.845077
\.


--
-- TOC entry 4823 (class 0 OID 28321)
-- Dependencies: 320
-- Data for Name: usuario_roles; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.usuario_roles (id_usuario_rol, id_usuario, id_rol) FROM stdin;
1	1	1
2	2	2
3	3	3
4	4	4
5	5	5
6	6	6
7	7	7
8	8	8
9	9	9
10	10	10
11	11	11
12	12	12
13	13	13
14	14	14
15	15	15
16	16	16
17	17	17
18	18	18
19	19	19
20	20	20
21	21	21
22	22	22
23	23	23
24	24	24
25	25	25
26	26	26
27	27	27
28	28	28
29	29	29
30	30	30
31	31	31
32	32	32
33	33	33
34	34	34
35	35	35
36	36	36
37	37	37
38	38	38
39	39	39
40	40	40
41	41	41
42	42	42
43	43	43
44	44	44
45	45	45
46	46	46
47	47	47
48	48	48
49	49	49
50	50	50
51	51	1
52	52	2
53	53	3
54	54	4
55	55	5
56	56	6
57	57	7
58	58	8
59	59	9
60	60	10
61	61	1
62	67	3
63	68	5
64	69	5
65	70	3
66	61	1
67	72	3
68	73	52
\.


--
-- TOC entry 4825 (class 0 OID 28328)
-- Dependencies: 322
-- Data for Name: usuarios; Type: TABLE DATA; Schema: public; Owner: adminuteq
--

COPY public.usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, id_pais, fecha_registro, estado_cuenta, fecha_nacimiento, actualizado_en) FROM stdin;
1	Juan	Sánchez	usuario1@ejemplo.com	$2b$12$a4a97ffc170ec7ab32b85b2129c69c50	1	2026-07-21 22:11:17.845077	t	2007-11-16	2026-08-02 03:11:17.845077+00
2	María	Cruz	usuario2@ejemplo.com	$2b$12$10dea63031376352d413a8e530654b8b	2	2026-07-10 22:11:17.845077	t	2007-02-26	2026-08-02 03:11:17.845077+00
3	Carlos	Vargas	usuario3@ejemplo.com	$2b$12$35559e8b5732fbd5029bef54aeab7a21	3	2026-06-29 22:11:17.845077	t	2006-06-08	2026-08-02 03:11:17.845077+00
4	Ana	Herrera	usuario4@ejemplo.com	$2b$12$c707dce7b5a990e349c873268cf5a968	4	2026-06-18 22:11:17.845077	t	2005-09-18	2026-08-02 03:11:17.845077+00
5	Luis	Rojas	usuario5@ejemplo.com	$2b$12$9d4ba1ec63d70f19106c2aec14926374	5	2026-06-07 22:11:17.845077	t	2004-12-29	2026-08-02 03:11:17.845077+00
6	Laura	Campos	usuario6@ejemplo.com	$2b$12$e5c4bd895be104cc1a928687c7fc922a	6	2026-05-27 22:11:17.845077	t	2004-04-10	2026-08-02 03:11:17.845077+00
7	Miguel	Cárdenas	usuario7@ejemplo.com	$2b$12$8b33d112c1a64f9fb374eb87b98990cf	7	2026-05-16 22:11:17.845077	t	2003-07-22	2026-08-02 03:11:17.845077+00
8	Sofía	Pérez	usuario8@ejemplo.com	$2b$12$7d905fbdc246912149bf8bdb2c43efd8	8	2026-05-05 22:11:17.845077	t	2002-11-01	2026-08-02 03:11:17.845077+00
9	Diego	Díaz	usuario9@ejemplo.com	$2b$12$68bfccf877bb29c1698663b8f6920a20	9	2026-04-24 22:11:17.845077	t	2002-02-11	2026-08-02 03:11:17.845077+00
10	Valentina	Ramos	usuario10@ejemplo.com	$2b$12$e823d38e2018737a77b4b9bf3e94c697	10	2026-04-13 22:11:17.845077	f	2001-05-24	2026-08-02 03:11:17.845077+00
11	Andrés	Ruiz	usuario11@ejemplo.com	$2b$12$df83832006c3ed2ecd41b30ac17135ba	11	2026-04-02 22:11:17.845077	t	2000-09-03	2026-08-02 03:11:17.845077+00
12	Camila	Castro	usuario12@ejemplo.com	$2b$12$e96f92777862ceec18f357286c8e9a25	12	2026-03-22 22:11:17.845077	t	1999-12-15	2026-08-02 03:11:17.845077+00
13	Javier	Fuentes	usuario13@ejemplo.com	$2b$12$9e2edf423232eaae1dacb2f75e893238	13	2026-03-11 22:11:17.845077	t	1999-03-27	2026-08-02 03:11:17.845077+00
14	Isabella	Miranda	usuario14@ejemplo.com	$2b$12$03dd756b38c0670f58784289db2de843	14	2026-02-28 22:11:17.845077	t	1998-07-07	2026-08-02 03:11:17.845077+00
15	Ricardo	González	usuario15@ejemplo.com	$2b$12$7cc93421caac896a47e28dbca3e4bf3a	15	2026-02-17 22:11:17.845077	t	1997-10-17	2026-08-02 03:11:17.845077+00
16	Daniela	Gómez	usuario16@ejemplo.com	$2b$12$aa0ec9ab40020a7204d82a3816887e34	16	2026-02-06 22:11:17.845077	t	1997-01-27	2026-08-02 03:11:17.845077+00
17	Fernando	Chávez	usuario17@ejemplo.com	$2b$12$ebf5fb5d1b229d775d5008a8d28e480c	17	2026-01-26 22:11:17.845077	t	1996-05-09	2026-08-02 03:11:17.845077+00
18	Gabriela	Mendoza	usuario18@ejemplo.com	$2b$12$9accf2548fc55b618861aa3bbe7a40f5	18	2026-01-15 22:11:17.845077	t	1995-08-20	2026-08-02 03:11:17.845077+00
19	Sergio	Vega	usuario19@ejemplo.com	$2b$12$5e4ae615649f44d0301c7ef1ea29431c	19	2026-01-04 22:11:17.845077	t	1994-11-30	2026-08-02 03:11:17.845077+00
20	Paula	Molina	usuario20@ejemplo.com	$2b$12$f61f5003a1f953afd964f6e36d1110e9	20	2025-12-24 22:11:17.845077	f	1994-03-12	2026-08-02 03:11:17.845077+00
21	Alejandro	Paredes	usuario21@ejemplo.com	$2b$12$9e78ace5ffcc133fec631f919f27e62e	21	2025-12-13 22:11:17.845077	t	1993-06-22	2026-08-02 03:11:17.845077+00
22	Lucía	López	usuario22@ejemplo.com	$2b$12$3461f0f301ddfca9fdd89bab7d7ed845	22	2025-12-02 22:11:17.845077	t	1992-10-02	2026-08-02 03:11:17.845077+00
23	Roberto	Rivera	usuario23@ejemplo.com	$2b$12$8d5f8bee06411cb1e189c294d437556c	23	2025-11-21 22:11:17.845077	t	1992-01-13	2026-08-02 03:11:17.845077+00
24	Elena	Gutiérrez	usuario24@ejemplo.com	$2b$12$8982c5012464a59c7682ca7c93b4288f	24	2025-11-10 22:11:17.845077	t	1991-04-25	2026-08-02 03:11:17.845077+00
25	Pablo	Álvarez	usuario25@ejemplo.com	$2b$12$2f968fd9e044d0aeb3d396abcbf4dcc4	25	2025-10-30 22:11:17.845077	t	1990-08-05	2026-08-02 03:11:17.845077+00
26	Carolina	Núñez	usuario26@ejemplo.com	$2b$12$3edf4c0533caea3af27af063a8553b11	26	2025-10-19 22:11:17.845077	t	1989-11-15	2026-08-02 03:11:17.845077+00
27	Manuel	Peña	usuario27@ejemplo.com	$2b$12$793b0aab1969bcc27a7e6de325f6136f	27	2025-10-08 22:11:17.845077	t	1989-02-25	2026-08-02 03:11:17.845077+00
28	Victoria	Cordero	usuario28@ejemplo.com	$2b$12$2357a0480d583d11529ce50948e5b9d5	28	2025-09-27 22:11:17.845077	t	1988-06-07	2026-08-02 03:11:17.845077+00
29	Jorge	Martínez	usuario29@ejemplo.com	$2b$12$8e5a7b2e2ba198fef4c7578608ee4830	29	2025-09-16 22:11:17.845077	t	1987-09-18	2026-08-02 03:11:17.845077+00
30	Natalia	Flores	usuario30@ejemplo.com	$2b$12$7e441a6c2bc9032c62765c0d1caf383f	30	2025-09-05 22:11:17.845077	f	1986-12-29	2026-08-02 03:11:17.845077+00
31	Raúl	Ortiz	usuario31@ejemplo.com	$2b$12$53bf93cc9b0c6f191cf4d1997bc065b8	31	2025-08-25 22:11:17.845077	t	1986-04-10	2026-08-02 03:11:17.845077+00
32	Marina	Romero	usuario32@ejemplo.com	$2b$12$f13b6c98aae222e3e3186e4aa91793dd	32	2025-08-14 22:11:17.845077	t	1985-07-21	2026-08-02 03:11:17.845077+00
33	Óscar	Silva	usuario33@ejemplo.com	$2b$12$7773171a0230788432b2f6f6bd6cd89f	33	2025-08-03 22:11:17.845077	t	1984-10-31	2026-08-02 03:11:17.845077+00
34	Adriana	Contreras	usuario34@ejemplo.com	$2b$12$deb10b676b85b8a517df92bea6712d9d	34	2025-07-23 22:11:17.845077	t	1984-02-11	2026-08-02 03:11:17.845077+00
35	Iván	Salazar	usuario35@ejemplo.com	$2b$12$bae90f0f0d2f24687d773383ef48fff7	35	2025-07-12 22:11:17.845077	t	1983-05-24	2026-08-02 03:11:17.845077+00
36	Patricia	Rodríguez	usuario36@ejemplo.com	$2b$12$046d76e5c42885d5a0f2115b3e4e1fd6	36	2025-07-01 22:11:17.845077	t	1982-09-03	2026-08-02 03:11:17.845077+00
37	Rubén	Torres	usuario37@ejemplo.com	$2b$12$db2a5589215de37b0626599d2658e521	37	2025-06-20 22:11:17.845077	t	1981-12-14	2026-08-02 03:11:17.845077+00
38	Silvia	Reyes	usuario38@ejemplo.com	$2b$12$cc0bf7854d28f8ea008960e8faa7a3bf	38	2025-06-09 22:11:17.845077	t	1981-03-26	2026-08-02 03:11:17.845077+00
39	Álvaro	Jiménez	usuario39@ejemplo.com	$2b$12$93bd5d5bb0b9f117b3f723d533603e00	39	2025-05-29 22:11:17.845077	t	1980-07-06	2026-08-02 03:11:17.845077+00
40	Cristina	Aguilar	usuario40@ejemplo.com	$2b$12$1daf7d005e596d1d16a8fb2a4d735b23	40	2025-05-18 22:11:17.845077	f	1979-10-17	2026-08-02 03:11:17.845077+00
41	Hugo	Delgado	usuario41@ejemplo.com	$2b$12$f4a9fac2cdada19a4521a98f21c10d6b	41	2025-05-07 22:11:17.845077	t	1979-01-27	2026-08-02 03:11:17.845077+00
42	Beatriz	Espinoza	usuario42@ejemplo.com	$2b$12$d57568936e676e5a7b53f9379832667c	42	2025-04-26 22:11:17.845077	t	1978-05-09	2026-08-02 03:11:17.845077+00
43	Mario	García	usuario43@ejemplo.com	$2b$12$c918d163ee4473648ef17b68bbd44bb5	43	2025-04-15 22:11:17.845077	t	1977-08-19	2026-08-02 03:11:17.845077+00
44	Rosa	Ramírez	usuario44@ejemplo.com	$2b$12$1695872dc3ecc303c013f257f312a8b6	44	2025-04-04 22:11:17.845077	t	1976-11-29	2026-08-02 03:11:17.845077+00
45	Antonio	Morales	usuario45@ejemplo.com	$2b$12$ddb9c7d2522d3687de72cdb5a7f557ba	45	2025-03-24 22:11:17.845077	t	1976-03-11	2026-08-02 03:11:17.845077+00
46	Teresa	Castillo	usuario46@ejemplo.com	$2b$12$3ef5bc0473b4e2f41dac96ad4ef1114e	46	2025-03-13 22:11:17.845077	t	1975-06-22	2026-08-02 03:11:17.845077+00
47	Francisco	Medina	usuario47@ejemplo.com	$2b$12$2daf2c205b4a8184bacb60b4977e9be1	47	2025-03-02 22:11:17.845077	t	1974-10-02	2026-08-02 03:11:17.845077+00
48	Claudia	Guerrero	usuario48@ejemplo.com	$2b$12$e960a1ab1ffa8f657aba1e6fcf54b28c	48	2025-02-19 22:11:17.845077	t	1974-01-12	2026-08-02 03:11:17.845077+00
49	Enrique	Cabrera	usuario49@ejemplo.com	$2b$12$5ea5849aacd2e9d5d1fc4cd6cc5798a8	49	2025-02-08 22:11:17.845077	t	1973-04-24	2026-08-02 03:11:17.845077+00
50	Verónica	Navarro	usuario50@ejemplo.com	$2b$12$8d284d0c6596839eaa80b0dcb37dcf9b	50	2025-01-28 22:11:17.845077	f	1972-08-04	2026-08-02 03:11:17.845077+00
51	Juan	Sánchez	usuario51@ejemplo.com	$2b$12$9a900f0e752fab53468e1d781eb3ef23	1	2025-01-17 22:11:17.845077	t	1971-11-15	2026-08-02 03:11:17.845077+00
52	María	Cruz	usuario52@ejemplo.com	$2b$12$3016a4f557bc29b37e0593a024464479	2	2025-01-06 22:11:17.845077	t	1971-02-25	2026-08-02 03:11:17.845077+00
53	Carlos	Vargas	usuario53@ejemplo.com	$2b$12$698eb1c2004dee6250385fc14a7356da	3	2024-12-26 22:11:17.845077	t	1970-06-07	2026-08-02 03:11:17.845077+00
54	Ana	Herrera	usuario54@ejemplo.com	$2b$12$6a1b2c501650bc0fbdc356aae0f418fe	4	2024-12-15 22:11:17.845077	t	1969-09-17	2026-08-02 03:11:17.845077+00
55	Luis	Rojas	usuario55@ejemplo.com	$2b$12$2a7223a0fb67b225cf55ff9d003b0ead	5	2024-12-04 22:11:17.845077	t	1968-12-28	2026-08-02 03:11:17.845077+00
56	Laura	Campos	usuario56@ejemplo.com	$2b$12$af3d9df9d68709bcf782acf1424184df	6	2024-11-23 22:11:17.845077	t	1968-04-09	2026-08-02 03:11:17.845077+00
57	Miguel	Cárdenas	usuario57@ejemplo.com	$2b$12$afd0268782578840c4bba98529c80f59	7	2024-11-12 22:11:17.845077	t	1967-07-21	2026-08-02 03:11:17.845077+00
58	Sofía	Pérez	usuario58@ejemplo.com	$2b$12$7dac1d0219a7efefa8715d743c04f1ab	8	2024-11-01 22:11:17.845077	t	2007-11-25	2026-08-02 03:11:17.845077+00
59	Diego	Díaz	usuario59@ejemplo.com	$2b$12$330caaac6f56e8207190be9caeabcb58	9	2024-10-21 22:11:17.845077	t	2007-03-07	2026-08-02 03:11:17.845077+00
60	Valentina	Ramos	usuario60@ejemplo.com	$2b$12$281905d0619807b0028a03dd3b5a264f	10	2024-10-10 22:11:17.845077	f	2006-06-17	2026-08-02 03:11:17.845077+00
61	Administrador	Artisync	admin@artisync.com	$2a$12$O26tVGE2jZ/6rNDZJYaKyOfDPE0.8E9HIbISLR4nXySuy.nvvycjK	1	2026-08-16 23:29:25.522246	t	1990-01-01	2026-08-17 01:06:26.31644+00
67	Usuario	Cliente	cliente@artisync.com	$2a$12$O26tVGE2jZ/6rNDZJYaKyOfDPE0.8E9HIbISLR4nXySuy.nvvycjK	7	2026-08-17 03:34:53.64749	t	2000-01-12	2026-08-17 03:36:53.349583+00
68	Usuario	Creador	creador@artisync.com	$2a$12$O26tVGE2jZ/6rNDZJYaKyOfDPE0.8E9HIbISLR4nXySuy.nvvycjK	7	2026-08-17 03:34:54.849642	t	2000-12-11	2026-08-17 03:38:15.241495+00
69	creador	CR	creador@gmail.com	$2a$12$9NrQX1aV7OmdR.ANgRXIh.TLdrsPN5ffTowsZso6ydOScY5ftToTO	51	2026-08-17 12:53:48.376845	t	2004-01-04	2026-08-17 12:53:48.376808+00
70	nose	nose	nose@nose.com	$2a$12$I71OJfEzKfXor7Kg5qvzA.GENxI69x6DiEWqoNzCVuCVv/rpJ5ouS	27	2026-08-17 18:13:58.640483	t	2002-12-11	2026-08-17 18:20:41.162291+00
72	Moderador	MOD	mod@gmail.com	$2a$12$LNse3snTva0hUZHOEJMOxuFaxUEkHmsf0oxezAjbEerDVnQ0Jy7dK	\N	2026-08-17 19:04:02.093403	t	\N	2026-08-17 19:04:02.093172+00
73	mod	mod	mod1@gmail.com	$2a$12$Qq/7bWZrrilG/DSN7nz5oOntesSZ4ENFFpMCG3I2oG5u5hYvr1hnm	55	2026-08-17 19:04:58.597183	t	2003-01-24	2026-08-17 19:05:53.759164+00
\.


--
-- TOC entry 5068 (class 0 OID 0)
-- Dependencies: 220
-- Name: atributos_dinamicos_id_atributo_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.atributos_dinamicos_id_atributo_seq', 50, true);


--
-- TOC entry 5069 (class 0 OID 0)
-- Dependencies: 222
-- Name: autenticacion_dos_factores_id_2fa_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.autenticacion_dos_factores_id_2fa_seq', 51, true);


--
-- TOC entry 5070 (class 0 OID 0)
-- Dependencies: 224
-- Name: briefing_enviados_id_briefing_enviado_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.briefing_enviados_id_briefing_enviado_seq', 60, true);


--
-- TOC entry 5071 (class 0 OID 0)
-- Dependencies: 226
-- Name: briefing_plantillas_id_briefing_plantilla_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.briefing_plantillas_id_briefing_plantilla_seq', 55, true);


--
-- TOC entry 5072 (class 0 OID 0)
-- Dependencies: 228
-- Name: briefing_preguntas_id_pregunta_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.briefing_preguntas_id_pregunta_seq', 80, true);


--
-- TOC entry 5073 (class 0 OID 0)
-- Dependencies: 230
-- Name: briefing_respuestas_id_respuesta_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.briefing_respuestas_id_respuesta_seq', 80, true);


--
-- TOC entry 5074 (class 0 OID 0)
-- Dependencies: 232
-- Name: categorias_id_categoria_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.categorias_id_categoria_seq', 50, true);


--
-- TOC entry 5075 (class 0 OID 0)
-- Dependencies: 234
-- Name: certificados_ia_id_certificado_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.certificados_ia_id_certificado_seq', 61, true);


--
-- TOC entry 5076 (class 0 OID 0)
-- Dependencies: 236
-- Name: codigos_respaldo_2fa_id_codigo_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.codigos_respaldo_2fa_id_codigo_seq', 78, true);


--
-- TOC entry 5077 (class 0 OID 0)
-- Dependencies: 238
-- Name: comentarios_portafolio_id_comentario_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.comentarios_portafolio_id_comentario_seq', 70, true);


--
-- TOC entry 5078 (class 0 OID 0)
-- Dependencies: 240
-- Name: contratos_id_contrato_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.contratos_id_contrato_seq', 50, true);


--
-- TOC entry 5079 (class 0 OID 0)
-- Dependencies: 242
-- Name: creador_habilidades_id_creador_habilidad_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.creador_habilidades_id_creador_habilidad_seq', 70, true);


--
-- TOC entry 5080 (class 0 OID 0)
-- Dependencies: 244
-- Name: documentos_adjuntos_id_adjunto_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.documentos_adjuntos_id_adjunto_seq', 60, true);


--
-- TOC entry 5081 (class 0 OID 0)
-- Dependencies: 246
-- Name: entregables_finales_id_entregable_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.entregables_finales_id_entregable_seq', 60, true);


--
-- TOC entry 5082 (class 0 OID 0)
-- Dependencies: 248
-- Name: estados_verificacion_id_estado_verificacion_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.estados_verificacion_id_estado_verificacion_seq', 54, true);


--
-- TOC entry 5083 (class 0 OID 0)
-- Dependencies: 250
-- Name: etapas_flujo_id_etapa_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.etapas_flujo_id_etapa_seq', 50, true);


--
-- TOC entry 5084 (class 0 OID 0)
-- Dependencies: 252
-- Name: etiquetas_id_etiqueta_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.etiquetas_id_etiqueta_seq', 50, true);


--
-- TOC entry 5085 (class 0 OID 0)
-- Dependencies: 254
-- Name: flujo_etapas_config_id_flujo_etapa_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.flujo_etapas_config_id_flujo_etapa_seq', 100, true);


--
-- TOC entry 5086 (class 0 OID 0)
-- Dependencies: 256
-- Name: flujos_trabajo_id_flujo_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.flujos_trabajo_id_flujo_seq', 50, true);


--
-- TOC entry 5087 (class 0 OID 0)
-- Dependencies: 259
-- Name: habilidades_id_habilidad_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.habilidades_id_habilidad_seq', 50, true);


--
-- TOC entry 5088 (class 0 OID 0)
-- Dependencies: 261
-- Name: historial_estados_pedido_id_historial_estado_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.historial_estados_pedido_id_historial_estado_seq', 90, true);


--
-- TOC entry 5089 (class 0 OID 0)
-- Dependencies: 263
-- Name: infracciones_mensaje_id_infraccion_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.infracciones_mensaje_id_infraccion_seq', 55, true);


--
-- TOC entry 5090 (class 0 OID 0)
-- Dependencies: 265
-- Name: likes_portafolio_id_like_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.likes_portafolio_id_like_seq', 80, true);


--
-- TOC entry 5091 (class 0 OID 0)
-- Dependencies: 267
-- Name: mensajes_id_mensaje_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.mensajes_id_mensaje_seq', 90, true);


--
-- TOC entry 5092 (class 0 OID 0)
-- Dependencies: 269
-- Name: motivos_rechazo_id_motivo_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.motivos_rechazo_id_motivo_seq', 50, true);


--
-- TOC entry 5093 (class 0 OID 0)
-- Dependencies: 271
-- Name: notificaciones_sistema_id_notificacion_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.notificaciones_sistema_id_notificacion_seq', 91, true);


--
-- TOC entry 5094 (class 0 OID 0)
-- Dependencies: 273
-- Name: pagos_garantia_id_pago_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.pagos_garantia_id_pago_seq', 50, true);


--
-- TOC entry 5095 (class 0 OID 0)
-- Dependencies: 275
-- Name: pais_id_pais_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.pais_id_pais_seq', 442, true);


--
-- TOC entry 5096 (class 0 OID 0)
-- Dependencies: 277
-- Name: participantes_sorteo_id_participacion_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.participantes_sorteo_id_participacion_seq', 70, true);


--
-- TOC entry 5097 (class 0 OID 0)
-- Dependencies: 279
-- Name: pedidos_id_pedido_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.pedidos_id_pedido_seq', 60, true);


--
-- TOC entry 5098 (class 0 OID 0)
-- Dependencies: 281
-- Name: perfiles_creadores_id_perfil_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.perfiles_creadores_id_perfil_seq', 52, true);


--
-- TOC entry 5099 (class 0 OID 0)
-- Dependencies: 283
-- Name: permisos_id_permiso_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.permisos_id_permiso_seq', 85, true);


--
-- TOC entry 5100 (class 0 OID 0)
-- Dependencies: 285
-- Name: plantillas_contrato_id_plantilla_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.plantillas_contrato_id_plantilla_seq', 50, true);


--
-- TOC entry 5101 (class 0 OID 0)
-- Dependencies: 287
-- Name: portafolio_items_id_item_portafolio_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.portafolio_items_id_item_portafolio_seq', 71, true);


--
-- TOC entry 5102 (class 0 OID 0)
-- Dependencies: 289
-- Name: portafolios_id_portafolio_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.portafolios_id_portafolio_seq', 51, true);


--
-- TOC entry 5103 (class 0 OID 0)
-- Dependencies: 291
-- Name: resenas_servicios_id_resena_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.resenas_servicios_id_resena_seq', 50, true);


--
-- TOC entry 5104 (class 0 OID 0)
-- Dependencies: 293
-- Name: rol_permisos_id_rol_permiso_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.rol_permisos_id_rol_permiso_seq', 185, true);


--
-- TOC entry 5105 (class 0 OID 0)
-- Dependencies: 295
-- Name: roles_id_rol_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.roles_id_rol_seq', 57, true);


--
-- TOC entry 5106 (class 0 OID 0)
-- Dependencies: 297
-- Name: salas_chat_id_sala_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.salas_chat_id_sala_seq', 50, true);


--
-- TOC entry 5107 (class 0 OID 0)
-- Dependencies: 299
-- Name: seguidores_id_seguimiento_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.seguidores_id_seguimiento_seq', 70, true);


--
-- TOC entry 5108 (class 0 OID 0)
-- Dependencies: 301
-- Name: servicio_atributos_id_servicio_atributo_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.servicio_atributos_id_servicio_atributo_seq', 70, true);


--
-- TOC entry 5109 (class 0 OID 0)
-- Dependencies: 303
-- Name: servicio_etiquetas_id_servicio_etiqueta_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.servicio_etiquetas_id_servicio_etiqueta_seq', 70, true);


--
-- TOC entry 5110 (class 0 OID 0)
-- Dependencies: 305
-- Name: servicios_id_servicio_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.servicios_id_servicio_seq', 60, true);


--
-- TOC entry 5111 (class 0 OID 0)
-- Dependencies: 307
-- Name: sesiones_usuario_id_sesion_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.sesiones_usuario_id_sesion_seq', 146, true);


--
-- TOC entry 5112 (class 0 OID 0)
-- Dependencies: 309
-- Name: sorteos_id_sorteo_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.sorteos_id_sorteo_seq', 50, true);


--
-- TOC entry 5113 (class 0 OID 0)
-- Dependencies: 311
-- Name: subcategorias_id_subcategoria_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.subcategorias_id_subcategoria_seq', 60, true);


--
-- TOC entry 5114 (class 0 OID 0)
-- Dependencies: 313
-- Name: tickets_revision_id_ticket_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.tickets_revision_id_ticket_seq', 60, true);


--
-- TOC entry 5115 (class 0 OID 0)
-- Dependencies: 315
-- Name: tipos_notificacion_id_tipo_notificacion_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.tipos_notificacion_id_tipo_notificacion_seq', 51, true);


--
-- TOC entry 5116 (class 0 OID 0)
-- Dependencies: 317
-- Name: tokens_recuperacion_id_token_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.tokens_recuperacion_id_token_seq', 60, true);


--
-- TOC entry 5117 (class 0 OID 0)
-- Dependencies: 319
-- Name: transacciones_pago_id_transaccion_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.transacciones_pago_id_transaccion_seq', 70, true);


--
-- TOC entry 5118 (class 0 OID 0)
-- Dependencies: 321
-- Name: usuario_roles_id_usuario_rol_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.usuario_roles_id_usuario_rol_seq', 68, true);


--
-- TOC entry 5119 (class 0 OID 0)
-- Dependencies: 323
-- Name: usuarios_id_usuario_seq; Type: SEQUENCE SET; Schema: public; Owner: adminuteq
--

SELECT pg_catalog.setval('public.usuarios_id_usuario_seq', 73, true);


--
-- TOC entry 4339 (class 2606 OID 28397)
-- Name: atributos_dinamicos atributos_dinamicos_nombre_atributo_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.atributos_dinamicos
    ADD CONSTRAINT atributos_dinamicos_nombre_atributo_key UNIQUE (nombre_atributo);


--
-- TOC entry 4341 (class 2606 OID 28399)
-- Name: atributos_dinamicos atributos_dinamicos_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.atributos_dinamicos
    ADD CONSTRAINT atributos_dinamicos_pkey PRIMARY KEY (id_atributo);


--
-- TOC entry 4343 (class 2606 OID 28401)
-- Name: autenticacion_dos_factores autenticacion_dos_factores_id_usuario_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.autenticacion_dos_factores
    ADD CONSTRAINT autenticacion_dos_factores_id_usuario_key UNIQUE (id_usuario);


--
-- TOC entry 4345 (class 2606 OID 28403)
-- Name: autenticacion_dos_factores autenticacion_dos_factores_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.autenticacion_dos_factores
    ADD CONSTRAINT autenticacion_dos_factores_pkey PRIMARY KEY (id_2fa);


--
-- TOC entry 4347 (class 2606 OID 28405)
-- Name: briefing_enviados briefing_enviados_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_enviados
    ADD CONSTRAINT briefing_enviados_pkey PRIMARY KEY (id_briefing_enviado);


--
-- TOC entry 4349 (class 2606 OID 28407)
-- Name: briefing_plantillas briefing_plantillas_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_plantillas
    ADD CONSTRAINT briefing_plantillas_pkey PRIMARY KEY (id_briefing_plantilla);


--
-- TOC entry 4351 (class 2606 OID 28409)
-- Name: briefing_preguntas briefing_preguntas_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_preguntas
    ADD CONSTRAINT briefing_preguntas_pkey PRIMARY KEY (id_pregunta);


--
-- TOC entry 4353 (class 2606 OID 28411)
-- Name: briefing_respuestas briefing_respuestas_id_briefing_enviado_id_pregunta_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_respuestas
    ADD CONSTRAINT briefing_respuestas_id_briefing_enviado_id_pregunta_key UNIQUE (id_briefing_enviado, id_pregunta);


--
-- TOC entry 4355 (class 2606 OID 28413)
-- Name: briefing_respuestas briefing_respuestas_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_respuestas
    ADD CONSTRAINT briefing_respuestas_pkey PRIMARY KEY (id_respuesta);


--
-- TOC entry 4357 (class 2606 OID 28415)
-- Name: categorias categorias_nombre_categoria_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.categorias
    ADD CONSTRAINT categorias_nombre_categoria_key UNIQUE (nombre_categoria);


--
-- TOC entry 4359 (class 2606 OID 28417)
-- Name: categorias categorias_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.categorias
    ADD CONSTRAINT categorias_pkey PRIMARY KEY (id_categoria);


--
-- TOC entry 4362 (class 2606 OID 28419)
-- Name: certificados_ia certificados_ia_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.certificados_ia
    ADD CONSTRAINT certificados_ia_pkey PRIMARY KEY (id_certificado);


--
-- TOC entry 4366 (class 2606 OID 28421)
-- Name: codigos_respaldo_2fa codigos_respaldo_2fa_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.codigos_respaldo_2fa
    ADD CONSTRAINT codigos_respaldo_2fa_pkey PRIMARY KEY (id_codigo);


--
-- TOC entry 4369 (class 2606 OID 28423)
-- Name: comentarios_portafolio comentarios_portafolio_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.comentarios_portafolio
    ADD CONSTRAINT comentarios_portafolio_pkey PRIMARY KEY (id_comentario);


--
-- TOC entry 4371 (class 2606 OID 28425)
-- Name: contratos contratos_id_pedido_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.contratos
    ADD CONSTRAINT contratos_id_pedido_key UNIQUE (id_pedido);


--
-- TOC entry 4373 (class 2606 OID 28427)
-- Name: contratos contratos_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.contratos
    ADD CONSTRAINT contratos_pkey PRIMARY KEY (id_contrato);


--
-- TOC entry 4375 (class 2606 OID 28429)
-- Name: creador_habilidades creador_habilidades_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.creador_habilidades
    ADD CONSTRAINT creador_habilidades_pkey PRIMARY KEY (id_creador_habilidad);


--
-- TOC entry 4377 (class 2606 OID 28431)
-- Name: documentos_adjuntos documentos_adjuntos_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.documentos_adjuntos
    ADD CONSTRAINT documentos_adjuntos_pkey PRIMARY KEY (id_adjunto);


--
-- TOC entry 4379 (class 2606 OID 28433)
-- Name: entregables_finales entregables_finales_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.entregables_finales
    ADD CONSTRAINT entregables_finales_pkey PRIMARY KEY (id_entregable);


--
-- TOC entry 4381 (class 2606 OID 28435)
-- Name: estados_verificacion estados_verificacion_nombre_estado_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.estados_verificacion
    ADD CONSTRAINT estados_verificacion_nombre_estado_key UNIQUE (nombre_estado);


--
-- TOC entry 4383 (class 2606 OID 28437)
-- Name: estados_verificacion estados_verificacion_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.estados_verificacion
    ADD CONSTRAINT estados_verificacion_pkey PRIMARY KEY (id_estado_verificacion);


--
-- TOC entry 4385 (class 2606 OID 28439)
-- Name: etapas_flujo etapas_flujo_nombre_etapa_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.etapas_flujo
    ADD CONSTRAINT etapas_flujo_nombre_etapa_key UNIQUE (nombre_etapa);


--
-- TOC entry 4387 (class 2606 OID 28441)
-- Name: etapas_flujo etapas_flujo_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.etapas_flujo
    ADD CONSTRAINT etapas_flujo_pkey PRIMARY KEY (id_etapa);


--
-- TOC entry 4389 (class 2606 OID 28443)
-- Name: etiquetas etiquetas_nombre_etiqueta_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.etiquetas
    ADD CONSTRAINT etiquetas_nombre_etiqueta_key UNIQUE (nombre_etiqueta);


--
-- TOC entry 4391 (class 2606 OID 28445)
-- Name: etiquetas etiquetas_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.etiquetas
    ADD CONSTRAINT etiquetas_pkey PRIMARY KEY (id_etiqueta);


--
-- TOC entry 4393 (class 2606 OID 28447)
-- Name: flujo_etapas_config flujo_etapas_config_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.flujo_etapas_config
    ADD CONSTRAINT flujo_etapas_config_pkey PRIMARY KEY (id_flujo_etapa);


--
-- TOC entry 4395 (class 2606 OID 28449)
-- Name: flujos_trabajo flujos_trabajo_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.flujos_trabajo
    ADD CONSTRAINT flujos_trabajo_pkey PRIMARY KEY (id_flujo);


--
-- TOC entry 4397 (class 2606 OID 28451)
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- TOC entry 4400 (class 2606 OID 28453)
-- Name: habilidades habilidades_nombre_habilidad_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.habilidades
    ADD CONSTRAINT habilidades_nombre_habilidad_key UNIQUE (nombre_habilidad);


--
-- TOC entry 4402 (class 2606 OID 28455)
-- Name: habilidades habilidades_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.habilidades
    ADD CONSTRAINT habilidades_pkey PRIMARY KEY (id_habilidad);


--
-- TOC entry 4404 (class 2606 OID 28457)
-- Name: historial_estados_pedido historial_estados_pedido_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.historial_estados_pedido
    ADD CONSTRAINT historial_estados_pedido_pkey PRIMARY KEY (id_historial_estado);


--
-- TOC entry 4406 (class 2606 OID 28459)
-- Name: infracciones_mensaje infracciones_mensaje_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.infracciones_mensaje
    ADD CONSTRAINT infracciones_mensaje_pkey PRIMARY KEY (id_infraccion);


--
-- TOC entry 4408 (class 2606 OID 28461)
-- Name: likes_portafolio likes_portafolio_id_item_portafolio_id_usuario_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.likes_portafolio
    ADD CONSTRAINT likes_portafolio_id_item_portafolio_id_usuario_key UNIQUE (id_item_portafolio, id_usuario);


--
-- TOC entry 4410 (class 2606 OID 28463)
-- Name: likes_portafolio likes_portafolio_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.likes_portafolio
    ADD CONSTRAINT likes_portafolio_pkey PRIMARY KEY (id_like);


--
-- TOC entry 4412 (class 2606 OID 28465)
-- Name: mensajes mensajes_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.mensajes
    ADD CONSTRAINT mensajes_pkey PRIMARY KEY (id_mensaje);


--
-- TOC entry 4414 (class 2606 OID 28467)
-- Name: motivos_rechazo motivos_rechazo_descripcion_motivo_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.motivos_rechazo
    ADD CONSTRAINT motivos_rechazo_descripcion_motivo_key UNIQUE (descripcion_motivo);


--
-- TOC entry 4416 (class 2606 OID 28469)
-- Name: motivos_rechazo motivos_rechazo_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.motivos_rechazo
    ADD CONSTRAINT motivos_rechazo_pkey PRIMARY KEY (id_motivo);


--
-- TOC entry 4418 (class 2606 OID 28471)
-- Name: notificaciones_sistema notificaciones_sistema_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.notificaciones_sistema
    ADD CONSTRAINT notificaciones_sistema_pkey PRIMARY KEY (id_notificacion);


--
-- TOC entry 4420 (class 2606 OID 28473)
-- Name: pagos_garantia pagos_garantia_id_contrato_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pagos_garantia
    ADD CONSTRAINT pagos_garantia_id_contrato_key UNIQUE (id_contrato);


--
-- TOC entry 4422 (class 2606 OID 28475)
-- Name: pagos_garantia pagos_garantia_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pagos_garantia
    ADD CONSTRAINT pagos_garantia_pkey PRIMARY KEY (id_pago);


--
-- TOC entry 4424 (class 2606 OID 28477)
-- Name: pais pais_nombre_pais_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pais
    ADD CONSTRAINT pais_nombre_pais_key UNIQUE (nombre_pais);


--
-- TOC entry 4426 (class 2606 OID 28479)
-- Name: pais pais_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pais
    ADD CONSTRAINT pais_pkey PRIMARY KEY (id_pais);


--
-- TOC entry 4428 (class 2606 OID 28481)
-- Name: participantes_sorteo participantes_sorteo_id_sorteo_id_usuario_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.participantes_sorteo
    ADD CONSTRAINT participantes_sorteo_id_sorteo_id_usuario_key UNIQUE (id_sorteo, id_usuario);


--
-- TOC entry 4430 (class 2606 OID 28483)
-- Name: participantes_sorteo participantes_sorteo_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.participantes_sorteo
    ADD CONSTRAINT participantes_sorteo_pkey PRIMARY KEY (id_participacion);


--
-- TOC entry 4432 (class 2606 OID 28485)
-- Name: pedidos pedidos_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pedidos
    ADD CONSTRAINT pedidos_pkey PRIMARY KEY (id_pedido);


--
-- TOC entry 4434 (class 2606 OID 28487)
-- Name: perfiles_creadores perfiles_creadores_id_usuario_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.perfiles_creadores
    ADD CONSTRAINT perfiles_creadores_id_usuario_key UNIQUE (id_usuario);


--
-- TOC entry 4436 (class 2606 OID 28489)
-- Name: perfiles_creadores perfiles_creadores_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.perfiles_creadores
    ADD CONSTRAINT perfiles_creadores_pkey PRIMARY KEY (id_perfil);


--
-- TOC entry 4438 (class 2606 OID 28491)
-- Name: permisos permisos_nombre_permiso_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.permisos
    ADD CONSTRAINT permisos_nombre_permiso_key UNIQUE (nombre_permiso);


--
-- TOC entry 4440 (class 2606 OID 28493)
-- Name: permisos permisos_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.permisos
    ADD CONSTRAINT permisos_pkey PRIMARY KEY (id_permiso);


--
-- TOC entry 4442 (class 2606 OID 28495)
-- Name: plantillas_contrato plantillas_contrato_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.plantillas_contrato
    ADD CONSTRAINT plantillas_contrato_pkey PRIMARY KEY (id_plantilla);


--
-- TOC entry 4444 (class 2606 OID 28497)
-- Name: plantillas_contrato plantillas_contrato_version_legal_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.plantillas_contrato
    ADD CONSTRAINT plantillas_contrato_version_legal_key UNIQUE (version_legal);


--
-- TOC entry 4446 (class 2606 OID 28499)
-- Name: portafolio_items portafolio_items_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.portafolio_items
    ADD CONSTRAINT portafolio_items_pkey PRIMARY KEY (id_item_portafolio);


--
-- TOC entry 4448 (class 2606 OID 28501)
-- Name: portafolios portafolios_id_perfil_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.portafolios
    ADD CONSTRAINT portafolios_id_perfil_key UNIQUE (id_perfil);


--
-- TOC entry 4450 (class 2606 OID 28503)
-- Name: portafolios portafolios_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.portafolios
    ADD CONSTRAINT portafolios_pkey PRIMARY KEY (id_portafolio);


--
-- TOC entry 4452 (class 2606 OID 28505)
-- Name: resenas_servicios resenas_servicios_id_pedido_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.resenas_servicios
    ADD CONSTRAINT resenas_servicios_id_pedido_key UNIQUE (id_pedido);


--
-- TOC entry 4454 (class 2606 OID 28507)
-- Name: resenas_servicios resenas_servicios_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.resenas_servicios
    ADD CONSTRAINT resenas_servicios_pkey PRIMARY KEY (id_resena);


--
-- TOC entry 4456 (class 2606 OID 28509)
-- Name: rol_permisos rol_permisos_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.rol_permisos
    ADD CONSTRAINT rol_permisos_pkey PRIMARY KEY (id_rol_permiso);


--
-- TOC entry 4460 (class 2606 OID 28511)
-- Name: roles roles_nombre_rol_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_nombre_rol_key UNIQUE (nombre_rol);


--
-- TOC entry 4462 (class 2606 OID 28513)
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id_rol);


--
-- TOC entry 4464 (class 2606 OID 28515)
-- Name: salas_chat salas_chat_id_pedido_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.salas_chat
    ADD CONSTRAINT salas_chat_id_pedido_key UNIQUE (id_pedido);


--
-- TOC entry 4466 (class 2606 OID 28517)
-- Name: salas_chat salas_chat_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.salas_chat
    ADD CONSTRAINT salas_chat_pkey PRIMARY KEY (id_sala);


--
-- TOC entry 4468 (class 2606 OID 28519)
-- Name: seguidores seguidores_id_usuario_seguidor_id_perfil_creador_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.seguidores
    ADD CONSTRAINT seguidores_id_usuario_seguidor_id_perfil_creador_key UNIQUE (id_usuario_seguidor, id_perfil_creador);


--
-- TOC entry 4470 (class 2606 OID 28521)
-- Name: seguidores seguidores_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.seguidores
    ADD CONSTRAINT seguidores_pkey PRIMARY KEY (id_seguimiento);


--
-- TOC entry 4472 (class 2606 OID 28523)
-- Name: servicio_atributos servicio_atributos_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicio_atributos
    ADD CONSTRAINT servicio_atributos_pkey PRIMARY KEY (id_servicio_atributo);


--
-- TOC entry 4474 (class 2606 OID 28525)
-- Name: servicio_etiquetas servicio_etiquetas_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicio_etiquetas
    ADD CONSTRAINT servicio_etiquetas_pkey PRIMARY KEY (id_servicio_etiqueta);


--
-- TOC entry 4476 (class 2606 OID 28527)
-- Name: servicios servicios_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicios
    ADD CONSTRAINT servicios_pkey PRIMARY KEY (id_servicio);


--
-- TOC entry 4480 (class 2606 OID 28529)
-- Name: sesiones_usuario sesiones_usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.sesiones_usuario
    ADD CONSTRAINT sesiones_usuario_pkey PRIMARY KEY (id_sesion);


--
-- TOC entry 4484 (class 2606 OID 28531)
-- Name: sorteos sorteos_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.sorteos
    ADD CONSTRAINT sorteos_pkey PRIMARY KEY (id_sorteo);


--
-- TOC entry 4486 (class 2606 OID 28533)
-- Name: subcategorias subcategorias_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.subcategorias
    ADD CONSTRAINT subcategorias_pkey PRIMARY KEY (id_subcategoria);


--
-- TOC entry 4488 (class 2606 OID 28535)
-- Name: tickets_revision tickets_revision_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.tickets_revision
    ADD CONSTRAINT tickets_revision_pkey PRIMARY KEY (id_ticket);


--
-- TOC entry 4490 (class 2606 OID 28537)
-- Name: tipos_notificacion tipos_notificacion_nombre_evento_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.tipos_notificacion
    ADD CONSTRAINT tipos_notificacion_nombre_evento_key UNIQUE (nombre_evento);


--
-- TOC entry 4492 (class 2606 OID 28539)
-- Name: tipos_notificacion tipos_notificacion_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.tipos_notificacion
    ADD CONSTRAINT tipos_notificacion_pkey PRIMARY KEY (id_tipo_notificacion);


--
-- TOC entry 4494 (class 2606 OID 28541)
-- Name: tokens_recuperacion tokens_recuperacion_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.tokens_recuperacion
    ADD CONSTRAINT tokens_recuperacion_pkey PRIMARY KEY (id_token);


--
-- TOC entry 4496 (class 2606 OID 28543)
-- Name: transacciones_pago transacciones_pago_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.transacciones_pago
    ADD CONSTRAINT transacciones_pago_pkey PRIMARY KEY (id_transaccion);


--
-- TOC entry 4458 (class 2606 OID 28545)
-- Name: rol_permisos uk_rol_permiso; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.rol_permisos
    ADD CONSTRAINT uk_rol_permiso UNIQUE (id_rol, id_permiso);


--
-- TOC entry 4482 (class 2606 OID 28931)
-- Name: sesiones_usuario uq_sesiones_usuario_jti; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.sesiones_usuario
    ADD CONSTRAINT uq_sesiones_usuario_jti UNIQUE (jti);


--
-- TOC entry 4498 (class 2606 OID 28547)
-- Name: usuario_roles usuario_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.usuario_roles
    ADD CONSTRAINT usuario_roles_pkey PRIMARY KEY (id_usuario_rol);


--
-- TOC entry 4500 (class 2606 OID 28549)
-- Name: usuarios usuarios_correo_key; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_correo_key UNIQUE (correo);


--
-- TOC entry 4502 (class 2606 OID 28551)
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id_usuario);


--
-- TOC entry 4398 (class 1259 OID 28552)
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: adminuteq
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- TOC entry 4360 (class 1259 OID 28939)
-- Name: idx_categorias_id_flujo; Type: INDEX; Schema: public; Owner: adminuteq
--

CREATE INDEX idx_categorias_id_flujo ON public.categorias USING btree (id_flujo);


--
-- TOC entry 4363 (class 1259 OID 28926)
-- Name: idx_certificados_ia_estado; Type: INDEX; Schema: public; Owner: adminuteq
--

CREATE INDEX idx_certificados_ia_estado ON public.certificados_ia USING btree (id_estado_verificacion);


--
-- TOC entry 4364 (class 1259 OID 28925)
-- Name: idx_certificados_ia_hash; Type: INDEX; Schema: public; Owner: adminuteq
--

CREATE INDEX idx_certificados_ia_hash ON public.certificados_ia USING btree (hash_documento);


--
-- TOC entry 4367 (class 1259 OID 28553)
-- Name: idx_codigos_respaldo_usuario; Type: INDEX; Schema: public; Owner: adminuteq
--

CREATE INDEX idx_codigos_respaldo_usuario ON public.codigos_respaldo_2fa USING btree (id_usuario);


--
-- TOC entry 4477 (class 1259 OID 28933)
-- Name: idx_sesiones_usuario_fecha_expiracion; Type: INDEX; Schema: public; Owner: adminuteq
--

CREATE INDEX idx_sesiones_usuario_fecha_expiracion ON public.sesiones_usuario USING btree (fecha_expiracion);


--
-- TOC entry 4478 (class 1259 OID 28932)
-- Name: idx_sesiones_usuario_id_usuario; Type: INDEX; Schema: public; Owner: adminuteq
--

CREATE INDEX idx_sesiones_usuario_id_usuario ON public.sesiones_usuario USING btree (id_usuario);


--
-- TOC entry 4566 (class 2620 OID 28554)
-- Name: atributos_dinamicos trg_atributos_dinamicos_actualizado_en; Type: TRIGGER; Schema: public; Owner: adminuteq
--

CREATE TRIGGER trg_atributos_dinamicos_actualizado_en BEFORE UPDATE ON public.atributos_dinamicos FOR EACH ROW EXECUTE FUNCTION public.set_actualizado_en();


--
-- TOC entry 4567 (class 2620 OID 28555)
-- Name: categorias trg_categorias_actualizado_en; Type: TRIGGER; Schema: public; Owner: adminuteq
--

CREATE TRIGGER trg_categorias_actualizado_en BEFORE UPDATE ON public.categorias FOR EACH ROW EXECUTE FUNCTION public.set_actualizado_en();


--
-- TOC entry 4568 (class 2620 OID 28556)
-- Name: etiquetas trg_etiquetas_actualizado_en; Type: TRIGGER; Schema: public; Owner: adminuteq
--

CREATE TRIGGER trg_etiquetas_actualizado_en BEFORE UPDATE ON public.etiquetas FOR EACH ROW EXECUTE FUNCTION public.set_actualizado_en();


--
-- TOC entry 4569 (class 2620 OID 28557)
-- Name: portafolios trg_portafolios_actualizado_en; Type: TRIGGER; Schema: public; Owner: adminuteq
--

CREATE TRIGGER trg_portafolios_actualizado_en BEFORE UPDATE ON public.portafolios FOR EACH ROW EXECUTE FUNCTION public.set_actualizado_en();


--
-- TOC entry 4570 (class 2620 OID 28558)
-- Name: servicio_atributos trg_servicio_atributos_actualizado_en; Type: TRIGGER; Schema: public; Owner: adminuteq
--

CREATE TRIGGER trg_servicio_atributos_actualizado_en BEFORE UPDATE ON public.servicio_atributos FOR EACH ROW EXECUTE FUNCTION public.set_actualizado_en();


--
-- TOC entry 4571 (class 2620 OID 28559)
-- Name: servicio_etiquetas trg_servicio_etiquetas_actualizado_en; Type: TRIGGER; Schema: public; Owner: adminuteq
--

CREATE TRIGGER trg_servicio_etiquetas_actualizado_en BEFORE UPDATE ON public.servicio_etiquetas FOR EACH ROW EXECUTE FUNCTION public.set_actualizado_en();


--
-- TOC entry 4572 (class 2620 OID 28560)
-- Name: servicios trg_servicios_actualizado_en; Type: TRIGGER; Schema: public; Owner: adminuteq
--

CREATE TRIGGER trg_servicios_actualizado_en BEFORE UPDATE ON public.servicios FOR EACH ROW EXECUTE FUNCTION public.set_actualizado_en();


--
-- TOC entry 4573 (class 2620 OID 28561)
-- Name: subcategorias trg_subcategorias_actualizado_en; Type: TRIGGER; Schema: public; Owner: adminuteq
--

CREATE TRIGGER trg_subcategorias_actualizado_en BEFORE UPDATE ON public.subcategorias FOR EACH ROW EXECUTE FUNCTION public.set_actualizado_en();


--
-- TOC entry 4574 (class 2620 OID 28562)
-- Name: usuarios trg_usuarios_actualizado_en; Type: TRIGGER; Schema: public; Owner: adminuteq
--

CREATE TRIGGER trg_usuarios_actualizado_en BEFORE UPDATE ON public.usuarios FOR EACH ROW EXECUTE FUNCTION public.set_actualizado_en();


--
-- TOC entry 4503 (class 2606 OID 28563)
-- Name: autenticacion_dos_factores autenticacion_dos_factores_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.autenticacion_dos_factores
    ADD CONSTRAINT autenticacion_dos_factores_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4504 (class 2606 OID 28568)
-- Name: briefing_enviados briefing_enviados_id_briefing_plantilla_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_enviados
    ADD CONSTRAINT briefing_enviados_id_briefing_plantilla_fkey FOREIGN KEY (id_briefing_plantilla) REFERENCES public.briefing_plantillas(id_briefing_plantilla);


--
-- TOC entry 4505 (class 2606 OID 28573)
-- Name: briefing_enviados briefing_enviados_id_pedido_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_enviados
    ADD CONSTRAINT briefing_enviados_id_pedido_fkey FOREIGN KEY (id_pedido) REFERENCES public.pedidos(id_pedido) ON DELETE CASCADE;


--
-- TOC entry 4506 (class 2606 OID 28578)
-- Name: briefing_plantillas briefing_plantillas_id_perfil_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_plantillas
    ADD CONSTRAINT briefing_plantillas_id_perfil_fkey FOREIGN KEY (id_perfil) REFERENCES public.perfiles_creadores(id_perfil) ON DELETE CASCADE;


--
-- TOC entry 4507 (class 2606 OID 28583)
-- Name: briefing_preguntas briefing_preguntas_id_briefing_plantilla_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_preguntas
    ADD CONSTRAINT briefing_preguntas_id_briefing_plantilla_fkey FOREIGN KEY (id_briefing_plantilla) REFERENCES public.briefing_plantillas(id_briefing_plantilla) ON DELETE CASCADE;


--
-- TOC entry 4508 (class 2606 OID 28588)
-- Name: briefing_respuestas briefing_respuestas_id_briefing_enviado_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_respuestas
    ADD CONSTRAINT briefing_respuestas_id_briefing_enviado_fkey FOREIGN KEY (id_briefing_enviado) REFERENCES public.briefing_enviados(id_briefing_enviado) ON DELETE CASCADE;


--
-- TOC entry 4509 (class 2606 OID 28593)
-- Name: briefing_respuestas briefing_respuestas_id_pregunta_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.briefing_respuestas
    ADD CONSTRAINT briefing_respuestas_id_pregunta_fkey FOREIGN KEY (id_pregunta) REFERENCES public.briefing_preguntas(id_pregunta);


--
-- TOC entry 4510 (class 2606 OID 28934)
-- Name: categorias categorias_id_flujo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.categorias
    ADD CONSTRAINT categorias_id_flujo_fkey FOREIGN KEY (id_flujo) REFERENCES public.flujos_trabajo(id_flujo);


--
-- TOC entry 4511 (class 2606 OID 28598)
-- Name: certificados_ia certificados_ia_id_estado_verificacion_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.certificados_ia
    ADD CONSTRAINT certificados_ia_id_estado_verificacion_fkey FOREIGN KEY (id_estado_verificacion) REFERENCES public.estados_verificacion(id_estado_verificacion);


--
-- TOC entry 4512 (class 2606 OID 28917)
-- Name: certificados_ia certificados_ia_id_moderador_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.certificados_ia
    ADD CONSTRAINT certificados_ia_id_moderador_fkey FOREIGN KEY (id_moderador) REFERENCES public.usuarios(id_usuario);


--
-- TOC entry 4513 (class 2606 OID 28603)
-- Name: certificados_ia certificados_ia_id_perfil_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.certificados_ia
    ADD CONSTRAINT certificados_ia_id_perfil_fkey FOREIGN KEY (id_perfil) REFERENCES public.perfiles_creadores(id_perfil) ON DELETE CASCADE;


--
-- TOC entry 4514 (class 2606 OID 28608)
-- Name: codigos_respaldo_2fa codigos_respaldo_2fa_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.codigos_respaldo_2fa
    ADD CONSTRAINT codigos_respaldo_2fa_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4515 (class 2606 OID 28613)
-- Name: comentarios_portafolio comentarios_portafolio_id_item_portafolio_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.comentarios_portafolio
    ADD CONSTRAINT comentarios_portafolio_id_item_portafolio_fkey FOREIGN KEY (id_item_portafolio) REFERENCES public.portafolio_items(id_item_portafolio) ON DELETE CASCADE;


--
-- TOC entry 4516 (class 2606 OID 28618)
-- Name: comentarios_portafolio comentarios_portafolio_id_usuario_autor_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.comentarios_portafolio
    ADD CONSTRAINT comentarios_portafolio_id_usuario_autor_fkey FOREIGN KEY (id_usuario_autor) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4517 (class 2606 OID 28623)
-- Name: contratos contratos_id_pedido_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.contratos
    ADD CONSTRAINT contratos_id_pedido_fkey FOREIGN KEY (id_pedido) REFERENCES public.pedidos(id_pedido) ON DELETE CASCADE;


--
-- TOC entry 4518 (class 2606 OID 28628)
-- Name: contratos contratos_id_plantilla_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.contratos
    ADD CONSTRAINT contratos_id_plantilla_fkey FOREIGN KEY (id_plantilla) REFERENCES public.plantillas_contrato(id_plantilla);


--
-- TOC entry 4519 (class 2606 OID 28633)
-- Name: creador_habilidades creador_habilidades_id_habilidad_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.creador_habilidades
    ADD CONSTRAINT creador_habilidades_id_habilidad_fkey FOREIGN KEY (id_habilidad) REFERENCES public.habilidades(id_habilidad) ON DELETE CASCADE;


--
-- TOC entry 4520 (class 2606 OID 28638)
-- Name: creador_habilidades creador_habilidades_id_perfil_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.creador_habilidades
    ADD CONSTRAINT creador_habilidades_id_perfil_fkey FOREIGN KEY (id_perfil) REFERENCES public.perfiles_creadores(id_perfil) ON DELETE CASCADE;


--
-- TOC entry 4521 (class 2606 OID 28643)
-- Name: documentos_adjuntos documentos_adjuntos_id_mensaje_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.documentos_adjuntos
    ADD CONSTRAINT documentos_adjuntos_id_mensaje_fkey FOREIGN KEY (id_mensaje) REFERENCES public.mensajes(id_mensaje) ON DELETE CASCADE;


--
-- TOC entry 4522 (class 2606 OID 28648)
-- Name: entregables_finales entregables_finales_id_pedido_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.entregables_finales
    ADD CONSTRAINT entregables_finales_id_pedido_fkey FOREIGN KEY (id_pedido) REFERENCES public.pedidos(id_pedido) ON DELETE CASCADE;


--
-- TOC entry 4523 (class 2606 OID 28653)
-- Name: flujo_etapas_config flujo_etapas_config_id_etapa_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.flujo_etapas_config
    ADD CONSTRAINT flujo_etapas_config_id_etapa_fkey FOREIGN KEY (id_etapa) REFERENCES public.etapas_flujo(id_etapa) ON DELETE CASCADE;


--
-- TOC entry 4524 (class 2606 OID 28658)
-- Name: flujo_etapas_config flujo_etapas_config_id_flujo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.flujo_etapas_config
    ADD CONSTRAINT flujo_etapas_config_id_flujo_fkey FOREIGN KEY (id_flujo) REFERENCES public.flujos_trabajo(id_flujo) ON DELETE CASCADE;


--
-- TOC entry 4525 (class 2606 OID 28663)
-- Name: historial_estados_pedido historial_estados_pedido_id_etapa_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.historial_estados_pedido
    ADD CONSTRAINT historial_estados_pedido_id_etapa_fkey FOREIGN KEY (id_etapa) REFERENCES public.etapas_flujo(id_etapa);


--
-- TOC entry 4526 (class 2606 OID 28668)
-- Name: historial_estados_pedido historial_estados_pedido_id_pedido_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.historial_estados_pedido
    ADD CONSTRAINT historial_estados_pedido_id_pedido_fkey FOREIGN KEY (id_pedido) REFERENCES public.pedidos(id_pedido) ON DELETE CASCADE;


--
-- TOC entry 4527 (class 2606 OID 28673)
-- Name: infracciones_mensaje infracciones_mensaje_id_pedido_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.infracciones_mensaje
    ADD CONSTRAINT infracciones_mensaje_id_pedido_fkey FOREIGN KEY (id_pedido) REFERENCES public.pedidos(id_pedido) ON DELETE CASCADE;


--
-- TOC entry 4528 (class 2606 OID 28678)
-- Name: infracciones_mensaje infracciones_mensaje_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.infracciones_mensaje
    ADD CONSTRAINT infracciones_mensaje_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4529 (class 2606 OID 28683)
-- Name: likes_portafolio likes_portafolio_id_item_portafolio_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.likes_portafolio
    ADD CONSTRAINT likes_portafolio_id_item_portafolio_fkey FOREIGN KEY (id_item_portafolio) REFERENCES public.portafolio_items(id_item_portafolio) ON DELETE CASCADE;


--
-- TOC entry 4530 (class 2606 OID 28688)
-- Name: likes_portafolio likes_portafolio_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.likes_portafolio
    ADD CONSTRAINT likes_portafolio_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4531 (class 2606 OID 28693)
-- Name: mensajes mensajes_id_remitente_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.mensajes
    ADD CONSTRAINT mensajes_id_remitente_fkey FOREIGN KEY (id_remitente) REFERENCES public.usuarios(id_usuario);


--
-- TOC entry 4532 (class 2606 OID 28698)
-- Name: mensajes mensajes_id_sala_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.mensajes
    ADD CONSTRAINT mensajes_id_sala_fkey FOREIGN KEY (id_sala) REFERENCES public.salas_chat(id_sala) ON DELETE CASCADE;


--
-- TOC entry 4533 (class 2606 OID 28703)
-- Name: notificaciones_sistema notificaciones_sistema_id_tipo_notificacion_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.notificaciones_sistema
    ADD CONSTRAINT notificaciones_sistema_id_tipo_notificacion_fkey FOREIGN KEY (id_tipo_notificacion) REFERENCES public.tipos_notificacion(id_tipo_notificacion);


--
-- TOC entry 4534 (class 2606 OID 28708)
-- Name: notificaciones_sistema notificaciones_sistema_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.notificaciones_sistema
    ADD CONSTRAINT notificaciones_sistema_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4535 (class 2606 OID 28713)
-- Name: pagos_garantia pagos_garantia_id_contrato_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pagos_garantia
    ADD CONSTRAINT pagos_garantia_id_contrato_fkey FOREIGN KEY (id_contrato) REFERENCES public.contratos(id_contrato) ON DELETE CASCADE;


--
-- TOC entry 4536 (class 2606 OID 28718)
-- Name: participantes_sorteo participantes_sorteo_id_sorteo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.participantes_sorteo
    ADD CONSTRAINT participantes_sorteo_id_sorteo_fkey FOREIGN KEY (id_sorteo) REFERENCES public.sorteos(id_sorteo) ON DELETE CASCADE;


--
-- TOC entry 4537 (class 2606 OID 28723)
-- Name: participantes_sorteo participantes_sorteo_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.participantes_sorteo
    ADD CONSTRAINT participantes_sorteo_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4538 (class 2606 OID 28728)
-- Name: pedidos pedidos_id_flujo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pedidos
    ADD CONSTRAINT pedidos_id_flujo_fkey FOREIGN KEY (id_flujo) REFERENCES public.flujos_trabajo(id_flujo);


--
-- TOC entry 4539 (class 2606 OID 28733)
-- Name: pedidos pedidos_id_servicio_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pedidos
    ADD CONSTRAINT pedidos_id_servicio_fkey FOREIGN KEY (id_servicio) REFERENCES public.servicios(id_servicio);


--
-- TOC entry 4540 (class 2606 OID 28738)
-- Name: pedidos pedidos_id_usuario_cliente_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.pedidos
    ADD CONSTRAINT pedidos_id_usuario_cliente_fkey FOREIGN KEY (id_usuario_cliente) REFERENCES public.usuarios(id_usuario);


--
-- TOC entry 4541 (class 2606 OID 28743)
-- Name: perfiles_creadores perfiles_creadores_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.perfiles_creadores
    ADD CONSTRAINT perfiles_creadores_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4542 (class 2606 OID 28748)
-- Name: portafolio_items portafolio_items_id_portafolio_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.portafolio_items
    ADD CONSTRAINT portafolio_items_id_portafolio_fkey FOREIGN KEY (id_portafolio) REFERENCES public.portafolios(id_portafolio) ON DELETE CASCADE;


--
-- TOC entry 4543 (class 2606 OID 28753)
-- Name: portafolios portafolios_id_perfil_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.portafolios
    ADD CONSTRAINT portafolios_id_perfil_fkey FOREIGN KEY (id_perfil) REFERENCES public.perfiles_creadores(id_perfil) ON DELETE CASCADE;


--
-- TOC entry 4544 (class 2606 OID 28758)
-- Name: resenas_servicios resenas_servicios_id_pedido_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.resenas_servicios
    ADD CONSTRAINT resenas_servicios_id_pedido_fkey FOREIGN KEY (id_pedido) REFERENCES public.pedidos(id_pedido) ON DELETE CASCADE;


--
-- TOC entry 4545 (class 2606 OID 28763)
-- Name: rol_permisos rol_permisos_id_permiso_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.rol_permisos
    ADD CONSTRAINT rol_permisos_id_permiso_fkey FOREIGN KEY (id_permiso) REFERENCES public.permisos(id_permiso) ON DELETE CASCADE;


--
-- TOC entry 4546 (class 2606 OID 28768)
-- Name: rol_permisos rol_permisos_id_rol_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.rol_permisos
    ADD CONSTRAINT rol_permisos_id_rol_fkey FOREIGN KEY (id_rol) REFERENCES public.roles(id_rol) ON DELETE CASCADE;


--
-- TOC entry 4547 (class 2606 OID 28773)
-- Name: salas_chat salas_chat_id_pedido_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.salas_chat
    ADD CONSTRAINT salas_chat_id_pedido_fkey FOREIGN KEY (id_pedido) REFERENCES public.pedidos(id_pedido) ON DELETE CASCADE;


--
-- TOC entry 4548 (class 2606 OID 28778)
-- Name: seguidores seguidores_id_perfil_creador_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.seguidores
    ADD CONSTRAINT seguidores_id_perfil_creador_fkey FOREIGN KEY (id_perfil_creador) REFERENCES public.perfiles_creadores(id_perfil) ON DELETE CASCADE;


--
-- TOC entry 4549 (class 2606 OID 28783)
-- Name: seguidores seguidores_id_usuario_seguidor_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.seguidores
    ADD CONSTRAINT seguidores_id_usuario_seguidor_fkey FOREIGN KEY (id_usuario_seguidor) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4550 (class 2606 OID 28788)
-- Name: servicio_atributos servicio_atributos_id_atributo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicio_atributos
    ADD CONSTRAINT servicio_atributos_id_atributo_fkey FOREIGN KEY (id_atributo) REFERENCES public.atributos_dinamicos(id_atributo) ON DELETE CASCADE;


--
-- TOC entry 4551 (class 2606 OID 28793)
-- Name: servicio_atributos servicio_atributos_id_servicio_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicio_atributos
    ADD CONSTRAINT servicio_atributos_id_servicio_fkey FOREIGN KEY (id_servicio) REFERENCES public.servicios(id_servicio) ON DELETE CASCADE;


--
-- TOC entry 4552 (class 2606 OID 28798)
-- Name: servicio_etiquetas servicio_etiquetas_id_etiqueta_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicio_etiquetas
    ADD CONSTRAINT servicio_etiquetas_id_etiqueta_fkey FOREIGN KEY (id_etiqueta) REFERENCES public.etiquetas(id_etiqueta) ON DELETE CASCADE;


--
-- TOC entry 4553 (class 2606 OID 28803)
-- Name: servicio_etiquetas servicio_etiquetas_id_servicio_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicio_etiquetas
    ADD CONSTRAINT servicio_etiquetas_id_servicio_fkey FOREIGN KEY (id_servicio) REFERENCES public.servicios(id_servicio) ON DELETE CASCADE;


--
-- TOC entry 4554 (class 2606 OID 28808)
-- Name: servicios servicios_id_perfil_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicios
    ADD CONSTRAINT servicios_id_perfil_fkey FOREIGN KEY (id_perfil) REFERENCES public.perfiles_creadores(id_perfil) ON DELETE CASCADE;


--
-- TOC entry 4555 (class 2606 OID 28813)
-- Name: servicios servicios_id_subcategoria_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.servicios
    ADD CONSTRAINT servicios_id_subcategoria_fkey FOREIGN KEY (id_subcategoria) REFERENCES public.subcategorias(id_subcategoria);


--
-- TOC entry 4556 (class 2606 OID 28818)
-- Name: sesiones_usuario sesiones_usuario_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.sesiones_usuario
    ADD CONSTRAINT sesiones_usuario_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4557 (class 2606 OID 28823)
-- Name: sorteos sorteos_id_perfil_creador_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.sorteos
    ADD CONSTRAINT sorteos_id_perfil_creador_fkey FOREIGN KEY (id_perfil_creador) REFERENCES public.perfiles_creadores(id_perfil) ON DELETE CASCADE;


--
-- TOC entry 4558 (class 2606 OID 28828)
-- Name: subcategorias subcategorias_id_categoria_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.subcategorias
    ADD CONSTRAINT subcategorias_id_categoria_fkey FOREIGN KEY (id_categoria) REFERENCES public.categorias(id_categoria) ON DELETE CASCADE;


--
-- TOC entry 4559 (class 2606 OID 28833)
-- Name: tickets_revision tickets_revision_id_motivo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.tickets_revision
    ADD CONSTRAINT tickets_revision_id_motivo_fkey FOREIGN KEY (id_motivo) REFERENCES public.motivos_rechazo(id_motivo);


--
-- TOC entry 4560 (class 2606 OID 28838)
-- Name: tickets_revision tickets_revision_id_pedido_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.tickets_revision
    ADD CONSTRAINT tickets_revision_id_pedido_fkey FOREIGN KEY (id_pedido) REFERENCES public.pedidos(id_pedido) ON DELETE CASCADE;


--
-- TOC entry 4561 (class 2606 OID 28843)
-- Name: tokens_recuperacion tokens_recuperacion_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.tokens_recuperacion
    ADD CONSTRAINT tokens_recuperacion_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4562 (class 2606 OID 28848)
-- Name: transacciones_pago transacciones_pago_id_pago_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.transacciones_pago
    ADD CONSTRAINT transacciones_pago_id_pago_fkey FOREIGN KEY (id_pago) REFERENCES public.pagos_garantia(id_pago) ON DELETE CASCADE;


--
-- TOC entry 4563 (class 2606 OID 28853)
-- Name: usuario_roles usuario_roles_id_rol_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.usuario_roles
    ADD CONSTRAINT usuario_roles_id_rol_fkey FOREIGN KEY (id_rol) REFERENCES public.roles(id_rol) ON DELETE CASCADE;


--
-- TOC entry 4564 (class 2606 OID 28858)
-- Name: usuario_roles usuario_roles_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.usuario_roles
    ADD CONSTRAINT usuario_roles_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 4565 (class 2606 OID 28863)
-- Name: usuarios usuarios_id_pais_fkey; Type: FK CONSTRAINT; Schema: public; Owner: adminuteq
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_id_pais_fkey FOREIGN KEY (id_pais) REFERENCES public.pais(id_pais);


--
-- TOC entry 4832 (class 0 OID 0)
-- Dependencies: 5
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: azure_pg_admin
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO rol_app_backend;
GRANT USAGE ON SCHEMA public TO rol_moderador;
GRANT USAGE ON SCHEMA public TO rol_soporte;
GRANT USAGE ON SCHEMA public TO rol_auditor_fin;
GRANT USAGE ON SCHEMA public TO rol_solo_lectura;
GRANT CREATE ON SCHEMA public TO rol_administrador;


--
-- TOC entry 4833 (class 0 OID 0)
-- Dependencies: 336
-- Name: FUNCTION pg_replication_origin_advance(text, pg_lsn); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_advance(text, pg_lsn) TO azure_pg_admin;


--
-- TOC entry 4834 (class 0 OID 0)
-- Dependencies: 328
-- Name: FUNCTION pg_replication_origin_create(text); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_create(text) TO azure_pg_admin;


--
-- TOC entry 4835 (class 0 OID 0)
-- Dependencies: 329
-- Name: FUNCTION pg_replication_origin_drop(text); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_drop(text) TO azure_pg_admin;


--
-- TOC entry 4836 (class 0 OID 0)
-- Dependencies: 337
-- Name: FUNCTION pg_replication_origin_oid(text); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_oid(text) TO azure_pg_admin;


--
-- TOC entry 4837 (class 0 OID 0)
-- Dependencies: 330
-- Name: FUNCTION pg_replication_origin_progress(text, boolean); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_progress(text, boolean) TO azure_pg_admin;


--
-- TOC entry 4838 (class 0 OID 0)
-- Dependencies: 331
-- Name: FUNCTION pg_replication_origin_session_is_setup(); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_session_is_setup() TO azure_pg_admin;


--
-- TOC entry 4839 (class 0 OID 0)
-- Dependencies: 332
-- Name: FUNCTION pg_replication_origin_session_progress(boolean); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_session_progress(boolean) TO azure_pg_admin;


--
-- TOC entry 4840 (class 0 OID 0)
-- Dependencies: 333
-- Name: FUNCTION pg_replication_origin_session_reset(); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_session_reset() TO azure_pg_admin;


--
-- TOC entry 4841 (class 0 OID 0)
-- Dependencies: 334
-- Name: FUNCTION pg_replication_origin_session_setup(text); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_session_setup(text) TO azure_pg_admin;


--
-- TOC entry 4842 (class 0 OID 0)
-- Dependencies: 338
-- Name: FUNCTION pg_replication_origin_xact_reset(); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_xact_reset() TO azure_pg_admin;


--
-- TOC entry 4843 (class 0 OID 0)
-- Dependencies: 335
-- Name: FUNCTION pg_replication_origin_xact_setup(pg_lsn, timestamp with time zone); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_replication_origin_xact_setup(pg_lsn, timestamp with time zone) TO azure_pg_admin;


--
-- TOC entry 4844 (class 0 OID 0)
-- Dependencies: 339
-- Name: FUNCTION pg_show_replication_origin_status(OUT local_id oid, OUT external_id text, OUT remote_lsn pg_lsn, OUT local_lsn pg_lsn); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_show_replication_origin_status(OUT local_id oid, OUT external_id text, OUT remote_lsn pg_lsn, OUT local_lsn pg_lsn) TO azure_pg_admin;


--
-- TOC entry 4845 (class 0 OID 0)
-- Dependencies: 324
-- Name: FUNCTION pg_stat_reset(); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_stat_reset() TO azure_pg_admin;


--
-- TOC entry 4846 (class 0 OID 0)
-- Dependencies: 340
-- Name: FUNCTION pg_stat_reset_shared(target text); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_stat_reset_shared(target text) TO azure_pg_admin;


--
-- TOC entry 4847 (class 0 OID 0)
-- Dependencies: 327
-- Name: FUNCTION pg_stat_reset_single_function_counters(oid); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_stat_reset_single_function_counters(oid) TO azure_pg_admin;


--
-- TOC entry 4848 (class 0 OID 0)
-- Dependencies: 326
-- Name: FUNCTION pg_stat_reset_single_table_counters(oid); Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT ALL ON FUNCTION pg_catalog.pg_stat_reset_single_table_counters(oid) TO azure_pg_admin;


--
-- TOC entry 4849 (class 0 OID 0)
-- Dependencies: 352
-- Name: FUNCTION fn_listar_cola_verificacion(p_estado character varying, p_limite integer, p_offset integer); Type: ACL; Schema: public; Owner: adminuteq
--

GRANT ALL ON FUNCTION public.fn_listar_cola_verificacion(p_estado character varying, p_limite integer, p_offset integer) TO rol_app_backend;
GRANT ALL ON FUNCTION public.fn_listar_cola_verificacion(p_estado character varying, p_limite integer, p_offset integer) TO rol_administrador;
GRANT ALL ON FUNCTION public.fn_listar_cola_verificacion(p_estado character varying, p_limite integer, p_offset integer) TO artisync_app;


--
-- TOC entry 4850 (class 0 OID 0)
-- Dependencies: 325
-- Name: FUNCTION set_actualizado_en(); Type: ACL; Schema: public; Owner: adminuteq
--

GRANT ALL ON FUNCTION public.set_actualizado_en() TO rol_app_backend;
GRANT ALL ON FUNCTION public.set_actualizado_en() TO rol_administrador;


--
-- TOC entry 4851 (class 0 OID 0)
-- Dependencies: 353
-- Name: PROCEDURE sp_registrar_decision_verificacion(IN p_id_certificado bigint, IN p_id_estado bigint, IN p_id_moderador bigint, IN p_nota text); Type: ACL; Schema: public; Owner: adminuteq
--

GRANT ALL ON PROCEDURE public.sp_registrar_decision_verificacion(IN p_id_certificado bigint, IN p_id_estado bigint, IN p_id_moderador bigint, IN p_nota text) TO rol_app_backend;
GRANT ALL ON PROCEDURE public.sp_registrar_decision_verificacion(IN p_id_certificado bigint, IN p_id_estado bigint, IN p_id_moderador bigint, IN p_nota text) TO rol_administrador;
GRANT ALL ON PROCEDURE public.sp_registrar_decision_verificacion(IN p_id_certificado bigint, IN p_id_estado bigint, IN p_id_moderador bigint, IN p_nota text) TO artisync_app;


--
-- TOC entry 4852 (class 0 OID 0)
-- Dependencies: 98
-- Name: COLUMN pg_config.name; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(name) ON TABLE pg_catalog.pg_config TO azure_pg_admin;


--
-- TOC entry 4853 (class 0 OID 0)
-- Dependencies: 98
-- Name: COLUMN pg_config.setting; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(setting) ON TABLE pg_catalog.pg_config TO azure_pg_admin;


--
-- TOC entry 4854 (class 0 OID 0)
-- Dependencies: 94
-- Name: COLUMN pg_hba_file_rules.line_number; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(line_number) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- TOC entry 4855 (class 0 OID 0)
-- Dependencies: 94
-- Name: COLUMN pg_hba_file_rules.type; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(type) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- TOC entry 4856 (class 0 OID 0)
-- Dependencies: 94
-- Name: COLUMN pg_hba_file_rules.database; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(database) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- TOC entry 4857 (class 0 OID 0)
-- Dependencies: 94
-- Name: COLUMN pg_hba_file_rules.user_name; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(user_name) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- TOC entry 4858 (class 0 OID 0)
-- Dependencies: 94
-- Name: COLUMN pg_hba_file_rules.address; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(address) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- TOC entry 4859 (class 0 OID 0)
-- Dependencies: 94
-- Name: COLUMN pg_hba_file_rules.netmask; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(netmask) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- TOC entry 4860 (class 0 OID 0)
-- Dependencies: 94
-- Name: COLUMN pg_hba_file_rules.auth_method; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(auth_method) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- TOC entry 4861 (class 0 OID 0)
-- Dependencies: 94
-- Name: COLUMN pg_hba_file_rules.options; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(options) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- TOC entry 4862 (class 0 OID 0)
-- Dependencies: 94
-- Name: COLUMN pg_hba_file_rules.error; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(error) ON TABLE pg_catalog.pg_hba_file_rules TO azure_pg_admin;


--
-- TOC entry 4863 (class 0 OID 0)
-- Dependencies: 146
-- Name: COLUMN pg_replication_origin_status.local_id; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(local_id) ON TABLE pg_catalog.pg_replication_origin_status TO azure_pg_admin;


--
-- TOC entry 4864 (class 0 OID 0)
-- Dependencies: 146
-- Name: COLUMN pg_replication_origin_status.external_id; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(external_id) ON TABLE pg_catalog.pg_replication_origin_status TO azure_pg_admin;


--
-- TOC entry 4865 (class 0 OID 0)
-- Dependencies: 146
-- Name: COLUMN pg_replication_origin_status.remote_lsn; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(remote_lsn) ON TABLE pg_catalog.pg_replication_origin_status TO azure_pg_admin;


--
-- TOC entry 4866 (class 0 OID 0)
-- Dependencies: 146
-- Name: COLUMN pg_replication_origin_status.local_lsn; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(local_lsn) ON TABLE pg_catalog.pg_replication_origin_status TO azure_pg_admin;


--
-- TOC entry 4867 (class 0 OID 0)
-- Dependencies: 99
-- Name: COLUMN pg_shmem_allocations.name; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(name) ON TABLE pg_catalog.pg_shmem_allocations TO azure_pg_admin;


--
-- TOC entry 4868 (class 0 OID 0)
-- Dependencies: 99
-- Name: COLUMN pg_shmem_allocations.off; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(off) ON TABLE pg_catalog.pg_shmem_allocations TO azure_pg_admin;


--
-- TOC entry 4869 (class 0 OID 0)
-- Dependencies: 99
-- Name: COLUMN pg_shmem_allocations.size; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(size) ON TABLE pg_catalog.pg_shmem_allocations TO azure_pg_admin;


--
-- TOC entry 4870 (class 0 OID 0)
-- Dependencies: 99
-- Name: COLUMN pg_shmem_allocations.allocated_size; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(allocated_size) ON TABLE pg_catalog.pg_shmem_allocations TO azure_pg_admin;


--
-- TOC entry 4871 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.starelid; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(starelid) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4872 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.staattnum; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staattnum) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4873 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stainherit; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stainherit) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4874 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stanullfrac; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanullfrac) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4875 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stawidth; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stawidth) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4876 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stadistinct; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stadistinct) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4877 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stakind1; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stakind1) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4878 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stakind2; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stakind2) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4879 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stakind3; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stakind3) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4880 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stakind4; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stakind4) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4881 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stakind5; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stakind5) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4882 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.staop1; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staop1) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4883 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.staop2; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staop2) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4884 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.staop3; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staop3) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4885 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.staop4; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staop4) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4886 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.staop5; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(staop5) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4887 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stacoll1; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stacoll1) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4888 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stacoll2; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stacoll2) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4889 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stacoll3; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stacoll3) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4890 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stacoll4; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stacoll4) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4891 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stacoll5; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stacoll5) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4892 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stanumbers1; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanumbers1) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4893 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stanumbers2; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanumbers2) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4894 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stanumbers3; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanumbers3) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4895 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stanumbers4; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanumbers4) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4896 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stanumbers5; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stanumbers5) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4897 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stavalues1; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stavalues1) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4898 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stavalues2; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stavalues2) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4899 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stavalues3; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stavalues3) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4900 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stavalues4; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stavalues4) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4901 (class 0 OID 0)
-- Dependencies: 39
-- Name: COLUMN pg_statistic.stavalues5; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(stavalues5) ON TABLE pg_catalog.pg_statistic TO azure_pg_admin;


--
-- TOC entry 4902 (class 0 OID 0)
-- Dependencies: 64
-- Name: COLUMN pg_subscription.oid; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(oid) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- TOC entry 4903 (class 0 OID 0)
-- Dependencies: 64
-- Name: COLUMN pg_subscription.subdbid; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subdbid) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- TOC entry 4904 (class 0 OID 0)
-- Dependencies: 64
-- Name: COLUMN pg_subscription.subname; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subname) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- TOC entry 4905 (class 0 OID 0)
-- Dependencies: 64
-- Name: COLUMN pg_subscription.subowner; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subowner) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- TOC entry 4906 (class 0 OID 0)
-- Dependencies: 64
-- Name: COLUMN pg_subscription.subenabled; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subenabled) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- TOC entry 4907 (class 0 OID 0)
-- Dependencies: 64
-- Name: COLUMN pg_subscription.subconninfo; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subconninfo) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- TOC entry 4908 (class 0 OID 0)
-- Dependencies: 64
-- Name: COLUMN pg_subscription.subslotname; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subslotname) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- TOC entry 4909 (class 0 OID 0)
-- Dependencies: 64
-- Name: COLUMN pg_subscription.subsynccommit; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subsynccommit) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- TOC entry 4910 (class 0 OID 0)
-- Dependencies: 64
-- Name: COLUMN pg_subscription.subpublications; Type: ACL; Schema: pg_catalog; Owner: azuresu
--

GRANT SELECT(subpublications) ON TABLE pg_catalog.pg_subscription TO azure_pg_admin;


--
-- TOC entry 4911 (class 0 OID 0)
-- Dependencies: 219
-- Name: TABLE atributos_dinamicos; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.atributos_dinamicos TO rol_app_backend;
GRANT ALL ON TABLE public.atributos_dinamicos TO rol_administrador;
GRANT SELECT ON TABLE public.atributos_dinamicos TO rol_solo_lectura;


--
-- TOC entry 4913 (class 0 OID 0)
-- Dependencies: 220
-- Name: SEQUENCE atributos_dinamicos_id_atributo_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.atributos_dinamicos_id_atributo_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.atributos_dinamicos_id_atributo_seq TO rol_administrador;


--
-- TOC entry 4914 (class 0 OID 0)
-- Dependencies: 221
-- Name: TABLE autenticacion_dos_factores; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.autenticacion_dos_factores TO rol_app_backend;
GRANT ALL ON TABLE public.autenticacion_dos_factores TO rol_administrador;
GRANT SELECT ON TABLE public.autenticacion_dos_factores TO rol_solo_lectura;


--
-- TOC entry 4916 (class 0 OID 0)
-- Dependencies: 222
-- Name: SEQUENCE autenticacion_dos_factores_id_2fa_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.autenticacion_dos_factores_id_2fa_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.autenticacion_dos_factores_id_2fa_seq TO rol_administrador;


--
-- TOC entry 4917 (class 0 OID 0)
-- Dependencies: 223
-- Name: TABLE briefing_enviados; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.briefing_enviados TO rol_app_backend;
GRANT ALL ON TABLE public.briefing_enviados TO rol_administrador;
GRANT SELECT ON TABLE public.briefing_enviados TO rol_solo_lectura;


--
-- TOC entry 4919 (class 0 OID 0)
-- Dependencies: 224
-- Name: SEQUENCE briefing_enviados_id_briefing_enviado_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.briefing_enviados_id_briefing_enviado_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.briefing_enviados_id_briefing_enviado_seq TO rol_administrador;


--
-- TOC entry 4920 (class 0 OID 0)
-- Dependencies: 225
-- Name: TABLE briefing_plantillas; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.briefing_plantillas TO rol_app_backend;
GRANT ALL ON TABLE public.briefing_plantillas TO rol_administrador;
GRANT SELECT ON TABLE public.briefing_plantillas TO rol_solo_lectura;


--
-- TOC entry 4922 (class 0 OID 0)
-- Dependencies: 226
-- Name: SEQUENCE briefing_plantillas_id_briefing_plantilla_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.briefing_plantillas_id_briefing_plantilla_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.briefing_plantillas_id_briefing_plantilla_seq TO rol_administrador;


--
-- TOC entry 4923 (class 0 OID 0)
-- Dependencies: 227
-- Name: TABLE briefing_preguntas; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.briefing_preguntas TO rol_app_backend;
GRANT ALL ON TABLE public.briefing_preguntas TO rol_administrador;
GRANT SELECT ON TABLE public.briefing_preguntas TO rol_solo_lectura;


--
-- TOC entry 4925 (class 0 OID 0)
-- Dependencies: 228
-- Name: SEQUENCE briefing_preguntas_id_pregunta_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.briefing_preguntas_id_pregunta_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.briefing_preguntas_id_pregunta_seq TO rol_administrador;


--
-- TOC entry 4926 (class 0 OID 0)
-- Dependencies: 229
-- Name: TABLE briefing_respuestas; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.briefing_respuestas TO rol_app_backend;
GRANT ALL ON TABLE public.briefing_respuestas TO rol_administrador;
GRANT SELECT ON TABLE public.briefing_respuestas TO rol_solo_lectura;


--
-- TOC entry 4928 (class 0 OID 0)
-- Dependencies: 230
-- Name: SEQUENCE briefing_respuestas_id_respuesta_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.briefing_respuestas_id_respuesta_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.briefing_respuestas_id_respuesta_seq TO rol_administrador;


--
-- TOC entry 4929 (class 0 OID 0)
-- Dependencies: 231
-- Name: TABLE categorias; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.categorias TO rol_app_backend;
GRANT ALL ON TABLE public.categorias TO rol_administrador;
GRANT SELECT ON TABLE public.categorias TO rol_soporte;
GRANT SELECT ON TABLE public.categorias TO rol_solo_lectura;


--
-- TOC entry 4931 (class 0 OID 0)
-- Dependencies: 232
-- Name: SEQUENCE categorias_id_categoria_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.categorias_id_categoria_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.categorias_id_categoria_seq TO rol_administrador;


--
-- TOC entry 4932 (class 0 OID 0)
-- Dependencies: 233
-- Name: TABLE certificados_ia; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.certificados_ia TO rol_app_backend;
GRANT ALL ON TABLE public.certificados_ia TO rol_administrador;
GRANT SELECT,UPDATE ON TABLE public.certificados_ia TO rol_moderador;
GRANT SELECT ON TABLE public.certificados_ia TO rol_solo_lectura;


--
-- TOC entry 4934 (class 0 OID 0)
-- Dependencies: 234
-- Name: SEQUENCE certificados_ia_id_certificado_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.certificados_ia_id_certificado_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.certificados_ia_id_certificado_seq TO rol_administrador;
GRANT USAGE ON SEQUENCE public.certificados_ia_id_certificado_seq TO rol_moderador;


--
-- TOC entry 4935 (class 0 OID 0)
-- Dependencies: 235
-- Name: TABLE codigos_respaldo_2fa; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.codigos_respaldo_2fa TO rol_app_backend;
GRANT ALL ON TABLE public.codigos_respaldo_2fa TO rol_administrador;
GRANT SELECT ON TABLE public.codigos_respaldo_2fa TO rol_solo_lectura;


--
-- TOC entry 4937 (class 0 OID 0)
-- Dependencies: 236
-- Name: SEQUENCE codigos_respaldo_2fa_id_codigo_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.codigos_respaldo_2fa_id_codigo_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.codigos_respaldo_2fa_id_codigo_seq TO rol_administrador;


--
-- TOC entry 4938 (class 0 OID 0)
-- Dependencies: 237
-- Name: TABLE comentarios_portafolio; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.comentarios_portafolio TO rol_app_backend;
GRANT ALL ON TABLE public.comentarios_portafolio TO rol_administrador;
GRANT SELECT,DELETE,UPDATE ON TABLE public.comentarios_portafolio TO rol_moderador;
GRANT SELECT ON TABLE public.comentarios_portafolio TO rol_solo_lectura;


--
-- TOC entry 4940 (class 0 OID 0)
-- Dependencies: 238
-- Name: SEQUENCE comentarios_portafolio_id_comentario_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.comentarios_portafolio_id_comentario_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.comentarios_portafolio_id_comentario_seq TO rol_administrador;


--
-- TOC entry 4941 (class 0 OID 0)
-- Dependencies: 239
-- Name: TABLE contratos; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.contratos TO rol_app_backend;
GRANT ALL ON TABLE public.contratos TO rol_administrador;
GRANT SELECT ON TABLE public.contratos TO rol_soporte;
GRANT SELECT ON TABLE public.contratos TO rol_auditor_fin;
GRANT SELECT ON TABLE public.contratos TO rol_solo_lectura;


--
-- TOC entry 4943 (class 0 OID 0)
-- Dependencies: 240
-- Name: SEQUENCE contratos_id_contrato_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.contratos_id_contrato_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.contratos_id_contrato_seq TO rol_administrador;


--
-- TOC entry 4944 (class 0 OID 0)
-- Dependencies: 241
-- Name: TABLE creador_habilidades; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.creador_habilidades TO rol_app_backend;
GRANT ALL ON TABLE public.creador_habilidades TO rol_administrador;
GRANT SELECT ON TABLE public.creador_habilidades TO rol_moderador;
GRANT SELECT ON TABLE public.creador_habilidades TO rol_solo_lectura;


--
-- TOC entry 4946 (class 0 OID 0)
-- Dependencies: 242
-- Name: SEQUENCE creador_habilidades_id_creador_habilidad_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.creador_habilidades_id_creador_habilidad_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.creador_habilidades_id_creador_habilidad_seq TO rol_administrador;


--
-- TOC entry 4947 (class 0 OID 0)
-- Dependencies: 243
-- Name: TABLE documentos_adjuntos; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.documentos_adjuntos TO rol_app_backend;
GRANT ALL ON TABLE public.documentos_adjuntos TO rol_administrador;
GRANT SELECT ON TABLE public.documentos_adjuntos TO rol_soporte;
GRANT SELECT ON TABLE public.documentos_adjuntos TO rol_solo_lectura;


--
-- TOC entry 4949 (class 0 OID 0)
-- Dependencies: 244
-- Name: SEQUENCE documentos_adjuntos_id_adjunto_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.documentos_adjuntos_id_adjunto_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.documentos_adjuntos_id_adjunto_seq TO rol_administrador;


--
-- TOC entry 4950 (class 0 OID 0)
-- Dependencies: 245
-- Name: TABLE entregables_finales; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.entregables_finales TO rol_app_backend;
GRANT ALL ON TABLE public.entregables_finales TO rol_administrador;
GRANT SELECT ON TABLE public.entregables_finales TO rol_auditor_fin;
GRANT SELECT ON TABLE public.entregables_finales TO rol_solo_lectura;


--
-- TOC entry 4952 (class 0 OID 0)
-- Dependencies: 246
-- Name: SEQUENCE entregables_finales_id_entregable_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.entregables_finales_id_entregable_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.entregables_finales_id_entregable_seq TO rol_administrador;


--
-- TOC entry 4953 (class 0 OID 0)
-- Dependencies: 247
-- Name: TABLE estados_verificacion; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.estados_verificacion TO rol_app_backend;
GRANT ALL ON TABLE public.estados_verificacion TO rol_administrador;
GRANT SELECT ON TABLE public.estados_verificacion TO rol_moderador;
GRANT SELECT ON TABLE public.estados_verificacion TO rol_solo_lectura;


--
-- TOC entry 4955 (class 0 OID 0)
-- Dependencies: 248
-- Name: SEQUENCE estados_verificacion_id_estado_verificacion_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.estados_verificacion_id_estado_verificacion_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.estados_verificacion_id_estado_verificacion_seq TO rol_administrador;


--
-- TOC entry 4956 (class 0 OID 0)
-- Dependencies: 249
-- Name: TABLE etapas_flujo; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.etapas_flujo TO rol_app_backend;
GRANT ALL ON TABLE public.etapas_flujo TO rol_administrador;
GRANT SELECT ON TABLE public.etapas_flujo TO rol_solo_lectura;


--
-- TOC entry 4958 (class 0 OID 0)
-- Dependencies: 250
-- Name: SEQUENCE etapas_flujo_id_etapa_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.etapas_flujo_id_etapa_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.etapas_flujo_id_etapa_seq TO rol_administrador;


--
-- TOC entry 4959 (class 0 OID 0)
-- Dependencies: 251
-- Name: TABLE etiquetas; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.etiquetas TO rol_app_backend;
GRANT ALL ON TABLE public.etiquetas TO rol_administrador;
GRANT SELECT ON TABLE public.etiquetas TO rol_solo_lectura;


--
-- TOC entry 4961 (class 0 OID 0)
-- Dependencies: 252
-- Name: SEQUENCE etiquetas_id_etiqueta_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.etiquetas_id_etiqueta_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.etiquetas_id_etiqueta_seq TO rol_administrador;


--
-- TOC entry 4962 (class 0 OID 0)
-- Dependencies: 253
-- Name: TABLE flujo_etapas_config; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.flujo_etapas_config TO rol_app_backend;
GRANT ALL ON TABLE public.flujo_etapas_config TO rol_administrador;
GRANT SELECT ON TABLE public.flujo_etapas_config TO rol_solo_lectura;


--
-- TOC entry 4964 (class 0 OID 0)
-- Dependencies: 254
-- Name: SEQUENCE flujo_etapas_config_id_flujo_etapa_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.flujo_etapas_config_id_flujo_etapa_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.flujo_etapas_config_id_flujo_etapa_seq TO rol_administrador;


--
-- TOC entry 4965 (class 0 OID 0)
-- Dependencies: 255
-- Name: TABLE flujos_trabajo; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.flujos_trabajo TO rol_app_backend;
GRANT ALL ON TABLE public.flujos_trabajo TO rol_administrador;
GRANT SELECT ON TABLE public.flujos_trabajo TO rol_solo_lectura;


--
-- TOC entry 4967 (class 0 OID 0)
-- Dependencies: 256
-- Name: SEQUENCE flujos_trabajo_id_flujo_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.flujos_trabajo_id_flujo_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.flujos_trabajo_id_flujo_seq TO rol_administrador;


--
-- TOC entry 4968 (class 0 OID 0)
-- Dependencies: 257
-- Name: TABLE flyway_schema_history; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.flyway_schema_history TO rol_app_backend;
GRANT ALL ON TABLE public.flyway_schema_history TO rol_administrador;
GRANT SELECT ON TABLE public.flyway_schema_history TO rol_solo_lectura;


--
-- TOC entry 4969 (class 0 OID 0)
-- Dependencies: 258
-- Name: TABLE habilidades; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.habilidades TO rol_app_backend;
GRANT ALL ON TABLE public.habilidades TO rol_administrador;
GRANT SELECT ON TABLE public.habilidades TO rol_moderador;
GRANT SELECT ON TABLE public.habilidades TO rol_solo_lectura;


--
-- TOC entry 4971 (class 0 OID 0)
-- Dependencies: 259
-- Name: SEQUENCE habilidades_id_habilidad_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.habilidades_id_habilidad_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.habilidades_id_habilidad_seq TO rol_administrador;


--
-- TOC entry 4972 (class 0 OID 0)
-- Dependencies: 260
-- Name: TABLE historial_estados_pedido; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.historial_estados_pedido TO rol_app_backend;
GRANT ALL ON TABLE public.historial_estados_pedido TO rol_administrador;
GRANT SELECT ON TABLE public.historial_estados_pedido TO rol_soporte;
GRANT SELECT ON TABLE public.historial_estados_pedido TO rol_solo_lectura;


--
-- TOC entry 4974 (class 0 OID 0)
-- Dependencies: 261
-- Name: SEQUENCE historial_estados_pedido_id_historial_estado_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.historial_estados_pedido_id_historial_estado_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.historial_estados_pedido_id_historial_estado_seq TO rol_administrador;


--
-- TOC entry 4975 (class 0 OID 0)
-- Dependencies: 262
-- Name: TABLE infracciones_mensaje; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.infracciones_mensaje TO rol_app_backend;
GRANT ALL ON TABLE public.infracciones_mensaje TO rol_administrador;
GRANT SELECT ON TABLE public.infracciones_mensaje TO rol_solo_lectura;


--
-- TOC entry 4977 (class 0 OID 0)
-- Dependencies: 263
-- Name: SEQUENCE infracciones_mensaje_id_infraccion_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.infracciones_mensaje_id_infraccion_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.infracciones_mensaje_id_infraccion_seq TO rol_administrador;


--
-- TOC entry 4978 (class 0 OID 0)
-- Dependencies: 264
-- Name: TABLE likes_portafolio; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.likes_portafolio TO rol_app_backend;
GRANT ALL ON TABLE public.likes_portafolio TO rol_administrador;
GRANT SELECT ON TABLE public.likes_portafolio TO rol_moderador;
GRANT SELECT ON TABLE public.likes_portafolio TO rol_solo_lectura;


--
-- TOC entry 4980 (class 0 OID 0)
-- Dependencies: 265
-- Name: SEQUENCE likes_portafolio_id_like_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.likes_portafolio_id_like_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.likes_portafolio_id_like_seq TO rol_administrador;


--
-- TOC entry 4981 (class 0 OID 0)
-- Dependencies: 266
-- Name: TABLE mensajes; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.mensajes TO rol_app_backend;
GRANT ALL ON TABLE public.mensajes TO rol_administrador;
GRANT SELECT ON TABLE public.mensajes TO rol_soporte;
GRANT SELECT ON TABLE public.mensajes TO rol_solo_lectura;


--
-- TOC entry 4983 (class 0 OID 0)
-- Dependencies: 267
-- Name: SEQUENCE mensajes_id_mensaje_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.mensajes_id_mensaje_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.mensajes_id_mensaje_seq TO rol_administrador;


--
-- TOC entry 4984 (class 0 OID 0)
-- Dependencies: 268
-- Name: TABLE motivos_rechazo; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.motivos_rechazo TO rol_app_backend;
GRANT ALL ON TABLE public.motivos_rechazo TO rol_administrador;
GRANT SELECT ON TABLE public.motivos_rechazo TO rol_soporte;
GRANT SELECT ON TABLE public.motivos_rechazo TO rol_solo_lectura;


--
-- TOC entry 4986 (class 0 OID 0)
-- Dependencies: 269
-- Name: SEQUENCE motivos_rechazo_id_motivo_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.motivos_rechazo_id_motivo_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.motivos_rechazo_id_motivo_seq TO rol_administrador;


--
-- TOC entry 4987 (class 0 OID 0)
-- Dependencies: 270
-- Name: TABLE notificaciones_sistema; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.notificaciones_sistema TO rol_app_backend;
GRANT ALL ON TABLE public.notificaciones_sistema TO rol_administrador;
GRANT SELECT ON TABLE public.notificaciones_sistema TO rol_soporte;
GRANT SELECT ON TABLE public.notificaciones_sistema TO rol_solo_lectura;


--
-- TOC entry 4989 (class 0 OID 0)
-- Dependencies: 271
-- Name: SEQUENCE notificaciones_sistema_id_notificacion_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.notificaciones_sistema_id_notificacion_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.notificaciones_sistema_id_notificacion_seq TO rol_administrador;


--
-- TOC entry 4990 (class 0 OID 0)
-- Dependencies: 272
-- Name: TABLE pagos_garantia; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.pagos_garantia TO rol_app_backend;
GRANT ALL ON TABLE public.pagos_garantia TO rol_administrador;
GRANT SELECT ON TABLE public.pagos_garantia TO rol_auditor_fin;
GRANT SELECT ON TABLE public.pagos_garantia TO rol_solo_lectura;


--
-- TOC entry 4992 (class 0 OID 0)
-- Dependencies: 273
-- Name: SEQUENCE pagos_garantia_id_pago_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.pagos_garantia_id_pago_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.pagos_garantia_id_pago_seq TO rol_administrador;


--
-- TOC entry 4993 (class 0 OID 0)
-- Dependencies: 274
-- Name: TABLE pais; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.pais TO rol_app_backend;
GRANT ALL ON TABLE public.pais TO rol_administrador;
GRANT SELECT ON TABLE public.pais TO rol_solo_lectura;


--
-- TOC entry 4995 (class 0 OID 0)
-- Dependencies: 275
-- Name: SEQUENCE pais_id_pais_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.pais_id_pais_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.pais_id_pais_seq TO rol_administrador;


--
-- TOC entry 4996 (class 0 OID 0)
-- Dependencies: 276
-- Name: TABLE participantes_sorteo; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.participantes_sorteo TO rol_app_backend;
GRANT ALL ON TABLE public.participantes_sorteo TO rol_administrador;
GRANT SELECT ON TABLE public.participantes_sorteo TO rol_moderador;
GRANT SELECT ON TABLE public.participantes_sorteo TO rol_solo_lectura;


--
-- TOC entry 4998 (class 0 OID 0)
-- Dependencies: 277
-- Name: SEQUENCE participantes_sorteo_id_participacion_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.participantes_sorteo_id_participacion_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.participantes_sorteo_id_participacion_seq TO rol_administrador;


--
-- TOC entry 4999 (class 0 OID 0)
-- Dependencies: 278
-- Name: TABLE pedidos; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.pedidos TO rol_app_backend;
GRANT ALL ON TABLE public.pedidos TO rol_administrador;
GRANT SELECT ON TABLE public.pedidos TO rol_soporte;
GRANT SELECT ON TABLE public.pedidos TO rol_auditor_fin;
GRANT SELECT ON TABLE public.pedidos TO rol_solo_lectura;


--
-- TOC entry 5001 (class 0 OID 0)
-- Dependencies: 279
-- Name: SEQUENCE pedidos_id_pedido_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.pedidos_id_pedido_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.pedidos_id_pedido_seq TO rol_administrador;


--
-- TOC entry 5002 (class 0 OID 0)
-- Dependencies: 280
-- Name: TABLE perfiles_creadores; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.perfiles_creadores TO rol_app_backend;
GRANT ALL ON TABLE public.perfiles_creadores TO rol_administrador;
GRANT SELECT ON TABLE public.perfiles_creadores TO rol_moderador;
GRANT SELECT ON TABLE public.perfiles_creadores TO rol_soporte;
GRANT SELECT ON TABLE public.perfiles_creadores TO rol_solo_lectura;


--
-- TOC entry 5004 (class 0 OID 0)
-- Dependencies: 281
-- Name: SEQUENCE perfiles_creadores_id_perfil_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.perfiles_creadores_id_perfil_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.perfiles_creadores_id_perfil_seq TO rol_administrador;


--
-- TOC entry 5005 (class 0 OID 0)
-- Dependencies: 282
-- Name: TABLE permisos; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.permisos TO rol_app_backend;
GRANT ALL ON TABLE public.permisos TO rol_administrador;
GRANT SELECT ON TABLE public.permisos TO rol_solo_lectura;


--
-- TOC entry 5007 (class 0 OID 0)
-- Dependencies: 283
-- Name: SEQUENCE permisos_id_permiso_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.permisos_id_permiso_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.permisos_id_permiso_seq TO rol_administrador;


--
-- TOC entry 5008 (class 0 OID 0)
-- Dependencies: 284
-- Name: TABLE plantillas_contrato; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.plantillas_contrato TO rol_app_backend;
GRANT ALL ON TABLE public.plantillas_contrato TO rol_administrador;
GRANT SELECT ON TABLE public.plantillas_contrato TO rol_auditor_fin;
GRANT SELECT ON TABLE public.plantillas_contrato TO rol_solo_lectura;


--
-- TOC entry 5010 (class 0 OID 0)
-- Dependencies: 285
-- Name: SEQUENCE plantillas_contrato_id_plantilla_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.plantillas_contrato_id_plantilla_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.plantillas_contrato_id_plantilla_seq TO rol_administrador;


--
-- TOC entry 5011 (class 0 OID 0)
-- Dependencies: 286
-- Name: TABLE portafolio_items; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.portafolio_items TO rol_app_backend;
GRANT ALL ON TABLE public.portafolio_items TO rol_administrador;
GRANT SELECT ON TABLE public.portafolio_items TO rol_moderador;
GRANT SELECT ON TABLE public.portafolio_items TO rol_solo_lectura;


--
-- TOC entry 5013 (class 0 OID 0)
-- Dependencies: 287
-- Name: SEQUENCE portafolio_items_id_item_portafolio_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.portafolio_items_id_item_portafolio_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.portafolio_items_id_item_portafolio_seq TO rol_administrador;


--
-- TOC entry 5014 (class 0 OID 0)
-- Dependencies: 288
-- Name: TABLE portafolios; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.portafolios TO rol_app_backend;
GRANT ALL ON TABLE public.portafolios TO rol_administrador;
GRANT SELECT ON TABLE public.portafolios TO rol_moderador;
GRANT SELECT ON TABLE public.portafolios TO rol_solo_lectura;


--
-- TOC entry 5016 (class 0 OID 0)
-- Dependencies: 289
-- Name: SEQUENCE portafolios_id_portafolio_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.portafolios_id_portafolio_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.portafolios_id_portafolio_seq TO rol_administrador;


--
-- TOC entry 5017 (class 0 OID 0)
-- Dependencies: 290
-- Name: TABLE resenas_servicios; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.resenas_servicios TO rol_app_backend;
GRANT ALL ON TABLE public.resenas_servicios TO rol_administrador;
GRANT SELECT ON TABLE public.resenas_servicios TO rol_solo_lectura;


--
-- TOC entry 5019 (class 0 OID 0)
-- Dependencies: 291
-- Name: SEQUENCE resenas_servicios_id_resena_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.resenas_servicios_id_resena_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.resenas_servicios_id_resena_seq TO rol_administrador;


--
-- TOC entry 5020 (class 0 OID 0)
-- Dependencies: 292
-- Name: TABLE rol_permisos; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.rol_permisos TO rol_app_backend;
GRANT ALL ON TABLE public.rol_permisos TO rol_administrador;
GRANT SELECT ON TABLE public.rol_permisos TO rol_solo_lectura;


--
-- TOC entry 5022 (class 0 OID 0)
-- Dependencies: 293
-- Name: SEQUENCE rol_permisos_id_rol_permiso_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.rol_permisos_id_rol_permiso_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.rol_permisos_id_rol_permiso_seq TO rol_administrador;


--
-- TOC entry 5023 (class 0 OID 0)
-- Dependencies: 294
-- Name: TABLE roles; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.roles TO rol_app_backend;
GRANT ALL ON TABLE public.roles TO rol_administrador;
GRANT SELECT ON TABLE public.roles TO rol_soporte;
GRANT SELECT ON TABLE public.roles TO rol_solo_lectura;


--
-- TOC entry 5025 (class 0 OID 0)
-- Dependencies: 295
-- Name: SEQUENCE roles_id_rol_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.roles_id_rol_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.roles_id_rol_seq TO rol_administrador;


--
-- TOC entry 5026 (class 0 OID 0)
-- Dependencies: 296
-- Name: TABLE salas_chat; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.salas_chat TO rol_app_backend;
GRANT ALL ON TABLE public.salas_chat TO rol_administrador;
GRANT SELECT ON TABLE public.salas_chat TO rol_soporte;
GRANT SELECT ON TABLE public.salas_chat TO rol_solo_lectura;


--
-- TOC entry 5028 (class 0 OID 0)
-- Dependencies: 297
-- Name: SEQUENCE salas_chat_id_sala_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.salas_chat_id_sala_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.salas_chat_id_sala_seq TO rol_administrador;


--
-- TOC entry 5029 (class 0 OID 0)
-- Dependencies: 298
-- Name: TABLE seguidores; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.seguidores TO rol_app_backend;
GRANT ALL ON TABLE public.seguidores TO rol_administrador;
GRANT SELECT ON TABLE public.seguidores TO rol_solo_lectura;


--
-- TOC entry 5031 (class 0 OID 0)
-- Dependencies: 299
-- Name: SEQUENCE seguidores_id_seguimiento_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.seguidores_id_seguimiento_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.seguidores_id_seguimiento_seq TO rol_administrador;


--
-- TOC entry 5032 (class 0 OID 0)
-- Dependencies: 300
-- Name: TABLE servicio_atributos; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.servicio_atributos TO rol_app_backend;
GRANT ALL ON TABLE public.servicio_atributos TO rol_administrador;
GRANT SELECT ON TABLE public.servicio_atributos TO rol_solo_lectura;


--
-- TOC entry 5034 (class 0 OID 0)
-- Dependencies: 301
-- Name: SEQUENCE servicio_atributos_id_servicio_atributo_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.servicio_atributos_id_servicio_atributo_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.servicio_atributos_id_servicio_atributo_seq TO rol_administrador;


--
-- TOC entry 5035 (class 0 OID 0)
-- Dependencies: 302
-- Name: TABLE servicio_etiquetas; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.servicio_etiquetas TO rol_app_backend;
GRANT ALL ON TABLE public.servicio_etiquetas TO rol_administrador;
GRANT SELECT ON TABLE public.servicio_etiquetas TO rol_solo_lectura;


--
-- TOC entry 5037 (class 0 OID 0)
-- Dependencies: 303
-- Name: SEQUENCE servicio_etiquetas_id_servicio_etiqueta_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.servicio_etiquetas_id_servicio_etiqueta_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.servicio_etiquetas_id_servicio_etiqueta_seq TO rol_administrador;


--
-- TOC entry 5038 (class 0 OID 0)
-- Dependencies: 304
-- Name: TABLE servicios; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.servicios TO rol_app_backend;
GRANT ALL ON TABLE public.servicios TO rol_administrador;
GRANT SELECT ON TABLE public.servicios TO rol_moderador;
GRANT SELECT ON TABLE public.servicios TO rol_soporte;
GRANT SELECT ON TABLE public.servicios TO rol_auditor_fin;
GRANT SELECT ON TABLE public.servicios TO rol_solo_lectura;


--
-- TOC entry 5040 (class 0 OID 0)
-- Dependencies: 305
-- Name: SEQUENCE servicios_id_servicio_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.servicios_id_servicio_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.servicios_id_servicio_seq TO rol_administrador;


--
-- TOC entry 5041 (class 0 OID 0)
-- Dependencies: 306
-- Name: TABLE sesiones_usuario; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.sesiones_usuario TO rol_app_backend;
GRANT ALL ON TABLE public.sesiones_usuario TO rol_administrador;
GRANT SELECT ON TABLE public.sesiones_usuario TO rol_soporte;
GRANT SELECT ON TABLE public.sesiones_usuario TO rol_solo_lectura;


--
-- TOC entry 5043 (class 0 OID 0)
-- Dependencies: 307
-- Name: SEQUENCE sesiones_usuario_id_sesion_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.sesiones_usuario_id_sesion_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.sesiones_usuario_id_sesion_seq TO rol_administrador;


--
-- TOC entry 5044 (class 0 OID 0)
-- Dependencies: 308
-- Name: TABLE sorteos; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.sorteos TO rol_app_backend;
GRANT ALL ON TABLE public.sorteos TO rol_administrador;
GRANT SELECT,DELETE,UPDATE ON TABLE public.sorteos TO rol_moderador;
GRANT SELECT ON TABLE public.sorteos TO rol_solo_lectura;


--
-- TOC entry 5046 (class 0 OID 0)
-- Dependencies: 309
-- Name: SEQUENCE sorteos_id_sorteo_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.sorteos_id_sorteo_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.sorteos_id_sorteo_seq TO rol_administrador;


--
-- TOC entry 5047 (class 0 OID 0)
-- Dependencies: 310
-- Name: TABLE subcategorias; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.subcategorias TO rol_app_backend;
GRANT ALL ON TABLE public.subcategorias TO rol_administrador;
GRANT SELECT ON TABLE public.subcategorias TO rol_soporte;
GRANT SELECT ON TABLE public.subcategorias TO rol_solo_lectura;


--
-- TOC entry 5049 (class 0 OID 0)
-- Dependencies: 311
-- Name: SEQUENCE subcategorias_id_subcategoria_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.subcategorias_id_subcategoria_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.subcategorias_id_subcategoria_seq TO rol_administrador;


--
-- TOC entry 5050 (class 0 OID 0)
-- Dependencies: 312
-- Name: TABLE tickets_revision; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.tickets_revision TO rol_app_backend;
GRANT ALL ON TABLE public.tickets_revision TO rol_administrador;
GRANT SELECT ON TABLE public.tickets_revision TO rol_soporte;
GRANT SELECT ON TABLE public.tickets_revision TO rol_solo_lectura;


--
-- TOC entry 5052 (class 0 OID 0)
-- Dependencies: 313
-- Name: SEQUENCE tickets_revision_id_ticket_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.tickets_revision_id_ticket_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.tickets_revision_id_ticket_seq TO rol_administrador;


--
-- TOC entry 5053 (class 0 OID 0)
-- Dependencies: 314
-- Name: TABLE tipos_notificacion; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.tipos_notificacion TO rol_app_backend;
GRANT ALL ON TABLE public.tipos_notificacion TO rol_administrador;
GRANT SELECT ON TABLE public.tipos_notificacion TO rol_soporte;
GRANT SELECT ON TABLE public.tipos_notificacion TO rol_solo_lectura;


--
-- TOC entry 5055 (class 0 OID 0)
-- Dependencies: 315
-- Name: SEQUENCE tipos_notificacion_id_tipo_notificacion_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.tipos_notificacion_id_tipo_notificacion_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.tipos_notificacion_id_tipo_notificacion_seq TO rol_administrador;


--
-- TOC entry 5056 (class 0 OID 0)
-- Dependencies: 316
-- Name: TABLE tokens_recuperacion; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.tokens_recuperacion TO rol_app_backend;
GRANT ALL ON TABLE public.tokens_recuperacion TO rol_administrador;
GRANT SELECT ON TABLE public.tokens_recuperacion TO rol_soporte;
GRANT SELECT ON TABLE public.tokens_recuperacion TO rol_solo_lectura;


--
-- TOC entry 5058 (class 0 OID 0)
-- Dependencies: 317
-- Name: SEQUENCE tokens_recuperacion_id_token_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.tokens_recuperacion_id_token_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.tokens_recuperacion_id_token_seq TO rol_administrador;


--
-- TOC entry 5059 (class 0 OID 0)
-- Dependencies: 318
-- Name: TABLE transacciones_pago; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.transacciones_pago TO rol_app_backend;
GRANT ALL ON TABLE public.transacciones_pago TO rol_administrador;
GRANT SELECT ON TABLE public.transacciones_pago TO rol_auditor_fin;
GRANT SELECT ON TABLE public.transacciones_pago TO rol_solo_lectura;


--
-- TOC entry 5061 (class 0 OID 0)
-- Dependencies: 319
-- Name: SEQUENCE transacciones_pago_id_transaccion_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.transacciones_pago_id_transaccion_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.transacciones_pago_id_transaccion_seq TO rol_administrador;


--
-- TOC entry 5062 (class 0 OID 0)
-- Dependencies: 320
-- Name: TABLE usuario_roles; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.usuario_roles TO rol_app_backend;
GRANT ALL ON TABLE public.usuario_roles TO rol_administrador;
GRANT SELECT ON TABLE public.usuario_roles TO rol_soporte;
GRANT SELECT ON TABLE public.usuario_roles TO rol_solo_lectura;


--
-- TOC entry 5064 (class 0 OID 0)
-- Dependencies: 321
-- Name: SEQUENCE usuario_roles_id_usuario_rol_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.usuario_roles_id_usuario_rol_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.usuario_roles_id_usuario_rol_seq TO rol_administrador;


--
-- TOC entry 5065 (class 0 OID 0)
-- Dependencies: 322
-- Name: TABLE usuarios; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.usuarios TO rol_app_backend;
GRANT ALL ON TABLE public.usuarios TO rol_administrador;
GRANT SELECT ON TABLE public.usuarios TO rol_moderador;
GRANT SELECT ON TABLE public.usuarios TO rol_soporte;
GRANT SELECT ON TABLE public.usuarios TO rol_auditor_fin;
GRANT SELECT ON TABLE public.usuarios TO rol_solo_lectura;


--
-- TOC entry 5067 (class 0 OID 0)
-- Dependencies: 323
-- Name: SEQUENCE usuarios_id_usuario_seq; Type: ACL; Schema: public; Owner: adminuteq
--

GRANT SELECT,USAGE ON SEQUENCE public.usuarios_id_usuario_seq TO rol_app_backend;
GRANT ALL ON SEQUENCE public.usuarios_id_usuario_seq TO rol_administrador;


--
-- TOC entry 2330 (class 826 OID 28909)
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: adminuteq
--

ALTER DEFAULT PRIVILEGES FOR ROLE adminuteq IN SCHEMA public GRANT SELECT,USAGE ON SEQUENCES TO rol_app_backend;
ALTER DEFAULT PRIVILEGES FOR ROLE adminuteq IN SCHEMA public GRANT ALL ON SEQUENCES TO rol_administrador;


--
-- TOC entry 2331 (class 826 OID 28910)
-- Name: DEFAULT PRIVILEGES FOR FUNCTIONS; Type: DEFAULT ACL; Schema: public; Owner: adminuteq
--

ALTER DEFAULT PRIVILEGES FOR ROLE adminuteq IN SCHEMA public GRANT ALL ON FUNCTIONS TO rol_app_backend;
ALTER DEFAULT PRIVILEGES FOR ROLE adminuteq IN SCHEMA public GRANT ALL ON FUNCTIONS TO rol_administrador;


--
-- TOC entry 2332 (class 826 OID 28908)
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: adminuteq
--

ALTER DEFAULT PRIVILEGES FOR ROLE adminuteq IN SCHEMA public GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES TO rol_app_backend;
ALTER DEFAULT PRIVILEGES FOR ROLE adminuteq IN SCHEMA public GRANT ALL ON TABLES TO rol_administrador;
ALTER DEFAULT PRIVILEGES FOR ROLE adminuteq IN SCHEMA public GRANT SELECT ON TABLES TO rol_solo_lectura;


-- Completed on 2026-08-18 23:30:38

--
-- PostgreSQL database dump complete
--

\unrestrict opQuSbFsbY5COQ33Dq4lhdMGnEsUJivMOE1NNvzSaIgoLhq75hgnUYvnDBhRYVI

