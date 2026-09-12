package uteq.edu.ec.artisync.service.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uteq.edu.ec.artisync.repository.seguridad.RevokedSessionProjection;
import uteq.edu.ec.artisync.repository.seguridad.UserSessionRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.security.JwtService;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionRevocationServiceTest {

    @Mock
    private UserSessionRepository sesionUsuarioRepository;
    @Mock
    private UserRepository usuarioRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SessionRevocationService servicio;

    private RevokedSessionProjection mockProyeccion(String jti, int segundosRestantes) {
        return new RevokedSessionProjection() {
            @Override
            public String getJti() { return jti; }
            @Override
            public Integer getSegundosRestantes() { return segundosRestantes; }
        };
    }

    @Test
    void revokeUserSessions_ok() {
        RevokedSessionProjection proy = mockProyeccion("jti-1", 100);
        when(sesionUsuarioRepository.revocarSesionesUsuario(1L)).thenReturn(List.of(proy));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        servicio.revokeUserSessions(1L);

        verify(valueOperations).set(eq("jti:jti-1"), eq("revocado"), eq(Duration.ofSeconds(100L)));
    }

    @Test
    void revokeUserSessions_sinJti_ok() {
        RevokedSessionProjection proy = mockProyeccion(null, 100);
        when(sesionUsuarioRepository.revocarSesionesUsuario(1L)).thenReturn(List.of(proy));

        servicio.revokeUserSessions(1L); // No debería fallar ni llamar redis
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void changeAccountStatus_ok() {
        RevokedSessionProjection proy = mockProyeccion("jti-2", 200);
        when(usuarioRepository.cambiarEstadoCuenta(1L, false)).thenReturn(List.of(proy));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        servicio.changeAccountStatus(1L, false);

        verify(valueOperations).set(eq("jti:jti-2"), eq("revocado"), eq(Duration.ofSeconds(200L)));
    }

    @Test
    void changeAccountStatus_error() {
        when(usuarioRepository.cambiarEstadoCuenta(1L, false)).thenThrow(new RuntimeException("SQL Error"));
        assertThrows(RuntimeException.class, () -> servicio.changeAccountStatus(1L, false));
    }

    @Test
    void revokeTokenFromHeader_ok() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtService.extraerJti("token123")).thenReturn("jti-3");
        when(jwtService.extraerTiempoRestante("token123")).thenReturn(300000L); // 5 mins

        servicio.revokeTokenFromHeader("Bearer token123");

        verify(valueOperations).set(eq("jti:jti-3"), eq("revocado"), eq(Duration.ofMillis(300000L)));
        verify(sesionUsuarioRepository).deleteByJti("jti-3");
    }

    @Test
    void revokeTokenFromHeader_noBearer() {
        servicio.revokeTokenFromHeader("Basic token123");
        verifyNoInteractions(jwtService);
        verifyNoInteractions(redisTemplate);
        verifyNoInteractions(sesionUsuarioRepository);
    }

    @Test
    void revokeToken_errorRedis() {
        when(jwtService.extraerJti("token123")).thenReturn("jti-3");
        when(jwtService.extraerTiempoRestante("token123")).thenReturn(300000L);
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection error"));

        servicio.revokeToken("token123"); // shouldn't throw error
        verify(redisTemplate).opsForValue();
    }

    @Test
    void deleteSessionByToken_jtiNull() {
        when(jwtService.extraerJti("token123")).thenReturn(null);
        // No hay manera de llamar a deleteSessionByToken directamente,
        // pero podemos pasar por revokeTokenFromHeader con un redis ok.

        servicio.revokeTokenFromHeader("Bearer token123");

        verify(sesionUsuarioRepository, never()).deleteByJti(any());
    }

    @Test
    void deleteSessionByToken_error() {
        when(jwtService.extraerJti("token123")).thenThrow(new RuntimeException("invalid token"));
        // Llamado a revokeToken falla en try/catch y luego deleteSessionByToken falla en su propio try/catch
        servicio.revokeTokenFromHeader("Bearer token123");

        verify(sesionUsuarioRepository, never()).deleteByJti(any());
    }
}
