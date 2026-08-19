package uteq.edu.ec.artisync.service.perfil;

import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPerfil;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPerfil;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPerfil;

import java.util.List;

public interface IPerfilCreadorServicio {

    /**
     * Crea un perfil de creador. El {@code idUsuario} del cuerpo solo se respeta
     * si el solicitante es ADMIN; para el resto el perfil se crea siempre a
     * nombre del usuario autenticado, porque el @PreAuthorize del controlador
     * comprueba el rol pero no de quién es el recurso.
     */
    RespuestaPerfil crearPerfil(PeticionCrearPerfil peticion, String correoSolicitante, boolean esAdmin);

    RespuestaPerfil obtenerPerfilPorId(Long idPerfil);
    RespuestaPerfil obtenerPerfilPorUsuario(Long idUsuario);
    List<RespuestaPerfil> listarPerfiles();

    /**
     * Actualiza un perfil. Salvo que el solicitante sea ADMIN, debe ser el
     * propietario del perfil: el rol CREADOR por sí solo no autoriza a editar el
     * perfil de otro creador.
     */
    RespuestaPerfil actualizarPerfil(Long idPerfil, PeticionActualizarPerfil peticion, String correoSolicitante, boolean esAdmin);

    void eliminarPerfil(Long idPerfil);
}
