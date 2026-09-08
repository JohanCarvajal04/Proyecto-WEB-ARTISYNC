package uteq.edu.ec.artisync.controller.legal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPago;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IPagoServicio;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PagoControlador {

    private final IPagoServicio pagoServicio;

    /**
     * Crea una orden de pago de PayPal para un pedido.
     *
     * @param idPedido identificador del pedido
     * @param userDetails usuario autenticado que inicia el pago
     * @return la orden de pago creada
     * @throws ExcepcionRecursoNoEncontrado si no existe contrato para el pedido
     * @throws ExcepcionReglaNegocio si el usuario no es el cliente del pedido, el contrato no está firmado por ambas partes,
     *      o ocurre un error al comunicarse con PayPal
     */
    @PostMapping("/{idPedido}/pago")
    @PreAuthorize("hasAuthority('PEDIDO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaPago> crearOrdenPago(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pagoServicio.crearOrdenPayPal(idPedido, userDetails.getIdUsuario(), null));
    }

    /**
     * Obtiene el estado del pago asociado a un pedido.
     *
     * @param idPedido identificador del pedido
     * @param userDetails usuario autenticado que consulta el pago
     * @return el estado actual del pago
     * @throws ExcepcionRecursoNoEncontrado si no existe contrato o pago registrado para el pedido
     * @throws ExcepcionReglaNegocio si el usuario no tiene acceso al pago del pedido
     */
    @GetMapping("/{idPedido}/pago/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaPago> obtenerEstadoPago(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pagoServicio.obtenerEstadoPago(idPedido, userDetails.getIdUsuario()));
    }
}
