package uteq.edu.ec.artisync.controller.legal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPago;
import uteq.edu.ec.artisync.service.legal.IPagoServicio;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PagoControlador {

    private final IPagoServicio pagoServicio;

    @PostMapping("/{idPedido}/pago")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public ResponseEntity<RespuestaPago> crearOrdenPago(@PathVariable Long idPedido) {
        return ResponseEntity.ok(pagoServicio.crearOrdenPayPal(idPedido, null));
    }

    @GetMapping("/{idPedido}/pago/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaPago> obtenerEstadoPago(@PathVariable Long idPedido) {
        return ResponseEntity.ok(pagoServicio.obtenerEstadoPago(idPedido));
    }
}
