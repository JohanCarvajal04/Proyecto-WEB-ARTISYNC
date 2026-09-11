package uteq.edu.ec.artisync.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Componente Core de Seguridad: Implementacion de UserDetailsService.
 * 
 * Propósito: Servir como el origen principal de datos de usuario para el proveedor de autenticacion durante el login y validacion de tokens.
 * 
 * Flujo interno: Consulta la base de datos (UserRepository) por correo electronico, valida el estado de la cuenta, y mapea los privilegios para retornar un CustomUserDetails.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    /**
     * Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md Â§8) -
     * fn_permisos_efectivos_usuario resuelve en una unica llamada STABLE lo
     * que antes eran 4-8 consultas separadas por peticion: findByCorreo +
     * findByUsuarioIdUsuario en usuario_roles + un SELECT por cada rol al
     * acceder a Role.permisos (FetchType.EAGER). Al resolverse en una sola
     * sentencia en el motor, roles y permisos quedan garantizados coherentes
     * entre si (mismo snapshot), algo que las consultas independientes no
     * garantizaban bajo READ COMMITTED si una sincronizacion de roles o
     * permisos se colaba justo entre ellas.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        String permisosJson = usuarioRepository.permisosEfectivos(correo);
        if (permisosJson == null) {
            throw new UsernameNotFoundException("User no encontrado con correo: " + correo);
        }

        JsonNode nodo;
        try {
            nodo = objectMapper.readTree(permisosJson);
        } catch (Exception e) {
            throw new UsernameNotFoundException(
                    "No se pudieron interpretar los permisos efectivos del usuario: " + correo, e);
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        nodo.get("authorities").forEach(a -> authorities.add(new SimpleGrantedAuthority(a.asText())));

        boolean enabled = nodo.get("estadoCuenta").asBoolean();

        return new CustomUserDetails(
                nodo.get("idUsuario").asLong(),
                nodo.get("correo").asText(),
                nodo.get("contrasenaHash").asText(),
                enabled,
                true,
                true,
                true,
                authorities
        );
    }
}

