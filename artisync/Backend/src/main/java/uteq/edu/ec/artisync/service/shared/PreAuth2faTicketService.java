package uteq.edu.ec.artisync.service.shared;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * §2.1 / OBS-AUTO-05: ticket de un solo uso que vincula el paso de contraseña
 * (AuthServiceImpl.login) con el de código 2FA (verify2Fa). Antes,
 * /api/v1/auth/2fa/verify aceptaba {correo, codigo} sin ninguna prueba de que el
 * llamante hubiera pasado por login() — habilitar 2FA degradaba la cuenta de
 * "contraseña" a "6 dígitos", fuerza-bruteables sin límite.
 *
 * Token OPACO (no un JWT): el uso único y el tope de intentos exigen estado
 * en Redis de todas formas, así que un JWT type=2fa_pending no aportaría nada
 * y sí una clase nueva de bug de confused-deputy (no puede confundirse con un
 * access token porque no es un JWT en absoluto).
 *
 * Fail-closed ante caída de Redis: sin ticket no hay prueba de que la
 * contraseña se validó (a diferencia del rate limit, que es fail-open).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreAuth2faTicketService {

    private static final String PREFIJO_CLAVE = "2fa:ticket:";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int INTENTOS_MAXIMOS = 5;
    private static final int LONGITUD_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public record DatosTicket(Long idUsuario, String correo) {
    }

    /** Se llama únicamente tras validar la contraseña en login(). */
    public String emitir(Long idUsuario, String correo) {
        byte[] bytes = new byte[LONGITUD_BYTES];
        secureRandom.nextBytes(bytes);
        String ticketPlano = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        String clave = construirClave(ticketPlano);
        try {
            HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
            Map<String, String> valor = Map.of(
                    "idUsuario", String.valueOf(idUsuario),
                    "correo", correo,
                    "intentos", "0");
            hashOps.putAll(clave, valor);
            redisTemplate.expire(clave, TTL);
        } catch (DataAccessException e) {
            log.error("No se pudo emitir el ticket pre-auth de 2FA en Redis (fail-closed): {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Servicio de autenticación temporalmente no disponible");
        }
        return ticketPlano;
    }

    /**
     * Lee los datos del ticket e incrementa su contador de intentos (tanto si el
     * código resulta correcto como si no — el llamante decide qué hacer con el
     * resultado). Si supera {@link #INTENTOS_MAXIMOS}, el ticket se invalida de
     * inmediato y hay que rehacer el login. Vacío si el ticket no existe, ya
     * expiró, o se acaba de invalidar por exceso de intentos.
     */
    public Optional<DatosTicket> resolver(String ticketPlano) {
        if (ticketPlano == null || ticketPlano.isBlank()) {
            return Optional.empty();
        }
        String clave = construirClave(ticketPlano);
        try {
            HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
            Map<String, String> valor = hashOps.entries(clave);
            if (valor.isEmpty()) {
                return Optional.empty();
            }

            Long intentos = hashOps.increment(clave, "intentos", 1);
            if (intentos != null && intentos > INTENTOS_MAXIMOS) {
                redisTemplate.delete(clave);
                log.warn("Ticket pre-auth de 2FA invalidado por exceso de intentos");
                return Optional.empty();
            }

            return Optional.of(new DatosTicket(Long.valueOf(valor.get("idUsuario")), valor.get("correo")));
        } catch (DataAccessException e) {
            log.error("No se pudo validar el ticket pre-auth de 2FA en Redis (fail-closed): {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Servicio de autenticación temporalmente no disponible");
        }
    }

    /**
     * Consume (borra) el ticket tras un código correcto — uso único, sin carrera
     * de doble envío: {@code delete} devuelve true solo para quien de verdad
     * borró la clave, así que solo esa petición puede continuar y emitir tokens.
     */
    public boolean consumir(String ticketPlano) {
        String clave = construirClave(ticketPlano);
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(clave));
        } catch (DataAccessException e) {
            log.error("No se pudo consumir el ticket pre-auth de 2FA en Redis: {}", e.getMessage());
            return false;
        }
    }

    private String construirClave(String ticketPlano) {
        return PREFIJO_CLAVE + hashSha256(ticketPlano);
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
