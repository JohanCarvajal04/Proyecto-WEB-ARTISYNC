package uteq.edu.ec.artisync.service.shared;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.entity.seguridad.AutenticacionDosFactores;
import uteq.edu.ec.artisync.entity.seguridad.Permiso;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;
import uteq.edu.ec.artisync.repository.seguridad.AutenticacionDosFactoresRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRolRepository;
import uteq.edu.ec.artisync.service.shared.almacenamiento.UrlFotoPerfil;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UsuarioMapper {

    private final UsuarioRolRepository usuarioRolRepository;
    private final AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;

    /** Mapeo de una sola fila (getUserById, tras crear/editar un usuario): una consulta por usuario es aceptable aquí. */
    public UserResponse toUserResponse(Usuario usuario) {
        List<UsuarioRol> usuarioRoles = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());
        boolean dosFactoresHabilitado = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .map(AutenticacionDosFactores::getEstaHabilitado)
                .map(Boolean.TRUE::equals)
                .orElse(false);
        return construir(usuario, usuarioRoles, dosFactoresHabilitado);
    }

    /**
     * Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §8) - mapea
     * una PAGINA completa de usuarios con solo dos consultas adicionales
     * (findByUsuarioIdUsuarioIn de roles y de 2FA), en vez de invocar
     * toUserResponse() por cada fila -- eso disparaba dos consultas por
     * usuario (N+1): con una pagina de 20 usuarios, ~40 consultas en vez de 2.
     */
    public List<UserResponse> toUserResponseList(List<Usuario> usuarios) {
        if (usuarios.isEmpty()) {
            return List.of();
        }

        List<Long> idsUsuario = usuarios.stream().map(Usuario::getIdUsuario).toList();

        Map<Long, List<UsuarioRol>> rolesPorUsuario = usuarioRolRepository.findByUsuarioIdUsuarioIn(idsUsuario).stream()
                .collect(Collectors.groupingBy(ur -> ur.getUsuario().getIdUsuario()));

        Set<Long> con2faHabilitado = autenticacionDosFactoresRepository.findByUsuarioIdUsuarioIn(idsUsuario).stream()
                .filter(df -> Boolean.TRUE.equals(df.getEstaHabilitado()))
                .map(df -> df.getUsuario().getIdUsuario())
                .collect(Collectors.toCollection(HashSet::new));

        return usuarios.stream()
                .map(usuario -> construir(
                        usuario,
                        rolesPorUsuario.getOrDefault(usuario.getIdUsuario(), List.of()),
                        con2faHabilitado.contains(usuario.getIdUsuario())))
                .toList();
    }

    private UserResponse construir(Usuario usuario, List<UsuarioRol> usuarioRoles, boolean dosFactoresHabilitado) {
        List<String> roles = usuarioRoles.stream()
                .map(ur -> ur.getRol().getNombreRol())
                .toList();

        List<String> permisos = usuarioRoles.stream()
                .filter(ur -> ur.getRol().getPermisos() != null)
                .flatMap(ur -> ur.getRol().getPermisos().stream())
                .map(Permiso::getNombrePermiso)
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
                .urlFotoPerfil(UrlFotoPerfil.construir(usuario.getUrlFotoPerfil()))
                .build();
    }
}
