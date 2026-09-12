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
import uteq.edu.ec.artisync.service.shared.UserMapper;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;
import uteq.edu.ec.artisync.service.shared.almacenamiento.FilePolicy;
import uteq.edu.ec.artisync.service.shared.almacenamiento.StoragePrefix;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository usuarioRepository;
    private final CountryRepository paisRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper usuarioMapper;
    private final SessionRevocationService sessionRevocationService;
    private final DocumentStorage almacenamientoDocumentos;

    @Override
    @Transactional(readOnly = true)
    /**
     * @param correo correo electrónico del usuario autenticado
     * @return los datos del usuario
     * @throws org.springframework.web.server.ResponseStatusException 404 si no existe un usuario con ese correo
     */
    public UserResponse getCurrentUser(String correo) {
        User usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado"));

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    /**
     * Actualiza los datos personales del usuario autenticado; los campos {@code null} u
     * omitidos en la petición no se modifican.
     *
     * @param correo correo electrónico del usuario autenticado
     * @param request campos a actualizar (nombres, apellidos, fecha de nacimiento, país)
     * @return el usuario ya actualizado
     * @throws org.springframework.web.server.ResponseStatusException 404 si el usuario no existe,
     *         o 400 si el país indicado no existe
     */
    public UserResponse updateCurrentUser(String correo, UpdateUserRequest request) {
        User usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado"));

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
            Country pais = paisRepository.findById(request.getIdPais())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "País no encontrado"));
            usuario.setPais(pais);
        }

        usuario = usuarioRepository.save(usuario);

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    /**
     * Cambia la contraseña del usuario autenticado y revoca todas sus sesiones
     * activas, para forzar un nuevo inicio de sesión con la contraseña nueva.
     *
     * @param correo correo electrónico del usuario autenticado
     * @param request contraseña actual (para verificar identidad) y contraseña nueva
     * @return mensaje de confirmación
     * @throws org.springframework.web.server.ResponseStatusException 404 si el usuario no existe,
     *         400 si la contraseña actual no coincide, o 409 si otra sesión cambió la contraseña
     *         concurrentemente (compare-and-swap de {@code sp_cambiar_contrasena})
     */
    public RespuestaMensaje changePassword(String correo, ChangePasswordRequest request) {
        User usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado"));

        if (!passwordEncoder.matches(request.getContrasenaActual(), usuario.getContrasenaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }

        // Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §6):
        // sp_cambiar_contrasena aplica el UPDATE solo si el hash sigue siendo
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
    /**
     * Desactiva (soft delete) la cuenta del usuario autenticado y revoca sus
     * sesiones activas de forma atómica.
     *
     * @param correo correo electrónico del usuario autenticado
     * @return mensaje de confirmación
     * @throws org.springframework.web.server.ResponseStatusException 404 si el usuario no existe
     */
    public RespuestaMensaje deleteOwnAccount(String correo) {
        User usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado"));

        // Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5):
        // fn_cambiar_estado_cuenta desactiva la cuenta (soft delete) y revoca
        // sus sesiones atomicamente, bajo SELECT FOR UPDATE.
        sessionRevocationService.cambiarEstadoCuenta(usuario.getIdUsuario(), false);

        return new RespuestaMensaje("Cuenta desactivada exitosamente");
    }

    @Override
    @Transactional
    /**
     * @param correo correo electrónico del usuario autenticado
     * @return mensaje de confirmación
     * @throws org.springframework.web.server.ResponseStatusException 404 si el usuario no existe
     */
    public RespuestaMensaje revokeAllMySessions(String correo) {
        User usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado"));
        sessionRevocationService.revocarSesionesUsuario(usuario.getIdUsuario());
        return new RespuestaMensaje("Todas las sesiones activas han sido cerradas.");
    }

    @Override
    @Transactional
    /**
     * Sube una nueva foto de perfil, reemplazando la anterior en el almacenamiento
     * (si existía; un fallo al borrar la anterior no interrumpe la subida).
     *
     * @param correo correo electrónico del usuario autenticado
     * @param file archivo de imagen, validado contra {@code FilePolicy.PERFIL}
     * @return el usuario con la URL de foto de perfil actualizada
     * @throws org.springframework.web.server.ResponseStatusException 404 si el usuario no existe
     */
    public UserResponse uploadProfilePicture(String correo, MultipartFile file) {
        FilePolicy.PERFIL.validar(file);

        User usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado"));

        if (usuario.getUrlFotoPerfil() != null) {
            try {
                almacenamientoDocumentos.eliminar(usuario.getUrlFotoPerfil());
            } catch (Exception e) {
                // Ignore failure to delete old picture
            }
        }
        
        String nuevaReferencia = almacenamientoDocumentos.guardar(file, StoragePrefix.PERFILES);
        usuario.setUrlFotoPerfil(nuevaReferencia);
        usuarioRepository.save(usuario);
        
        return usuarioMapper.toUserResponse(usuario);
    }
}

