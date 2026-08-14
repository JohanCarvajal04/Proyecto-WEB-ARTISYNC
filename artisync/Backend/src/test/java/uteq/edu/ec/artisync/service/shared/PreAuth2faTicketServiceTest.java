package uteq.edu.ec.artisync.service.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreAuth2faTicketServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, String, String> hashOperations;

    private PreAuth2faTicketService service;

    @BeforeEach
    void setUp() {
        service = new PreAuth2faTicketService(redisTemplate);
    }

    @Test
    void emitir_ShouldStoreHashedTicketWithTtl() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);

        String ticket = service.emitir(1L, "juan@example.com");

        assertNotNull(ticket);
        assertFalse(ticket.isBlank());

        // La clave en Redis es el HASH del ticket, no el ticket en claro.
        verify(hashOperations).putAll(argThat(clave -> clave.startsWith("2fa:ticket:") && !clave.contains(ticket)), anyMap());
        verify(redisTemplate).expire(anyString(), eq(java.time.Duration.ofMinutes(5)));
    }

    @Test
    void emitir_ShouldNotStorePlaintextTicket() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);

        String ticket = service.emitir(1L, "juan@example.com");

        verify(hashOperations).putAll(argThat(clave -> !clave.contains(ticket)), anyMap());
    }

    @Test
    void resolver_ShouldReturnEmpty_WhenKeyAbsent() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(Map.of());

        Optional<PreAuth2faTicketService.DatosTicket> resultado = service.resolver("ticket-inexistente");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void resolver_ShouldReturnEmpty_WhenTicketBlank() {
        Optional<PreAuth2faTicketService.DatosTicket> resultado = service.resolver("");

        assertTrue(resultado.isEmpty());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void resolver_ShouldIncrementAttempts() {
        Map<String, String> almacenado = new HashMap<>();
        almacenado.put("idUsuario", "1");
        almacenado.put("correo", "juan@example.com");
        almacenado.put("intentos", "0");

        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(almacenado);
        when(hashOperations.increment(anyString(), eq("intentos"), eq(1L))).thenReturn(1L);

        Optional<PreAuth2faTicketService.DatosTicket> resultado = service.resolver("ticket-valido");

        assertTrue(resultado.isPresent());
        assertEquals(1L, resultado.get().idUsuario());
        assertEquals("juan@example.com", resultado.get().correo());
        verify(hashOperations).increment(anyString(), eq("intentos"), eq(1L));
    }

    @Test
    void resolver_ShouldDeleteTicketAndReturnEmpty_WhenAttemptsExceeded() {
        Map<String, String> almacenado = new HashMap<>();
        almacenado.put("idUsuario", "1");
        almacenado.put("correo", "juan@example.com");
        almacenado.put("intentos", "5");

        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(almacenado);
        when(hashOperations.increment(anyString(), eq("intentos"), eq(1L))).thenReturn(6L); // supera el tope de 5

        Optional<PreAuth2faTicketService.DatosTicket> resultado = service.resolver("ticket-con-muchos-intentos");

        assertTrue(resultado.isEmpty());
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void consumir_ShouldReturnTrue_OnFirstCall() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        assertTrue(service.consumir("ticket-valido"));
    }

    @Test
    void consumir_ShouldReturnFalse_OnSecondCall() {
        when(redisTemplate.delete(anyString())).thenReturn(false); // ya no existe: alguien más lo borró primero

        assertFalse(service.consumir("ticket-valido"));
    }

    @Test
    void emitir_ShouldThrowServiceUnavailable_WhenRedisFails() {
        when(redisTemplate.<String, String>opsForHash()).thenThrow(new QueryTimeoutException("Redis no disponible"));

        // Fail-closed: sin ticket no hay prueba de que la contraseña se validó.
        assertThrows(ResponseStatusException.class, () -> service.emitir(1L, "juan@example.com"));
    }

    @Test
    void resolver_ShouldThrowServiceUnavailable_WhenRedisFails() {
        when(redisTemplate.<String, String>opsForHash()).thenThrow(new QueryTimeoutException("Redis no disponible"));

        assertThrows(ResponseStatusException.class, () -> service.resolver("cualquier-ticket"));
    }
}
