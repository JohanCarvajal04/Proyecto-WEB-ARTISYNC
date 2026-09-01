package uteq.edu.ec.artisync.service.perfil;

import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaColaVerificacion;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaEstadoIdentidad;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaVerificacion;
import uteq.edu.ec.artisync.entity.perfil.TipoDocumentoVerificacion;

import java.util.List;

/**
 * La IA solo asiste (analizarConIa); el único método que puede cambiar
 * id_estado_verificacion es registrarDecision, restringido en el controlador
 * al permiso CERTIFICADO_REVISAR.
 */
public interface IVerificacionServicio {

    RespuestaVerificacion subir(Long idUsuarioSolicitante, TipoDocumentoVerificacion tipo, MultipartFile documento);

    List<RespuestaColaVerificacion> listarCola(String nombreEstado, int limite, int offset);

    RespuestaVerificacion obtenerPorId(Long idCertificado, Long idUsuarioSolicitante, boolean esRevisor);

    byte[] obtenerDocumento(Long idCertificado);

    RespuestaVerificacion analizarConIa(Long idCertificado);

    RespuestaVerificacion registrarDecision(Long idCertificado, Long idModerador, Long idNuevoEstado, String notaModerador);

    /**
     * Gating de "publicar un servicio" (Creador) y "crear un pedido" (Cliente):
     * ¿este usuario tiene una verificación de identidad (tipo IDENTIDAD) en
     * estado APROBADO? Se usa desde otros módulos, no solo desde este.
     */
    boolean estaIdentidadVerificada(Long idUsuario);

    /** Estado de identidad del propio usuario, para pintar el aviso en el frontend. */
    RespuestaEstadoIdentidad obtenerEstadoIdentidad(Long idUsuario);
}
