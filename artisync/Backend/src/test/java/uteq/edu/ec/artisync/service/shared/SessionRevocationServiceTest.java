package uteq.edu.ec.artisync.service.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uteq.edu.ec.artisync.repository.seguridad.SesionRevocadaProyeccion;
import uteq.edu.ec.artisync.repository.seguridad.SesionUsuarioRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
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
    private SesionUsuarioRepository sesionUsuarioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SessionRevocationService servicio;

    private SesionRevocadaProyeccion mockProyeccion(String jti, int segundosRestantes) {
        return new SesionRevocadaProyeccion() {
            @Override
            public String getJti() { return jti; }
            @Override
            public Integer getSegundosRestantes() { return segundosRestantes; }
        };
    }

    @Test
    void revocarSesionesUsuario_ok() {
        SesionRevocadaProyeccion proy = mockProyeccion("jti-1", 100);
        when(sesionUsuarioRepository.revocarSesionesUsuario(1L)).thenReturn(List.of(proy));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        servicio.revocarSesionesUsuario(1L);

        verify(valueOperations).set(eq("jti:jti-1"), eq("revocado"), eq(Duration.ofSeconds(100L)));
    }
    
    @Test
    void revocarSesionesUsuario_sinJti_ok() {
        SesionRevocadaProyeccion proy = mockProyeccion(null, 100);
        when(sesionUsuarioRepository.revocarSesionesUsuario(1L)).thenReturn(List.of(proy));

        servicio.revocarSesionesUsuario(1L); // No debería fallar ni llamar redis
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void cambiarEstadoCuenta_ok() {
        SesionRevocadaProyeccion proy = mockProyeccion("jti-2", 200);
        when(usuarioRepository.cambiarEstadoCuenta(1L, false)).thenReturn(List.of(proy));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        servicio.cambiarEstadoCuenta(1L, false);

        verify(valueOperations).set(eq("jti:jti-2"), eq("revocado"), eq(Duration.ofSeconds(200L)));
    }

    @Test
    void cambiarEstadoCuenta_error() {
        when(usuarioRepository.cambiarEstadoCuenta(1L, false)).thenThrow(new RuntimeException("SQL Error"));
        assertThrows(RuntimeException.class, () -> servicio.cambiarEstadoCuenta(1L, false));
    }

    @Test
    void revocarTokenPorCabecera_ok() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtService.extraerJti("token123")).thenReturn("jti-3");
        when(jwtService.extraerTiempoRestante("token123")).thenReturn(300000L); // 5 mins
        
        servicio.revocarTokenPorCabecera("Bearer token123");

        verify(valueOperations).set(eq("jti:jti-3"), eq("revocado"), eq(Duration.ofMillis(300000L)));
        verify(sesionUsuarioRepository).deleteByJti("jti-3");
    }

    @Test
    void revocarTokenPorCabecera_noBearer() {
        servicio.revocarTokenPorCabecera("Basic token123");
        verifyNoInteractions(jwtService);
        verifyNoInteractions(redisTemplate);
        verifyNoInteractions(sesionUsuarioRepository);
    }

    @Test
    void revocarToken_errorRedis() {
        when(jwtService.extraerJti("token123")).thenReturn("jti-3");
        when(jwtService.extraerTiempoRestante("token123")).thenReturn(300000L); 
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection error"));
        
        servicio.revocarToken("token123"); // shouldn't throw error
        verify(redisTemplate).opsForValue();
    }
    
    @Test
    void eliminarSesionPorToken_jtiNull() {
        when(jwtService.extraerJti("token123")).thenReturn(null);
        // No hay manera de llamar a eliminarSesionPorToken directamente, 
        // pero podemos pasar por revocarTokenPorCabecera con un redis ok.
        
        servicio.revocarTokenPorCabecera("Bearer token123");
        
        verify(sesionUsuarioRepository, never()).deleteByJti(any());
    }

    @Test
    void eliminarSesionPorToken_error() {
        when(jwtService.extraerJti("token123")).thenThrow(new RuntimeException("invalid token"));
        // Llamado a revocarToken falla en try/catch y luego eliminarSesionPorToken falla en su propio try/catch
        servicio.revocarTokenPorCabecera("Bearer token123");
        
        verify(sesionUsuarioRepository, never()).deleteByJti(any());
    }
}
