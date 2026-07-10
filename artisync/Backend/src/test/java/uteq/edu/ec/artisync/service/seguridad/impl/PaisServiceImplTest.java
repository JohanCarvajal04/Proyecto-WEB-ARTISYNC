package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.controller.seguridad.*;
import uteq.edu.ec.artisync.service.seguridad.*;
import uteq.edu.ec.artisync.service.seguridad.impl.*;
import uteq.edu.ec.artisync.service.shared.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import uteq.edu.ec.artisync.dto.seguridad.request.PaisRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.PaisResponse;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.entity.seguridad.Pais;
import uteq.edu.ec.artisync.repository.seguridad.PaisRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaisServiceImplTest {

    @Mock
    private PaisRepository paisRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PaisServiceImpl paisService;

    private Pais pais;

    @BeforeEach
    void setUp() {
        pais = Pais.builder().idPais(1L).nombrePais("Ecuador").build();
    }

    @Test
    void getAllPaises_ShouldReturnList() {
        when(paisRepository.findAll(any(Sort.class))).thenReturn(List.of(pais));

        List<PaisResponse> result = paisService.getAllPaises();

        assertEquals(1, result.size());
        assertEquals("Ecuador", result.get(0).getNombrePais());
    }

    @Test
    void createPais_ShouldThrowDuplicate_WhenNameExists() {
        PaisRequest request = new PaisRequest("Ecuador");
        when(paisRepository.findByNombrePais("Ecuador")).thenReturn(Optional.of(pais));

        assertThrows(ExcepcionRecursoDuplicado.class, () -> paisService.createPais(request));
    }

    @Test
    void createPais_ShouldCreateSuccessfully() {
        PaisRequest request = new PaisRequest("Ecuador");
        when(paisRepository.findByNombrePais("Ecuador")).thenReturn(Optional.empty());
        when(paisRepository.save(any(Pais.class))).thenReturn(pais);

        PaisResponse result = paisService.createPais(request);

        assertNotNull(result);
        assertEquals("Ecuador", result.getNombrePais());
    }

    @Test
    void deletePais_ShouldThrowBusinessRule_WhenUsersAssociated() {
        when(paisRepository.findById(1L)).thenReturn(Optional.of(pais));
        when(usuarioRepository.existsByPaisIdPais(1L)).thenReturn(true);

        assertThrows(ExcepcionReglaNegocio.class, () -> paisService.deletePais(1L));
    }

    @Test
    void deletePais_ShouldDeleteSuccessfully() {
        when(paisRepository.findById(1L)).thenReturn(Optional.of(pais));
        when(usuarioRepository.existsByPaisIdPais(1L)).thenReturn(false);

        RespuestaMensaje response = paisService.deletePais(1L);

        assertEquals("País eliminado exitosamente", response.getMensaje());
        verify(paisRepository).delete(pais);
    }
}

