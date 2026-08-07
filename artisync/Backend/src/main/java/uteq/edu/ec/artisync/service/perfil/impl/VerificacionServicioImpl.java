package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaColaVerificacion;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaVerificacion;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.entity.perfil.EstadoVerificacion;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.perfil.TipoDocumentoVerificacion;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.repository.perfil.EstadoVerificacionRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.service.perfil.IVerificacionServicio;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;
import uteq.edu.ec.artisync.service.shared.ia.IaService;
import uteq.edu.ec.artisync.service.shared.imagen.PreprocesadorImagenIa;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificacionServicioImpl implements IVerificacionServicio {

    private final PerfilCreadorRepository perfilCreadorRepository;
    private final EstadoVerificacionRepository estadoVerificacionRepository;
    private final CertificadoIaRepository certificadoIaRepository;
    private final AlmacenamientoDocumentos almacenamiento;
    private final PreprocesadorImagenIa preprocesador;
    private final IaService iaService;
    // Jackson 3, no com.fasterxml — usado desde la Tarea 16 para serializar
    // los datos que la IA extrae del documento (datos_extraidos_ia).
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RespuestaVerificacion subir(Long idUsuarioSolicitante, TipoDocumentoVerificacion tipo, MultipartFile documento) {
        PerfilCreador perfil = perfilCreadorRepository.findByUsuarioIdUsuario(idUsuarioSolicitante)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Debes tener un perfil de creador para solicitar una verificación."));

        preprocesador.validarFormato(documento);

        EstadoVerificacion pendiente = estadoVerificacionRepository.findByNombreEstado("PENDIENTE")
                .orElseThrow(() -> new ExcepcionReglaNegocio(
                        "El estado PENDIENTE no está sembrado en estados_verificacion (ver migración V6)."));

        String hash = calcularHash(documento);
        String referenciaAlmacenamiento = almacenamiento.guardar(documento);

        CertificadoIa certificado = CertificadoIa.builder()
                .perfil(perfil)
                .estadoVerificacion(pendiente)
                .urlDocumentoS3(referenciaAlmacenamiento)
                .tipoDocumento(tipo.name())
                .hashDocumento(hash)
                .documentoEliminado(false)
                .build();

        CertificadoIa guardado = certificadoIaRepository.save(certificado);
        log.info("Verificación {} creada para perfil {} [tipo={}]", guardado.getIdCertificado(), perfil.getIdPerfil(), tipo);
        return mapearARespuesta(guardado);
    }

    @Override
    public List<RespuestaColaVerificacion> listarCola(String nombreEstado, int limite, int offset) {
        throw new UnsupportedOperationException("Se implementa en la Tarea 18");
    }

    @Override
    public RespuestaVerificacion obtenerPorId(Long idCertificado, Long idUsuarioSolicitante, boolean esRevisor) {
        throw new UnsupportedOperationException("Se implementa en la Tarea 18");
    }

    @Override
    public byte[] obtenerDocumento(Long idCertificado) {
        throw new UnsupportedOperationException("Se implementa en la Tarea 18");
    }

    @Override
    public RespuestaVerificacion analizarConIa(Long idCertificado) {
        throw new UnsupportedOperationException("Se implementa en la Tarea 16");
    }

    @Override
    public RespuestaVerificacion registrarDecision(Long idCertificado, Long idModerador, Long idNuevoEstado, String notaModerador) {
        throw new UnsupportedOperationException("Se implementa en la Tarea 17");
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

    private RespuestaVerificacion mapearARespuesta(CertificadoIa c) {
        return RespuestaVerificacion.builder()
                .idCertificado(c.getIdCertificado())
                .idPerfil(c.getPerfil().getIdPerfil())
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
