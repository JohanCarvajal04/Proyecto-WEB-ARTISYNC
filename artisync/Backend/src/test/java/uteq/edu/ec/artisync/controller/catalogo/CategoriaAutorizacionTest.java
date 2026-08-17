package uteq.edu.ec.artisync.controller.catalogo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearCategoria;
import uteq.edu.ec.artisync.service.catalogo.ICategoriaServicio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * La gestión de categorías se autoriza por permiso y no por rol. Estaba escrita
 * como hasRole('ADMIN'), lo que devolvía 403 a MODERADOR pese a que la semilla
 * le concede CATEGORIA_GESTIONAR y su panel expone la pantalla de categorías.
 *
 * <p>Levanta un contexto mínimo con seguridad de métodos: @PreAuthorize se
 * aplica por AOP, así que invocar el controlador a mano —como hacen el resto de
 * pruebas de controlador— nunca evaluaría la expresión.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CategoriaAutorizacionTest.ContextoDePrueba.class)
class CategoriaAutorizacionTest {

    @Configuration
    @EnableMethodSecurity
    static class ContextoDePrueba {

        @Bean
        ICategoriaServicio categoriaServicio() {
            return mock(ICategoriaServicio.class);
        }

        @Bean
        CategoriaControlador categoriaControlador(ICategoriaServicio servicio) {
            return new CategoriaControlador(servicio);
        }
    }

    @Autowired
    private CategoriaControlador controlador;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String... authorities) {
        var concedidas = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario", "x", concedidas));
    }

    private PeticionCrearCategoria peticion() {
        return new PeticionCrearCategoria();
    }

    @Test
    void crearCategoria_moderadorConCategoriaGestionar_estaAutorizado() {
        autenticar("ROLE_MODERADOR", "CATEGORIA_GESTIONAR");

        controlador.crearCategoria(peticion()); // no debe lanzar AccessDeniedException
    }

    @Test
    void crearCategoria_administrador_sigueAutorizado() {
        autenticar("ROLE_ADMIN");

        controlador.crearCategoria(peticion());
    }

    @Test
    void crearCategoria_rolSinElPermiso_esRechazado() {
        autenticar("ROLE_CREADOR");

        assertThrows(AccessDeniedException.class, () -> controlador.crearCategoria(peticion()));
    }

    @Test
    void actualizarYEliminar_moderadorConElPermiso_estanAutorizados() {
        autenticar("ROLE_MODERADOR", "CATEGORIA_GESTIONAR");

        controlador.actualizarCategoria(1L, new PeticionActualizarCategoria());
        controlador.eliminarCategoria(1L);
    }

    @Test
    void listarTodas_moderadorConElPermiso_estaAutorizado() {
        autenticar("ROLE_MODERADOR", "CATEGORIA_GESTIONAR");

        controlador.listarTodasLasCategorias();
    }

    @Test
    void listarTodas_sinPermisoNiRol_esRechazado() {
        autenticar("ROLE_CLIENTE");

        assertThrows(AccessDeniedException.class, () -> controlador.listarTodasLasCategorias());
    }

    /** El listado público debe seguir abierto: sin autenticación no debe lanzar. */
    @Test
    void listarActivas_siguePublico() {
        assertDoesNotThrow(() -> controlador.listarCategoriasActivas());
    }
}
