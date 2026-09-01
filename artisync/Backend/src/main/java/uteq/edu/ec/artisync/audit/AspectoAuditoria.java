package uteq.edu.ec.artisync.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.service.auditoria.IAuditoriaServicio;
import uteq.edu.ec.artisync.service.shared.ActorAutenticado;
import uteq.edu.ec.artisync.service.shared.ContextoSolicitud;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Captura transversal de eventos de auditoría (REQ-NF-013). Anotar un método
 * de servicio con {@link Auditable} basta para que aquí se registre actor,
 * IP, resultado y detalle del cambio, sin ensuciar la lógica de negocio.
 *
 * El @Order es la pieza crítica de este aspecto: el interceptor de
 * @Transactional de Spring corre con Ordered.LOWEST_PRECEDENCE, así que con
 * HIGHEST_PRECEDENCE + 20 este aspecto queda MÁS EXTERNO. Cuando el método de
 * negocio lanza, su transacción ya hizo rollback y se cerró ANTES de que este
 * aspecto intente registrar el evento; el REQUIRES_NEW de
 * AuditoriaServicioImpl.registrar() abre entonces una transacción limpia. Si
 * el aspecto estuviera por dentro de @Transactional, escribir en una
 * transacción ya marcada rollback-only lanzaría UnexpectedRollbackException
 * al confirmar la externa. No cambiar este @Order sin repetir el test de
 * "evento FALLIDO persiste tras rollback" en AspectoAuditoriaTest.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AspectoAuditoria {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer DESCUBRIDOR_PARAMETROS = new DefaultParameterNameDiscoverer();

    private final IAuditoriaServicio auditoriaServicio;

    @Around("@annotation(auditable)")
    public Object auditar(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        long inicioNanos = System.nanoTime();

        // Snapshot ANTES de proceder, mientras el hilo aún tiene el contexto
        // HTTP y de seguridad del llamante original.
        ActorAutenticado.Actor actorSesion = ActorAutenticado.actual();
        ContextoSolicitud.Datos solicitud = ContextoSolicitud.actual();

        Object resultado = null;
        Throwable error = null;
        // Inicializado a un valor conservador: javac no puede probar por
        // análisis estático que este try/catch/finally SIEMPRE asigna
        // resultadoEvento antes del finally (es una limitación conocida del
        // compilador con este patrón), aunque en tiempo de ejecución los tres
        // caminos (return, ambos catch) lo asignan siempre.
        ResultadoAuditoria resultadoEvento = ResultadoAuditoria.FALLIDO;

        try {
            resultado = pjp.proceed();
            resultadoEvento = ResultadoAuditoria.EXITO;
            return resultado;
        } catch (AccessDeniedException e) {
            error = e;
            resultadoEvento = ResultadoAuditoria.DENEGADO;
            throw e;
        } catch (Throwable t) {
            error = t;
            resultadoEvento = ResultadoAuditoria.FALLIDO;
            throw t;
        } finally {
            try {
                registrarEvento(pjp, auditable, actorSesion, solicitud, resultado, error, resultadoEvento, inicioNanos);
            } finally {
                // Siempre se limpia, incluso si registrarEvento explota: una fuga
                // de este ThreadLocal contaminaría la siguiente petición atendida
                // por el mismo hilo del pool de Tomcat.
                ContextoAuditoria.limpiar();
            }
        }
    }

    private void registrarEvento(
            ProceedingJoinPoint pjp, Auditable auditable,
            ActorAutenticado.Actor actorSesion, ContextoSolicitud.Datos solicitud,
            Object resultado, Throwable error, ResultadoAuditoria resultadoEvento,
            long inicioNanos) {
        try {
            StandardEvaluationContext contextoSpel = construirContextoSpel(pjp, resultado, error);

            String correoActor = resolverCorreoActor(auditable, actorSesion, contextoSpel);
            Long idActor = actorSesion.correo().equals(correoActor) ? actorSesion.id() : null;

            Long idEntidad = evaluarLong(auditable.idEntidad(), contextoSpel);
            Map<String, Object> detalle = evaluarDetalle(auditable.detalle(), contextoSpel);
            Map<String, Object> detalleConAntes = fusionarConContextoAuditoria(detalle);

            String mensajeError = error != null
                    ? truncar(error.getClass().getSimpleName() + ": " + error.getMessage(), 480)
                    : null;

            int duracionMs = (int) ((System.nanoTime() - inicioNanos) / 1_000_000);

            DatosEventoAuditoria datos = new DatosEventoAuditoria(
                    LocalDateTime.now(),
                    idActor,
                    correoActor,
                    auditable.modulo(),
                    auditable.accion(),
                    resultadoEvento,
                    blankToNull(auditable.entidad()),
                    idEntidad,
                    SanitizadorAuditoria.sanitizar(detalleConAntes),
                    mensajeError,
                    solicitud.direccionIp(),
                    truncar(solicitud.agenteUsuario(), 255),
                    solicitud.metodoHttp(),
                    truncar(solicitud.rutaSolicitud(), 255),
                    duracionMs
            );

            auditoriaServicio.registrar(datos);
        } catch (Exception e) {
            // Auditar nunca puede tumbar la operación de negocio: el resultado o
            // la excepción del método ya se resolvió por encima de este bloque.
            log.error("AUDITORIA_PERDIDA accion={} metodo={} motivo={}",
                    auditable.accion(), pjp.getSignature().toShortString(), e.toString(), e);
        }
    }

    private StandardEvaluationContext construirContextoSpel(ProceedingJoinPoint pjp, Object resultado, Throwable error) {
        StandardEvaluationContext contexto = new StandardEvaluationContext();

        MethodSignature firma = (MethodSignature) pjp.getSignature();
        Method metodo = firma.getMethod();
        String[] nombresParametros = DESCUBRIDOR_PARAMETROS.getParameterNames(metodo);
        Object[] argumentos = pjp.getArgs();
        if (nombresParametros != null) {
            for (int i = 0; i < nombresParametros.length && i < argumentos.length; i++) {
                contexto.setVariable(nombresParametros[i], argumentos[i]);
            }
        }

        contexto.setVariable("resultado", resultado);
        contexto.setVariable("error", error);
        return contexto;
    }

    private String resolverCorreoActor(Auditable auditable, ActorAutenticado.Actor actorSesion, StandardEvaluationContext contextoSpel) {
        if (!auditable.correoActor().isBlank()) {
            try {
                Object valor = PARSER.parseExpression(auditable.correoActor()).getValue(contextoSpel);
                if (valor != null) {
                    return String.valueOf(valor);
                }
            } catch (ParseException | EvaluationException e) {
                log.warn("No se pudo evaluar correoActor '{}' de @Auditable en {}: {}",
                        auditable.correoActor(), auditable.accion(), e.getMessage());
            }
        }
        return actorSesion.correo();
    }

    private Long evaluarLong(String expresionSpel, StandardEvaluationContext contexto) {
        if (expresionSpel.isBlank()) {
            return null;
        }
        try {
            Object valor = PARSER.parseExpression(expresionSpel).getValue(contexto);
            if (valor == null) {
                return null;
            }
            if (valor instanceof Number numero) {
                return numero.longValue();
            }
            return Long.valueOf(String.valueOf(valor));
        } catch (ParseException | EvaluationException | NumberFormatException e) {
            log.warn("No se pudo evaluar idEntidad '{}': {}", expresionSpel, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> evaluarDetalle(String expresionSpel, StandardEvaluationContext contexto) {
        if (expresionSpel.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Expression expresion = PARSER.parseExpression(expresionSpel);
            Object valor = expresion.getValue(contexto);
            if (valor instanceof Map<?, ?> mapa) {
                return (Map<String, Object>) mapa;
            }
            if (valor == null) {
                return new LinkedHashMap<>();
            }
            Map<String, Object> envuelto = new LinkedHashMap<>();
            envuelto.put("valor", valor);
            return envuelto;
        } catch (ParseException | EvaluationException e) {
            // Una expresión SpEL rota (p. ej. NPE en #resultado.campo cuando el
            // método lanzó antes de construir el resultado) NUNCA debe hacer
            // perder el evento entero: se registra igual, dejando constancia.
            Map<String, Object> conError = new LinkedHashMap<>();
            conError.put("_error_detalle", "No se pudo evaluar la expresión de detalle: " + e.getMessage());
            return conError;
        }
    }

    private Map<String, Object> fusionarConContextoAuditoria(Map<String, Object> detalle) {
        Map<String, Object> aportadoPorElServicio = ContextoAuditoria.drenar();
        if (aportadoPorElServicio.isEmpty()) {
            return detalle;
        }
        Map<String, Object> fusionado = new LinkedHashMap<>(detalle);
        fusionado.putAll(aportadoPorElServicio);
        return fusionado;
    }

    private static String truncar(String texto, int maximo) {
        if (texto == null || texto.length() <= maximo) {
            return texto;
        }
        return texto.substring(0, maximo) + "…";
    }

    private static String blankToNull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto;
    }
}
