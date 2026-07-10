package uteq.edu.ec.artisync.service.perfil;

import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPerfil;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPerfil;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPerfil;

import java.util.List;

public interface IPerfilCreadorServicio {
    RespuestaPerfil crearPerfil(PeticionCrearPerfil peticion);
    RespuestaPerfil obtenerPerfilPorId(Long idPerfil);
    RespuestaPerfil obtenerPerfilPorUsuario(Long idUsuario);
    List<RespuestaPerfil> listarPerfiles();
    RespuestaPerfil actualizarPerfil(Long idPerfil, PeticionActualizarPerfil peticion);
    void eliminarPerfil(Long idPerfil);
}
