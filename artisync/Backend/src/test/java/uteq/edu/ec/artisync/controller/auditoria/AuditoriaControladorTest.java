package uteq.edu.ec.artisync.controller.auditoria;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.RespuestaEventoAuditoria;
import uteq.edu.ec.artisync.service.auditoria.IAuditoriaServicio;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaControladorTest {

    @Mock
    private IAuditoriaServicio auditoriaServicio;

    @InjectMocks
    private AuditoriaControlador auditoriaControlador;

    @Test
    void obtenerPorId_DebeRetornarEvento() {
        RespuestaEventoAuditoria evento = new RespuestaEventoAuditoria();
        evento.setIdEventoAuditoria(99L);
        when(auditoriaServicio.obtenerPorId(99L)).thenReturn(evento);

        ResponseEntity<RespuestaEventoAuditoria> result = auditoriaControlador.obtenerPorId(99L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(99L, result.getBody().getIdEventoAuditoria());
    }

    @Test
    void listarAcciones_DebeRetornarLista() {
        when(auditoriaServicio.listarAccionesDisponibles()).thenReturn(List.of("ACCION_1", "ACCION_2"));

        ResponseEntity<List<String>> result = auditoriaControlador.listarAcciones();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
    }
}
