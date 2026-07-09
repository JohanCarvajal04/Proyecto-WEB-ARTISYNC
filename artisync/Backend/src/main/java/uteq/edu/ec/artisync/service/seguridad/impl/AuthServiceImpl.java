package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.service.seguridad.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.shared.MessageResponse;
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
import uteq.edu.ec.artisync.security.CustomUserDetailsService;
import uteq.edu.ec.artisync.security.JwtService;
import uteq.edu.ec.artisync.service.seguridad.AuthService;
import uteq.edu.ec.artisync.service.seguridad.TwoFactorService;
import uteq.edu.ec.artisync.service.shared.EmailService;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PerfilCreadorRepository perfilCreadorRepository;
    private final AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final SesionUsuarioRepository sesionUsuarioRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SessionRevocationService sessionRevocationService;
    private final EmailService emailService;
    private final TwoFactorService twoFactorService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado en la plataforma");
        }

        // Validación RNF-12: Mayoría de edad (>= 18 años)
        if (request.getFechaNacimiento() == null || LocalDate.now().minusYears(18).isBefore(request.getFechaNacimiento())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes tener al menos 18 años para registrarte en ARTISYNC (RNF-12)");
        }

        Usuario usuario = Usuario.builder()
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .correo(request.getCorreo())
                .contrasenaHash(passwordEncoder.encode(request.getContrasena()))
                .fechaNacimiento(request.getFechaNacimiento())
                .estadoCuenta(true)
                .build();
        usuario = usuarioRepository.save(usuario);

        String rolNombre = request.getRol() != null && !request.getRol().isBlank() ? request.getRol().toUpperCase() : "CLIENTE";
        if (!List.of("CLIENTE", "CREADOR").contains(rolNombre)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no permitido en registro. Solo se permiten CLIENTE o CREADOR");
        }
        Rol rol = rolRepository.findByNombreRol(rolNombre)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol especificado no existe en el sistema: " + rolNombre));

        UsuarioRol usuarioRol = UsuarioRol.builder()
                .usuario(usuario)
                .rol(rol)
                .build();
        usuarioRolRepository.save(usuarioRol);

        if ("CREADOR".equals(rolNombre)) {
            PerfilCreador perfil = PerfilCreador.builder()
                    .usuario(usuario)
                    .biografia("¡Hola! Soy un creador en ARTISYNC.")
                    .build();
            perfilCreadorRepository.save(perfil);
        }

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
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena())
        );

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        AutenticacionDosFactores dosFactores = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElse(null);

        if (dosFactores != null && Boolean.TRUE.equals(dosFactores.getEstaHabilitado())) {
            return TokenResponse.builder()
                    .correo(usuario.getCorreo())
                    .idUsuario(usuario.getIdUsuario())
                    .requiere2fa(true)
                    .build();
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getCorreo());
        String accessToken = jwtService.generarToken(userDetails);
        String refreshToken = jwtService.generarRefreshToken(userDetails);

        List<String> roles = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .map(ur -> ur.getRol().getNombreRol())
                .toList();

        List<String> permisos = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();

        registrarSesion(usuario, accessToken, jwtService.getExpirationMs());
        registrarSesion(usuario, refreshToken, jwtService.getRefreshExpirationMs());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .idUsuario(usuario.getIdUsuario())
                .correo(usuario.getCorreo())
                .roles(roles)
                .permisos(permisos)
                .requiere2fa(false)
                .build();
    }

    @Override
    @Transactional
    public TokenResponse verify2Fa(TwoFactorRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        AutenticacionDosFactores dosFactores = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "2FA no configurado para este usuario"));

        if (!Boolean.TRUE.equals(dosFactores.getEstaHabilitado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El 2FA no se encuentra habilitado");
        }

        if (!twoFactorService.validarCodigoOBackup(request.getCorreo(), request.getCodigo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido o expirado");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getCorreo());
        String accessToken = jwtService.generarToken(userDetails);
        String refreshToken = jwtService.generarRefreshToken(userDetails);

        List<String> roles = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .map(ur -> ur.getRol().getNombreRol())
                .toList();

        List<String> permisos = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();

        registrarSesion(usuario, accessToken, jwtService.getExpirationMs());
        registrarSesion(usuario, refreshToken, jwtService.getRefreshExpirationMs());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .idUsuario(usuario.getIdUsuario())
                .correo(usuario.getCorreo())
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
            if (jti != null && sesionUsuarioRepository.findByTokenJwt(refreshToken).isEmpty()) {
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
            sesionUsuarioRepository.findByTokenJwt(refreshToken).ifPresent(sesionUsuarioRepository::delete);

            String nuevoAccessToken = jwtService.generarToken(userDetails);
            String nuevoRefreshToken = jwtService.generarRefreshToken(userDetails);

            registrarSesion(usuario, nuevoAccessToken, jwtService.getExpirationMs());
            registrarSesion(usuario, nuevoRefreshToken, jwtService.getRefreshExpirationMs());

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
    public MessageResponse logout(String tokenHeader, String refreshToken) {
        sessionRevocationService.revocarTokenPorCabecera(tokenHeader);
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessionRevocationService.revocarToken(refreshToken);
            sesionUsuarioRepository.findByTokenJwt(refreshToken).ifPresent(sesionUsuarioRepository::delete);
        }
        return new MessageResponse("Sesión cerrada exitosamente");
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        usuarioRepository.findByCorreo(request.getCorreo()).ifPresent(usuario -> {
            String tokenPlain = UUID.randomUUID().toString();
            String tokenHash = hashSha256(tokenPlain);
            TokenRecuperacion tokenRec = TokenRecuperacion.builder()
                    .usuario(usuario)
                    .hashToken(tokenHash)
                    .usado(false)
                    .build();
            tokenRecuperacionRepository.save(tokenRec);
            log.debug("Generado token de recuperación para usuario ID: {}", usuario.getIdUsuario());
            emailService.enviarCorreoRecuperacion(usuario.getCorreo(), usuario.getNombres(), tokenPlain);
        });
        return new MessageResponse("Si el correo se encuentra registrado, recibirás un enlace de recuperación");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashSha256(request.getToken());
        TokenRecuperacion tokenRec = tokenRecuperacionRepository.findByHashTokenAndUsadoFalse(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este enlace ya ha sido utilizado o ha expirado"));

        if (tokenRec.getFechaGeneracion().plusMinutes(60).isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este enlace ya ha sido utilizado o ha expirado");
        }

        Usuario usuario = tokenRec.getUsuario();
        usuario.setContrasenaHash(passwordEncoder.encode(request.getNuevaContrasena()));
        usuarioRepository.save(usuario);

        tokenRec.setUsado(true);
        tokenRecuperacionRepository.save(tokenRec);

        return new MessageResponse("Contraseña reestablecida exitosamente");
    }

    private void registrarSesion(Usuario usuario, String token, long expirationMs) {
        try {
            String ip = null;
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest() != null) {
                ip = attributes.getRequest().getRemoteAddr();
            }
            SesionUsuario sesion = SesionUsuario.builder()
                    .usuario(usuario)
                    .tokenJwt(token)
                    .direccionIp(ip)
                    .fechaExpiracion(LocalDateTime.now().plus(Duration.ofMillis(expirationMs)))
                    .build();
            sesionUsuarioRepository.save(sesion);
        } catch (Exception e) {
            log.error("Error registrando sesión de usuario: {}", e.getMessage());
        }
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
