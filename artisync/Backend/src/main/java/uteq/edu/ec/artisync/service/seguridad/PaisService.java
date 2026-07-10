package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import uteq.edu.ec.artisync.dto.seguridad.request.PaisRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.PaisResponse;

import java.util.List;

public interface PaisService {
    List<PaisResponse> getAllPaises();
    PaisResponse getPaisById(Long id);
    PaisResponse createPais(PaisRequest request);
    PaisResponse updatePais(Long id, PaisRequest request);
    RespuestaMensaje deletePais(Long id);
}

