package uteq.edu.ec.artisync.controller.perfil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.perfil.CreateAiCertificateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.AiCertificateResponse;
import uteq.edu.ec.artisync.service.perfil.IAiCertificateService;

import java.util.List;

/**
 * CRUD administrativo de certificados de IA.
 *
 * <p>El alta de verificaciones ya no pasa por {@link #issueCertificate}: el
 * camino vigente es {@code POST /api/v1/verificaciones} (ver
 * {@code VerificationController}). Este {@code POST} se conserva restringido
 * a {@code ADMIN} solo para no romper clientes existentes del CRUD original
 * de {@code AiCertificate}.</p>
 *
 * <p>Este controlador ya NO expone un endpoint para cambiar
 * {@code id_estado_verificacion}: el único camino auditado para registrar la
 * decisión de un moderador es {@code PATCH /api/v1/verificaciones/{id}/decision}
 * (ver {@code VerificationController}), que pasa por
 * {@code sp_registrar_decision_verificacion} y deja rastro de moderador, fecha
 * y nota. El antiguo {@code PATCH /{id}/estado/{idNuevoEstado}} escribía el
 * estado directamente sin ninguna de esas garantías y fue eliminado.</p>
 */
@RestController
@RequestMapping("/api/v1/certificados")
@RequiredArgsConstructor
public class AiCertificateController {

    private final IAiCertificateService certificadoServicio;

    /**
     * Emite un certificado de IA para un usuario. Conservado por compatibilidad;
     * el alta vigente de verificaciones es {@code POST /api/v1/verificaciones}.
     *
     * @param peticion datos del certificado a emitir
     * @return el certificado emitido, con estado 201
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario o el estado de verificación indicados no existen
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<AiCertificateResponse> issueCertificate(@Valid @RequestBody CreateAiCertificateRequest peticion) {
        AiCertificateResponse respuesta = certificadoServicio.issueCertificate(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Obtiene un certificado de IA por su identificador.
     *
     * @param id identificador del certificado
     * @return el certificado solicitado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el certificado no existe
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<AiCertificateResponse> getCertificateById(@PathVariable Long id) {
        return ResponseEntity.ok(certificadoServicio.getCertificateById(id));
    }

    /**
     * Lista los certificados de IA de un usuario.
     *
     * @param idUsuario identificador del usuario
     * @return listado de certificados del usuario
     */
    @GetMapping("/usuario/{idUsuario}")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<List<AiCertificateResponse> > listCertificatesByUser(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(certificadoServicio.listCertificatesByUser(idUsuario));
    }

    /**
     * Lista todos los certificados de IA del sistema.
     *
     * @return listado completo de certificados
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<List<AiCertificateResponse> > listAllCertificates() {
        return ResponseEntity.ok(certificadoServicio.listAllCertificates());
    }

    /**
     * Elimina un certificado de IA.
     *
     * @param id identificador del certificado a eliminar
     * @return mensaje de confirmación de la eliminación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el certificado no existe
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CERTIFICADO_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> deleteCertificate(@PathVariable Long id) {
        certificadoServicio.deleteCertificate(id);
        return ResponseEntity.ok(new RespuestaMensaje("Certificado de IA eliminado exitosamente"));
    }
}
