package uteq.edu.ec.artisync.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de servicio para que {@link AspectoAuditoria} registre un
 * evento en la bitácora de auditoria_eventos (REQ-NF-013) cada vez que se
 * invoque a través del proxy Spring.
 *
 * Las expresiones {@code idEntidad} y {@code detalle} son SpEL, evaluadas con
 * los parámetros del método por nombre (requiere -parameters, activo por
 * herencia de spring-boot-starter-parent) y, tras invocar el método, también
 * con {@code #resultado} (el valor devuelto) y {@code #error} (la excepción,
 * si la hubo). Un fallo al evaluar el SpEL nunca hace perder el evento: se
 * registra igual con un detalle de error.
 *
 * ADVERTENCIA: solo funciona en llamadas que atraviesan el proxy AOP. Una
 * auto-invocación (this.metodo() dentro de la misma clase) no genera evento.
 * Anotar únicamente métodos de una interfaz de servicio invocados desde un
 * controlador u otro bean.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Código de la acción, p. ej. "USUARIO_CREAR". Va a accion_auditoria. */
    String accion();

    /** Módulo de negocio. Va a modulo_auditoria. */
    ModuloAuditoria modulo();

    /** Nombre de la tabla/entidad afectada, p. ej. "pedidos". Opcional. */
    String entidad() default "";

    /**
     * SpEL que resuelve el id de la entidad afectada, p. ej. "#idPedido" o
     * "#resultado.idPedido" cuando el id solo se conoce al terminar.
     */
    String idEntidad() default "";

    /**
     * SpEL que debe evaluar a un Map (se admite el literal de mapa de SpEL:
     * "{clave: #valor}") u otro objeto que el sanitizador pueda normalizar.
     * NUNCA incluir aquí objetos completos de petición que puedan contener
     * contraseñas, tokens o cuerpos de mensajes: el sanitizador enmascara por
     * nombre de clave, pero declarar campos concretos es la primera defensa.
     */
    String detalle() default "";

    /**
     * SpEL para el correo del actor cuando aún no hay SecurityContext (login,
     * registro). Si se omite, el actor se resuelve de SecurityContextHolder.
     */
    String correoActor() default "";
}
