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

    /**
     * Sube un documento de verificación (identidad o certificado) para su revisión posterior.
     * Nunca conserva el archivo original más allá de lo que exige el flujo (REQ-F-006).
     *
     * @param idUsuarioSolicitante id del usuario que sube el documento
     * @param tipo                 tipo de documento (identidad o certificado)
     * @param documento            archivo del documento a verificar
     * @return la verificación recién creada, en estado PENDIENTE
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya existe una verificación pendiente para el usuario, o si el estado PENDIENTE no está sembrado
     */
    RespuestaVerificacion subir(Long idUsuarioSolicitante, TipoDocumentoVerificacion tipo, MultipartFile documento);

    /**
     * Lista la cola de verificaciones para revisión, filtrada por estado y paginada.
     *
     * @param nombreEstado nombre del estado a filtrar, o {@code null} para todos
     * @param limite       cantidad máxima de resultados
     * @param offset       desplazamiento para paginación
     * @return las verificaciones que cumplen el filtro
     */
    List<RespuestaColaVerificacion> listarCola(String nombreEstado, int limite, int offset);

    /**
     * Obtiene el detalle de una verificación, restringido al dueño del documento o a un revisor.
     *
     * @param idCertificado        id de la verificación
     * @param idUsuarioSolicitante id del usuario que consulta
     * @param esRevisor            si el solicitante tiene permiso de revisión
     * @return el detalle de la verificación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la verificación no existe
     */
    RespuestaVerificacion obtenerPorId(Long idCertificado, Long idUsuarioSolicitante, boolean esRevisor);

    /**
     * Obtiene el contenido binario del documento subido para una verificación.
     *
     * @param idCertificado id de la verificación
     * @return los bytes del documento almacenado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la verificación no existe
     */
    byte[] obtenerDocumento(Long idCertificado);

    /**
     * Envía el documento de una verificación al servicio de IA para obtener un dictamen sugerido
     * (veredicto, confianza y datos extraídos), sin cambiar el estado de la verificación.
     *
     * @param idCertificado id de la verificación a analizar
     * @return la verificación con el dictamen de IA ya registrado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la verificación no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el documento ya fue eliminado
     * @throws uteq.edu.ec.artisync.exception.AiServiceUnavailableException si el servicio de IA falla y el fallo no es reintentable, o falla también en el reintento
     */
    RespuestaVerificacion analizarConIa(Long idCertificado);

    /**
     * Registra la decisión final de un moderador sobre una verificación. Es el único método
     * que puede cambiar el estado de verificación; para estados terminales (aprobado/rechazado)
     * el documento original se elimina del almacenamiento.
     *
     * @param idCertificado  id de la verificación
     * @param idModerador    id del moderador que decide
     * @param idNuevoEstado  id del nuevo estado de verificación
     * @param notaModerador  nota opcional del moderador
     * @return la verificación con la decisión ya registrada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la verificación no existe o el estado indicado no existe
     */
    RespuestaVerificacion registrarDecision(Long idCertificado, Long idModerador, Long idNuevoEstado, String notaModerador);

    /**
     * Gating de "publicar un servicio" (Creador) y "crear un pedido" (Cliente):
     * ¿este usuario tiene una verificación de identidad (tipo IDENTIDAD) en
     * estado APROBADO? Se usa desde otros módulos, no solo desde este.
     *
     * @param idUsuario id del usuario a consultar
     * @return {@code true} si el usuario tiene una verificación de identidad aprobada
     */
    boolean estaIdentidadVerificada(Long idUsuario);

    /**
     * Estado de identidad del propio usuario, para pintar el aviso en el frontend.
     *
     * @param idUsuario id del usuario a consultar
     * @return si está verificado y el estado de su última verificación de identidad
     */
    RespuestaEstadoIdentidad obtenerEstadoIdentidad(Long idUsuario);
}
