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
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.entity.seguridad.Pais;
import uteq.edu.ec.artisync.repository.seguridad.PaisRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaisServiceImplTest {

    @Mock
    private PaisRepository paisRepository;

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

    // Fase 3 concurrencia: createPais/updatePais delegan en fn_guardar_pais,
    // que captura unique_violation sobre el nombre en vez de una comprobacion
    // findByNombrePais previa no atomica (A9). El tipo de excepcion de negocio
    // (ExcepcionRecursoDuplicado/NoEncontrado) se preserva vía traducirExcepcionDuplicado.
    @Test
    void createPais_ShouldThrowDuplicate_WhenNameExists() {
        PaisRequest request = new PaisRequest("Ecuador");
        when(paisRepository.guardarPais(isNull(), eq("Ecuador")))
                .thenThrow(excepcionSql("23505", "Ya existe un pais registrado con el nombre: Ecuador"));

        assertThrows(ExcepcionRecursoDuplicado.class, () -> paisService.createPais(request));
    }

    @Test
    void createPais_ShouldCreateSuccessfully() {
        PaisRequest request = new PaisRequest("Ecuador");
        when(paisRepository.guardarPais(isNull(), eq("Ecuador"))).thenReturn(1L);
        when(paisRepository.findById(1L)).thenReturn(Optional.of(pais));

        PaisResponse result = paisService.createPais(request);

        assertNotNull(result);
        assertEquals("Ecuador", result.getNombrePais());
    }

    /** Simula lo que Spring Data envuelve cuando fn_x lanza RAISE EXCEPTION ... USING ERRCODE = '...'. */
    private static RuntimeException excepcionSql(String sqlState, String mensaje) {
        return new RuntimeException(new java.sql.SQLException(mensaje, sqlState));
    }

    // deletePais es un interruptor: la baja ya no borra la fila, invierte el
    // estado. Por eso hay una prueba por sentido, y ninguna que compruebe la
    // antigua regla de "no se puede eliminar si tiene usuarios asociados":
    // esa restricción desapareció junto con el borrado físico.
    @Test
    void deletePais_ShouldDeactivate_WhenActive() {
        when(paisRepository.findById(1L)).thenReturn(Optional.of(pais));

        RespuestaMensaje response = paisService.deletePais(1L);

        assertEquals("País desactivado exitosamente", response.getMensaje());
        assertFalse(pais.getEstado());
        verify(paisRepository).save(pais);
        verify(paisRepository, never()).delete(any(Pais.class));
    }

    @Test
    void deletePais_ShouldReactivate_WhenInactive() {
        pais.setEstado(false);
        when(paisRepository.findById(1L)).thenReturn(Optional.of(pais));

        RespuestaMensaje response = paisService.deletePais(1L);

        assertEquals("País reactivado exitosamente", response.getMensaje());
        assertTrue(pais.getEstado());
        verify(paisRepository).save(pais);
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
        // El trim ahora ocurre dentro de fn_guardar_pais (btrim); este test
        // solo verifica que Java pase el nombre sin trimear (la funcion es la
        // responsable del recorte) y construya la respuesta desde lo guardado.
        PaisRequest request = new PaisRequest("  Peru  ");
        Pais nuevo = Pais.builder().idPais(2L).nombrePais("Peru").build();
        when(paisRepository.guardarPais(isNull(), eq("  Peru  "))).thenReturn(2L);
        when(paisRepository.findById(2L)).thenReturn(Optional.of(nuevo));

        PaisResponse result = paisService.createPais(request);

        assertEquals("Peru", result.getNombrePais());
    }

    @Test
    void updatePais_ShouldUpdateSuccessfully_WhenNoConflict() {
        Pais actualizado = Pais.builder().idPais(1L).nombrePais("Ecuador Nuevo").build();
        PaisRequest request = new PaisRequest("Ecuador Nuevo");
        when(paisRepository.existsById(1L)).thenReturn(true);
        when(paisRepository.guardarPais(1L, "Ecuador Nuevo")).thenReturn(1L);
        when(paisRepository.findById(1L)).thenReturn(Optional.of(actualizado));

        PaisResponse result = paisService.updatePais(1L, request);

        assertNotNull(result);
        assertEquals("Ecuador Nuevo", result.getNombrePais());
    }

    @Test
    void updatePais_ShouldAllowSameName_WhenSamePais() {
        // Renombrar un pais a su propio nombre actual no dispara la
        // restriccion UNIQUE (es la misma fila): fn_guardar_pais no lanza.
        PaisRequest request = new PaisRequest("Ecuador");
        when(paisRepository.existsById(1L)).thenReturn(true);
        when(paisRepository.guardarPais(1L, "Ecuador")).thenReturn(1L);
        when(paisRepository.findById(1L)).thenReturn(Optional.of(pais));

        assertDoesNotThrow(() -> paisService.updatePais(1L, request));
    }

    @Test
    void updatePais_ShouldThrowDuplicate_WhenNameBelongsToAnotherPais() {
        PaisRequest request = new PaisRequest("Peru");
        when(paisRepository.existsById(1L)).thenReturn(true);
        when(paisRepository.guardarPais(1L, "Peru"))
                .thenThrow(excepcionSql("23505", "Ya existe un pais registrado con el nombre: Peru"));

        assertThrows(ExcepcionRecursoDuplicado.class, () -> paisService.updatePais(1L, request));
    }

    @Test
    void updatePais_ShouldThrowNotFound_WhenPaisNoExiste() {
        PaisRequest request = new PaisRequest("Ecuador");
        when(paisRepository.existsById(99L)).thenReturn(false);

        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> paisService.updatePais(99L, request));
    }
}

