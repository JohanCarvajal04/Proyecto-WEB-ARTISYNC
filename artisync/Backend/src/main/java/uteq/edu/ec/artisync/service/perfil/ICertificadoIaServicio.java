package uteq.edu.ec.artisync.service.perfil;

import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearCertificadoIa;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaCertificadoIa;

import java.util.List;

public interface ICertificadoIaServicio {

    /**
     * Emite un certificado de verificación por IA para un usuario.
     *
     * @param peticion id de usuario y estado de verificación inicial del certificado
     * @return el certificado recién emitido
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el usuario o el estado de verificación no existen
     */
    RespuestaCertificadoIa emitirCertificado(PeticionCrearCertificadoIa peticion);

    /**
     * Obtiene un certificado por su id.
     *
     * @param idCertificado id del certificado
     * @return el certificado encontrado
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el certificado no existe
     */
    RespuestaCertificadoIa obtenerCertificadoPorId(Long idCertificado);

    /**
     * Lista los certificados emitidos para un usuario.
     *
     * @param idUsuario id del usuario
     * @return los certificados del usuario
     */
    List<RespuestaCertificadoIa> listarCertificadosPorUsuario(Long idUsuario);

    /**
     * Lista todos los certificados emitidos en el sistema.
     *
     * @return todos los certificados
     */
    List<RespuestaCertificadoIa> listarTodosLosCertificados();

    /**
     * Elimina un certificado.
     *
     * @param idCertificado id del certificado a eliminar
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el certificado no existe
     */
    void eliminarCertificado(Long idCertificado);
}
