package uteq.edu.ec.artisync.service.impl;

import uteq.edu.ec.artisync.dto.request.*;
import uteq.edu.ec.artisync.dto.response.*;
import uteq.edu.ec.artisync.model.seguridad.*;
import uteq.edu.ec.artisync.model.seguridad.SesionUsuario;
import uteq.edu.ec.artisync.model.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.*;
import uteq.edu.ec.artisync.security.*;
import uteq.edu.ec.artisync.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired private AuthenticationManager authManager;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private RolRepository rolRepo;
    @Autowired private SesionUsuarioRepository sesionRepo;
    @Autowired private TokenRecuperacionRepository tokenRecuperacionRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = jwtUtils.generarToken(auth);
        UsuarioPrincipal userPrincipal = (UsuarioPrincipal) auth.getPrincipal();

        // Guardar sesión en BD
        Usuario usuario = usuarioRepo.findByCorreo(request.getCorreo()).orElseThrow();
        SesionUsuario sesion = new SesionUsuario();
        sesion.setUsuario(usuario);
        sesion.setTokenJwt(jwt);
        sesion.setDireccionIp(httpRequest.getRemoteAddr());
        sesion.setFechaCreacion(LocalDateTime.now());
        sesion.setFechaExpiracion(LocalDateTime.now().plusDays(1));
        sesionRepo.save(sesion);

        List<String> roles = userPrincipal.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .filter(a -> a.startsWith("ROLE_"))
            .collect(Collectors.toList());

        List<String> permisos = userPrincipal.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .filter(a -> !a.startsWith("ROLE_"))
            .collect(Collectors.toList());

        return AuthResponse.builder()
            .token(jwt)
            .tipo("Bearer")
            .idUsuario(userPrincipal.getId())
            .nombres(userPrincipal.getNombres())
            .correo(userPrincipal.getCorreo())
            .roles(roles)
            .permisos(permisos)
            .build();
    }

    @Override
    @Transactional
    public UsuarioResponse registro(RegistroRequest request) {
        if (usuarioRepo.existsByCorreo(request.getCorreo())) {
            throw new RuntimeException("El correo ya está registrado: " + request.getCorreo());
        }

        Usuario usuario = new Usuario();
        usuario.setNombres(request.getNombres());
        usuario.setApellidos(request.getApellidos());
        usuario.setCorreo(request.getCorreo());
        usuario.setContrasenaHash(passwordEncoder.encode(request.getContrasena()));
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setEstadoCuenta(true);

        // Asignar rol por defecto: CLIENTE
        Rol rolCliente = rolRepo.findByNombreRol("CLIENTE")
            .orElseThrow(() -> new RuntimeException("Rol CLIENTE no encontrado en BD"));
        usuario.setRoles(List.of(rolCliente));

        Usuario guardado = usuarioRepo.save(usuario);
        return mapToUsuarioResponse(guardado);
    }

    @Override
    @Transactional
    public MensajeResponse logout(String token) {
        // Eliminar la sesión activa del token
        sesionRepo.findByTokenJwt(token).ifPresent(sesionRepo::delete);
        SecurityContextHolder.clearContext();
        return new MensajeResponse("Sesión cerrada correctamente", true);
    }

    @Override
    @Transactional
    public MensajeResponse solicitarRecuperacion(String correo) {
        usuarioRepo.findByCorreo(correo).ifPresent(usuario -> {
            String token = UUID.randomUUID().toString();
            TokenRecuperacion tr = new TokenRecuperacion();
            tr.setUsuario(usuario);
            tr.setHashToken(token);
            tr.setFechaGeneracion(LocalDateTime.now());
            tr.setUsado(false);
            tokenRecuperacionRepo.save(tr);
            // Aquí iría el envío de email con el token
            // emailService.enviarRecuperacion(correo, token);
        });
        return new MensajeResponse(
            "Si el correo existe, recibirás instrucciones de recuperación", true);
    }

    @Override
    @Transactional
    public MensajeResponse cambiarPassword(CambioPasswordRequest request) {
        TokenRecuperacion tr = tokenRecuperacionRepo
            .findByHashTokenAndUsadoFalse(request.getTokenRecuperacion())
            .orElseThrow(() -> new RuntimeException("Token inválido o ya utilizado"));

        Usuario usuario = tr.getUsuario();
        usuario.setContrasenaHash(passwordEncoder.encode(request.getNuevaContrasena()));
        usuarioRepo.save(usuario);

        tr.setUsado(true);
        tokenRecuperacionRepo.save(tr);

        // Invalidar todas las sesiones activas del usuario
        sesionRepo.deleteAllByUsuarioId(usuario.getIdUsuario());

        return new MensajeResponse("Contraseña actualizada correctamente", true);
    }

    @Override
    public UsuarioResponse obtenerPerfil(String correo) {
        Usuario usuario = usuarioRepo.findByCorreo(correo)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return mapToUsuarioResponse(usuario);
    }

    private UsuarioResponse mapToUsuarioResponse(Usuario u) {
        List<String> roles = u.getRoles() == null ? List.of() :
            u.getRoles().stream().map(Rol::getNombreRol).collect(Collectors.toList());

        List<String> permisos = u.getRoles() == null ? List.of() :
            u.getRoles().stream()
                .flatMap(r -> r.getPermisos().stream())
                .map(Permiso::getNombrePermiso)
                .distinct()
                .collect(Collectors.toList());

        return UsuarioResponse.builder()
            .idUsuario(u.getIdUsuario())
            .nombres(u.getNombres())
            .apellidos(u.getApellidos())
            .correo(u.getCorreo())
            .pais(u.getPais() != null ? u.getPais().getNombrePais() : null)
            .fechaRegistro(u.getFechaRegistro())
            .estadoCuenta(u.getEstadoCuenta())
            .roles(roles)
            .permisos(permisos)
            .build();
    }
}
