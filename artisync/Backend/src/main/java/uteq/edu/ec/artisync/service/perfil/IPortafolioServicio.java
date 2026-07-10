package uteq.edu.ec.artisync.service.perfil;

import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolio;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPortafolio;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolio;

import java.util.List;

public interface IPortafolioServicio {
    RespuestaPortafolio crearPortafolio(PeticionCrearPortafolio peticion);
    RespuestaPortafolio obtenerPortafolioPorId(Long idPortafolio);
    RespuestaPortafolio obtenerPortafolioPorPerfil(Long idPerfil);
    List<RespuestaPortafolio> listarPortafolios();
    RespuestaPortafolio actualizarPortafolio(Long idPortafolio, PeticionActualizarPortafolio peticion);
    void incrementarVisitas(Long idPortafolio);
    void eliminarPortafolio(Long idPortafolio);
}
