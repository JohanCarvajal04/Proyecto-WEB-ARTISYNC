package uteq.edu.ec.artisync.service.catalogo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
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
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaEtiqueta> listarEtiquetas() {
        return etiquetaRepository.findAll()
                .stream()
                .map(this::mapearAEtiquetaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idEtiqueta identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaEtiqueta obtenerPorId(Long idEtiqueta) {
        Etiqueta et = etiquetaRepository.findById(idEtiqueta)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Etiqueta no encontrada con ID: " + idEtiqueta));
        return mapearAEtiquetaRespuesta(et);
    }

    @Override
    @Transactional
    @Auditable(accion = "ETIQUETA_CREAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "etiquetas", idEntidad = "#resultado.idEtiqueta",
            detalle = "{nombreEtiqueta: #peticion.nombreEtiqueta}")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
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
    @Auditable(accion = "ETIQUETA_ELIMINAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "etiquetas", idEntidad = "#idEtiqueta")
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idEtiqueta identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
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
