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
            revocarTokenEnRedis(sesion.getTokenJwt(), idUsuario);
        }
        sesionUsuarioRepository.deleteByUsuarioIdUsuario(idUsuario);
    }

    @Transactional
    public void revocarTokenPorCabecera(String tokenHeader) {
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
            String token = tokenHeader.substring(7);
            revocarTokenEnRedis(token, null);
            sesionUsuarioRepository.findByTokenJwt(token).ifPresent(sesionUsuarioRepository::delete);
        }
    }

    public void revocarToken(String token) {
        revocarTokenEnRedis(token, null);
    }

    private void revocarTokenEnRedis(String token, Long idUsuario) {
        try {
            String jti = jwtService.extraerJti(token);
            long tiempoRestante = jwtService.extraerTiempoRestante(token);
            if (jti != null && tiempoRestante > 0) {
                redisTemplate.opsForValue().set("jti:" + jti, "revocado", Duration.ofMillis(tiempoRestante));
            }
        } catch (Exception e) {
            log.warn("Error revocando token en redis para usuario {}: {}", idUsuario, e.getMessage());
        }
    }
}
