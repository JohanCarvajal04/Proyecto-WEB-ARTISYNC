package uteq.edu.ec.artisync.service.catalog.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.request.catalog.CreateTagRequest;
import uteq.edu.ec.artisync.dto.response.catalog.TagResponse;
import uteq.edu.ec.artisync.entity.catalog.Tag;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalog.TagRepository;
import uteq.edu.ec.artisync.service.catalog.ITagService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements ITagService {

    private final TagRepository etiquetaRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> listTags() {
        return etiquetaRepository.findAll()
                .stream()
                .map(this::mapToTagResponse)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public TagResponse getById(Long idEtiqueta) {
        Tag et = etiquetaRepository.findById(idEtiqueta)
                .orElseThrow(() -> new ResourceNotFoundException("Tag no encontrada con ID: " + idEtiqueta));
        return mapToTagResponse(et);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @Auditable(accion = "ETIQUETA_CREAR", modulo = AuditModule.CATALOGO,
            entidad = "etiquetas", idEntidad = "#resultado.idEtiqueta",
            detalle = "{nombreEtiqueta: #peticion.nombreEtiqueta}")
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    @Auditable(accion = "ETIQUETA_ELIMINAR", modulo = AuditModule.CATALOGO,
            entidad = "etiquetas", idEntidad = "#idEtiqueta")
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
