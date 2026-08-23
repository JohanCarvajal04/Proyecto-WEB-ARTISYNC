package uteq.edu.ec.artisync.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §8):
 * {@link CustomUserDetailsService} ahora construye el {@link UserDetails} a
 * partir del JSONB de fn_permisos_efectivos_usuario en vez de recorrer
 * usuario_roles + Rol.permisos en Java, asi que estas pruebas fijan el
 * contrato de ese JSON en vez de mockear repositorios de roles/permisos.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(usuarioRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("construye authorities con roles ROLE_* y permisos deduplicados del JSONB")
    void loadUserByUsername_construyeAuthoritiesDesdeJson() {
        String json = """
                {
                  "idUsuario": 1,
                  "correo": "admin@example.com",
                  "contrasenaHash": "hash-bcrypt",
                  "estadoCuenta": true,
                  "authorities": ["ROLE_ADMIN", "USUARIO_VER", "USUARIO_EDITAR"]
                }
                """;
        given(usuarioRepository.permisosEfectivos("admin@example.com")).willReturn(json);

        UserDetails userDetails = service.loadUserByUsername("admin@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("admin@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("hash-bcrypt");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "USUARIO_VER", "USUARIO_EDITAR");
        assertThat(((CustomUserDetails) userDetails).getIdUsuario()).isEqualTo(1L);
    }

    @Test
    @DisplayName("marca la cuenta deshabilitada cuando estadoCuenta es false")
    void loadUserByUsername_cuentaDeshabilitada() {
        String json = """
                {"idUsuario": 2, "correo": "inactivo@example.com", "contrasenaHash": "x",
                 "estadoCuenta": false, "authorities": []}
                """;
        given(usuarioRepository.permisosEfectivos("inactivo@example.com")).willReturn(json);

        UserDetails userDetails = service.loadUserByUsername("inactivo@example.com");

        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("lanza UsernameNotFoundException cuando fn_permisos_efectivos_usuario devuelve NULL")
    void loadUserByUsername_usuarioInexistente() {
        given(usuarioRepository.permisosEfectivos("fantasma@example.com")).willReturn(null);

        assertThatThrownBy(() -> service.loadUserByUsername("fantasma@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("lanza UsernameNotFoundException si el JSONB devuelto es ilegible")
    void loadUserByUsername_jsonInvalido() {
        given(usuarioRepository.permisosEfectivos("raro@example.com")).willReturn("no-es-json");

        assertThatThrownBy(() -> service.loadUserByUsername("raro@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
