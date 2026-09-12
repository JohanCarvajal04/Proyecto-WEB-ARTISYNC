package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.peticion.legal.UpdateOwnAgreementTemplateRequest;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateOwnAgreementTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateResponse;

import java.util.List;

/**
 * Autoservicio de plantillas de acuerdo propias del creador (V45).
 * Complementa, sin reemplazar, el catálogo general curado por ADMIN
 * ({@link IContractTemplateAdminService}): una plantilla creada aquí solo
 * la puede usar, update o deactivate su propio dueño, y nunca puede
 * convertirse en la predeterminada del catálogo general.
 */
public interface ICreatorAgreementTemplateService {

    /**
     * Crea una plantilla de acuerdo privada para el creador autenticado.
     *
     * @param idUsuarioCreador id del usuario dueño de la nueva plantilla
     * @param peticion nombre y texto legal de la plantilla
     * @return la plantilla recién creada
     */
    ContractTemplateResponse create(Long idUsuarioCreador, CreateOwnAgreementTemplateRequest peticion);

    /**
     * Edita una plantilla de acuerdo propia existente.
     *
     * @param idUsuarioCreador id del usuario autenticado
     * @param idPlantilla id de la plantilla a update
     * @param peticion nuevos datos de la plantilla
     * @return la plantilla ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la plantilla no existe o no es propia de este creador
     */
    ContractTemplateResponse update(Long idUsuarioCreador, Long idPlantilla, UpdateOwnAgreementTemplateRequest peticion);

    /**
     * Lista las plantillas de acuerdo propias del creador autenticado, activas e inactivas.
     *
     * @param idUsuarioCreador id del usuario autenticado
     * @return sus plantillas propias
     */
    List<ContractTemplateResponse> listOwn(Long idUsuarioCreador);

    /**
     * Desactiva una plantilla de acuerdo propia.
     *
     * @param idUsuarioCreador id del usuario autenticado
     * @param idPlantilla id de la plantilla a deactivate
     * @return mensaje de confirmación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la plantilla no existe o no es propia de este creador
     */
    RespuestaMensaje deactivate(Long idUsuarioCreador, Long idPlantilla);
}
