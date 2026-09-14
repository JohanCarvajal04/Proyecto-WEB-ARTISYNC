package uteq.edu.ec.artisync.service.shared;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.dto.security.response.UserResponse;
import uteq.edu.ec.artisync.entity.security.TwoFactorAuthentication;
import uteq.edu.ec.artisync.entity.security.Permission;
import uteq.edu.ec.artisync.entity.security.User;
import uteq.edu.ec.artisync.entity.security.UserRole;
import uteq.edu.ec.artisync.repository.security.TwoFactorAuthenticationRepository;
import uteq.edu.ec.artisync.repository.security.UserRoleRepository;
import uteq.edu.ec.artisync.service.shared.storage.ProfilePhotoUrl;

import uteq.edu.ec.artisync.service.profile.IVerificationService;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final UserRoleRepository usuarioRolRepository;
    private final TwoFactorAuthenticationRepository autenticacionDosFactoresRepository;
    private final IVerificationService verificacionServicio;

    /**
     * Mapea un único {@link User} a su {@link UserResponse}, resolviendo sus roles,
     * si tiene 2FA habilitado y si su identidad está verificada. Pensado para el
     * mapeo de una sola fila (por ejemplo {@code getUserById}, o tras crear/editar
     * un usuario): las consultas adicionales que dispara son aceptables aquí porque
     * ocurren una sola vez, a diferencia de {@link #toUserResponseList(List)}.
     *
     * @param usuario entidad de usuario a mapear
     * @return la representación de respuesta del usuario, con roles, permisos y estado de verificación
     */
    public UserResponse toUserResponse(User usuario) {
        List<UserRole> usuarioRoles = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());
        boolean dosFactoresHabilitado = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .map(TwoFactorAuthentication::getEstaHabilitado)
                .map(Boolean.TRUE::equals)
                .orElse(false);
        boolean identidadVerificada = verificacionServicio.isIdentityVerified(usuario.getIdUsuario());
        return build(usuario, usuarioRoles, dosFactoresHabilitado, identidadVerificada);
    }

    /**
     * Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §8) - mapea
     * una PAGINA completa de usuarios con solo dos consultas adicionales
     * (findByUsuarioIdUsuarioIn de roles y de 2FA), en vez de invocar
     * toUserResponse() por cada fila -- eso disparaba dos consultas por
     * usuario (N+1): con una pagina de 20 usuarios, ~40 consultas en vez de 2.
     */
    public List<UserResponse> toUserResponseList(List<User> usuarios) {
        if (usuarios.isEmpty()) {
            return List.of();
        }

        List<Long> idsUsuario = usuarios.stream().map(User::getIdUsuario).toList();

        Map<Long, List<UserRole>> rolesPorUsuario = usuarioRolRepository.findByUsuarioIdUsuarioIn(idsUsuario).stream()
                .collect(Collectors.groupingBy(ur -> ur.getUsuario().getIdUsuario()));

        Set<Long> con2faHabilitado = autenticacionDosFactoresRepository.findByUsuarioIdUsuarioIn(idsUsuario).stream()
                .filter(df -> Boolean.TRUE.equals(df.getEstaHabilitado()))
                .map(df -> df.getUsuario().getIdUsuario())
                .collect(Collectors.toCollection(HashSet::new));

        // En un caso ideal deberiamos consultar todas las verificaciones de una en vez de N consultas,
        // pero por ahora reutilizamos el servicio.
        return usuarios.stream()
                .map(usuario -> build(
                        usuario,
                        rolesPorUsuario.getOrDefault(usuario.getIdUsuario(), List.of()),
                        con2faHabilitado.contains(usuario.getIdUsuario()),
                        verificacionServicio.isIdentityVerified(usuario.getIdUsuario())))
                .toList();
    }

    private UserResponse build(User usuario, List<UserRole> usuarioRoles, boolean dosFactoresHabilitado, boolean identidadVerificada) {
        List<String> roles = usuarioRoles.stream()
                .map(ur -> ur.getRol().getNombreRol())
                .toList();

        List<String> permisos = usuarioRoles.stream()
                .filter(ur -> ur.getRol().getPermisos() != null)
                .flatMap(ur -> ur.getRol().getPermisos().stream())
                .map(Permission::getNombrePermiso)
                .distinct()
                .toList();

        return UserResponse.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .correo(usuario.getCorreo())
                .fechaNacimiento(usuario.getFechaNacimiento())
                .idPais(usuario.getPais() != null ? usuario.getPais().getIdPais() : null)
                .nombrePais(usuario.getPais() != null ? usuario.getPais().getNombrePais() : null)
                .fechaRegistro(usuario.getFechaRegistro())
                .estadoCuenta(usuario.getEstadoCuenta())
                .roles(roles)
                .permisos(permisos)
                .dosFactoresHabilitado(dosFactoresHabilitado)
                .urlFotoPerfil(ProfilePhotoUrl.build(usuario.getUrlFotoPerfil()))
                .identidadVerificada(identidadVerificada)
                .build();
    }
}
