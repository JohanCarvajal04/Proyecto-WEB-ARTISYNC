package uteq.edu.ec.artisync.controller.seguridad;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionControllerTest {

    private final PermissionController controller = new PermissionController();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getMyPermissions — 401 cuando no hay autenticacion en el contexto")
    void getMyPermissions_sinAutenticacion_devuelve401() {
        SecurityContextHolder.clearContext();

        ResponseEntity<List<String>> respuesta = controller.getMyPermissions();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("getMyPermissions — 401 cuando la autenticacion no esta autenticada")
    void getMyPermissions_noAutenticada_devuelve401() {
        TestingAuthenticationToken authNoAutenticado = new TestingAuthenticationToken("ana", "x");
        authNoAutenticado.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authNoAutenticado);

        ResponseEntity<List<String>> respuesta = controller.getMyPermissions();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("getMyPermissions — filtra los ROLE_ y devuelve solo los permisos")
    void getMyPermissions_filtraRoles_devuelveSoloPermisos() {
        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("CATALOGO_VER"),
                new SimpleGrantedAuthority("USUARIO_EDITAR"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ana", null, authorities));

        ResponseEntity<List<String>> respuesta = controller.getMyPermissions();

        assertThat(respuesta.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(respuesta.getBody()).containsExactlyInAnyOrder("CATALOGO_VER", "USUARIO_EDITAR");
    }
}
