package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.legal.UpdateContractTemplateRequest;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateContractTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateSummaryResponse;
import uteq.edu.ec.artisync.entity.pedido.ContractTemplate;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.pedido.ContractTemplateRepository;
import uteq.edu.ec.artisync.service.legal.IContractTemplateAdminService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractTemplateAdminServiceImpl implements IContractTemplateAdminService {

    private final ContractTemplateRepository plantillaContratoRepository;

    @Override
    @Transactional
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ContractTemplateResponse create(CreateContractTemplateRequest peticion) {
        if (plantillaContratoRepository.findByVersionLegal(peticion.getVersionLegal()).isPresent()) {
            throw new BusinessRuleException("Ya existe una plantilla con la version legal '" + peticion.getVersionLegal() + "'");
        }

        if (peticion.isEsPredeterminada()) {
            limpiarPredeterminadaActual();
        }

        ContractTemplate plantilla = ContractTemplate.builder()
                .nombrePlantilla(peticion.getNombrePlantilla().trim())
                .versionLegal(peticion.getVersionLegal().trim())
                .cuerpoHtmlPlantilla(peticion.getCuerpoHtmlPlantilla())
                .esPredeterminada(peticion.isEsPredeterminada())
                .activa(true)
                .build();

        plantilla = plantillaContratoRepository.save(plantilla);
        log.info("Plantilla de contrato '{}' creada (id {})", plantilla.getNombrePlantilla(), plantilla.getIdPlantilla());
        return mapToRespuesta(plantilla);
    }

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idPlantilla identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ContractTemplateResponse update(Long idPlantilla, UpdateContractTemplateRequest peticion) {
        ContractTemplate plantilla = plantillaContratoRepository.findById(idPlantilla)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla de contrato no encontrada: " + idPlantilla));

        if (peticion.isEsPredeterminada() && !Boolean.TRUE.equals(plantilla.getEsPredeterminada())) {
            limpiarPredeterminadaActual();
        }
        if (!peticion.isEsPredeterminada() && Boolean.TRUE.equals(plantilla.getEsPredeterminada())) {
            throw new BusinessRuleException(
                    "Debe existir siempre una plantilla predeterminada; marca otra como predeterminada antes de quitarle esta condición");
        }

        plantilla.setNombrePlantilla(peticion.getNombrePlantilla().trim());
        plantilla.setVersionLegal(peticion.getVersionLegal().trim());
        plantilla.setCuerpoHtmlPlantilla(peticion.getCuerpoHtmlPlantilla());
        plantilla.setEsPredeterminada(peticion.isEsPredeterminada());
        plantilla.setActiva(peticion.isActiva());

        plantilla = plantillaContratoRepository.save(plantilla);
        log.info("Plantilla de contrato {} actualizada", idPlantilla);
        return mapToRespuesta(plantilla);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<ContractTemplateResponse> listAll() {
        return plantillaContratoRepository.findAll().stream()
                .map(this::mapToRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idPlantilla identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaMensaje deactivate(Long idPlantilla) {
        ContractTemplate plantilla = plantillaContratoRepository.findById(idPlantilla)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla de contrato no encontrada: " + idPlantilla));

        if (Boolean.TRUE.equals(plantilla.getEsPredeterminada())) {
            throw new BusinessRuleException(
                    "No se puede deactivate la plantilla predeterminada; marca otra como predeterminada primero");
        }

        plantilla.setActiva(false);
        plantillaContratoRepository.save(plantilla);
        log.info("Plantilla de contrato {} desactivada", idPlantilla);
        return new RespuestaMensaje("Plantilla desactivada correctamente");
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<ContractTemplateSummaryResponse> listActive() {
        return plantillaContratoRepository.findByActivaTrueOrderByNombrePlantillaAsc().stream()
                .map(p -> ContractTemplateSummaryResponse.builder()
                        .idPlantilla(p.getIdPlantilla())
                        .nombrePlantilla(p.getNombrePlantilla())
                        .esPropia(false)
                        .build())
                .collect(Collectors.toList());
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
    public List<ContractTemplateSummaryResponse> listActiveVisibleTo(Long idUsuarioCreador) {
        return plantillaContratoRepository.findActivasVisiblesParaCreador(idUsuarioCreador).stream()
                .map(p -> ContractTemplateSummaryResponse.builder()
                        .idPlantilla(p.getIdPlantilla())
                        .nombrePlantilla(p.getNombrePlantilla())
                        .esPropia(p.getIdCreador() != null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Flush obligatorio: ux_plantilla_contrato_predeterminada es un índice
     * único parcial (WHERE es_predeterminada). Sin forzar el UPDATE que
     * desmarca la plantilla vieja antes del INSERT/UPDATE de la nueva, ambas
     * filas podrían coexistir con es_predeterminada = true dentro del mismo
     * flush y violar la restricción.
     */
    private void limpiarPredeterminadaActual() {
        plantillaContratoRepository.findByEsPredeterminadaTrue().ifPresent(actual -> {
            actual.setEsPredeterminada(false);
            plantillaContratoRepository.save(actual);
            plantillaContratoRepository.flush();
        });
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
