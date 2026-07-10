package uteq.edu.ec.artisync.service.perfil;

import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearCertificadoIa;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaCertificadoIa;

import java.util.List;

public interface ICertificadoIaServicio {
    RespuestaCertificadoIa emitirCertificado(PeticionCrearCertificadoIa peticion);
    RespuestaCertificadoIa obtenerCertificadoPorId(Long idCertificado);
    List<RespuestaCertificadoIa> listarCertificadosPorPerfil(Long idPerfil);
    List<RespuestaCertificadoIa> listarTodosLosCertificados();
    RespuestaCertificadoIa actualizarEstadoVerificacion(Long idCertificado, Long idNuevoEstado);
    void eliminarCertificado(Long idCertificado);
}
