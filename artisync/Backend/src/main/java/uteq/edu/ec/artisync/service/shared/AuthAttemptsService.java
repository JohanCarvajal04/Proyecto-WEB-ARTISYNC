package uteq.edu.ec.artisync.service.shared;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import uteq.edu.ec.artisync.exception.QuotaExceededException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/**
 * §2.2 / OBS-AUTO-06: cuota de intentos POR CUENTA (a diferencia de
 * AuthRateLimitFilter, que limita por IP). Necesaria porque un rate limit
 * solo por IP es un bucket compartido: cualquiera detrás del mismo NAT puede
 * agotarlo, y un atacante distribuido en varias IPs lo esquiva por completo.
 *
 * Fail-open ante caída de Redis, igual criterio que AuthRateLimitFilter: una
 * mitigación no debe bloquear el login completo del sistema.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAttemptsService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Incrementa el contador de (ambito, identificador) y lanza
     * {@link QuotaExceededException} (429, con Retry-After) si supera el
     * límite dentro de la ventana.
     *
     * En AuthServiceImpl.login() se invoca únicamente cuando la autenticación
     * ACABA de fallar (no antes de intentarla): así, un usuario que sabe su
     * contraseña nunca se ve afectado, y uno que se equivoca varias veces
     * antes de acertar puede seguir intentándolo mientras no supere el
     * límite. En forgotPassword() se invoca de forma incondicional al inicio,
     * porque ahí no hay noción de "fallo": cada llamada implica el mismo
     * costo de abuso (email potencialmente enviado) exista o no la cuenta.
     * @param ambito el ambito
     * @param identificador el identificador
     * @param limite el limite
     * @param ventana el ventana
     */
    public void checkQuota(String ambito, String identificador, int limite, Duration ventana) {
        String clave = buildKey(ambito, identificador);
        try {
            Long intentos = redisTemplate.opsForValue().increment(clave);
            if (intentos != null && intentos == 1L) {
                redisTemplate.expire(clave, ventana);
            }

            if (intentos != null && intentos > limite) {
                log.warn("Cuota de intentos por cuenta excedida para ámbito={}", ambito);
                throw new QuotaExceededException(
                        "Demasiados intentos. Intenta nuevamente en unos minutos.",
                        ventana.getSeconds());
            }
        } catch (DataAccessException e) {
            log.warn("No se pudo contactar a Redis para la cuota de {}; se permite la solicitud (fail-open): {}",
                    ambito, e.getMessage());
        }
    }

    /**
     * Limpia el contador de intentos de (ambito, identificador) — se invoca tras un
     * éxito de autenticación para que el conteo de fallos no se acumule entre sesiones.
     *
     * @param ambito espacio de la cuota (por ejemplo "login" o "forgotPassword")
     * @param identificador identificador de la cuenta cuyo contador se limpia (se hashea internamente)
     */
    public void clear(String ambito, String identificador) {
        try {
            redisTemplate.delete(buildKey(ambito, identificador));
        } catch (DataAccessException e) {
            log.warn("No se pudo limpiar la cuota de {} en Redis: {}", ambito, e.getMessage());
        }
    }

    /** Hashea el identificador (correo) para que un volcado de Redis no sea una lista de usuarios. */
    private String buildKey(String ambito, String identificador) {
        return "rl:" + ambito + ":" + hashSha256(identificador);
    }

    private String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar hash SHA-256", e);
        }
    }
}
