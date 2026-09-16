package uteq.edu.ec.artisync.service.profile.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.request.profile.CreateAiCertificateRequest;
import uteq.edu.ec.artisync.dto.response.profile.AiCertificateResponse;
import uteq.edu.ec.artisync.entity.profile.AiCertificate;
import uteq.edu.ec.artisync.entity.profile.VerificationStatus;
import uteq.edu.ec.artisync.entity.security.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.profile.AiCertificateRepository;
import uteq.edu.ec.artisync.repository.profile.VerificationStatusRepository;
import uteq.edu.ec.artisync.repository.security.UserRepository;
import uteq.edu.ec.artisync.service.profile.IAiCertificateService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiCertificateServiceImpl implements IAiCertificateService {

    private final AiCertificateRepository certificadoRepository;
    private final UserRepository usuarioRepository;
    private final VerificationStatusRepository estadoRepository;

    /**
     * {@inheritDoc}
     * @param peticion el peticion
     * @return el resultado de la operacion, de tipo {@code AiCertificateResponse}
     */
    @Override
    @Transactional
    @Auditable(accion = "CERTIFICADO_EMITIR", modulo = AuditModule.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#resultado.idCertificado",
            detalle = "{idUsuario: #peticion.idUsuario, idEstadoVerificacion: #peticion.idEstadoVerificacion}")
    public AiCertificateResponse issueCertificate(CreateAiCertificateRequest peticion) {
        User usuario = usuarioRepository.findById(peticion.idUsuario())
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado con ID: " + peticion.idUsuario()));

        VerificationStatus estado = estadoRepository.findById(peticion.idEstadoVerificacion())
                .orElseThrow(() -> new ResourceNotFoundException("Estado de verificación no encontrado con ID: " + peticion.idEstadoVerificacion()));

        AiCertificate certificado = AiCertificate.builder()
                .usuario(usuario)
                .estadoVerificacion(estado)
                .urlDocumentoS3(peticion.urlDocumentoS3())
                .puntajeConfianzaIa(peticion.puntajeConfianzaIa())
                .build();

        AiCertificate guardado = certificadoRepository.save(certificado);
        return mapToResponse(guardado);
    }

    /**
     * {@inheritDoc}
     * @param idCertificado el identificador de certificado
     * @return el resultado de la operacion, de tipo {@code AiCertificateResponse}
     */
    @Override
    @Transactional(readOnly = true)
    public AiCertificateResponse getCertificateById(Long idCertificado) {
        AiCertificate certificado = certificadoRepository.findById(idCertificado)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado IA no encontrado con ID: " + idCertificado));
        return mapToResponse(certificado);
    }

    /**
     * {@inheritDoc}
     * @param idUsuario el identificador de usuario
     * @return la lista de AiCertificateResponse encontrados
     */
    @Override
    @Transactional(readOnly = true)
    public List<AiCertificateResponse> listCertificatesByUser(Long idUsuario) {
        return certificadoRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     * @return la lista de AiCertificateResponse encontrados
     */
    @Override
    @Transactional(readOnly = true)
    public List<AiCertificateResponse> listAllCertificates() {
        return certificadoRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     * @param idCertificado el identificador de certificado
     */
    @Override
    @Transactional
    @Auditable(accion = "CERTIFICADO_ELIMINAR", modulo = AuditModule.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#idCertificado")
    public void deleteCertificate(Long idCertificado) {
        if (!certificadoRepository.existsById(idCertificado)) {
            throw new ResourceNotFoundException("Certificado IA no encontrado con ID: " + idCertificado);
        }
        certificadoRepository.deleteById(idCertificado);
    }

    private AiCertificateResponse mapToResponse(AiCertificate certificado) {
        return AiCertificateResponse.builder()
                .idCertificado(certificado.getIdCertificado())
                .idUsuario(certificado.getUsuario() != null ? certificado.getUsuario().getIdUsuario() : null)
                .idEstadoVerificacion(certificado.getEstadoVerificacion() != null ? certificado.getEstadoVerificacion().getIdEstadoVerificacion() : null)
                .nombreEstadoVerificacion(certificado.getEstadoVerificacion() != null ? certificado.getEstadoVerificacion().getNombreEstado() : null)
                .urlDocumentoS3(certificado.getUrlDocumentoS3())
                .puntajeConfianzaIa(certificado.getPuntajeConfianzaIa())
                .fechaAnalisis(certificado.getFechaAnalisis())
                .build();
    }
}

