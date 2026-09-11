package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionActualizarPlantillaAcuerdoPropia;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionCrearPlantillaAcuerdoPropia;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPlantillaContrato;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.pedido.PlantillaContratoRepository;
import uteq.edu.ec.artisync.service.legal.IPlantillaAcuerdoCreadorServicio;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Autoservicio de plantillas de acuerdo propias del creador (V45). No toca
 * {@link PlantillaContratoAdminServicioImpl}: el catálogo general de ADMIN
 * sigue intacto, esto es una vía adicional y aislada por id_creador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaAcuerdoCreadorServicioImpl implements IPlantillaAcuerdoCreadorServicio {

    private final PlantillaContratoRepository plantillaContratoRepository;

    @Override
    @Transactional
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param idUsuarioCreador identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaPlantillaContrato crear(Long idUsuarioCreador, PeticionCrearPlantillaAcuerdoPropia peticion) {
        PlantillaContrato plantilla = PlantillaContrato.builder()
                .nombrePlantilla(peticion.getNombrePlantilla().trim())
                // No es un dato de negocio para una plantilla privada (a diferencia
                // del catálogo general, donde el admin la declara a mano para
                // versionar cambios legales revisados) — se genera solo para
                // satisfacer la columna version_legal NOT NULL UNIQUE heredada de V13.
                .versionLegal(generarVersionLegalPropia(idUsuarioCreador))
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaPlantillaContrato editar(Long idUsuarioCreador, Long idPlantilla, PeticionActualizarPlantillaAcuerdoPropia peticion) {
        PlantillaContrato plantilla = obtenerPropiaOFallar(idUsuarioCreador, idPlantilla);

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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaPlantillaContrato> listarPropias(Long idUsuarioCreador) {
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaMensaje desactivar(Long idUsuarioCreador, Long idPlantilla) {
        PlantillaContrato plantilla = obtenerPropiaOFallar(idUsuarioCreador, idPlantilla);
        plantilla.setActiva(false);
        plantillaContratoRepository.save(plantilla);
        log.info("Plantilla de acuerdo propia {} desactivada por el usuario {}", idPlantilla, idUsuarioCreador);
        return new RespuestaMensaje("Plantilla desactivada correctamente");
    }

    /** Mismo criterio "no encontrado" (no "prohibido") que resolverBriefingPlantillaPropia/resolverFlujoPropio: no revela si el recurso existe a nombre de otro. */
    private PlantillaContrato obtenerPropiaOFallar(Long idUsuarioCreador, Long idPlantilla) {
        return plantillaContratoRepository.findByIdPlantillaAndIdCreador(idPlantilla, idUsuarioCreador)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Plantilla de acuerdo no encontrada: " + idPlantilla));
    }

    private String generarVersionLegalPropia(Long idUsuarioCreador) {
        return "propia-" + idUsuarioCreador + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private RespuestaPlantillaContrato mapToRespuesta(PlantillaContrato plantilla) {
        return RespuestaPlantillaContrato.builder()
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
