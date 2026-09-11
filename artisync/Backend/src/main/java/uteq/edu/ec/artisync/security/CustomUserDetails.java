package uteq.edu.ec.artisync.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Componente de Seguridad: Implementacion personalizada de UserDetails.
 * 
 * Propósito: Adaptar la entidad Usuario del dominio interno a la estructura de contrato exigida por Spring Security.
 * 
 * Flujo interno: Alberga la identidad, credenciales y coleccion de autoridades (roles/permisos) del usuario en sesion, facilitando el acceso a propiedades personalizadas desde el SecurityContext.
 */
@Getter
public class CustomUserDetails extends User {

    private final Long idUsuario;

    public CustomUserDetails(Long idUsuario, String username, String password, boolean enabled,
                             boolean accountNonExpired, boolean credentialsNonExpired,
                             boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.idUsuario = idUsuario;
    }
}

