package uteq.edu.ec.artisync.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.entity.seguridad.Permiso;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRolRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));

        List<UsuarioRol> rolesUsuario = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());

        Set<String> authoritiesSet = new HashSet<>();

        for (UsuarioRol ur : rolesUsuario) {
            Rol rol = ur.getRol();
            String roleName = rol.getNombreRol().toUpperCase();
            if (!roleName.startsWith("ROLE_")) {
                authoritiesSet.add("ROLE_" + roleName);
            } else {
                authoritiesSet.add(roleName);
            }

            if (rol.getPermisos() != null) {
                for (Permiso p : rol.getPermisos()) {
                    authoritiesSet.add(p.getNombrePermiso().toUpperCase());
                }
            }
        }

        List<GrantedAuthority> authorities = authoritiesSet.stream()
                .map(SimpleGrantedAuthority::new)
                .map(ga -> (GrantedAuthority) ga)
                .toList();

        boolean enabled = Boolean.TRUE.equals(usuario.getEstadoCuenta());

        return new CustomUserDetails(
                usuario.getIdUsuario(),
                usuario.getCorreo(),
                usuario.getContrasenaHash(),
                enabled,
                true,
                true,
                true,
                authorities
        );
    }
}
