package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.ia.IaVerificacionResponse;
import uteq.edu.ec.artisync.dto.respuesta.perfil.VerificationQueueResponse;
import uteq.edu.ec.artisync.dto.respuesta.perfil.IdentityStatusResponse;
import uteq.edu.ec.artisync.dto.respuesta.perfil.VerificationResponse;
import uteq.edu.ec.artisync.entity.perfil.AiCertificate;
import uteq.edu.ec.artisync.entity.perfil.VerificationStatus;
import uteq.edu.ec.artisync.entity.perfil.VerificationDocumentType;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.exception.AiServiceUnavailableException;
import uteq.edu.ec.artisync.repository.perfil.AiCertificateRepository;
import uteq.edu.ec.artisync.repository.perfil.VerificationStatusRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.perfil.IVerificationService;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;
import uteq.edu.ec.artisync.service.shared.ia.AiService;
import uteq.edu.ec.artisync.service.shared.imagen.AiImagePreprocessor;
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

    @Override
    @Transactional
    // Nunca el contenido ni el nombre del documento: REQ-F-006 exige
    // eliminarlo tras la respuesta, y guardarlo aquí lo contradiría.
    @Auditable(accion = "VERIFICACION_SOLICITAR", modulo = AuditModule.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#resultado.idCertificado",
            detalle = "{tipoDocumento: #tipo}")
    /**
     * Sube un documento de identidad o certificado profesional para
     * verificación, en estado {@code PENDIENTE}.
     *
     * @param idUsuarioSolicitante identificador del usuario que solicita la verificación
     * @param tipo tipo de documento (identidad o certificado profesional)
     * @param documento archivo del documento, validado contra el formato esperado
     * @return la verificación creada, pendiente de análisis
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el usuario ya tiene una
     *         verificación pendiente, o si el documento no cumple el formato esperado
     */
    public VerificationResponse subir(Long idUsuarioSolicitante, VerificationDocumentType tipo, MultipartFile documento) {
        User usuario = usuarioRepository.findById(idUsuarioSolicitante)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado: " + idUsuarioSolicitante));

        if (certificadoIaRepository.existsByUsuarioIdUsuarioAndEstadoVerificacionNombreEstado(
                idUsuarioSolicitante, "PENDIENTE")) {
            throw new BusinessRuleException(
                    "Ya existe una verificación pendiente para tu cuenta. Espera a que sea revisada antes de subir otra.");
        }

        preprocesador.validarFormato(documento);

        VerificationStatus pendiente = estadoVerificacionRepository.findByNombreEstado("PENDIENTE")
                .orElseThrow(() -> new BusinessRuleException(
                        "El estado PENDIENTE no está sembrado en estados_verificacion (ver migración V6)."));

        String hash = calcularHash(documento);
        String referenciaAlmacenamiento = almacenamiento.guardar(documento);

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
        return mapearARespuesta(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Lista la cola de verificaciones pendientes de revisión por un moderador.
     *
     * @param nombreEstado estado a filtrar (p. ej. "PENDIENTE")
     * @param limite cantidad máxima de resultados
     * @param offset posición inicial del resultado
     * @return las verificaciones que cumplen el filtro
     */
    public List<VerificationQueueResponse> listarCola(String nombreEstado, int limite, int offset) {
        return certificadoIaRepository.listarCola(nombreEstado, limite, offset).stream()
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

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idCertificado identificador de la verificación
     * @param idUsuarioSolicitante identificador de quien consulta
     * @param esRevisor {@code true} si quien consulta es un moderador (puede ver cualquier verificación)
     * @return la verificación solicitada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la verificación no existe
     * @throws org.springframework.security.access.AccessDeniedException si quien consulta no es
     *         revisor ni dueño de la verificación
     */
    public VerificationResponse obtenerPorId(Long idCertificado, Long idUsuarioSolicitante, boolean esRevisor) {
        AiCertificate certificado = buscarPorId(idCertificado);
        boolean esDueno = certificado.getUsuario().getIdUsuario().equals(idUsuarioSolicitante);
        if (!esRevisor && !esDueno) {
            throw new AccessDeniedException("No tienes acceso a esta verificación.");
        }
        return mapearARespuesta(certificado);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] obtenerDocumento(Long idCertificado) {
        AiCertificate certificado = buscarPorId(idCertificado);
        return almacenamiento.leer(certificado.getUrlDocumentoS3());
    }

    @Override
    @Transactional
    /**
     * Envía el documento de una verificación a la IA para su análisis
     * (identidad o certificado profesional) y registra el dictamen.
     *
     * @param idCertificado identificador de la verificación a analizar
     * @return la verificación con el dictamen de IA ya registrado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la verificación no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el documento ya fue eliminado
     * @throws uteq.edu.ec.artisync.exception.AiServiceUnavailableException si el proveedor de IA no responde
     *         tras el reintento (en fallos transitorios) o ante un fallo no transitorio
     */
    public VerificationResponse analizarConIa(Long idCertificado) {
        AiCertificate certificado = buscarPorId(idCertificado);

        if (certificado.isDocumentoEliminado()) {
            throw new BusinessRuleException("El documento ya fue eliminado; no se puede reanalizar.");
        }

        byte[] original = almacenamiento.leer(certificado.getUrlDocumentoS3());
        byte[] comprimido = preprocesador.comprimirParaIa(original);
        log.info("Documento {} comprimido a {} bytes para envío a IA", idCertificado, comprimido.length);

        IaVerificacionResponse dictamen = analizarConReintento(certificado, comprimido);

        certificado.setVeredictoIa(dictamen.isAprobado() ? "SUGIERE_APROBAR" : "SUGIERE_RECHAZAR");
        certificado.setPuntajeConfianzaIa(dictamen.getConfianza());
        certificado.setRazonIa(dictamen.getRazonRechazo());
        certificado.setDatosExtraidosIa(serializarDatosExtraidos(dictamen));
        certificado.setFechaDictamenIa(LocalDateTime.now());

        AiCertificate guardado = certificadoIaRepository.save(certificado);
        log.info("Dictamen de IA registrado para verificación {}: {}", idCertificado, certificado.getVeredictoIa());
        return mapearARespuesta(guardado);
    }

    @Override
    @Transactional
    @Auditable(accion = "VERIFICACION_DECIDIR", modulo = AuditModule.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#idCertificado",
            detalle = "{idNuevoEstado: #idNuevoEstado}")
    /**
     * Registra la decisión de un moderador sobre una verificación (aprobar,
     * rechazar o pedir aclaración); el documento se elimina del almacenamiento
     * si el nuevo estado es terminal (aprobado o rechazado).
     *
     * @param idCertificado identificador de la verificación a decidir
     * @param idModerador identificador del moderador que decide
     * @param idNuevoEstado nuevo estado de verificación a asignar
     * @param notaModerador justificación de la decisión
     * @return la verificación con la decisión ya registrada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la verificación o el
     *         nuevo estado no existen
     */
    public VerificationResponse registrarDecision(Long idCertificado, Long idModerador, Long idNuevoEstado, String notaModerador) {
        AiCertificate certificado = buscarPorId(idCertificado);

        estadoVerificacionRepository.findById(idNuevoEstado)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de verificación " + idNuevoEstado + " no existe."));

        certificadoIaRepository.registrarDecision(idCertificado, idNuevoEstado, idModerador, notaModerador);

        // El procedimiento escribió por fuera del ciclo de vida de Hibernate:
        // sin este refresh, `certificado` (ya gestionado) devolvería datos obsoletos.
        entityManager.refresh(certificado);

        // El procedimiento es la única fuente de verdad sobre si el documento
        // debe borrarse: solo lo hace para estados terminales (APROBADO,
        // RECHAZADO), no para REQUIERE_ACLARACION. Actuamos sobre el flag ya
        // refrescado en vez de borrar incondicionalmente.
        if (certificado.isDocumentoEliminado()) {
            almacenamiento.eliminar(certificado.getUrlDocumentoS3());
        }

        log.info("Decisión registrada para verificación {}: estado={}, moderador={}",
                idCertificado, certificado.getEstadoVerificacion().getNombreEstado(), idModerador);
        return mapearARespuesta(certificado);
    }

    /**
     * Un intento + 1 reintento, solo si el fallo es transitorio (429/timeout,
     * ver AiServiceUnavailableException#isReintentable). 401/413 fallarían
     * exactamente igual en el segundo intento y solo duplicarían la espera
     * del moderador, así que se propagan de inmediato.
     */
    private IaVerificacionResponse analizarConReintento(AiCertificate certificado, byte[] comprimido) {
        boolean esCertificado = "CERTIFICADO".equals(certificado.getTipoDocumento());
        try {
            return esCertificado
                    ? iaService.analizarCertificado(comprimido, "image/jpeg")
                    : iaService.verificarIdentidad(comprimido, "image/jpeg");
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
                    ? iaService.analizarCertificado(comprimido, "image/jpeg")
                    : iaService.verificarIdentidad(comprimido, "image/jpeg");
        }
    }

    private AiCertificate buscarPorId(Long idCertificado) {
        return certificadoIaRepository.findById(idCertificado)
                .orElseThrow(() -> new ResourceNotFoundException("Verificación " + idCertificado + " no encontrada."));
    }

    private String calcularHash(MultipartFile documento) {
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

    private String serializarDatosExtraidos(IaVerificacionResponse dictamen) {
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

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idUsuario identificador del usuario
     * @return {@code true} si el usuario tiene un documento de identidad aprobado
     */
    public boolean estaIdentidadVerificada(Long idUsuario) {
        return certificadoIaRepository.existsByUsuarioIdUsuarioAndTipoDocumentoAndEstadoVerificacionNombreEstado(
                idUsuario, "IDENTIDAD", "APROBADO");
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idUsuario identificador del usuario
     * @return si la identidad del usuario está verificada, y el estado de su última solicitud
     */
    public IdentityStatusResponse obtenerEstadoIdentidad(Long idUsuario) {
        boolean verificado = estaIdentidadVerificada(idUsuario);
        String estadoActual = certificadoIaRepository
                .findTopByUsuarioIdUsuarioAndTipoDocumentoOrderByFechaAnalisisDesc(idUsuario, "IDENTIDAD")
                .map(c -> c.getEstadoVerificacion().getNombreEstado())
                .orElse(null);
        return IdentityStatusResponse.builder()
                .verificado(verificado)
                .estadoActual(estadoActual)
                .build();
    }

    private VerificationResponse mapearARespuesta(AiCertificate c) {
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
