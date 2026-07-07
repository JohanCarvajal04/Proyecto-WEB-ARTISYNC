package uteq.edu.ec.artisync.service;

import uteq.edu.ec.artisync.dto.request.PaisRequest;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.PaisResponse;

import java.util.List;

public interface PaisService {
    List<PaisResponse> getAllPaises();
    PaisResponse getPaisById(Long id);
    PaisResponse createPais(PaisRequest request);
    PaisResponse updatePais(Long id, PaisRequest request);
    MessageResponse deletePais(Long id);
}
