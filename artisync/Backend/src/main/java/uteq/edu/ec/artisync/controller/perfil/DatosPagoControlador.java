package uteq.edu.ec.artisync.controller.perfil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionDatosPago;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaDatosPago;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IDatosPagoServicio;

/**
 * Correo de PayPal del creador para recibir retiros. Ruta separada a
 * propósito de /api/v1/perfiles/{id}: ese endpoint es público y este dato de
 * cobro nunca debe colgar de él.
 */
@RestController
@RequestMapping("/api/v1/perfiles/mis-datos-pago")
@RequiredArgsConstructor
public class DatosPagoControlador {

    private final IDatosPagoServicio datosPagoServicio;

    /**
     * Obtiene los datos de pago (correo de PayPal) del creador autenticado.
     *
     * @param userDetails usuario autenticado
     * @return los datos de pago del creador
     */
    @GetMapping
    @PreAuthorize("hasAuthority('RETIROS_SOLICITAR')")
    public ResponseEntity<RespuestaDatosPago> obtenerMisDatosPago(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(datosPagoServicio.obtenerMisDatosPago(userDetails.getIdUsuario()));
    }

    /**
     * Actualiza el correo de PayPal del creador autenticado para recibir retiros.
     *
     * @param userDetails usuario autenticado
     * @param peticion nuevo correo de PayPal
     * @return los datos de pago actualizados
     */
    @PutMapping
    @PreAuthorize("hasAuthority('RETIROS_SOLICITAR')")
    public ResponseEntity<RespuestaDatosPago> actualizarCorreoPaypal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PeticionDatosPago peticion) {
        return ResponseEntity.ok(datosPagoServicio.actualizarCorreoPaypal(userDetails.getIdUsuario(), peticion));
    }
}
