package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.ia.IaVerificacionResponse;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaColaVerificacion;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaEstadoIdentidad;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaVerificacion;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.entity.perfil.EstadoVerificacion;
import uteq.edu.ec.artisync.entity.perfil.TipoDocumentoVerificacion;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.repository.perfil.EstadoVerificacionRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.perfil.IVerificacionServicio;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;
import uteq.edu.ec.artisync.service.shared.ia.IaService;
import uteq.edu.ec.artisync.service.shared.imagen.PreprocesadorImagenIa;
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
public class VerificacionServicioImpl implements IVerificacionServicio {

    private final UsuarioRepository usuarioRepository;
    private final EstadoVerificacionRepository estadoVerificacionRepository;
    private final CertificadoIaRepository certificadoIaRepository;
    private final AlmacenamientoDocumentos almacenamiento;
    private final PreprocesadorImagenIa preprocesador;
    private final IaService iaService;
    // Jackson 3, no com.fasterxml — usado desde la Tarea 16 para serializar
    // los datos que la IA extrae del documento (datos_extraidos_ia).
    private final ObjectMapper objectMapper;
    private final jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    // Nunca el contenido ni el nombre del documento: REQ-F-006 exige
    // eliminarlo tras la respuesta, y guardarlo aquí lo contradiría.
    @Auditable(accion = "VERIFICACION_SOLICITAR", modulo = ModuloAuditoria.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#resultado.idCertificado",
            detalle = "{tipoDocumento: #tipo}")
    public RespuestaVerificacion subir(Long idUsuarioSolicitante, TipoDocumentoVerificacion tipo, MultipartFile documento) {
        Usuario usuario = usuarioRepository.findById(idUsuarioSolicitante)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado: " + idUsuarioSolicitante));

        if (certificadoIaRepository.existsByUsuarioIdUsuarioAndEstadoVerificacionNombreEstado(
                idUsuarioSolicitante, "PENDIENTE")) {
            throw new ExcepcionReglaNegocio(
                    "Ya existe una verificación pendiente para tu cuenta. Espera a que sea revisada antes de subir otra.");
        }

        preprocesador.validarFormato(documento);

        EstadoVerificacion pendiente = estadoVerificacionRepository.findByNombreEstado("PENDIENTE")
                .orElseThrow(() -> new ExcepcionReglaNegocio(
                        "El estado PENDIENTE no está sembrado en estados_verificacion (ver migración V6)."));

        String hash = calcularHash(documento);
        String referenciaAlmacenamiento = almacenamiento.guardar(documento);

        CertificadoIa certificado = CertificadoIa.builder()
                .usuario(usuario)
                .estadoVerificacion(pendiente)
                .urlDocumentoS3(referenciaAlmacenamiento)
                .tipoDocumento(tipo.name())
                .hashDocumento(hash)
                .documentoEliminado(false)
                .build();

        CertificadoIa guardado = certificadoIaRepository.save(certificado);
        log.info("Verificación {} creada para usuario {} [tipo={}]", guardado.getIdCertificado(), idUsuarioSolicitante, tipo);
        return mapearARespuesta(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaColaVerificacion> listarCola(String nombreEstado, int limite, int offset) {
        return certificadoIaRepository.listarCola(nombreEstado, limite, offset).stream()
                .map(fila -> RespuestaColaVerificacion.builder()
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
    public RespuestaVerificacion obtenerPorId(Long idCertificado, Long idUsuarioSolicitante, boolean esRevisor) {
        CertificadoIa certificado = buscarPorId(idCertificado);
        boolean esDueno = certificado.getUsuario().getIdUsuario().equals(idUsuarioSolicitante);
        if (!esRevisor && !esDueno) {
            throw new AccessDeniedException("No tienes acceso a esta verificación.");
        }
        return mapearARespuesta(certificado);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] obtenerDocumento(Long idCertificado) {
        CertificadoIa certificado = buscarPorId(idCertificado);
        return almacenamiento.leer(certificado.getUrlDocumentoS3());
    }

    @Override
    @Transactional
    public RespuestaVerificacion analizarConIa(Long idCertificado) {
        CertificadoIa certificado = buscarPorId(idCertificado);

        if (certificado.isDocumentoEliminado()) {
            throw new ExcepcionReglaNegocio("El documento ya fue eliminado; no se puede reanalizar.");
        }

        byte[] original = almacenamiento.leer(certificado.getUrlDocumentoS3());
        byte[] comprimido = preprocesador.comprimirParaIa(original);

        IaVerificacionResponse dictamen = "CERTIFICADO".equals(certificado.getTipoDocumento())
                ? iaService.analizarCertificado(comprimido, "image/jpeg")
                : iaService.verificarIdentidad(comprimido, "image/jpeg");

        certificado.setVeredictoIa(dictamen.isAprobado() ? "SUGIERE_APROBAR" : "SUGIERE_RECHAZAR");
        certificado.setPuntajeConfianzaIa(dictamen.getConfianza());
        certificado.setRazonIa(dictamen.getRazonRechazo());
        certificado.setDatosExtraidosIa(serializarDatosExtraidos(dictamen));
        certificado.setFechaDictamenIa(LocalDateTime.now());

        CertificadoIa guardado = certificadoIaRepository.save(certificado);
        log.info("Dictamen de IA registrado para verificación {}: {}", idCertificado, certificado.getVeredictoIa());
        return mapearARespuesta(guardado);
    }

    @Override
    @Transactional
    @Auditable(accion = "VERIFICACION_DECIDIR", modulo = ModuloAuditoria.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#idCertificado",
            detalle = "{idNuevoEstado: #idNuevoEstado}")
    public RespuestaVerificacion registrarDecision(Long idCertificado, Long idModerador, Long idNuevoEstado, String notaModerador) {
        CertificadoIa certificado = buscarPorId(idCertificado);

        estadoVerificacionRepository.findById(idNuevoEstado)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Estado de verificación " + idNuevoEstado + " no existe."));

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

    private CertificadoIa buscarPorId(Long idCertificado) {
        return certificadoIaRepository.findById(idCertificado)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Verificación " + idCertificado + " no encontrada."));
    }

    private String calcularHash(MultipartFile documento) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(documento.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (IOException e) {
            throw new ExcepcionReglaNegocio("No se pudo leer el documento para calcular su huella.");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM.", e);
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
    public boolean estaIdentidadVerificada(Long idUsuario) {
        return certificadoIaRepository.existsByUsuarioIdUsuarioAndTipoDocumentoAndEstadoVerificacionNombreEstado(
                idUsuario, "IDENTIDAD", "APROBADO");
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaEstadoIdentidad obtenerEstadoIdentidad(Long idUsuario) {
        boolean verificado = estaIdentidadVerificada(idUsuario);
        String estadoActual = certificadoIaRepository
                .findTopByUsuarioIdUsuarioAndTipoDocumentoOrderByFechaAnalisisDesc(idUsuario, "IDENTIDAD")
                .map(c -> c.getEstadoVerificacion().getNombreEstado())
                .orElse(null);
        return RespuestaEstadoIdentidad.builder()
                .verificado(verificado)
                .estadoActual(estadoActual)
                .build();
    }

    private RespuestaVerificacion mapearARespuesta(CertificadoIa c) {
        return RespuestaVerificacion.builder()
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
