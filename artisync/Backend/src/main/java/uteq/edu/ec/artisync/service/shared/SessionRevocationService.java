package uteq.edu.ec.artisync.service.shared;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.repository.security.RevokedSessionProjection;
import uteq.edu.ec.artisync.repository.security.UserSessionRepository;
import uteq.edu.ec.artisync.repository.security.UserRepository;
import uteq.edu.ec.artisync.security.JwtService;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRevocationService {

    private final UserSessionRepository sesionUsuarioRepository;
    private final UserRepository usuarioRepository;
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
    public void revokeUserSessions(Long idUsuario) {
        List<RevokedSessionProjection> revocadas = sesionUsuarioRepository.revocarSesionesUsuario(idUsuario);
        for (RevokedSessionProjection sesion : revocadas) {
            revokeJtiInRedis(sesion.getJti(), Duration.ofSeconds(sesion.getSegundosRestantes()), idUsuario);
        }
    }

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5) -
     * fn_cambiar_estado_cuenta cambia estado_cuenta y, si hubo transicion
     * activa->inactiva, revoca las sesiones del usuario, todo bajo un unico
     * {@code SELECT ... FOR UPDATE} en el motor. Sustituye al patron
     * "leer estadoAnterior en Java -> comparar -> revocar aparte" que usaban
     * AdminUserServiceImpl.changeStatus/deleteUser/updateUser y
     * UserServiceImpl.deleteOwnAccount: sin el FOR UPDATE, dos administradores
     * concurrentes sobre el mismo usuario podian pisarse la decision de si
     * correspondia revocar sesiones (actualizacion perdida, A6 del plan).
     */
    @Transactional
    public void changeAccountStatus(Long idUsuario, boolean estado) {
        List<RevokedSessionProjection> revocadas;
        try {
            revocadas = usuarioRepository.cambiarEstadoCuenta(idUsuario, estado);
        } catch (RuntimeException e) {
            throw StoredProcedureExceptionTranslator.translate(e, HttpStatus.NOT_FOUND);
        }
        for (RevokedSessionProjection sesion : revocadas) {
            revokeJtiInRedis(sesion.getJti(), Duration.ofSeconds(sesion.getSegundosRestantes()), idUsuario);
        }
    }

    /**
     * Extrae el token JWT de una cabecera {@code Authorization: Bearer ...} (por
     * ejemplo, al cerrar sesión) y revoca tanto su entrada en Redis como su fila de
     * sesión en base de datos. Si la cabecera no trae el prefijo {@code "Bearer "},
     * no hace nada.
     *
     * @param tokenHeader valor de la cabecera HTTP {@code Authorization}
     */
    @Transactional
    public void revokeTokenFromHeader(String tokenHeader) {
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
            String token = tokenHeader.substring(7);
            revokeToken(token);
            deleteSessionByToken(token);
        }
    }

    /**
     * Marca el {@code jti} del token como revocado en Redis, con expiración igual al
     * tiempo de vida restante del propio token, de forma que la entrada de la
     * lista negra desaparezca sola cuando el token habría expirado de todos modos.
     * Cualquier error al extraer el {@code jti} o contactar Redis solo se registra
     * en el log, sin propagarse.
     *
     * @param token JWT en texto plano cuyo {@code jti} se revoca
     */
    public void revokeToken(String token) {
        try {
            String jti = jwtService.extractJti(token);
            long tiempoRestanteMs = jwtService.extractRemainingTime(token);
            revokeJtiInRedis(jti, Duration.ofMillis(tiempoRestanteMs), null);
        } catch (Exception e) {
            log.warn("Error revocando token en redis: {}", e.getMessage());
        }
    }

    private void deleteSessionByToken(String token) {
        try {
            String jti = jwtService.extractJti(token);
            if (jti != null) {
                sesionUsuarioRepository.deleteByJti(jti);
            }
        } catch (Exception e) {
            log.debug("No se pudo extraer el jti del token para eliminar su sesión (probablemente expirado o inválido): {}", e.getMessage());
        }
    }

    private void revokeJtiInRedis(String jti, Duration tiempoRestante, Long idUsuario) {
        try {
            if (jti != null && tiempoRestante != null && tiempoRestante.compareTo(Duration.ZERO) > 0) {
                redisTemplate.opsForValue().set("jti:" + jti, "revocado", tiempoRestante);
            }
        } catch (Exception e) {
            log.warn("Error revocando jti {} en redis para usuario {}: {}", jti, idUsuario, e.getMessage());
        }
    }
}
