package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.peticion.legal.PeticionActualizarPlantillaContrato;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionCrearPlantillaContrato;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPlantillaContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPlantillaContratoResumen;

import java.util.List;

/**
 * Catálogo de plantillas de contrato, curado por ADMIN (REQ-F-017 ampliado):
 * el creador elige, entre estas plantillas, cuál aplica a cada uno de sus
 * servicios; no puede escribir texto legal libre.
 */
public interface IPlantillaContratoAdminServicio {

    RespuestaPlantillaContrato crear(PeticionCrearPlantillaContrato peticion);

    RespuestaPlantillaContrato editar(Long idPlantilla, PeticionActualizarPlantillaContrato peticion);

    List<RespuestaPlantillaContrato> listarTodas();

    RespuestaMensaje desactivar(Long idPlantilla);

    /** Para el selector del creador al crear/editar un servicio. */
    List<RespuestaPlantillaContratoResumen> listarActivas();
}
