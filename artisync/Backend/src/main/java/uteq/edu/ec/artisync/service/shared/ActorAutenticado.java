package uteq.edu.ec.artisync.service.shared;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uteq.edu.ec.artisync.security.CustomUserDetails;

/**
 * Resuelve el usuario autenticado a partir de SecurityContextHolder, para uso
 * exclusivo de infraestructura transversal (AspectoAuditoria).
 *
 * ADVERTENCIA DE CONVENCIÓN: desde 626f955 ("exige propiedad del perfil de
 * creador") la regla del proyecto es que los SERVICIOS de negocio no lean
 * SecurityContextHolder directamente — el controlador resuelve el actor desde
 * Authentication y se lo pasa al servicio como parámetro escalar
 * (correoSolicitante, esAdmin), precisamente para que el servicio no dependa
 * de un contexto estático y siga siendo comprobable con un test unitario
 * corriente. Esa regla es correcta y se mantiene para la lógica de negocio.
 *
 * Esta clase es la excepción deliberada: el aspecto de auditoría es
 * infraestructura transversal que envuelve métodos ya invocados, no puede
 * recibir un parámetro adicional del llamante sin cambiar la firma de medio
 * centenar de métodos de negocio, y su propio test unitario monta el
 * SecurityContext a mano en un @BeforeEach/@AfterEach — el mismo patrón que
 * ya usa RolePermissionServiceImplTest.autenticarComoUsuario(Long).
 */
public final class ActorAutenticado {

    private ActorAutenticado() {
    }

    public record Actor(Long id, String correo) {
    }

    private static final Actor ANONIMO = new Actor(null, "anonimo");

    public static Actor actual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails detalles) {
            return new Actor(detalles.getIdUsuario(), detalles.getUsername());
        }
        return ANONIMO;
    }
}
