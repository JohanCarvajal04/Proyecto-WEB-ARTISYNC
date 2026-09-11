package uteq.edu.ec.artisync.controller.seguridad;
import uteq.edu.ec.artisync.controller.seguridad.*;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.seguridad.request.CountryRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.CountryResponse;
import uteq.edu.ec.artisync.service.seguridad.CountryService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryControllerTest {

    @Mock
    private CountryService paisService;

    @InjectMocks
    private CountryController paisController;

    @Test
    void getAllPaises_ShouldReturnOk() {
        when(paisService.getAllPaises()).thenReturn(List.of());
        ResponseEntity<List<CountryResponse>> result = paisController.getAllPaises();
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void createPais_ShouldReturnCreated() {
        CountryRequest request = new CountryRequest("Perú");
        CountryResponse response = new CountryResponse(2L, "Perú", true);
        when(paisService.createPais(request)).thenReturn(response);

        ResponseEntity<CountryResponse> result = paisController.createPais(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("Perú", result.getBody().getNombrePais());
    }

    @Test
    void deletePais_ShouldReturnOk() {
        when(paisService.deletePais(1L)).thenReturn(new RespuestaMensaje("Eliminado"));
        ResponseEntity<RespuestaMensaje> result = paisController.deletePais(1L);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}

