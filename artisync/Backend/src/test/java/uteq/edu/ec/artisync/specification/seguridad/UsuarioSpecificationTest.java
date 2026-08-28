package uteq.edu.ec.artisync.specification.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;
import uteq.edu.ec.artisync.repository.seguridad.RolRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRolRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hallazgo 1.3 (INFORME-REVISION-COMPLETA.md): verifica contra JPA real (no
 * mocks) que la Specification arma correctamente el LIKE case-insensitive y,
 * sobre todo, la subquery de rol — la única parte no trivial, ya que Usuario
 * no tiene una colección de roles mapeada directamente. No requiere Postgres
 * (solo Criteria API estándar, sin PL/pgSQL), corre sobre el H2 del perfil de
 * test por defecto.
 */
@DataJpaTest
class UsuarioSpecificationTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioRolRepository usuarioRolRepository;
    @Autowired private RolRepository rolRepository;

    private Usuario ana;
    private Usuario beto;

    @BeforeEach
    void sembrarDatos() {
        ana = usuarioRepository.save(Usuario.builder()
                .nombres("Ana").apellidos("García").correo("ana.garcia@test.dev")
                .contrasenaHash("x").estadoCuenta(true).build());
        beto = usuarioRepository.save(Usuario.builder()
                .nombres("Beto").apellidos("Pérez").correo("beto.perez@test.dev")
                .contrasenaHash("x").estadoCuenta(false).build());

        Rol admin = rolRepository.save(Rol.builder().nombreRol("ADMIN_TEST_1_3").build());
        usuarioRolRepository.save(UsuarioRol.builder().usuario(ana).rol(admin).build());
    }

    @Test
    @DisplayName("busqueda: coincide por nombres, apellidos o correo, sin distinguir mayúsculas")
    void conFiltros_busquedaCaseInsensitive() {
        List<Usuario> porNombre = usuarioRepository.findAll(
                UsuarioSpecification.conFiltros("ANA", null, null));
        assertThat(porNombre).extracting(Usuario::getIdUsuario).containsExactly(ana.getIdUsuario());

        List<Usuario> porApellido = usuarioRepository.findAll(
                UsuarioSpecification.conFiltros("perez", null, null));
        assertThat(porApellido).extracting(Usuario::getIdUsuario).containsExactly(beto.getIdUsuario());

        List<Usuario> porCorreo = usuarioRepository.findAll(
                UsuarioSpecification.conFiltros("garcia@test", null, null));
        assertThat(porCorreo).extracting(Usuario::getIdUsuario).containsExactly(ana.getIdUsuario());
    }

    @Test
    @DisplayName("estadoCuenta: filtra exactamente por activo/suspendido")
    void conFiltros_estadoCuenta() {
        List<Usuario> activos = usuarioRepository.findAll(
                UsuarioSpecification.conFiltros(null, null, true));
        assertThat(activos).extracting(Usuario::getIdUsuario).containsExactly(ana.getIdUsuario());
    }

    @Test
    @DisplayName("rol: solo devuelve usuarios con ese rol asignado, vía subquery sobre usuario_roles")
    void conFiltros_rol() {
        List<Usuario> conRol = usuarioRepository.findAll(
                UsuarioSpecification.conFiltros(null, "admin_test_1_3", null));
        assertThat(conRol).extracting(Usuario::getIdUsuario).containsExactly(ana.getIdUsuario());

        List<Usuario> sinCoincidencia = usuarioRepository.findAll(
                UsuarioSpecification.conFiltros(null, "ROL_INEXISTENTE", null));
        assertThat(sinCoincidencia).isEmpty();
    }

    @Test
    @DisplayName("sin filtros: devuelve todos")
    void conFiltros_vacio_devuelveTodos() {
        List<Usuario> todos = usuarioRepository.findAll(
                UsuarioSpecification.conFiltros(null, null, null));
        assertThat(todos).extracting(Usuario::getIdUsuario)
                .containsExactlyInAnyOrder(ana.getIdUsuario(), beto.getIdUsuario());
    }
}
