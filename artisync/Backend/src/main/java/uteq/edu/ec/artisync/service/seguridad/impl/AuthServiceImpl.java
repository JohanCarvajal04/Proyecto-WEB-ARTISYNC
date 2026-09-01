package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.service.seguridad.*;

import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.TokenResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.*;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.pedido.*;
import uteq.edu.ec.artisync.repository.legal.*;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.social.*;
import uteq.edu.ec.artisync.security.ClientIpResolver;
import uteq.edu.ec.artisync.security.CustomUserDetailsService;
import uteq.edu.ec.artisync.security.JwtService;
import uteq.edu.ec.artisync.service.seguridad.AuthService;
import uteq.edu.ec.artisync.service.seguridad.TwoFactorService;
import uteq.edu.ec.artisync.service.shared.EmailService;
import uteq.edu.ec.artisync.service.shared.IntentosAutenticacionService;
import uteq.edu.ec.artisync.service.shared.PreAuth2faTicketService;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.StoredProcedureExceptionTranslator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // §2.2 (OBS-AUTO-06): cuota por CUENTA, complementaria a la cuota por IP de
    // AuthRateLimitFilter. Ventana acotada a 15 min a propósito: un contador por
    // cuenta es, en sí mismo, un vector de bloqueo dirigido contra una cuenta
    // conocida (p. ej. un admin); una ventana corta limita ese daño. No existe
    // endpoint de desbloqueo — ver runbook: `redis-cli DEL rl:login-cuenta:<hash>`.
    private static final String AMBITO_LOGIN = "login-cuenta";
    private static final int LIMITE_INTENTOS_LOGIN = 5;
    private static final Duration VENTANA_INTENTOS_LOGIN = Duration.ofMinutes(15);

    private static final String AMBITO_RECUPERACION = "recuperacion-cuenta";
    private static final int LIMITE_INTENTOS_RECUPERACION = 3;
    private static final Duration VENTANA_INTENTOS_RECUPERACION = Duration.ofHours(1);

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final SesionUsuarioRepository sesionUsuarioRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SessionRevocationService sessionRevocationService;
    private final EmailService emailService;
    private final TwoFactorService twoFactorService;
    private final IntentosAutenticacionService intentosAutenticacionService;
    private final PreAuth2faTicketService preAuth2faTicketService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @Auditable(accion = "Usuario registrado desde el panel de crear cuenta", modulo = ModuloAuditoria.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#resultado.idUsuario",
            correoActor = "#request.correo",
            detalle = "{rol: #request.rol}")
    public UserResponse register(RegisterRequest request) {
        String rolNombre = request.getRol() != null && !request.getRol().isBlank()
                ? request.getRol().toUpperCase() : "CLIENTE";

        // REQ-F-001: fn_registrar_usuario valida correo unico, mayoria de edad
        // (RNF-12) y rol permitido, e inserta usuario + usuario_roles + perfil
        // de creador opcional en una unica transaccion atomica en el motor.
        Long idUsuario;
        try {
            idUsuario = usuarioRepository.registrarUsuario(
                    request.getNombres(),
                    request.getApellidos(),
                    request.getCorreo(),
                    passwordEncoder.encode(request.getContrasena()),
                    request.getFechaNacimiento(),
                    rolNombre);
        } catch (RuntimeException e) {
            throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.BAD_REQUEST);
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al registrar el usuario"));

        return UserResponse.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .correo(usuario.getCorreo())
                .fechaNacimiento(usuario.getFechaNacimiento())
                .fechaRegistro(usuario.getFechaRegistro())
                .estadoCuenta(usuario.getEstadoCuenta())
                .roles(List.of(rolNombre))
                .dosFactoresHabilitado(false)
                .build();
    }

    @Override
    @Transactional
    @Auditable(accion = "AUTENTICACION_LOGIN", modulo = ModuloAuditoria.SEGURIDAD,
            correoActor = "#request.correo")
    public TokenResponse login(LoginRequest request) {
        String ip = obtenerIpActual();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena())
            );
        } catch (AuthenticationException e) {
            log.warn("evento=LOGIN resultado=FALLIDO correo={} ip={}", request.getCorreo(), ip);
            // Cuota por CUENTA: se incrementa únicamente cuando la autenticación
            // ACABA de fallar (no antes de intentarla), para que un usuario que
            // conoce su contraseña nunca se vea afectado — solo los fallos cuentan.
            intentosAutenticacionService.verificarCuota(
                    AMBITO_LOGIN, request.getCorreo(), LIMITE_INTENTOS_LOGIN, VENTANA_INTENTOS_LOGIN);
            throw e;
        }

        // Autenticación correcta: limpiar cualquier cuota acumulada por fallos previos.
        intentosAutenticacionService.limpiar(AMBITO_LOGIN, request.getCorreo());

        // REQ-F-002: fn_resolver_estado_login resuelve en una sola llamada el
        // estado de cuenta, el flag de 2FA y los roles (join usuario_roles-roles),
        // en vez de tres consultas separadas.
        String estadoLoginJson = usuarioRepository.resolverEstadoLogin(request.getCorreo());
        if (estadoLoginJson == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        JsonNode estado = parseEstadoLogin(estadoLoginJson);
        Long idUsuario = estado.get("idUsuario").asLong();
        String correoUsuario = estado.get("correo").asText();
        boolean dosFactoresHabilitado = estado.get("dosFactoresHabilitado").asBoolean();

        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);

        if (dosFactoresHabilitado) {
            log.info("evento=LOGIN resultado=PENDIENTE_2FA correo={} ip={} sub={}", correoUsuario, ip, idUsuario);
            // §2.1 (OBS-AUTO-05): ticket de un solo uso que prueba que la
            // contraseña ya se validó — AuthController lo mueve a una cookie
            // HttpOnly. Sin esto, verify2Fa() no tenía forma de saber si el
            // llamante había pasado por aquí.
            String ticket = preAuth2faTicketService.emitir(idUsuario, correoUsuario);
            return TokenResponse.builder()
                    .correo(correoUsuario)
                    .idUsuario(idUsuario)
                    .requiere2fa(true)
                    .preAuthTicket(ticket)
                    .build();
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(correoUsuario);
        String accessToken = jwtService.generarToken(userDetails);
        String refreshToken = jwtService.generarRefreshToken(userDetails);

        List<String> roles = extraerRoles(estado);

        List<String> permisos = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();

        registrarSesionMejorEsfuerzo(usuario, jwtService.extraerJti(accessToken), jwtService.getExpirationMs());
        registrarSesionObligatoria(usuario, jwtService.extraerJti(refreshToken), jwtService.getRefreshExpirationMs());

        log.info("evento=LOGIN resultado=EXITOSO correo={} ip={} sub={}", correoUsuario, ip, idUsuario);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .idUsuario(idUsuario)
                .correo(correoUsuario)
                .roles(roles)
                .permisos(permisos)
                .requiere2fa(false)
                .build();
    }

    @Override
    @Transactional
    // El correo real se resuelve DENTRO del método a partir del ticket
    // (preAuthTicket), no está disponible como parámetro para el SpEL de
    // correoActor; el evento queda con actor "anonimo", que es correcto: en
    // este punto el usuario aún no tiene una sesión completa.
    @Auditable(accion = "AUTENTICACION_2FA_VERIFICAR", modulo = ModuloAuditoria.SEGURIDAD)
    public TokenResponse verify2Fa(String preAuthTicket, TwoFactorRequest request) {
        // §2.1 (OBS-AUTO-05): el usuario se resuelve EXCLUSIVAMENTE desde el
        // ticket emitido por login() tras validar la contraseña — ya no desde
        // un correo en el body, que no probaba nada.
        PreAuth2faTicketService.DatosTicket datos = preAuth2faTicketService.resolver(preAuthTicket)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Sesión de verificación inválida o expirada. Vuelve a iniciar sesión."));

        // REQ-F-002: misma funcion que login() -- consolida usuario + roles + 2FA
        // en una sola llamada en vez de findByCorreo + findByUsuarioIdUsuario.
        String estadoLoginJson = usuarioRepository.resolverEstadoLogin(datos.correo());
        if (estadoLoginJson == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        JsonNode estado = parseEstadoLogin(estadoLoginJson);
        Long idUsuario = estado.get("idUsuario").asLong();
        boolean dosFactoresHabilitado = estado.get("dosFactoresHabilitado").asBoolean();

        if (!dosFactoresHabilitado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El 2FA no se encuentra habilitado para este usuario");
        }

        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);

        if (!twoFactorService.validarCodigoOBackup(datos.correo(), request.getCodigo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido o expirado");
        }

        // Uso único: si otra petición concurrente ya consumió este ticket, no se
        // emiten tokens dos veces para el mismo ticket.
        if (!preAuth2faTicketService.consumir(preAuthTicket)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Sesión de verificación inválida o expirada. Vuelve a iniciar sesión.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(datos.correo());
        String accessToken = jwtService.generarToken(userDetails);
        String refreshToken = jwtService.generarRefreshToken(userDetails);

        List<String> roles = extraerRoles(estado);

        List<String> permisos = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();

        registrarSesionMejorEsfuerzo(usuario, jwtService.extraerJti(accessToken), jwtService.getExpirationMs());
        registrarSesionObligatoria(usuario, jwtService.extraerJti(refreshToken), jwtService.getRefreshExpirationMs());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .idUsuario(idUsuario)
                .correo(datos.correo())
                .roles(roles)
                .permisos(permisos)
                .requiere2fa(false)
                .build();
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token no proporcionado");
        }

        try {
            String jti = jwtService.extraerJti(refreshToken);
            // §2.5 (OBS-AUTO-06): un jti nulo cortocircuitaba el && anterior y SALTABA
            // por completo la comprobación de revocación (bug F3). Ahora un jti
            // ausente se trata igual que "no encontrado": rechazado.
            if (jti == null || sesionUsuarioRepository.findByJti(jti).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revocado o expirado");
            }

            String username = jwtService.extraerUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.esRefreshTokenValido(refreshToken, userDetails)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido");
            }

            Usuario usuario = usuarioRepository.findByCorreo(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

            if (!Boolean.TRUE.equals(usuario.getEstadoCuenta())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta del usuario está inactiva");
            }

            sessionRevocationService.revocarToken(refreshToken);
            sesionUsuarioRepository.deleteByJti(jti);

            String nuevoAccessToken = jwtService.generarToken(userDetails);
            String nuevoRefreshToken = jwtService.generarRefreshToken(userDetails);

            registrarSesionMejorEsfuerzo(usuario, jwtService.extraerJti(nuevoAccessToken), jwtService.getExpirationMs());
            registrarSesionObligatoria(usuario, jwtService.extraerJti(nuevoRefreshToken), jwtService.getRefreshExpirationMs());

            List<String> roles = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                    .map(ur -> ur.getRol().getNombreRol())
                    .toList();

            List<String> permisos = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> !a.startsWith("ROLE_"))
                    .toList();

            return TokenResponse.builder()
                    .accessToken(nuevoAccessToken)
                    .refreshToken(nuevoRefreshToken)
                    .idUsuario(usuario.getIdUsuario())
                    .correo(usuario.getCorreo())
                    .roles(roles)
                    .permisos(permisos)
                    .requiere2fa(false)
                    .build();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error al procesar refresh token: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido o malformado");
        }
    }

    @Override
    @Transactional
    // tokenHeader y refreshToken son secretos: nunca en detalle. No hace falta
    // correoActor explícito, hay SecurityContext porque se llama autenticado.
    @Auditable(accion = "AUTENTICACION_LOGOUT", modulo = ModuloAuditoria.SEGURIDAD)
    public RespuestaMensaje logout(String tokenHeader, String refreshToken) {
        sessionRevocationService.revocarTokenPorCabecera(tokenHeader);
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessionRevocationService.revocarToken(refreshToken);
            // Best-effort: un refresh token ya expirado o malformado en el momento del
            // logout no debe impedir cerrar sesión (no hay fila que borrar de todas formas).
            try {
                String jti = jwtService.extraerJti(refreshToken);
                if (jti != null) {
                    sesionUsuarioRepository.deleteByJti(jti);
                }
            } catch (Exception e) {
                log.debug("No se pudo extraer el jti del refresh token en logout (probablemente expirado o inválido): {}", e.getMessage());
            }
        }
        return new RespuestaMensaje("Sesión cerrada exitosamente");
    }

    @Override
    @Transactional
    @Auditable(accion = "CONTRASENA_SOLICITAR_RESET", modulo = ModuloAuditoria.SEGURIDAD,
            correoActor = "#request.correo")
    public RespuestaMensaje forgotPassword(ForgotPasswordRequest request) {
        // Incondicional (a diferencia de login): aquí no hay noción de "fallo", toda
        // llamada implica el mismo costo de abuso (correo potencialmente enviado)
        // exista o no la cuenta — mail-bombing a una víctima, cuota SMTP agotada.
        intentosAutenticacionService.verificarCuota(
                AMBITO_RECUPERACION, request.getCorreo(), LIMITE_INTENTOS_RECUPERACION, VENTANA_INTENTOS_RECUPERACION);

        String tokenPlain = UUID.randomUUID().toString();
        String tokenHash = hashSha256(tokenPlain);

        // Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §6):
        // fn_solicitar_recuperacion invalida los tokens previos no usados del
        // usuario e inserta el nuevo en la MISMA transaccion, cerrando la
        // ventana en la que una cuenta acumulaba N tokens de recuperacion
        // validos simultaneamente (A5). Devuelve NULL (no encontro cuenta
        // activa) sin lanzar, preservando la respuesta indistinguible.
        String resultadoJson = usuarioRepository.solicitarRecuperacion(request.getCorreo(), tokenHash);
        if (resultadoJson != null) {
            JsonNode resultado = parseJson(resultadoJson, "Error al interpretar la solicitud de recuperación");
            String nombres = resultado.get("nombres").asText();
            log.debug("Generado token de recuperación para usuario ID: {}", resultado.get("idUsuario").asLong());
            emailService.enviarCorreoRecuperacion(request.getCorreo(), nombres, tokenPlain);
        }
        return new RespuestaMensaje("Si el correo se encuentra registrado, recibirás un enlace de recuperación");
    }

    @Override
    @Transactional
    // request.getToken() y la nueva contraseña son secretos: jamás #request
    // completo en detalle. El usuario se resuelve dentro por el token, no hay
    // correo disponible como parámetro.
    @Auditable(accion = "CONTRASENA_RESTABLECER", modulo = ModuloAuditoria.SEGURIDAD)
    public RespuestaMensaje resetPassword(ResetPasswordRequest request) {
        // REQ-F-005: fn_restablecer_contrasena valida (con FOR UPDATE) que el
        // token exista, no este usado y no haya expirado, y actualiza usuarios +
        // tokens_recuperacion de forma atomica, evitando la ventana de doble uso
        // concurrente que tenia la version en dos save() secuenciales.
        String tokenHash = hashSha256(request.getToken());
        try {
            usuarioRepository.restablecerContrasena(tokenHash, passwordEncoder.encode(request.getNuevaContrasena()));
        } catch (RuntimeException e) {
            throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.BAD_REQUEST);
        }

        return new RespuestaMensaje("Contraseña reestablecida exitosamente");
    }

    /** OBS-08 (A09 OWASP): IP del solicitante actual, usada tanto en el registro de sesión como en el log de auditoría de login. */
    private String obtenerIpActual() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null && attributes.getRequest() != null) {
            return ClientIpResolver.resolver(attributes.getRequest());
        }
        return null;
    }

    /**
     * Registro del access token: best-effort. Es telemetría (auditoría/listado de
     * sesiones); si falla, el usuario ya tiene su access token en la respuesta y
     * puede seguir operando con él con normalidad.
     */
    private void registrarSesionMejorEsfuerzo(Usuario usuario, String jti, long expirationMs) {
        try {
            registrarSesion(usuario, jti, expirationMs);
        } catch (Exception e) {
            log.error("Error registrando sesión de acceso para usuario {}", usuario.getIdUsuario(), e);
        }
    }

    /**
     * Registro del refresh token: obligatorio (propaga la excepción). A diferencia
     * del access token, la corrección del siguiente /api/v1/auth/refresh DEPENDE de que
     * esta fila exista (ver refreshToken(): busca por jti). Antes ambos registros
     * fallaban en silencio (catch genérico que solo logueaba), lo que dejaba al
     * usuario con un refresh token que el sistema rechazaría como "revocado o
     * expirado" en el siguiente intento, sin ninguna pista del motivo real.
     */
    private void registrarSesionObligatoria(Usuario usuario, String jti, long expirationMs) {
        registrarSesion(usuario, jti, expirationMs);
    }

    private void registrarSesion(Usuario usuario, String jti, long expirationMs) {
        String ip = obtenerIpActual();
        SesionUsuario sesion = SesionUsuario.builder()
                .usuario(usuario)
                .jti(jti)
                .direccionIp(ip)
                .fechaExpiracion(LocalDateTime.now().plus(Duration.ofMillis(expirationMs)))
                .build();
        sesionUsuarioRepository.save(sesion);
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

    /** Deserializa el JSONB devuelto por fn_resolver_estado_login (REQ-F-002). */
    private JsonNode parseEstadoLogin(String estadoLoginJson) {
        return parseJson(estadoLoginJson, "Error al interpretar el estado de login");
    }

    /** Deserializa el JSONB devuelto por una rutina de db/procs/, con un mensaje de error especifico del llamante. */
    private JsonNode parseJson(String json, String mensajeError) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, mensajeError);
        }
    }

    private List<String> extraerRoles(JsonNode estado) {
        List<String> roles = new ArrayList<>();
        estado.get("roles").forEach(nodo -> roles.add(nodo.asText()));
        return roles;
    }
}

