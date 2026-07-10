package uteq.edu.ec.artisync.controller.perfil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearCertificadoIa;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaCertificadoIa;
import uteq.edu.ec.artisync.service.perfil.ICertificadoIaServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1/certificados")
@RequiredArgsConstructor
public class CertificadoIaControlador {

    private final ICertificadoIaServicio certificadoServicio;

    @PostMapping
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaCertificadoIa> emitirCertificado(@Valid @RequestBody PeticionCrearCertificadoIa peticion) {
        RespuestaCertificadoIa respuesta = certificadoServicio.emitirCertificado(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespuestaCertificadoIa> obtenerCertificadoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(certificadoServicio.obtenerCertificadoPorId(id));
    }

    @GetMapping("/perfil/{idPerfil}")
    public ResponseEntity<List<RespuestaCertificadoIa> > listarCertificadosPorPerfil(@PathVariable Long idPerfil) {
        return ResponseEntity.ok(certificadoServicio.listarCertificadosPorPerfil(idPerfil));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERADOR', 'ADMIN')")
    public ResponseEntity<List<RespuestaCertificadoIa> > listarTodosLosCertificados() {
        return ResponseEntity.ok(certificadoServicio.listarTodosLosCertificados());
    }

    @PatchMapping("/{id}/estado/{idNuevoEstado}")
    @PreAuthorize("hasAnyRole('MODERADOR', 'ADMIN')")
    public ResponseEntity<RespuestaCertificadoIa> actualizarEstadoVerificacion(
            @PathVariable Long id,
            @PathVariable Long idNuevoEstado) {
        return ResponseEntity.ok(certificadoServicio.actualizarEstadoVerificacion(id, idNuevoEstado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarCertificado(@PathVariable Long id) {
        certificadoServicio.eliminarCertificado(id);
        return ResponseEntity.ok(new RespuestaMensaje("Certificado de IA eliminado exitosamente"));
    }
}
