package uteq.edu.ec.artisync.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.request.*;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.UserResponse;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.*;
import uteq.edu.ec.artisync.repository.*;
import uteq.edu.ec.artisync.security.JwtService;
import uteq.edu.ec.artisync.service.AdminUserService;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.PagedResponseBuilder;

import java.time.Duration;
import java.util.List;

import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.UsuarioMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PaisRepository paisRepository;
    private final PerfilCreadorRepository perfilCreadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final SessionRevocationService sessionRevocationService;
    private final AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;
    private final CodigoRespaldo2FaRepository codigoRespaldo2FaRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<Usuario> usuariosPage = usuarioRepository.findAll(pageable);
        return PagedResponseBuilder.buildAndMap(usuariosPage, usuarioMapper::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con ID: " + id));
        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        Pais pais = null;
        if (request.getIdPais() != null) {
            pais = paisRepository.findById(request.getIdPais())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "País no encontrado"));
        }

        Usuario usuario = Usuario.builder()
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .correo(request.getCorreo())
                .contrasenaHash(passwordEncoder.encode(request.getContrasena()))
                .fechaNacimiento(request.getFechaNacimiento())
                .pais(pais)
                .estadoCuenta(request.getEstadoCuenta() != null ? request.getEstadoCuenta() : true)
                .build();
        usuario = usuarioRepository.save(usuario);

        List<String> rolesAsignar = (request.getRoles() != null && !request.getRoles().isEmpty())
                ? request.getRoles() : List.of("CLIENTE");

        for (String rolNombre : rolesAsignar) {
            String rolUpper = rolNombre.toUpperCase();
            Rol rol = rolRepository.findByNombreRol(rolUpper)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol especificado no existe en el sistema: " + rolUpper));
            usuarioRolRepository.save(UsuarioRol.builder().usuario(usuario).rol(rol).build());

            if ("CREADOR".equals(rolUpper) && perfilCreadorRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).isEmpty()) {
                perfilCreadorRepository.save(PerfilCreador.builder().usuario(usuario).biografia("Perfil creado por Administrador").build());
            }
        }

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, AdminUpdateUserRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
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
            if (request.getIdPais() <= 0) {
                usuario.setPais(null);
            } else {
                Pais pais = paisRepository.findById(request.getIdPais())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "País no encontrado"));
                usuario.setPais(pais);
            }
        }
        if (request.getEstadoCuenta() != null) {
            boolean estadoAnterior = usuario.getEstadoCuenta();
            usuario.setEstadoCuenta(request.getEstadoCuenta());
            if (estadoAnterior && !request.getEstadoCuenta()) {
                sessionRevocationService.revocarSesionesUsuario(usuario.getIdUsuario());
            }
        }

        if (Boolean.FALSE.equals(request.getDosFactoresHabilitado())) {
            final Long idUsuario = usuario.getIdUsuario();
            final String correoUsuario = usuario.getCorreo();
            autenticacionDosFactoresRepository.findByUsuarioIdUsuario(idUsuario)
                    .ifPresent(dosFactores -> {
                        dosFactores.setEstaHabilitado(false);
                        autenticacionDosFactoresRepository.save(dosFactores);
                        codigoRespaldo2FaRepository.deleteByUsuarioIdUsuario(idUsuario);
                        log.info("2FA desactivado por administrador para usuario: {}", correoUsuario);
                    });
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            actualizarRoles(usuario, request.getRoles());
        }

        usuario = usuarioRepository.save(usuario);
        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    public UserResponse changeEstado(Long id, ChangeEstadoRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        boolean estadoAnterior = usuario.getEstadoCuenta();
        usuario.setEstadoCuenta(request.getEstadoCuenta());
        usuario = usuarioRepository.save(usuario);

        if (estadoAnterior && !request.getEstadoCuenta()) {
            sessionRevocationService.revocarSesionesUsuario(usuario.getIdUsuario());
        }

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    public UserResponse assignRoles(Long id, AssignRolesRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        actualizarRoles(usuario, request.getRoles());
        sessionRevocationService.revocarSesionesUsuario(usuario.getIdUsuario()); // Revocar sesiones para obligar a refrescar claims JWT con nuevos roles

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        sessionRevocationService.revocarSesionesUsuario(usuario.getIdUsuario());
        usuario.setEstadoCuenta(false);
        usuarioRepository.save(usuario);
    }

    @Override
    public MessageResponse revokeUserSessions(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con ID: " + id);
        }
        sessionRevocationService.revocarSesionesUsuario(id);
        return new MessageResponse("Se han revocado exitosamente todas las sesiones del usuario ID: " + id);
    }

    private void actualizarRoles(Usuario usuario, List<String> nuevosRoles) {
        List<UsuarioRol> rolesActuales = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());
        usuarioRolRepository.deleteAll(rolesActuales);
        usuarioRolRepository.flush();

        for (String rolNombre : nuevosRoles) {
            String rolUpper = rolNombre.toUpperCase();
            Rol rol = rolRepository.findByNombreRol(rolUpper)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol especificado no existe en el sistema: " + rolUpper));
            usuarioRolRepository.save(UsuarioRol.builder().usuario(usuario).rol(rol).build());

            if ("CREADOR".equals(rolUpper) && perfilCreadorRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).isEmpty()) {
                perfilCreadorRepository.save(PerfilCreador.builder().usuario(usuario).biografia("¡Hola! Soy un creador en ARTISYNC.").build());
            }
        }
    }
}
