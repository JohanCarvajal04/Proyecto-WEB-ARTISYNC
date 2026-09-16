package uteq.edu.ec.artisync.service.profile.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.ai.AiVerificationResponse;
import uteq.edu.ec.artisync.dto.response.profile.VerificationQueueResponse;
import uteq.edu.ec.artisync.dto.response.profile.IdentityStatusResponse;
import uteq.edu.ec.artisync.dto.response.profile.VerificationResponse;
import uteq.edu.ec.artisync.entity.profile.AiCertificate;
import uteq.edu.ec.artisync.entity.profile.VerificationStatus;
import uteq.edu.ec.artisync.entity.profile.VerificationDocumentType;
import uteq.edu.ec.artisync.entity.security.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.exception.AiServiceUnavailableException;
import uteq.edu.ec.artisync.repository.profile.AiCertificateRepository;
import uteq.edu.ec.artisync.repository.profile.VerificationStatusRepository;
import uteq.edu.ec.artisync.repository.security.UserRepository;
import uteq.edu.ec.artisync.service.profile.IVerificationService;
import uteq.edu.ec.artisync.service.shared.storage.DocumentStorage;
import uteq.edu.ec.artisync.service.shared.ai.AiService;
import uteq.edu.ec.artisync.service.shared.image.AiImagePreprocessor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements IVerificationService {

    private final UserRepository usuarioRepository;
    private final VerificationStatusRepository estadoVerificacionRepository;
    private final AiCertificateRepository certificadoIaRepository;
    private final DocumentStorage almacenamiento;
    private final AiImagePreprocessor preprocesador;
    private final AiService iaService;
    // Jackson 3, no com.fasterxml — usado desde la Tarea 16 para serializar
    // los datos que la IA extrae del documento (datos_extraidos_ia).
    private final ObjectMapper objectMapper;
    private final jakarta.persistence.EntityManager entityManager;

    /** {@inheritDoc} */
    @Override
    @Transactional
    // Nunca el contenido ni el nombre del documento: REQ-F-006 exige
    // eliminarlo tras la respuesta, y guardarlo aquí lo contradiría.
    @Auditable(accion = "VERIFICACION_SOLICITAR", modulo = AuditModule.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#resultado.idCertificado",
            detalle = "{tipoDocumento: #tipo}")
    public VerificationResponse upload(Long idUsuarioSolicitante, VerificationDocumentType tipo, MultipartFile documento) {
        User usuario = usuarioRepository.findById(idUsuarioSolicitante)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado: " + idUsuarioSolicitante));

        if (certificadoIaRepository.existsByUsuarioIdUsuarioAndEstadoVerificacionNombreEstado(
                idUsuarioSolicitante, "PENDIENTE")) {
            throw new BusinessRuleException(
                    "Ya existe una verificación pendiente para tu cuenta. Espera a que sea revisada antes de upload otra.");
        }

        preprocesador.validateFormat(documento);

        VerificationStatus pendiente = estadoVerificacionRepository.findByNombreEstado("PENDIENTE")
                .orElseThrow(() -> new BusinessRuleException(
                        "El estado PENDIENTE no está sembrado en estados_verificacion (ver migración V6)."));

        String hash = calculateHash(documento);
        String referenciaAlmacenamiento = almacenamiento.save(documento);

        AiCertificate certificado = AiCertificate.builder()
                .usuario(usuario)
                .estadoVerificacion(pendiente)
                .urlDocumentoS3(referenciaAlmacenamiento)
                .tipoDocumento(tipo.name())
                .hashDocumento(hash)
                .documentoEliminado(false)
                .build();

        AiCertificate guardado = certificadoIaRepository.save(certificado);
        log.info("Verificación {} creada para usuario {} [tipo={}]", guardado.getIdCertificado(), idUsuarioSolicitante, tipo);
        return mapToResponse(guardado);
    }

    /**
     * {@inheritDoc}
     * @param nombreEstado el nombre de estado
     * @param limite el limite
     * @param offset el offset
     * @return la lista de VerificationQueueResponse encontrados
     */
    @Override
    @Transactional(readOnly = true)
    public List<VerificationQueueResponse> listQueue(String nombreEstado, int limite, int offset) {
        return certificadoIaRepository.listQueue(nombreEstado, limite, offset).stream()
                .map(fila -> VerificationQueueResponse.builder()
                        .idCertificado(fila.getIdCertificado())
                        .idUsuario(fila.getIdUsuario())
                        .nombreUsuario(fila.getNombreUsuario())
                        .tipoDocumento(fila.getTipoDocumento())
                        .nombreEstado(fila.getNombreEstado())
                        .veredictoIa(fila.getVeredictoIa())
                        .puntajeConfianzaIa(fila.getPuntajeConfianzaIa())
                        .fechaAnalisis(fila.getFechaAnalisis())
                        .build())
                .toList();
    }

    /**
     * {@inheritDoc}
     * @param idCertificado el identificador de certificado
     * @param idUsuarioSolicitante el identificador de usuario solicitante
     * @param esRevisor el es revisor
     * @return el resultado de la operacion, de tipo {@code VerificationResponse}
     */
    @Override
    @Transactional(readOnly = true)
    public VerificationResponse getById(Long idCertificado, Long idUsuarioSolicitante, boolean esRevisor) {
        AiCertificate certificado = findById(idCertificado);
        boolean esDueno = certificado.getUsuario().getIdUsuario().equals(idUsuarioSolicitante);
        if (!esRevisor && !esDueno) {
            throw new AccessDeniedException("No tienes acceso a esta verificación.");
        }
        return mapToResponse(certificado);
    }

    /**
     * {@inheritDoc}
     * @param idCertificado el identificador de certificado
     * @return el resultado de la operacion, de tipo {@code byte[]}
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] getDocument(Long idCertificado) {
        AiCertificate certificado = findById(idCertificado);
        return almacenamiento.read(certificado.getUrlDocumentoS3());
    }

    /**
     * {@inheritDoc}
     * @param idCertificado el identificador de certificado
     * @return el resultado de la operacion, de tipo {@code VerificationResponse}
     */
    @Override
    @Transactional
    public VerificationResponse analyzeWithAi(Long idCertificado) {
        AiCertificate certificado = findById(idCertificado);

        if (certificado.isDocumentoEliminado()) {
            throw new BusinessRuleException("El documento ya fue eliminado; no se puede reanalizar.");
        }

        byte[] original = almacenamiento.read(certificado.getUrlDocumentoS3());
        byte[] comprimido = preprocesador.comprimirParaIa(original);
        log.info("Documento {} comprimido a {} bytes para envío a IA", idCertificado, comprimido.length);

        AiVerificationResponse dictamen = analyzeWithRetry(certificado, comprimido);

        certificado.setVeredictoIa(dictamen.isAprobado() ? "SUGIERE_APROBAR" : "SUGIERE_RECHAZAR");
        certificado.setPuntajeConfianzaIa(dictamen.getConfianza());
        certificado.setRazonIa(dictamen.getRazonRechazo());
        certificado.setDatosExtraidosIa(serializeExtractedData(dictamen));
        certificado.setFechaDictamenIa(LocalDateTime.now());

        AiCertificate guardado = certificadoIaRepository.save(certificado);
        log.info("Dictamen de IA registrado para verificación {}: {}", idCertificado, certificado.getVeredictoIa());
        return mapToResponse(guardado);
    }

    /**
     * {@inheritDoc}
     * @param idCertificado el identificador de certificado
     * @param idModerador el identificador de moderador
     * @param idNuevoEstado el identificador de nuevo estado
     * @param notaModerador el nota moderador
     * @return el resultado de la operacion, de tipo {@code VerificationResponse}
     */
    @Override
    @Transactional
    @Auditable(accion = "VERIFICACION_DECIDIR", modulo = AuditModule.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#idCertificado",
            detalle = "{idNuevoEstado: #idNuevoEstado}")
    public VerificationResponse recordDecision(Long idCertificado, Long idModerador, Long idNuevoEstado, String notaModerador) {
        AiCertificate certificado = findById(idCertificado);

        estadoVerificacionRepository.findById(idNuevoEstado)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de verificación " + idNuevoEstado + " no existe."));

        certificadoIaRepository.recordDecision(idCertificado, idNuevoEstado, idModerador, notaModerador);

        // El procedimiento escribió por fuera del ciclo de vida de Hibernate:
        // sin este refresh, `certificado` (ya gestionado) devolvería datos obsoletos.
        entityManager.refresh(certificado);

        // El procedimiento es la única fuente de verdad sobre si el documento
        // debe borrarse: solo lo hace para estados terminales (APROBADO,
        // RECHAZADO), no para REQUIERE_ACLARACION. Actuamos sobre el flag ya
        // refrescado en vez de borrar incondicionalmente.
        if (certificado.isDocumentoEliminado()) {
            almacenamiento.delete(certificado.getUrlDocumentoS3());
        }

        log.info("Decisión registrada para verificación {}: estado={}, moderador={}",
                idCertificado, certificado.getEstadoVerificacion().getNombreEstado(), idModerador);
        return mapToResponse(certificado);
    }

    /**
     * Un intento + 1 reintento, solo si el fallo es transitorio (429/timeout,
     * ver AiServiceUnavailableException#isReintentable). 401/413 fallarían
     * exactamente igual en el segundo intento y solo duplicarían la espera
     * del moderador, así que se propagan de inmediato.
     */
    private AiVerificationResponse analyzeWithRetry(AiCertificate certificado, byte[] comprimido) {
        boolean esCertificado = "CERTIFICADO".equals(certificado.getTipoDocumento());
        try {
            return esCertificado
                    ? iaService.analyzeCertificate(comprimido, "image/jpeg")
                    : iaService.verifyIdentity(comprimido, "image/jpeg");
        } catch (AiServiceUnavailableException e) {
            if (!e.isReintentable()) {
                throw e;
            }
            log.warn("Fallo transitorio al analizar verificación {}, reintentando en 2s: {}",
                    certificado.getIdCertificado(), e.getMessage());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw e;
            }
            return esCertificado
                    ? iaService.analyzeCertificate(comprimido, "image/jpeg")
                    : iaService.verifyIdentity(comprimido, "image/jpeg");
        }
    }

    private AiCertificate findById(Long idCertificado) {
        return certificadoIaRepository.findById(idCertificado)
                .orElseThrow(() -> new ResourceNotFoundException("Verificación " + idCertificado + " no encontrada."));
    }

    private String calculateHash(MultipartFile documento) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(documento.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (IOException e) {
            throw new BusinessRuleException("No se pudo leer el documento para calcular su huella.");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible en esta JVM.", e);
        }
    }

    private String serializeExtractedData(AiVerificationResponse dictamen) {
        java.util.Map<String, String> datos = new java.util.LinkedHashMap<>();
        if (dictamen.getNombreDetectado() != null) datos.put("nombreDetectado", dictamen.getNombreDetectado());
        if (dictamen.getTipoDocumento() != null) datos.put("tipoDocumentoDetectado", dictamen.getTipoDocumento());
        if (dictamen.getFechaNacimiento() != null) datos.put("fechaNacimiento", dictamen.getFechaNacimiento());
        if (dictamen.getPaisEmision() != null) datos.put("paisEmision", dictamen.getPaisEmision());
        if (dictamen.getInstitucionEmisora() != null) datos.put("institucionEmisora", dictamen.getInstitucionEmisora());
        if (dictamen.getCampoEstudio() != null) datos.put("campoEstudio", dictamen.getCampoEstudio());
        if (dictamen.getFechaEmision() != null) datos.put("fechaEmision", dictamen.getFechaEmision());
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (Exception e) {
            log.warn("No se pudieron serializar los datos extraídos por la IA: {}", e.getMessage());
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * @param idUsuario el identificador de usuario
     * @return true si corresponde, false en caso contrario
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isIdentityVerified(Long idUsuario) {
        return certificadoIaRepository.existsByUsuarioIdUsuarioAndTipoDocumentoAndEstadoVerificacionNombreEstado(
                idUsuario, "IDENTIDAD", "APROBADO");
    }

    /**
     * {@inheritDoc}
     * @param idUsuario el identificador de usuario
     * @return el resultado de la operacion, de tipo {@code IdentityStatusResponse}
     */
    @Override
    @Transactional(readOnly = true)
    public IdentityStatusResponse getIdentityStatus(Long idUsuario) {
        boolean verificado = isIdentityVerified(idUsuario);
        String estadoActual = certificadoIaRepository
                .findTopByUsuarioIdUsuarioAndTipoDocumentoOrderByFechaAnalisisDesc(idUsuario, "IDENTIDAD")
                .map(c -> c.getEstadoVerificacion().getNombreEstado())
                .orElse(null);
        return IdentityStatusResponse.builder()
                .verificado(verificado)
                .estadoActual(estadoActual)
                .build();
    }

    private VerificationResponse mapToResponse(AiCertificate c) {
        return VerificationResponse.builder()
                .idCertificado(c.getIdCertificado())
                .idUsuario(c.getUsuario().getIdUsuario())
                .tipoDocumento(c.getTipoDocumento())
                .nombreEstadoVerificacion(c.getEstadoVerificacion().getNombreEstado())
                .veredictoIa(c.getVeredictoIa())
                .puntajeConfianzaIa(c.getPuntajeConfianzaIa())
                .razonIa(c.getRazonIa())
                .datosExtraidosIa(c.getDatosExtraidosIa())
                .fechaDictamenIa(c.getFechaDictamenIa())
                .idModerador(c.getModerador() != null ? c.getModerador().getIdUsuario() : null)
                .fechaDecision(c.getFechaDecision())
                .notaModerador(c.getNotaModerador())
                .fechaAnalisis(c.getFechaAnalisis())
                .build();
    }
}
