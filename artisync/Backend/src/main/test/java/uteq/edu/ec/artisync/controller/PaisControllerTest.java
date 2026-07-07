package uteq.edu.ec.artisync.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.request.PaisRequest;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.PaisResponse;
import uteq.edu.ec.artisync.service.PaisService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaisControllerTest {

    @Mock
    private PaisService paisService;

    @InjectMocks
    private PaisController paisController;

    @Test
    void getAllPaises_ShouldReturnOk() {
        when(paisService.getAllPaises()).thenReturn(List.of());
        ResponseEntity<List<PaisResponse>> result = paisController.getAllPaises();
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void createPais_ShouldReturnCreated() {
        PaisRequest request = new PaisRequest("Perú");
        PaisResponse response = new PaisResponse(2L, "Perú");
        when(paisService.createPais(request)).thenReturn(response);

        ResponseEntity<PaisResponse> result = paisController.createPais(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("Perú", result.getBody().getNombrePais());
    }

    @Test
    void deletePais_ShouldReturnOk() {
        when(paisService.deletePais(1L)).thenReturn(new MessageResponse("Eliminado"));
        ResponseEntity<MessageResponse> result = paisController.deletePais(1L);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
