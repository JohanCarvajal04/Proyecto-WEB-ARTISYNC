package uteq.edu.ec.artisync.controller.legal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionSolicitudRetiro;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaSaldoCreador;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaSolicitudRetiro;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.ISolicitudRetiroServicio;

import java.util.List;

/** Lado creador: consultar saldo, solicitar retiro y ver el propio historial. */
@RestController
@RequestMapping("/api/v1/retiros")
@RequiredArgsConstructor
public class SolicitudRetiroControlador {

    private final ISolicitudRetiroServicio solicitudRetiroServicio;

    /**
     * Obtiene el saldo disponible para retiro del creador autenticado.
     *
     * @param userDetails usuario autenticado
     * @return el saldo disponible del creador
     */
    @GetMapping("/saldo")
    @PreAuthorize("hasAuthority('RETIROS_SOLICITAR')")
    public ResponseEntity<RespuestaSaldoCreador> obtenerSaldo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(solicitudRetiroServicio.obtenerSaldo(userDetails.getIdUsuario()));
    }

    /**
     * Registra una nueva solicitud de retiro para el creador autenticado.
     *
     * @param userDetails usuario autenticado que solicita el retiro
     * @param peticion datos de la solicitud de retiro
     * @return la solicitud de retiro creada
     * @throws ExcepcionRecursoNoEncontrado si el usuario no existe
     * @throws ExcepcionReglaNegocio si el creador no ha configurado su correo de PayPal, ya tiene una solicitud
     *      en curso, el monto es menor al mínimo permitido, o supera su saldo disponible
     */
    @PostMapping
    @PreAuthorize("hasAuthority('RETIROS_SOLICITAR')")
    public ResponseEntity<RespuestaSolicitudRetiro> solicitar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PeticionSolicitudRetiro peticion) {
        return ResponseEntity.ok(solicitudRetiroServicio.solicitar(userDetails.getIdUsuario(), peticion));
    }

    /**
     * Lista el historial de solicitudes de retiro del creador autenticado.
     *
     * @param userDetails usuario autenticado
     * @return listado de solicitudes de retiro del creador
     */
    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasAuthority('RETIROS_SOLICITAR')")
    public ResponseEntity<List<RespuestaSolicitudRetiro>> misSolicitudes(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(solicitudRetiroServicio.misSolicitudes(userDetails.getIdUsuario()));
    }
}
