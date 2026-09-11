package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.peticion.legal.PeticionActualizarPlantillaAcuerdoPropia;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionCrearPlantillaAcuerdoPropia;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPlantillaContrato;

import java.util.List;

/**
 * Autoservicio de plantillas de acuerdo propias del creador (V45).
 * Complementa, sin reemplazar, el catálogo general curado por ADMIN
 * ({@link IPlantillaContratoAdminServicio}): una plantilla creada aquí solo
 * la puede usar, editar o desactivar su propio dueño, y nunca puede
 * convertirse en la predeterminada del catálogo general.
 */
public interface IPlantillaAcuerdoCreadorServicio {

    /**
     * Crea una plantilla de acuerdo privada para el creador autenticado.
     *
     * @param idUsuarioCreador id del usuario dueño de la nueva plantilla
     * @param peticion nombre y texto legal de la plantilla
     * @return la plantilla recién creada
     */
    RespuestaPlantillaContrato crear(Long idUsuarioCreador, PeticionCrearPlantillaAcuerdoPropia peticion);

    /**
     * Edita una plantilla de acuerdo propia existente.
     *
     * @param idUsuarioCreador id del usuario autenticado
     * @param idPlantilla id de la plantilla a editar
     * @param peticion nuevos datos de la plantilla
     * @return la plantilla ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la plantilla no existe o no es propia de este creador
     */
    RespuestaPlantillaContrato editar(Long idUsuarioCreador, Long idPlantilla, PeticionActualizarPlantillaAcuerdoPropia peticion);

    /**
     * Lista las plantillas de acuerdo propias del creador autenticado, activas e inactivas.
     *
     * @param idUsuarioCreador id del usuario autenticado
     * @return sus plantillas propias
     */
    List<RespuestaPlantillaContrato> listarPropias(Long idUsuarioCreador);

    /**
     * Desactiva una plantilla de acuerdo propia.
     *
     * @param idUsuarioCreador id del usuario autenticado
     * @param idPlantilla id de la plantilla a desactivar
     * @return mensaje de confirmación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la plantilla no existe o no es propia de este creador
     */
    RespuestaMensaje desactivar(Long idUsuarioCreador, Long idPlantilla);
}
