package uteq.edu.ec.artisync.specification.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import uteq.edu.ec.artisync.entity.seguridad.Role;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.entity.seguridad.UserRole;
import uteq.edu.ec.artisync.repository.seguridad.RoleRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRoleRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hallazgo 1.3 (INFORME-REVISION-COMPLETA.md): verifica contra JPA real (no
 * mocks) que la Specification arma correctamente el LIKE case-insensitive y,
 * sobre todo, la subquery de rol — la única parte no trivial, ya que User
 * no tiene una colección de roles mapeada directamente. No requiere Postgres
 * (solo Criteria API estándar, sin PL/pgSQL), corre sobre el H2 del perfil de
 * test por defecto.
 */
@DataJpaTest
class UserSpecificationTest {

    @Autowired private UserRepository usuarioRepository;
    @Autowired private UserRoleRepository usuarioRolRepository;
    @Autowired private RoleRepository rolRepository;

    private User ana;
    private User beto;

    @BeforeEach
    void sembrarDatos() {
        ana = usuarioRepository.save(User.builder()
                .nombres("Ana").apellidos("García").correo("ana.garcia@test.dev")
                .contrasenaHash("x").estadoCuenta(true).build());
        beto = usuarioRepository.save(User.builder()
                .nombres("Beto").apellidos("Pérez").correo("beto.perez@test.dev")
                .contrasenaHash("x").estadoCuenta(false).build());

        Role admin = rolRepository.save(Role.builder().nombreRol("ADMIN_TEST_1_3").build());
        usuarioRolRepository.save(UserRole.builder().usuario(ana).rol(admin).build());
    }

    @Test
    @DisplayName("busqueda: coincide por nombres, apellidos o correo, sin distinguir mayúsculas")
    void conFiltros_busquedaCaseInsensitive() {
        List<User> porNombre = usuarioRepository.findAll(
                UserSpecification.conFiltros("ANA", null, null));
        assertThat(porNombre).extracting(User::getIdUsuario).containsExactly(ana.getIdUsuario());

        List<User> porApellido = usuarioRepository.findAll(
                UserSpecification.conFiltros("perez", null, null));
        assertThat(porApellido).extracting(User::getIdUsuario).containsExactly(beto.getIdUsuario());

        List<User> porCorreo = usuarioRepository.findAll(
                UserSpecification.conFiltros("garcia@test", null, null));
        assertThat(porCorreo).extracting(User::getIdUsuario).containsExactly(ana.getIdUsuario());
    }

    @Test
    @DisplayName("estadoCuenta: filtra exactamente por activo/suspendido")
    void conFiltros_estadoCuenta() {
        List<User> activos = usuarioRepository.findAll(
                UserSpecification.conFiltros(null, null, true));
        assertThat(activos).extracting(User::getIdUsuario).containsExactly(ana.getIdUsuario());
    }

    @Test
    @DisplayName("rol: solo devuelve usuarios con ese rol asignado, vía subquery sobre usuario_roles")
    void conFiltros_rol() {
        List<User> conRol = usuarioRepository.findAll(
                UserSpecification.conFiltros(null, "admin_test_1_3", null));
        assertThat(conRol).extracting(User::getIdUsuario).containsExactly(ana.getIdUsuario());

        List<User> sinCoincidencia = usuarioRepository.findAll(
                UserSpecification.conFiltros(null, "ROL_INEXISTENTE", null));
        assertThat(sinCoincidencia).isEmpty();
    }

    @Test
    @DisplayName("sin filtros: devuelve todos")
    void conFiltros_vacio_devuelveTodos() {
        List<User> todos = usuarioRepository.findAll(
                UserSpecification.conFiltros(null, null, null));
        assertThat(todos).extracting(User::getIdUsuario)
                .containsExactlyInAnyOrder(ana.getIdUsuario(), beto.getIdUsuario());
    }
}
