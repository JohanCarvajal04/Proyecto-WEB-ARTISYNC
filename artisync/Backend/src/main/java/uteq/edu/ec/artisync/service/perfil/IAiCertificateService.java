package uteq.edu.ec.artisync.service.perfil;

import uteq.edu.ec.artisync.dto.peticion.perfil.CreateAiCertificateRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.AiCertificateResponse;

import java.util.List;

public interface IAiCertificateService {

    /**
     * Emite un certificado de verificación por IA para un usuario.
     *
     * @param peticion id de usuario y estado de verificación inicial del certificado
     * @return el certificado recién emitido
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario o el estado de verificación no existen
     */
    AiCertificateResponse issueCertificate(CreateAiCertificateRequest peticion);

    /**
     * Obtiene un certificado por su id.
     *
     * @param idCertificado id del certificado
     * @return el certificado encontrado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el certificado no existe
     */
    AiCertificateResponse getCertificateById(Long idCertificado);

    /**
     * Lista los certificados emitidos para un usuario.
     *
     * @param idUsuario id del usuario
     * @return los certificados del usuario
     */
    List<AiCertificateResponse> listCertificatesByUser(Long idUsuario);

    /**
     * Lista todos los certificados emitidos en el sistema.
     *
     * @return todos los certificados
     */
    List<AiCertificateResponse> listAllCertificates();

    /**
     * Elimina un certificado.
     *
     * @param idCertificado id del certificado a eliminar
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el certificado no existe
     */
    void deleteCertificate(Long idCertificado);
}
