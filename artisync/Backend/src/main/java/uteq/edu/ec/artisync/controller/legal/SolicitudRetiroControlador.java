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

    @GetMapping("/saldo")
    @PreAuthorize("hasAuthority('RETIROS_SOLICITAR')")
    public ResponseEntity<RespuestaSaldoCreador> obtenerSaldo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(solicitudRetiroServicio.obtenerSaldo(userDetails.getIdUsuario()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RETIROS_SOLICITAR')")
    public ResponseEntity<RespuestaSolicitudRetiro> solicitar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PeticionSolicitudRetiro peticion) {
        return ResponseEntity.ok(solicitudRetiroServicio.solicitar(userDetails.getIdUsuario(), peticion));
    }

    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasAuthority('RETIROS_SOLICITAR')")
    public ResponseEntity<List<RespuestaSolicitudRetiro>> misSolicitudes(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(solicitudRetiroServicio.misSolicitudes(userDetails.getIdUsuario()));
    }
}
