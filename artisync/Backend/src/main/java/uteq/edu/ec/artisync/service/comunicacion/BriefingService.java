package uteq.edu.ec.artisync.service.comunicacion;

import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearBriefingPlantilla;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaBriefing;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;

import java.util.List;

/**
 * Servicio de briefing interactivo.
 * REQ-F-016 ampliado: el cuestionario se asigna a un servicio
 * (Servicio.briefingPlantilla) y el Cliente lo responde al crear el pedido
 * (ver PedidoServicioImpl.crearPedido), no con un envío manual posterior del
 * Creador. Las respuestas son inmutables una vez creado el pedido.
 */
public interface BriefingService {

    // --- Gestión de plantillas (CREADOR) ---
    // idUsuario: el del JWT del creador autenticado (CustomUserDetails), no el
    // id_perfil — la implementación resuelve el PerfilCreador correspondiente.
    RespuestaBriefing crearPlantilla(Long idUsuario, PeticionCrearBriefingPlantilla peticion);
    List<RespuestaBriefing> obtenerMisPlantillas(Long idUsuario);
    RespuestaBriefing editarPlantilla(Long idPlantilla, Long idUsuario, PeticionCrearBriefingPlantilla peticion);
    RespuestaMensaje eliminarPlantilla(Long idPlantilla, Long idUsuario);

    /**
     * Obtiene el briefing respondido de un pedido (solo lectura). Solo el
     * cliente/creador del pedido o un ADMIN pueden consultarlo.
     */
    RespuestaBriefing obtenerBriefing(Long idPedido, Long idUsuarioSolicitante);
}
