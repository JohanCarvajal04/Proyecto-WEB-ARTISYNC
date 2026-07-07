package uteq.edu.ec.artisync.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.request.PaisRequest;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.PaisResponse;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.entity.seguridad.Pais;
import uteq.edu.ec.artisync.repository.PaisRepository;
import uteq.edu.ec.artisync.repository.UsuarioRepository;
import uteq.edu.ec.artisync.service.PaisService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaisServiceImpl implements PaisService {

    private final PaisRepository paisRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PaisResponse> getAllPaises() {
        return paisRepository.findAll(Sort.by(Sort.Direction.ASC, "nombrePais")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaisResponse getPaisById(Long id) {
        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("País no encontrado con ID: " + id));
        return toResponse(pais);
    }

    @Override
    @Transactional
    public PaisResponse createPais(PaisRequest request) {
        String nombreTrimmed = request.getNombrePais().trim();
        if (paisRepository.findByNombrePais(nombreTrimmed).isPresent()) {
            throw new DuplicateResourceException("Ya existe un país registrado con el nombre: " + nombreTrimmed);
        }

        Pais pais = Pais.builder()
                .nombrePais(nombreTrimmed)
                .build();

        return toResponse(paisRepository.save(pais));
    }

    @Override
    @Transactional
    public PaisResponse updatePais(Long id, PaisRequest request) {
        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("País no encontrado con ID: " + id));

        String nombreTrimmed = request.getNombrePais().trim();
        paisRepository.findByNombrePais(nombreTrimmed).ifPresent(p -> {
            if (!p.getIdPais().equals(id)) {
                throw new DuplicateResourceException("Ya existe otro país registrado con el nombre: " + nombreTrimmed);
            }
        });

        pais.setNombrePais(nombreTrimmed);
        return toResponse(paisRepository.save(pais));
    }

    @Override
    @Transactional
    public MessageResponse deletePais(Long id) {
        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("País no encontrado con ID: " + id));

        if (usuarioRepository.existsByPaisIdPais(id)) {
            throw new BusinessRuleException("No se puede eliminar el país porque tiene usuarios asociados.");
        }

        paisRepository.delete(pais);
        return new MessageResponse("País eliminado exitosamente");
    }

    private PaisResponse toResponse(Pais pais) {
        return PaisResponse.builder()
                .idPais(pais.getIdPais())
                .nombrePais(pais.getNombrePais())
                .build();
    }
}
