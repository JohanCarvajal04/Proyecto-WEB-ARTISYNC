package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import uteq.edu.ec.artisync.dto.seguridad.request.PaisRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.PaisResponse;

import java.util.List;

public interface PaisService {

    /**
     * Lista todos los países, activos e inactivos.
     *
     * @return todos los países registrados
     */
    List<PaisResponse> getAllPaises();

    /**
     * Lista únicamente los países marcados como activos.
     *
     * @return los países activos
     */
    List<PaisResponse> getPaisesActivos();

    /**
     * Obtiene un país por su id.
     *
     * @param id id del país
     * @return el país encontrado
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el país no existe
     */
    PaisResponse getPaisById(Long id);

    /**
     * Crea un país nuevo.
     *
     * @param request nombre y demás datos del país a crear
     * @return el país recién creado
     */
    PaisResponse createPais(PaisRequest request);

    /**
     * Actualiza los datos de un país existente.
     *
     * @param id      id del país a actualizar
     * @param request nuevos datos del país
     * @return el país ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el país no existe
     */
    PaisResponse updatePais(Long id, PaisRequest request);

    /**
     * Invierte el estado activo/inactivo de un país (a pesar del nombre, no lo elimina).
     *
     * @param id id del país a cambiar de estado
     * @return mensaje indicando si el país quedó activado o desactivado
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el país no existe
     */
    RespuestaMensaje deletePais(Long id);
}

