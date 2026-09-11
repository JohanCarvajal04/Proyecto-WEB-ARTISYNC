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

    /**
     * Emite un certificado de IA para un usuario. Conservado por compatibilidad;
     * el alta vigente de verificaciones es {@code POST /api/v1/verificaciones}.
     *
     * @param peticion datos del certificado a emitir
     * @return el certificado emitido, con estado 201
     * @throws ResourceNotFoundException si el usuario o el estado de verificación indicados no existen
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaCertificadoIa> emitirCertificado(@Valid @RequestBody PeticionCrearCertificadoIa peticion) {
        RespuestaCertificadoIa respuesta = certificadoServicio.emitirCertificado(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Obtiene un certificado de IA por su identificador.
     *
     * @param id identificador del certificado
     * @return el certificado solicitado
     * @throws ResourceNotFoundException si el certificado no existe
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaCertificadoIa> obtenerCertificadoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(certificadoServicio.obtenerCertificadoPorId(id));
    }

    /**
     * Lista los certificados de IA de un usuario.
     *
     * @param idUsuario identificador del usuario
     * @return listado de certificados del usuario
     */
    @GetMapping("/usuario/{idUsuario}")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaCertificadoIa> > listarCertificadosPorUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(certificadoServicio.listarCertificadosPorUsuario(idUsuario));
    }

    /**
     * Lista todos los certificados de IA del sistema.
     *
     * @return listado completo de certificados
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<List<RespuestaCertificadoIa> > listarTodosLosCertificados() {
        return ResponseEntity.ok(certificadoServicio.listarTodosLosCertificados());
    }

    /**
     * Elimina un certificado de IA.
     *
     * @param id identificador del certificado a eliminar
     * @return mensaje de confirmación de la eliminación
     * @throws ResourceNotFoundException si el certificado no existe
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> eliminarCertificado(@PathVariable Long id) {
        certificadoServicio.eliminarCertificado(id);
        return ResponseEntity.ok(new RespuestaMensaje("Certificado de IA eliminado exitosamente"));
    }
}
