package uteq.edu.ec.artisync.controller.auditoria;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.AuditEventResponse;
import uteq.edu.ec.artisync.service.auditoria.IAuditService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private IAuditService auditoriaServicio;

    @InjectMocks
    private AuditController auditoriaControlador;

    @Test
    void obtenerPorId_DebeRetornarEvento() {
        AuditEventResponse evento = new AuditEventResponse();
        evento.setIdEventoAuditoria(99L);
        when(auditoriaServicio.getById(99L)).thenReturn(evento);

        ResponseEntity<AuditEventResponse> result = auditoriaControlador.getById(99L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(99L, result.getBody().getIdEventoAuditoria());
    }

    @Test
    void listarAcciones_DebeRetornarLista() {
        when(auditoriaServicio.listAvailableActions()).thenReturn(List.of("ACCION_1", "ACCION_2"));

        ResponseEntity<List<String>> result = auditoriaControlador.listActions();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
    }
}
