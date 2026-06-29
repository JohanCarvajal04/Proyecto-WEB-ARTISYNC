package uteq.edu.ec.artisync.security;

import uteq.edu.ec.artisync.model.seguridad.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class UsuarioPrincipal implements UserDetails {

    private Long id;
    private String nombres;
    private String correo;

    @JsonIgnore
    private String contrasena;

    private Collection<? extends GrantedAuthority> authorities;

    public static UsuarioPrincipal build(Usuario usuario) {
        // Roles como ROLE_xxx
        List<GrantedAuthority> authorities = new ArrayList<>();

        usuario.getRoles().forEach(rol -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.getNombreRol().toUpperCase()));
            // Permisos individuales
            rol.getPermisos().forEach(permiso ->
                authorities.add(new SimpleGrantedAuthority(permiso.getNombrePermiso()))
            );
        });

        return new UsuarioPrincipal(
            usuario.getIdUsuario(),
            usuario.getNombres(),
            usuario.getCorreo(),
            usuario.getContrasenaHash(),
            authorities
        );
    }

    @Override public String getUsername() { return correo; }
    @Override public String getPassword() { return contrasena; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioPrincipal)) return false;
        return Objects.equals(id, ((UsuarioPrincipal) o).id);
    }
}
