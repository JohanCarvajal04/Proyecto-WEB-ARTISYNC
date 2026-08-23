package uteq.edu.ec.artisync.service.shared;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.repository.seguridad.SesionRevocadaProyeccion;
import uteq.edu.ec.artisync.repository.seguridad.SesionUsuarioRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.security.JwtService;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRevocationService {

    private final SesionUsuarioRepository sesionUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5) -
     * fn_revocar_sesiones_usuario lee y borra en el motor, en una sola
     * sentencia y un solo snapshot, las sesiones del usuario. Sustituye al
     * patrón anterior en tres pasos (findByUsuarioIdUsuario + revocar en Redis
     * + deleteByUsuarioIdUsuario), en el que una sesión creada entre el primer
     * y el último paso se borraba de la base sin haberse revocado nunca en
     * Redis (lectura no repetible: A6 del plan). La escritura en Redis
     * permanece aquí porque no participa de la transacción de PostgreSQL.
     */
    @Transactional
    public void revocarSesionesUsuario(Long idUsuario) {
        List<SesionRevocadaProyeccion> revocadas = sesionUsuarioRepository.revocarSesionesUsuario(idUsuario);
        for (SesionRevocadaProyeccion sesion : revocadas) {
            revocarJtiEnRedis(sesion.getJti(), Duration.ofSeconds(sesion.getSegundosRestantes()), idUsuario);
        }
    }

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5) -
     * fn_cambiar_estado_cuenta cambia estado_cuenta y, si hubo transicion
     * activa->inactiva, revoca las sesiones del usuario, todo bajo un unico
     * {@code SELECT ... FOR UPDATE} en el motor. Sustituye al patron
     * "leer estadoAnterior en Java -> comparar -> revocar aparte" que usaban
     * AdminUserServiceImpl.changeEstado/deleteUser/updateUser y
     * UserServiceImpl.deleteOwnAccount: sin el FOR UPDATE, dos administradores
     * concurrentes sobre el mismo usuario podian pisarse la decision de si
     * correspondia revocar sesiones (actualizacion perdida, A6 del plan).
     */
    @Transactional
    public void cambiarEstadoCuenta(Long idUsuario, boolean estado) {
        List<SesionRevocadaProyeccion> revocadas;
        try {
            revocadas = usuarioRepository.cambiarEstadoCuenta(idUsuario, estado);
        } catch (RuntimeException e) {
            throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.NOT_FOUND);
        }
        for (SesionRevocadaProyeccion sesion : revocadas) {
            revocarJtiEnRedis(sesion.getJti(), Duration.ofSeconds(sesion.getSegundosRestantes()), idUsuario);
        }
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
