package uteq.edu.ec.artisync.controller.legal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEstadoFirma;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IContratoServicio;

@RestController
@RequestMapping("/api/v1/contratos")
@RequiredArgsConstructor
public class ContratoControlador {

    private final IContratoServicio contratoServicio;

    @PostMapping("/pedido/{idPedido}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaContrato> generarContrato(@PathVariable Long idPedido) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contratoServicio.generarContrato(idPedido));
    }

    @PostMapping("/{id}/firmar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaContrato> firmarContrato(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(contratoServicio.firmarContrato(id, userDetails.getIdUsuario()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaContrato> obtenerContrato(@PathVariable Long id) {
        return ResponseEntity.ok(contratoServicio.obtenerContrato(id));
    }

    @GetMapping("/pedido/{idPedido}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaContrato> obtenerContratoPorPedido(@PathVariable Long idPedido) {
        return ResponseEntity.ok(contratoServicio.obtenerContratoPorPedido(idPedido));
    }

    @GetMapping("/{id}/estado-firma")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaEstadoFirma> obtenerEstadoFirma(@PathVariable Long id) {
        return ResponseEntity.ok(contratoServicio.obtenerEstadoFirma(id));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        byte[] pdf = contratoServicio.generarPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "contrato_" + id + ".pdf");
        headers.setContentLength(pdf.length);

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
