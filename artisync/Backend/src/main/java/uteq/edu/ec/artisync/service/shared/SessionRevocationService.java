package uteq.edu.ec.artisync.service.shared;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.entity.seguridad.SesionUsuario;
import uteq.edu.ec.artisync.repository.seguridad.SesionUsuarioRepository;
import uteq.edu.ec.artisync.security.JwtService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRevocationService {

    private final SesionUsuarioRepository sesionUsuarioRepository;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void revocarSesionesUsuario(Long idUsuario) {
        List<SesionUsuario> sesiones = sesionUsuarioRepository.findByUsuarioIdUsuario(idUsuario);
        for (SesionUsuario sesion : sesiones) {
            // §2.5 (OBS-AUTO-06): se lee el jti guardado directamente, sin volver a
            // parsear un JWT (que ya no se almacena). Esto además corrige un bug
            // (F4): antes, si el token guardado ya había expirado, extraerJti()
            // lanzaba ExpiredJwtException y el catch genérico lo tragaba en
            // silencio — la sesión no quedaba revocada en Redis en absoluto.
            Duration tiempoRestante = Duration.between(LocalDateTime.now(), sesion.getFechaExpiracion());
            revocarJtiEnRedis(sesion.getJti(), tiempoRestante, idUsuario);
        }
        sesionUsuarioRepository.deleteByUsuarioIdUsuario(idUsuario);
    }

    @Transactional
    public void revocarTokenPorCabecera(String tokenHeader) {
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
            String token = tokenHeader.substring(7);
            revocarToken(token);
            eliminarSesionPorToken(token);
        }
    }

    public void revocarToken(String token) {
        try {
            String jti = jwtService.extraerJti(token);
            long tiempoRestanteMs = jwtService.extraerTiempoRestante(token);
            revocarJtiEnRedis(jti, Duration.ofMillis(tiempoRestanteMs), null);
        } catch (Exception e) {
            log.warn("Error revocando token en redis: {}", e.getMessage());
        }
    }

    private void eliminarSesionPorToken(String token) {
        try {
            String jti = jwtService.extraerJti(token);
            if (jti != null) {
                sesionUsuarioRepository.deleteByJti(jti);
            }
        } catch (Exception e) {
            log.debug("No se pudo extraer el jti del token para eliminar su sesión (probablemente expirado o inválido): {}", e.getMessage());
        }
    }

    private void revocarJtiEnRedis(String jti, Duration tiempoRestante, Long idUsuario) {
        try {
            if (jti != null && tiempoRestante != null && tiempoRestante.compareTo(Duration.ZERO) > 0) {
                redisTemplate.opsForValue().set("jti:" + jti, "revocado", tiempoRestante);
            }
        } catch (Exception e) {
            log.warn("Error revocando jti {} en redis para usuario {}: {}", jti, idUsuario, e.getMessage());
        }
    }
}
