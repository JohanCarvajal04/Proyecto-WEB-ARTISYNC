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

    @Test
    void deletePais_ShouldThrowNotFound_WhenPaisNoExiste() {
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> paisService.deletePais(99L));
    }

    @Test
    void getPaisById_ShouldReturnPais_WhenExists() {
        when(paisRepository.findById(1L)).thenReturn(Optional.of(pais));

        PaisResponse result = paisService.getPaisById(1L);

        assertEquals("Ecuador", result.getNombrePais());
    }

    @Test
    void getPaisById_ShouldThrowNotFound_WhenDoesNotExist() {
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> paisService.getPaisById(99L));
    }

    @Test
    void createPais_ShouldThrowNotFound_Never_ButTrimsName() {
        PaisRequest request = new PaisRequest("  Peru  ");
        Pais nuevo = Pais.builder().idPais(2L).nombrePais("Peru").build();
        when(paisRepository.findByNombrePais("Peru")).thenReturn(Optional.empty());
        when(paisRepository.save(any(Pais.class))).thenReturn(nuevo);

        PaisResponse result = paisService.createPais(request);

        assertEquals("Peru", result.getNombrePais());
    }

    @Test
    void updatePais_ShouldUpdateSuccessfully_WhenNoConflict() {
        PaisRequest request = new PaisRequest("Ecuador Nuevo");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(pais));
        when(paisRepository.findByNombrePais("Ecuador Nuevo")).thenReturn(Optional.empty());
        when(paisRepository.save(any(Pais.class))).thenReturn(pais);

        PaisResponse result = paisService.updatePais(1L, request);

        assertNotNull(result);
        assertEquals("Ecuador Nuevo", pais.getNombrePais());
    }

    @Test
    void updatePais_ShouldAllowSameName_WhenSamePais() {
        PaisRequest request = new PaisRequest("Ecuador");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(pais));
        when(paisRepository.findByNombrePais("Ecuador")).thenReturn(Optional.of(pais));
        when(paisRepository.save(any(Pais.class))).thenReturn(pais);

        assertDoesNotThrow(() -> paisService.updatePais(1L, request));
    }

    @Test
    void updatePais_ShouldThrowDuplicate_WhenNameBelongsToAnotherPais() {
        Pais otroPais = Pais.builder().idPais(2L).nombrePais("Peru").build();
        PaisRequest request = new PaisRequest("Peru");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(pais));
        when(paisRepository.findByNombrePais("Peru")).thenReturn(Optional.of(otroPais));

        assertThrows(ExcepcionRecursoDuplicado.class, () -> paisService.updatePais(1L, request));
    }

    @Test
    void updatePais_ShouldThrowNotFound_WhenPaisNoExiste() {
        PaisRequest request = new PaisRequest("Ecuador");
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> paisService.updatePais(99L, request));
    }
}

