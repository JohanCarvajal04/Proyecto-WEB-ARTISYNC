package uteq.edu.ec.artisync.service.respaldo;

import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionActualizarProgramacion;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionCrearProgramacion;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.RespuestaProgramacion;

import java.util.List;

public interface IRespaldoProgramacionServicio {

    RespuestaProgramacion crear(PeticionCrearProgramacion peticion, String creadoPor);

    RespuestaProgramacion actualizar(Long idProgramacion, PeticionActualizarProgramacion peticion);

    RespuestaProgramacion cambiarEstado(Long idProgramacion, boolean activo);

    void eliminar(Long idProgramacion);

    List<RespuestaProgramacion> listar();

    RespuestaProgramacion obtenerPorId(Long idProgramacion);
}
