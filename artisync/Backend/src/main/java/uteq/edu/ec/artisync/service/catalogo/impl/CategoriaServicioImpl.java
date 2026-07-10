package uteq.edu.ec.artisync.service.catalogo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaCategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaSubcategoria;
import uteq.edu.ec.artisync.entity.catalogo.Categoria;
import uteq.edu.ec.artisync.entity.catalogo.Subcategoria;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.CategoriaRepository;
import uteq.edu.ec.artisync.repository.catalogo.SubcategoriaRepository;
import uteq.edu.ec.artisync.service.catalogo.ICategoriaServicio;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaServicioImpl implements ICategoriaServicio {

    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaCategoria> listarCategoriasActivas() {
        return categoriaRepository.findByEstadoActivaTrueOrderByNombreCategoriaAsc()
                .stream()
                .map(this::mapearACategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaCategoria> listarTodasLasCategorias() {
        return categoriaRepository.findAllByOrderByNombreCategoriaAsc()
                .stream()
                .map(this::mapearACategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaCategoria obtenerCategoriaPorId(Long idCategoria) {
        Categoria cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + idCategoria));
        return mapearACategoriaRespuesta(cat);
    }

    @Override
    @Transactional
    public RespuestaCategoria crearCategoria(PeticionCrearCategoria peticion) {
        if (categoriaRepository.existsByNombreCategoriaIgnoreCase(peticion.getNombreCategoria())) {
            throw new ExcepcionReglaNegocio("Ya existe una categoria con el nombre: " + peticion.getNombreCategoria());
        }
        Categoria cat = Categoria.builder()
                .nombreCategoria(peticion.getNombreCategoria().trim())
                .estadoActiva(peticion.getEstadoActiva() != null ? peticion.getEstadoActiva() : true)
                .build();
        cat = categoriaRepository.save(cat);
        return mapearACategoriaRespuesta(cat);
    }

    @Override
    @Transactional
    public RespuestaCategoria actualizarCategoria(Long idCategoria, PeticionActualizarCategoria peticion) {
        Categoria cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + idCategoria));

        if (peticion.getNombreCategoria() != null && !peticion.getNombreCategoria().isBlank()) {
            if (!cat.getNombreCategoria().equalsIgnoreCase(peticion.getNombreCategoria()) &&
                    categoriaRepository.existsByNombreCategoriaIgnoreCase(peticion.getNombreCategoria())) {
                throw new ExcepcionReglaNegocio("Ya existe una categoria con el nombre: " + peticion.getNombreCategoria());
            }
            cat.setNombreCategoria(peticion.getNombreCategoria().trim());
        }
        if (peticion.getEstadoActiva() != null) {
            cat.setEstadoActiva(peticion.getEstadoActiva());
        }
        cat = categoriaRepository.save(cat);
        return mapearACategoriaRespuesta(cat);
    }

    @Override
    @Transactional
    public void eliminarCategoria(Long idCategoria) {
        if (!categoriaRepository.existsById(idCategoria)) {
            throw new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + idCategoria);
        }
        categoriaRepository.deleteById(idCategoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaSubcategoria> listarSubcategoriasPorCategoria(Long idCategoria) {
        if (!categoriaRepository.existsById(idCategoria)) {
            throw new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + idCategoria);
        }
        return subcategoriaRepository.findByCategoriaIdCategoriaOrderByNombreSubcategoriaAsc(idCategoria)
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaSubcategoria> listarTodasLasSubcategorias() {
        return subcategoriaRepository.findAllByOrderByNombreSubcategoriaAsc()
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RespuestaSubcategoria crearSubcategoria(PeticionCrearSubcategoria peticion) {
        Categoria cat = categoriaRepository.findById(peticion.getIdCategoria())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + peticion.getIdCategoria()));

        if (subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(
                cat.getIdCategoria(), peticion.getNombreSubcategoria())) {
            throw new ExcepcionReglaNegocio("Ya existe la subcategoria " + peticion.getNombreSubcategoria() + " en esta categoria");
        }

        Subcategoria sub = Subcategoria.builder()
                .categoria(cat)
                .nombreSubcategoria(peticion.getNombreSubcategoria().trim())
                .build();
        sub = subcategoriaRepository.save(sub);
        return mapearASubcategoriaRespuesta(sub);
    }

    @Override
    @Transactional
    public void eliminarSubcategoria(Long idSubcategoria) {
        if (!subcategoriaRepository.existsById(idSubcategoria)) {
            throw new ExcepcionRecursoNoEncontrado("Subcategoria no encontrada con ID: " + idSubcategoria);
        }
        subcategoriaRepository.deleteById(idSubcategoria);
    }

    private RespuestaCategoria mapearACategoriaRespuesta(Categoria cat) {
        return RespuestaCategoria.builder()
                .idCategoria(cat.getIdCategoria())
                .nombreCategoria(cat.getNombreCategoria())
                .estadoActiva(cat.getEstadoActiva())
                .actualizadoEn(cat.getActualizadoEn())
                .build();
    }

    private RespuestaSubcategoria mapearASubcategoriaRespuesta(Subcategoria sub) {
        return RespuestaSubcategoria.builder()
                .idSubcategoria(sub.getIdSubcategoria())
                .idCategoria(sub.getCategoria().getIdCategoria())
                .nombreCategoria(sub.getCategoria().getNombreCategoria())
                .nombreSubcategoria(sub.getNombreSubcategoria())
                .actualizadoEn(sub.getActualizadoEn())
                .build();
    }
}
