package uteq.edu.ec.artisync.service.catalogo;

import org.springframework.data.domain.Page;
import uteq.edu.ec.artisync.dto.peticion.catalogo.*;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.*;

import java.math.BigDecimal;
import java.util.List;

public interface IServicioCatalogoServicio {

    RespuestaServicio crearServicio(Long idPerfilCreador, PeticionCrearServicio peticion);

    RespuestaServicio actualizarServicio(Long idServicio, PeticionActualizarServicio peticion);

    RespuestaServicio obtenerServicioPorId(Long idServicio);

    void eliminarServicio(Long idServicio);

    List<RespuestaServicioResumido> listarServiciosPorCreador(Long idPerfilCreador, String estadoPublicacion);

    Page<RespuestaServicioResumido> buscarCatalogoServicios(
            Long categoriaId,
            Long subcategoriaId,
            BigDecimal precioMin,
            BigDecimal precioMax,
            List<Long> etiquetaIds,
            String textoBusqueda,
            String sort,
            int page,
            int size);

    List<RespuestaAtributo> listarAtributosPorServicio(Long idServicio);

    RespuestaAtributo agregarAtributo(Long idServicio, PeticionCrearAtributo peticion);

    RespuestaAtributo actualizarAtributo(Long idServicio, Long idAtributo, PeticionActualizarAtributo peticion);

    void eliminarAtributo(Long idServicio, Long idAtributo);
}
