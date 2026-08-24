package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.service.seguridad.*;

import jakarta.persistence.EntityManager;
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
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ContextoAuditoria;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.seguridad.request.*;
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
import uteq.edu.ec.artisync.security.JwtService;
import uteq.edu.ec.artisync.service.seguridad.AdminUserService;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.PagedResponseBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.StoredProcedureExceptionTranslator;
import uteq.edu.ec.artisync.service.shared.UsuarioMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PaisRepository paisRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final SessionRevocationService sessionRevocationService;
    private final AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    // Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §8):
    // toUserResponseList batchea los roles/permisos/2FA de toda la pagina en
    // dos consultas IN (...), en vez del N+1 de invocar toUserResponse() (dos
    // consultas por fila) elemento a elemento. El Pageable/Sort de la peticion
    // se conserva intacto -- se sigue resolviendo con findAll(pageable), no se
    // reemplaza por una rutina con orden fijo.
    public PagedResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<Usuario> usuariosPage = usuarioRepository.findAll(pageable);
        return PagedResponseBuilder.buildAndMapList(usuariosPage, usuarioMapper::toUserResponseList);
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
    @Auditable(accion = "USUARIO_CREAR", modulo = ModuloAuditoria.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#resultado.idUsuario",
            detalle = "{correo: #request.correo, roles: #request.roles}")
    // Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §4): delega
    // en fn_crear_usuario_admin, que captura unique_violation sobre el correo
    // en vez de la comprobacion existsByCorreo previa a esta version, que no
    // era atomica respecto al save() (lectura fantasma, A3), y compone con
    // fn_sincronizar_roles_usuario (Fase 1) para los roles y el perfil de
    // creador en la misma transaccion.
    public UserResponse createUser(CreateUserRequest request) {
        List<String> rolesAsignar = (request.getRoles() != null && !request.getRoles().isEmpty())
                ? request.getRoles() : List.of("CLIENTE");
        String[] roles = rolesAsignar.stream().map(String::toUpperCase).toArray(String[]::new);

        Long idUsuario;
        try {
            idUsuario = usuarioRepository.crearUsuarioAdmin(
                    request.getNombres(),
                    request.getApellidos(),
                    request.getCorreo(),
                    passwordEncoder.encode(request.getContrasena()),
                    request.getFechaNacimiento(),
                    request.getIdPais(),
                    request.getEstadoCuenta(),
                    roles);
        } catch (RuntimeException e) {
            throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.BAD_REQUEST);
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear el usuario"));

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    @Auditable(accion = "USUARIO_EDITAR", modulo = ModuloAuditoria.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#id",
            detalle = "{estadoCuenta: #request.estadoCuenta, roles: #request.roles}")
    public UserResponse updateUser(Long id, AdminUpdateUserRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        ContextoAuditoria.aportar("antes", Map.of(
                "nombres", usuario.getNombres(),
                "apellidos", usuario.getApellidos(),
                "estadoCuenta", usuario.getEstadoCuenta()));

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
        if (Boolean.FALSE.equals(request.getDosFactoresHabilitado())) {
            // Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §7):
            // fn_desactivar_2fa desactiva el flag y purga los codigos de
            // respaldo en una unica transaccion; es idempotente (no lanza si
            // el usuario no tenia 2FA configurado, igual que el ifPresent
            // anterior). Unifica el codigo antes duplicado con
            // TwoFactorServiceImpl.disable2Fa.
            boolean desactivado = autenticacionDosFactoresRepository.desactivar2Fa(usuario.getIdUsuario());
            if (desactivado) {
                log.info("2FA desactivado por administrador para usuario: {}", usuario.getCorreo());
            }
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            actualizarRoles(usuario, request.getRoles());
        }

        // Persiste primero nombres/apellidos/fechaNacimiento/pais (los unicos
        // campos mutados directamente en la entidad hasta aqui) para que este
        // UPDATE no incluya estado_cuenta -- ese campo se cambia mas abajo por
        // la rutina atomica, nunca por esta escritura JPA.
        usuario = usuarioRepository.save(usuario);

        if (request.getEstadoCuenta() != null) {
            // Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5):
            // fn_cambiar_estado_cuenta decide "hubo transicion activa->inactiva?"
            // y revoca sesiones bajo SELECT FOR UPDATE en el motor, en vez de
            // comparar aqui un estadoAnterior que otro administrador concurrente
            // pudo dejar obsoleto (actualizacion perdida).
            //
            // entityManager.refresh() en vez de usuario.setEstadoCuenta(...):
            // el save() de arriba ya vacio cualquier cambio pendiente, asi que
            // no hay nada que perder; refresh() releae la fila (reflejando el
            // estado_cuenta que la funcion nativa acaba de escribir) y
            // resincroniza el snapshot de Hibernate, evitando que el commit de
            // esta transaccion dispare un SEGUNDO UPDATE redundante reescribiendo
            // el mismo valor que la funcion atomica ya persistio.
            sessionRevocationService.cambiarEstadoCuenta(usuario.getIdUsuario(), request.getEstadoCuenta());
            entityManager.refresh(usuario);
        }

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    @Auditable(accion = "USUARIO_CAMBIAR_ESTADO", modulo = ModuloAuditoria.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#id",
            detalle = "{estadoCuenta: #request.estadoCuenta}")
    public UserResponse changeEstado(Long id, ChangeEstadoRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5):
        // fn_cambiar_estado_cuenta aplica el cambio y revoca sesiones (si hubo
        // transicion activa->inactiva) en una unica transaccion serializada con
        // SELECT FOR UPDATE, sustituyendo el find+mutate+save+revocar en cuatro
        // pasos no atomicos que tenia esta operacion.
        //
        // entityManager.refresh() en vez de usuario.setEstadoCuenta(...): este
        // metodo no tiene ningun otro cambio pendiente sobre `usuario`, asi que
        // mutar el campo en el objeto managed lo marcaria "dirty" y Hibernate
        // emitiria, al confirmar la transaccion, un UPDATE adicional reescribiendo
        // el mismo valor que la funcion atomica ya persistio. refresh() releae la
        // fila real (sin escribir nada) y resincroniza el snapshot de Hibernate.
        sessionRevocationService.cambiarEstadoCuenta(usuario.getIdUsuario(), request.getEstadoCuenta());
        entityManager.refresh(usuario);

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    @Auditable(accion = "USUARIO_ASIGNAR_ROLES", modulo = ModuloAuditoria.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#id",
            detalle = "{roles: #request.roles}")
    public UserResponse assignRoles(Long id, AssignRolesRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        ContextoAuditoria.aportar("antes", Map.of("roles",
                usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                        .map(ur -> ur.getRol().getNombreRol())
                        .toList()));

        actualizarRoles(usuario, request.getRoles());
        sessionRevocationService.revocarSesionesUsuario(usuario.getIdUsuario()); // Revocar sesiones para obligar a refrescar claims JWT con nuevos roles

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    // Es un soft-delete (estadoCuenta=false), no un borrado físico: la acción
    // se llama USUARIO_DESACTIVAR y no USUARIO_ELIMINAR para que la bitácora
    // describa lo que realmente ocurre en la base de datos.
    @Auditable(accion = "USUARIO_DESACTIVAR", modulo = ModuloAuditoria.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#id")
    public void deleteUser(Long id) {
        // Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5):
        // fn_cambiar_estado_cuenta desactiva la cuenta y revoca sus sesiones
        // atomicamente; ya no hace falta cargar la entidad completa. El
        // existsById previo era redundante: fn_cambiar_estado_cuenta ya
        // lanza P0002 si el usuario no existe, y SessionRevocationService
        // ya lo traduce a 404 (revision de codigo, hallazgo de eficiencia).
        sessionRevocationService.cambiarEstadoCuenta(id, false);
    }

    @Override
    @Auditable(accion = "SESION_REVOCAR", modulo = ModuloAuditoria.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#id")
    public RespuestaMensaje revokeUserSessions(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con ID: " + id);
        }
        sessionRevocationService.revocarSesionesUsuario(id);
        return new RespuestaMensaje("Se han revocado exitosamente todas las sesiones del usuario ID: " + id);
    }

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §3) -
     * fn_sincronizar_roles_usuario reemplaza atomicamente, en una unica llamada
     * al motor, lo que antes eran ~10 viajes no atomicos (findByUsuarioIdUsuario
     * + deleteAll + flush + por cada rol: findByNombreRol + save + consulta de
     * perfil + save de perfil). Corrige dos anomalias: la lectura fantasma que
     * permitia roles duplicados cuando dos administradores editaban al mismo
     * usuario a la vez, y el estado a medias (usuario sin ningun rol) si el
     * bucle en Java fallaba despues del delete.
     */
    private void actualizarRoles(Usuario usuario, List<String> nuevosRoles) {
        try {
            usuarioRolRepository.sincronizarRoles(
                    usuario.getIdUsuario(),
                    nuevosRoles.stream().map(String::toUpperCase).toArray(String[]::new));
        } catch (RuntimeException e) {
            throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.BAD_REQUEST);
        }
    }
}

