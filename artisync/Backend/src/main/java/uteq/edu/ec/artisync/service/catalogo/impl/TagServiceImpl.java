package uteq.edu.ec.artisync.service.catalogo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateTagRequest;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.TagResponse;
import uteq.edu.ec.artisync.entity.catalogo.Tag;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.TagRepository;
import uteq.edu.ec.artisync.service.catalogo.ITagService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements ITagService {

    private final TagRepository etiquetaRepository;

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<TagResponse> listTags() {
        return etiquetaRepository.findAll()
                .stream()
                .map(this::mapToTagResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idEtiqueta identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public TagResponse getById(Long idEtiqueta) {
        Tag et = etiquetaRepository.findById(idEtiqueta)
                .orElseThrow(() -> new ResourceNotFoundException("Tag no encontrada con ID: " + idEtiqueta));
        return mapToTagResponse(et);
    }

    @Override
    @Transactional
    @Auditable(accion = "ETIQUETA_CREAR", modulo = AuditModule.CATALOGO,
            entidad = "etiquetas", idEntidad = "#resultado.idEtiqueta",
            detalle = "{nombreEtiqueta: #peticion.nombreEtiqueta}")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public TagResponse createTag(CreateTagRequest peticion) {
        if (etiquetaRepository.existsByNombreEtiquetaIgnoreCase(peticion.getNombreEtiqueta())) {
            throw new BusinessRuleException("Ya existe la etiqueta: " + peticion.getNombreEtiqueta());
        }
        Tag et = Tag.builder()
                .nombreEtiqueta(peticion.getNombreEtiqueta().trim())
                .build();
        et = etiquetaRepository.save(et);
        return mapToTagResponse(et);
    }

    @Override
    @Transactional
    @Auditable(accion = "ETIQUETA_ELIMINAR", modulo = AuditModule.CATALOGO,
            entidad = "etiquetas", idEntidad = "#idEtiqueta")
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idEtiqueta identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void deleteTag(Long idEtiqueta) {
        if (!etiquetaRepository.existsById(idEtiqueta)) {
            throw new ResourceNotFoundException("Tag no encontrada con ID: " + idEtiqueta);
        }
        etiquetaRepository.deleteById(idEtiqueta);
    }

    private TagResponse mapToTagResponse(Tag et) {
        return TagResponse.builder()
                .idEtiqueta(et.getIdEtiqueta())
                .nombreEtiqueta(et.getNombreEtiqueta())
                .actualizadoEn(et.getActualizadoEn())
                .build();
    }
}
