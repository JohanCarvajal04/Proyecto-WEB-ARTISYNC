package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.service.seguridad.*;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import uteq.edu.ec.artisync.dto.seguridad.request.ChangePasswordRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateUserRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.entity.seguridad.*;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.pedido.*;
import uteq.edu.ec.artisync.repository.legal.*;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.social.*;
import uteq.edu.ec.artisync.service.seguridad.UserService;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.StoredProcedureExceptionTranslator;
import uteq.edu.ec.artisync.service.shared.UsuarioMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UsuarioRepository usuarioRepository;
    private final PaisRepository paisRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final SessionRevocationService sessionRevocationService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(String correo, UpdateUserRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (request.getNombres() != null && !request.getNombres().isBlank()) {
            usuario.setNombres(request.getNombres());
        }
        if (request.getApellidos() != null && !request.getApellidos().isBlank()) {
            usuario.setApellidos(request.getApellidos());
        }
        if (request.getFechaNacimiento() != null) {
            usuario.setFechaNacimiento(request.getFechaNacimiento());
        }
        if (request.getIdPais() != null) {
            Pais pais = paisRepository.findById(request.getIdPais())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "País no encontrado"));
            usuario.setPais(pais);
        }

        usuario = usuarioRepository.save(usuario);

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    public RespuestaMensaje changePassword(String correo, ChangePasswordRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getContrasenaActual(), usuario.getContrasenaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }

        // Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §6):
        // fn_cambiar_contrasena aplica el UPDATE solo si el hash sigue siendo
        // el que se acaba de verificar con BCrypt (compare-and-swap), en vez
        // de un save() incondicional. Si otra sesion cambio la contrasena
        // justo entre la verificacion y este punto, la funcion lanza en vez
        // de pisar silenciosamente ese cambio (actualizacion perdida, A7).
        try {
            usuarioRepository.cambiarContrasena(usuario.getIdUsuario(), usuario.getContrasenaHash(),
                    passwordEncoder.encode(request.getNuevaContrasena()));
        } catch (RuntimeException e) {
            throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.CONFLICT);
        }

        sessionRevocationService.revocarSesionesUsuario(usuario.getIdUsuario());

        return new RespuestaMensaje("Contraseña cambiada exitosamente. Vuelve a iniciar sesión.");
    }

    @Override
    @Transactional
    public RespuestaMensaje deleteOwnAccount(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5):
        // fn_cambiar_estado_cuenta desactiva la cuenta (soft delete) y revoca
        // sus sesiones atomicamente, bajo SELECT FOR UPDATE.
        sessionRevocationService.cambiarEstadoCuenta(usuario.getIdUsuario(), false);

        return new RespuestaMensaje("Cuenta desactivada exitosamente");
    }

    @Override
    public RespuestaMensaje revokeAllMySessions(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        sessionRevocationService.revocarSesionesUsuario(usuario.getIdUsuario());
        return new RespuestaMensaje("Todas las sesiones activas han sido cerradas.");
    }
}

