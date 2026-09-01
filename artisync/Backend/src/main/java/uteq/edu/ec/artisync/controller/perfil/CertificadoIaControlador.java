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

/**
 * CRUD administrativo de certificados de IA.
 *
 * <p>El alta de verificaciones ya no pasa por {@link #emitirCertificado}: el
 * camino vigente es {@code POST /api/v1/verificaciones} (ver
 * {@code VerificacionControlador}). Este {@code POST} se conserva restringido
 * a {@code ADMIN} solo para no romper clientes existentes del CRUD original
 * de {@code CertificadoIa}.</p>
 *
 * <p>Este controlador ya NO expone un endpoint para cambiar
 * {@code id_estado_verificacion}: el único camino auditado para registrar la
 * decisión de un moderador es {@code PATCH /api/v1/verificaciones/{id}/decision}
 * (ver {@code VerificacionControlador}), que pasa por
 * {@code sp_registrar_decision_verificacion} y deja rastro de moderador, fecha
 * y nota. El antiguo {@code PATCH /{id}/estado/{idNuevoEstado}} escribía el
 * estado directamente sin ninguna de esas garantías y fue eliminado.</p>
 */
@RestController
@RequestMapping("/api/v1/certificados")
@RequiredArgsConstructor
public class CertificadoIaControlador {

    private final ICertificadoIaServicio certificadoServicio;

    @PostMapping
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaCertificadoIa> emitirCertificado(@Valid @RequestBody PeticionCrearCertificadoIa peticion) {
        RespuestaCertificadoIa respuesta = certificadoServicio.emitirCertificado(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaCertificadoIa> obtenerCertificadoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(certificadoServicio.obtenerCertificadoPorId(id));
    }

    @GetMapping("/usuario/{idUsuario}")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaCertificadoIa> > listarCertificadosPorUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(certificadoServicio.listarCertificadosPorUsuario(idUsuario));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaCertificadoIa> > listarTodosLosCertificados() {
        return ResponseEntity.ok(certificadoServicio.listarTodosLosCertificados());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarCertificado(@PathVariable Long id) {
        certificadoServicio.eliminarCertificado(id);
        return ResponseEntity.ok(new RespuestaMensaje("Certificado de IA eliminado exitosamente"));
    }
}
