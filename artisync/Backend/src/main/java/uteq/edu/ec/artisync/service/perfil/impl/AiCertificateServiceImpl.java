package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.perfil.CreateAiCertificateRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.AiCertificateResponse;
import uteq.edu.ec.artisync.entity.perfil.AiCertificate;
import uteq.edu.ec.artisync.entity.perfil.VerificationStatus;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.perfil.AiCertificateRepository;
import uteq.edu.ec.artisync.repository.perfil.VerificationStatusRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.perfil.IAiCertificateService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiCertificateServiceImpl implements IAiCertificateService {

    private final AiCertificateRepository certificadoRepository;
    private final UserRepository usuarioRepository;
    private final VerificationStatusRepository estadoRepository;

    @Override
    @Transactional
    @Auditable(accion = "CERTIFICADO_EMITIR", modulo = AuditModule.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#resultado.idCertificado",
            detalle = "{idUsuario: #peticion.idUsuario, idEstadoVerificacion: #peticion.idEstadoVerificacion}")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
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
        return mapearARespuesta(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idCertificado identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public AiCertificateResponse getCertificateById(Long idCertificado) {
        AiCertificate certificado = certificadoRepository.findById(idCertificado)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado IA no encontrado con ID: " + idCertificado));
        return mapearARespuesta(certificado);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<AiCertificateResponse> listCertificatesByUser(Long idUsuario) {
        return certificadoRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .map(this::mapearARespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<AiCertificateResponse> listAllCertificates() {
        return certificadoRepository.findAll().stream()
                .map(this::mapearARespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(accion = "CERTIFICADO_ELIMINAR", modulo = AuditModule.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#idCertificado")
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idCertificado identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void deleteCertificate(Long idCertificado) {
        if (!certificadoRepository.existsById(idCertificado)) {
            throw new ResourceNotFoundException("Certificado IA no encontrado con ID: " + idCertificado);
        }
        certificadoRepository.deleteById(idCertificado);
    }

    private AiCertificateResponse mapearARespuesta(AiCertificate certificado) {
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

