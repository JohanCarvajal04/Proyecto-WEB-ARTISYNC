package uteq.edu.ec.artisync.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Componente de Seguridad: Implementacion personalizada de UserDetails.
 * 
 * Propósito: Adaptar la entidad User del dominio interno a la estructura de contrato exigida por Spring Security.
 * 
 * Flujo interno: Alberga la identidad, credenciales y coleccion de autoridades (roles/permisos) del usuario en sesion, facilitando el acceso a propiedades personalizadas desde el SecurityContext.
 */
@Getter
public class CustomUserDetails extends User {

    private final Long idUsuario;

    /**
     * Crea el detalle de usuario de Spring Security, añadiendo el id interno del
     * usuario a los atributos estándar que ya expone {@link User}.
     *
     * @param idUsuario id interno del usuario, usado para resolver relaciones sin
     *                  otra consulta (p. ej. autorización de salas de chat)
     * @param username nombre de usuario (correo) usado por Spring Security
     * @param password contraseña codificada del usuario
     * @param enabled {@code true} si la cuenta está habilitada
     * @param accountNonExpired {@code true} si la cuenta no ha expirado
     * @param credentialsNonExpired {@code true} si las credenciales no han expirado
     * @param accountNonLocked {@code true} si la cuenta no está bloqueada
     * @param authorities roles/permisos concedidos al usuario
     */
    public CustomUserDetails(Long idUsuario, String username, String password, boolean enabled,
                             boolean accountNonExpired, boolean credentialsNonExpired,
                             boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.idUsuario = idUsuario;
    }
}

