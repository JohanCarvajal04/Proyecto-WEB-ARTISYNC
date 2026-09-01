package uteq.edu.ec.artisync.service.perfil;

import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolio;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPortafolio;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolio;

import java.util.List;

public interface IPortafolioServicio {
    RespuestaPortafolio crearPortafolio(PeticionCrearPortafolio peticion, Long idUsuarioLogueado);
    RespuestaPortafolio obtenerPortafolioPorId(Long idPortafolio);
    RespuestaPortafolio obtenerPortafolioPorPerfil(Long idPerfil);
    List<RespuestaPortafolio> listarPortafolios();
    RespuestaPortafolio actualizarPortafolio(Long idPortafolio, PeticionActualizarPortafolio peticion, Long idUsuarioLogueado);
    void incrementarVisitas(Long idPortafolio, Long idUsuario);
    void eliminarPortafolio(Long idPortafolio);
}
