package uteq.edu.ec.artisync.service.shared;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.dto.response.UserResponse;
import uteq.edu.ec.artisync.entity.seguridad.AutenticacionDosFactores;
import uteq.edu.ec.artisync.entity.seguridad.Permiso;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;
import uteq.edu.ec.artisync.repository.AutenticacionDosFactoresRepository;
import uteq.edu.ec.artisync.repository.UsuarioRolRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UsuarioMapper {

    private final UsuarioRolRepository usuarioRolRepository;
    private final AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;

    public UserResponse toUserResponse(Usuario usuario) {
        List<UsuarioRol> usuarioRoles = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());

        List<String> roles = usuarioRoles.stream()
                .map(ur -> ur.getRol().getNombreRol())
                .toList();

        List<String> permisos = usuarioRoles.stream()
                .filter(ur -> ur.getRol().getPermisos() != null)
                .flatMap(ur -> ur.getRol().getPermisos().stream())
                .map(Permiso::getNombrePermiso)
                .distinct()
                .toList();

        AutenticacionDosFactores dosFactores = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElse(null);

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
                .dosFactoresHabilitado(dosFactores != null && Boolean.TRUE.equals(dosFactores.getEstaHabilitado()))
                .build();
    }
}
