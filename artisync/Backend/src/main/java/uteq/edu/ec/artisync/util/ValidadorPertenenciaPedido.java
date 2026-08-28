package uteq.edu.ec.artisync.util;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uteq.edu.ec.artisync.entity.pedido.Pedido;

/**
 * Extraído de PedidoServicioImpl/TicketRevisionServicioImpl (duplicado
 * literal en ambos, OBS-08) para que cualquier servicio que exponga un
 * recurso colgado de un Pedido (tickets, contratos, entregables, ...)
 * valide titularidad de la misma forma, en vez de reimplementar el chequeo
 * y arriesgarse a un IDOR por omisión (H-02).
 */
public final class ValidadorPertenenciaPedido {

    private ValidadorPertenenciaPedido() {
    }

    /**
     * Solo el cliente dueño del pedido, el creador del servicio pedido o un
     * ADMIN pueden acceder al recurso. Lanza AccessDeniedException (403) en
     * cualquier otro caso.
     */
    public static void validarPertenenciaOAdmin(Pedido pedido, Long idUsuarioSolicitante) {
        boolean esCliente = pedido.getUsuarioCliente().getIdUsuario().equals(idUsuarioSolicitante);
        boolean esCreador = pedido.getServicio().getPerfil().getUsuario().getIdUsuario().equals(idUsuarioSolicitante);

        if (esCliente || esCreador) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!esAdmin) {
            throw new AccessDeniedException("No tienes permisos para acceder a este recurso");
        }
    }
}
