package uteq.edu.ec.artisync.service.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uteq.edu.ec.artisync.exception.QuotaExceededException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** §2.2 / OBS-AUTO-06: cuota de intentos de autenticación por cuenta, con fail-open ante caída de Redis. */
@ExtendWith(MockitoExtension.class)
class AuthAttemptsServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthAttemptsService intentosAutenticacionService;

    @Test
    @DisplayName("verificarCuota — primer intento fija el TTL de la ventana")
    void verificarCuota_primerIntento_fijaVentana() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willReturn(1L);

        assertThatCode(() -> intentosAutenticacionService.verificarCuota("2fa-confirm", "ana@artisync.dev", 5, Duration.ofMinutes(15)))
                .doesNotThrowAnyException();

        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("verificarCuota — intentos dentro del limite, sin reiniciar la ventana")
    void verificarCuota_dentroDelLimite_noReiniciaVentana() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willReturn(3L);

        assertThatCode(() -> intentosAutenticacionService.verificarCuota("2fa-confirm", "ana@artisync.dev", 5, Duration.ofMinutes(15)))
                .doesNotThrowAnyException();

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("verificarCuota — supera el limite y lanza QuotaExceededException")
    void verificarCuota_superaLimite_lanzaExcepcion() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willReturn(6L);

        assertThatThrownBy(() -> intentosAutenticacionService.verificarCuota("2fa-confirm", "ana@artisync.dev", 5, Duration.ofMinutes(15)))
                .isInstanceOf(QuotaExceededException.class);
    }

    @Test
    @DisplayName("verificarCuota — Redis caido, se permite la solicitud (fail-open)")
    void verificarCuota_redisCaido_permiteFailOpen() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willThrow(new QueryTimeoutException("timeout"));

        assertThatCode(() -> intentosAutenticacionService.verificarCuota("2fa-confirm", "ana@artisync.dev", 5, Duration.ofMinutes(15)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("limpiar — elimina la clave del ambito/identificador")
    void limpiar_eliminaClave() {
        assertThatCode(() -> intentosAutenticacionService.limpiar("2fa-confirm", "ana@artisync.dev"))
                .doesNotThrowAnyException();

        verify(redisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("limpiar — Redis caido, no propaga la excepcion (fail-open)")
    void limpiar_redisCaido_noPropaga() {
        given(redisTemplate.delete(anyString())).willThrow(new QueryTimeoutException("timeout"));

        assertThatCode(() -> intentosAutenticacionService.limpiar("2fa-confirm", "ana@artisync.dev"))
                .doesNotThrowAnyException();
    }
}
