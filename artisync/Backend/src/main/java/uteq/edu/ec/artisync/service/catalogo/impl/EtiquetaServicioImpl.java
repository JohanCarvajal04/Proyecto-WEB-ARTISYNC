package uteq.edu.ec.artisync.service.catalogo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearEtiqueta;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaEtiqueta;
import uteq.edu.ec.artisync.entity.catalogo.Etiqueta;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.EtiquetaRepository;
import uteq.edu.ec.artisync.service.catalogo.IEtiquetaServicio;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EtiquetaServicioImpl implements IEtiquetaServicio {

    private final EtiquetaRepository etiquetaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaEtiqueta> listarEtiquetas() {
        return etiquetaRepository.findAll()
                .stream()
                .map(this::mapearAEtiquetaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaEtiqueta obtenerPorId(Long idEtiqueta) {
        Etiqueta et = etiquetaRepository.findById(idEtiqueta)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Etiqueta no encontrada con ID: " + idEtiqueta));
        return mapearAEtiquetaRespuesta(et);
    }

    @Override
    @Transactional
    public RespuestaEtiqueta crearEtiqueta(PeticionCrearEtiqueta peticion) {
        if (etiquetaRepository.existsByNombreEtiquetaIgnoreCase(peticion.getNombreEtiqueta())) {
            throw new ExcepcionReglaNegocio("Ya existe la etiqueta: " + peticion.getNombreEtiqueta());
        }
        Etiqueta et = Etiqueta.builder()
                .nombreEtiqueta(peticion.getNombreEtiqueta().trim())
                .build();
        et = etiquetaRepository.save(et);
        return mapearAEtiquetaRespuesta(et);
    }

    @Override
    @Transactional
    public void eliminarEtiqueta(Long idEtiqueta) {
        if (!etiquetaRepository.existsById(idEtiqueta)) {
            throw new ExcepcionRecursoNoEncontrado("Etiqueta no encontrada con ID: " + idEtiqueta);
        }
        etiquetaRepository.deleteById(idEtiqueta);
    }

    private RespuestaEtiqueta mapearAEtiquetaRespuesta(Etiqueta et) {
        return RespuestaEtiqueta.builder()
                .idEtiqueta(et.getIdEtiqueta())
                .nombreEtiqueta(et.getNombreEtiqueta())
                .actualizadoEn(et.getActualizadoEn())
                .build();
    }
}
