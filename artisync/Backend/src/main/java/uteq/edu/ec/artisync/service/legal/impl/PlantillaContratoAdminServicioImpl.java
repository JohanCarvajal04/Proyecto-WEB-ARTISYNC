package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionActualizarPlantillaContrato;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionCrearPlantillaContrato;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPlantillaContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPlantillaContratoResumen;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.pedido.PlantillaContratoRepository;
import uteq.edu.ec.artisync.service.legal.IPlantillaContratoAdminServicio;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaContratoAdminServicioImpl implements IPlantillaContratoAdminServicio {

    private final PlantillaContratoRepository plantillaContratoRepository;

    @Override
    @Transactional
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaPlantillaContrato crear(PeticionCrearPlantillaContrato peticion) {
        if (plantillaContratoRepository.findByVersionLegal(peticion.getVersionLegal()).isPresent()) {
            throw new ExcepcionReglaNegocio("Ya existe una plantilla con la version legal '" + peticion.getVersionLegal() + "'");
        }

        if (peticion.isEsPredeterminada()) {
            limpiarPredeterminadaActual();
        }

        PlantillaContrato plantilla = PlantillaContrato.builder()
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaPlantillaContrato editar(Long idPlantilla, PeticionActualizarPlantillaContrato peticion) {
        PlantillaContrato plantilla = plantillaContratoRepository.findById(idPlantilla)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Plantilla de contrato no encontrada: " + idPlantilla));

        if (peticion.isEsPredeterminada() && !Boolean.TRUE.equals(plantilla.getEsPredeterminada())) {
            limpiarPredeterminadaActual();
        }
        if (!peticion.isEsPredeterminada() && Boolean.TRUE.equals(plantilla.getEsPredeterminada())) {
            throw new ExcepcionReglaNegocio(
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaPlantillaContrato> listarTodas() {
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaMensaje desactivar(Long idPlantilla) {
        PlantillaContrato plantilla = plantillaContratoRepository.findById(idPlantilla)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Plantilla de contrato no encontrada: " + idPlantilla));

        if (Boolean.TRUE.equals(plantilla.getEsPredeterminada())) {
            throw new ExcepcionReglaNegocio(
                    "No se puede desactivar la plantilla predeterminada; marca otra como predeterminada primero");
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaPlantillaContratoResumen> listarActivas() {
        return plantillaContratoRepository.findByActivaTrueOrderByNombrePlantillaAsc().stream()
                .map(p -> RespuestaPlantillaContratoResumen.builder()
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaPlantillaContratoResumen> listarActivasVisiblesPara(Long idUsuarioCreador) {
        return plantillaContratoRepository.findActivasVisiblesParaCreador(idUsuarioCreador).stream()
                .map(p -> RespuestaPlantillaContratoResumen.builder()
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
