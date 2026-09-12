package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.legal.UpdateOwnAgreementTemplateRequest;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateOwnAgreementTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateResponse;
import uteq.edu.ec.artisync.entity.pedido.ContractTemplate;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.pedido.ContractTemplateRepository;
import uteq.edu.ec.artisync.service.legal.ICreatorAgreementTemplateService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Autoservicio de plantillas de acuerdo propias del creador (V45). No toca
 * {@link ContractTemplateAdminServiceImpl}: el catálogo general de ADMIN
 * sigue intacto, esto es una vía adicional y aislada por id_creador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorAgreementTemplateServiceImpl implements ICreatorAgreementTemplateService {

    private final ContractTemplateRepository plantillaContratoRepository;

    @Override
    @Transactional
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param idUsuarioCreador identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ContractTemplateResponse create(Long idUsuarioCreador, CreateOwnAgreementTemplateRequest peticion) {
        ContractTemplate plantilla = ContractTemplate.builder()
                .nombrePlantilla(peticion.getNombrePlantilla().trim())
                // No es un dato de negocio para una plantilla privada (a diferencia
                // del catálogo general, donde el admin la declara a mano para
                // versionar cambios legales revisados) — se genera solo para
                // satisfacer la columna version_legal NOT NULL UNIQUE heredada de V13.
                .versionLegal(generateOwnLegalVersion(idUsuarioCreador))
                .cuerpoHtmlPlantilla(peticion.getCuerpoHtmlPlantilla())
                .esPredeterminada(false)
                .activa(true)
                .idCreador(idUsuarioCreador)
                .build();

        plantilla = plantillaContratoRepository.save(plantilla);
        log.info("Plantilla de acuerdo propia '{}' creada (id {}) por el usuario {}",
                plantilla.getNombrePlantilla(), plantilla.getIdPlantilla(), idUsuarioCreador);
        return mapToRespuesta(plantilla);
    }

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idUsuarioCreador identificador unico que referencia de manera univoca al registro
     * @param idPlantilla identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ContractTemplateResponse update(Long idUsuarioCreador, Long idPlantilla, UpdateOwnAgreementTemplateRequest peticion) {
        ContractTemplate plantilla = getOwnOrFail(idUsuarioCreador, idPlantilla);

        plantilla.setNombrePlantilla(peticion.getNombrePlantilla().trim());
        plantilla.setCuerpoHtmlPlantilla(peticion.getCuerpoHtmlPlantilla());
        plantilla.setActiva(peticion.isActiva());

        plantilla = plantillaContratoRepository.save(plantilla);
        log.info("Plantilla de acuerdo propia {} actualizada por el usuario {}", idPlantilla, idUsuarioCreador);
        return mapToRespuesta(plantilla);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idUsuarioCreador identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<ContractTemplateResponse> listOwn(Long idUsuarioCreador) {
        return plantillaContratoRepository.findByIdCreadorOrderByNombrePlantillaAsc(idUsuarioCreador).stream()
                .map(this::mapToRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idUsuarioCreador identificador unico que referencia de manera univoca al registro
     * @param idPlantilla identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaMensaje deactivate(Long idUsuarioCreador, Long idPlantilla) {
        ContractTemplate plantilla = getOwnOrFail(idUsuarioCreador, idPlantilla);
        plantilla.setActiva(false);
        plantillaContratoRepository.save(plantilla);
        log.info("Plantilla de acuerdo propia {} desactivada por el usuario {}", idPlantilla, idUsuarioCreador);
        return new RespuestaMensaje("Plantilla desactivada correctamente");
    }

    /** Mismo criterio "no encontrado" (no "prohibido") que resolverBriefingPlantillaPropia/resolverFlujoPropio: no revela si el recurso existe a nombre de otro. */
    private ContractTemplate getOwnOrFail(Long idUsuarioCreador, Long idPlantilla) {
        return plantillaContratoRepository.findByIdPlantillaAndIdCreador(idPlantilla, idUsuarioCreador)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla de acuerdo no encontrada: " + idPlantilla));
    }

    private String generateOwnLegalVersion(Long idUsuarioCreador) {
        return "propia-" + idUsuarioCreador + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private ContractTemplateResponse mapToRespuesta(ContractTemplate plantilla) {
        return ContractTemplateResponse.builder()
                .idPlantilla(plantilla.getIdPlantilla())
                .nombrePlantilla(plantilla.getNombrePlantilla())
                .versionLegal(plantilla.getVersionLegal())
                .cuerpoHtmlPlantilla(plantilla.getCuerpoHtmlPlantilla())
                .esPredeterminada(plantilla.getEsPredeterminada())
                .activa(plantilla.getActiva())
                .idCreador(plantilla.getIdCreador())
                .build();
    }
}
