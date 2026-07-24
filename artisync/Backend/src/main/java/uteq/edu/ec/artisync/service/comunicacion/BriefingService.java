package uteq.edu.ec.artisync.service.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearBriefingPlantilla;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarBriefing;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionResponderBriefing;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaBriefing;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;

import java.util.List;

/**
 * Servicio de briefing interactivo.
 * RF-16: El Creador envía un formulario de hasta 10 preguntas al Cliente.
 * Las respuestas son inmutables una vez enviadas.
 */
public interface BriefingService {

    // --- Gestión de plantillas (CREADOR) ---
    RespuestaBriefing crearPlantilla(Long idPerfilCreador, PeticionCrearBriefingPlantilla peticion);
    List<RespuestaBriefing> obtenerMisPlantillas(Long idPerfilCreador);
    RespuestaBriefing editarPlantilla(Long idPlantilla, Long idPerfilCreador, PeticionCrearBriefingPlantilla peticion);
    RespuestaMensaje eliminarPlantilla(Long idPlantilla, Long idPerfilCreador);

    // --- Envío y respuesta ---
    /** Envía un briefing al cliente de un pedido. Solo el Creador del servicio puede hacerlo. */
    RespuestaBriefing enviarBriefing(Long idPedido, PeticionEnviarBriefing peticion, Long idCreador);

    /** Obtiene el briefing enviado a un pedido (con estado de respuestas). */
    RespuestaBriefing obtenerBriefing(Long idPedido);

    /**
     * El Cliente responde el briefing. Inmutable: lanza ExcepcionReglaNegocio si ya fue completado.
     */
    RespuestaBriefing responderBriefing(Long idPedido, PeticionResponderBriefing peticion, Long idCliente);
}
