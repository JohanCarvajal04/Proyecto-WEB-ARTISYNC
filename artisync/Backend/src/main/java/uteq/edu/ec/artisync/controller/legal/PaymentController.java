package uteq.edu.ec.artisync.controller.legal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.CancelPaymentRequest;
import uteq.edu.ec.artisync.dto.respuesta.legal.PaymentResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IPaymentService;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService pagoServicio;

    /**
     * Crea una orden de pago de PayPal para un pedido.
     *
     * @param idPedido identificador del pedido
     * @param userDetails usuario autenticado que inicia el pago
     * @return la orden de pago creada
     * @throws ResourceNotFoundException si no existe contrato para el pedido
     * @throws BusinessRuleException si el usuario no es el cliente del pedido, el contrato no está firmado por ambas partes,
     *      o ocurre un error al comunicarse con PayPal
     */
    @PostMapping("/{idPedido}/pago")
    @PreAuthorize("hasAuthority('PEDIDO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> crearOrdenPago(
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
     * @throws ResourceNotFoundException si no existe contrato o pago registrado para el pedido
     * @throws BusinessRuleException si el usuario no tiene acceso al pago del pedido
     */
    @GetMapping("/{idPedido}/pago/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> obtenerEstadoPago(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pagoServicio.obtenerEstadoPago(idPedido, userDetails.getIdUsuario()));
    }

    /**
     * Cancela un pedido con fondos ya retenidos en escrow (REQ-NF-019),
     * reembolsando al cliente vía PayPal o (solo administrador) liberando los
     * fondos al creador. La autorización fina (cliente-o-admin, y que solo un
     * admin pueda liberar) vive en el servicio, igual que en obtenerEstadoPago.
     *
     * @param idPedido    identificador del pedido a cancelar
     * @param userDetails usuario autenticado que solicita la cancelación
     * @param peticion    acción sobre los fondos ("REEMBOLSAR"/"LIBERAR") y motivo; body opcional
     * @return el estado final del pago tras la cancelación
     * @throws ResourceNotFoundException si no existe contrato o pago registrado para el pedido
     * @throws BusinessRuleException si el usuario no tiene permiso, la acción no es válida,
     *      o el pago no está en un estado cancelable
     */
    @PostMapping("/{idPedido}/pago/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> cancelarPago(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) CancelPaymentRequest peticion) {
        String accionFondos = peticion != null ? peticion.getAccionFondos() : null;
        String motivo = peticion != null ? peticion.getMotivo() : null;
        return ResponseEntity.ok(pagoServicio.cancelarPedidoConFondosRetenidos(
                idPedido, userDetails.getIdUsuario(), accionFondos, motivo));
    }
}
