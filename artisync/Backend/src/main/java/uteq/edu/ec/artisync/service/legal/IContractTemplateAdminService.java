package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.peticion.legal.UpdateContractTemplateRequest;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateContractTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateSummaryResponse;

import java.util.List;

/**
 * Catálogo de plantillas de contrato, curado por ADMIN (REQ-F-017 ampliado):
 * el creador elige, entre estas plantillas, cuál aplica a cada uno de sus
 * servicios; no puede escribir texto legal libre.
 */
public interface IContractTemplateAdminService {

    /**
     * Crea una plantilla de contrato nueva.
     *
     * @param peticion texto legal y versión de la plantilla
     * @return la plantilla recién creada
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya existe una plantilla con la misma versión legal
     */
    ContractTemplateResponse crear(CreateContractTemplateRequest peticion);

    /**
     * Edita una plantilla de contrato existente.
     *
     * @param idPlantilla id de la plantilla a editar
     * @param peticion    nuevos datos de la plantilla
     * @return la plantilla ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la plantilla no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la nueva versión legal ya la usa otra plantilla
     */
    ContractTemplateResponse editar(Long idPlantilla, UpdateContractTemplateRequest peticion);

    /**
     * Lista todas las plantillas de contrato, activas e inactivas.
     *
     * @return todas las plantillas
     */
    List<ContractTemplateResponse> listarTodas();

    /**
     * Desactiva una plantilla de contrato, para que deje de estar disponible en el selector del creador.
     *
     * @param idPlantilla id de la plantilla a desactivar
     * @return mensaje de confirmación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la plantilla no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la plantilla ya está desactivada
     */
    RespuestaMensaje desactivar(Long idPlantilla);

    /**
     * Para el selector del creador al crear/editar un servicio: solo el
     * catálogo general curado por ADMIN, sin las plantillas privadas de
     * ningún creador (V45). Se conserva tal cual porque
     * {@link uteq.edu.ec.artisync.controller.legal.ContractTemplateController}
     * ya no la usa (ver {@link #listarActivasVisiblesPara}), pero la firma
     * pública no cambia sin necesidad.
     *
     * @return las plantillas activas del catálogo general, en formato resumido
     */
    List<ContractTemplateSummaryResponse> listarActivas();

    /**
     * Para el selector del creador al crear/editar un servicio (V45): el
     * catálogo general (ADMIN) más las plantillas privadas de ese creador,
     * nunca las de otro creador.
     *
     * @param idUsuarioCreador id del usuario autenticado que consulta
     * @return las plantillas activas visibles para ese creador, en formato resumido
     */
    List<ContractTemplateSummaryResponse> listarActivasVisiblesPara(Long idUsuarioCreador);
}
