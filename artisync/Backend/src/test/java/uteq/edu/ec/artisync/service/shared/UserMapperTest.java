package uteq.edu.ec.artisync.service.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.entity.seguridad.*;
import uteq.edu.ec.artisync.repository.seguridad.TwoFactorAuthenticationRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRoleRepository;
import uteq.edu.ec.artisync.service.perfil.IVerificacionServicio;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link UserMapper} ensambla el {@link UserResponse} de autenticacion:
 * roles, permisos deduplicados y el flag de 2FA dependen de repositorios
 * separados, asi que se prueban por separado de los servicios que lo llaman.
 */
@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock private UserRoleRepository usuarioRolRepository;
    @Mock private TwoFactorAuthenticationRepository autenticacionDosFactoresRepository;
    @Mock private IVerificacionServicio verificacionServicio;

    @InjectMocks
    private UserMapper usuarioMapper;

    private User usuario;

    @BeforeEach
    void setUp() {
        Country pais = Country.builder().idPais(1L).nombrePais("Ecuador").build();
        usuario = User.builder().idUsuario(1L).nombres("Ana").apellidos("Diaz")
                .correo("ana@test.com").pais(pais).estadoCuenta(true).build();
    }

    @Test
    @DisplayName("toUserResponse incluye roles y permisos deduplicados de todos los roles del usuario")
    void toUserResponse_incluyeRolesYPermisosDeduplicados() {
        Permission permisoComun = Permission.builder().idPermiso(1L).nombrePermiso("CATALOGO_VER").build();
        Role rolCliente = Role.builder().idRol(1L).nombreRol("CLIENTE").permisos(Set.of(permisoComun)).build();
        Role rolCreador = Role.builder().idRol(2L).nombreRol("CREADOR").permisos(Set.of(permisoComun)).build();

        given(usuarioRolRepository.findByUsuarioIdUsuario(1L)).willReturn(List.of(
                UserRole.builder().rol(rolCliente).build(),
                UserRole.builder().rol(rolCreador).build()
        ));
        given(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.empty());

        UserResponse respuesta = usuarioMapper.toUserResponse(usuario);

        assertThat(respuesta.getRoles()).containsExactlyInAnyOrder("CLIENTE", "CREADOR");
        assertThat(respuesta.getPermisos()).containsExactly("CATALOGO_VER");
        assertThat(respuesta.getIdPais()).isEqualTo(1L);
        assertThat(respuesta.getNombrePais()).isEqualTo("Ecuador");
        assertThat(respuesta.isDosFactoresHabilitado()).isFalse();
    }

    @Test
    @DisplayName("toUserResponse ignora roles sin permisos asignados")
    void toUserResponse_ignoraRolesSinPermisos() {
        Role rolSinPermisos = Role.builder().idRol(3L).nombreRol("MODERADOR").permisos(null).build();
        given(usuarioRolRepository.findByUsuarioIdUsuario(1L)).willReturn(List.of(UserRole.builder().rol(rolSinPermisos).build()));
        given(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.empty());

        UserResponse respuesta = usuarioMapper.toUserResponse(usuario);

        assertThat(respuesta.getRoles()).containsExactly("MODERADOR");
        assertThat(respuesta.getPermisos()).isEmpty();
    }

    @Test
    @DisplayName("toUserResponse marca 2FA habilitado cuando el registro existe y esta activo")
    void toUserResponse_marca2faHabilitado() {
        TwoFactorAuthentication dosFactores = TwoFactorAuthentication.builder().estaHabilitado(true).build();
        given(usuarioRolRepository.findByUsuarioIdUsuario(1L)).willReturn(List.of());
        given(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(dosFactores));

        assertThat(usuarioMapper.toUserResponse(usuario).isDosFactoresHabilitado()).isTrue();
    }

    @Test
    @DisplayName("toUserResponse no marca 2FA habilitado si el registro existe pero esta desactivado")
    void toUserResponse_no2faSiDesactivado() {
        TwoFactorAuthentication dosFactores = TwoFactorAuthentication.builder().estaHabilitado(false).build();
        given(usuarioRolRepository.findByUsuarioIdUsuario(1L)).willReturn(List.of());
        given(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(dosFactores));

        assertThat(usuarioMapper.toUserResponse(usuario).isDosFactoresHabilitado()).isFalse();
    }

    @Test
    @DisplayName("toUserResponse deja pais nulo cuando el usuario no tiene pais asignado")
    void toUserResponse_sinPais() {
        User sinPais = User.builder().idUsuario(2L).nombres("Luis").apellidos("Perez").correo("luis@test.com").build();
        given(usuarioRolRepository.findByUsuarioIdUsuario(2L)).willReturn(List.of());
        given(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(2L)).willReturn(Optional.empty());

        UserResponse respuesta = usuarioMapper.toUserResponse(sinPais);

        assertThat(respuesta.getIdPais()).isNull();
        assertThat(respuesta.getNombrePais()).isNull();
    }

    // ── toUserResponseList: Fase 2 rendimiento (batch, sin N+1) ─────────────
    // docs/basedatos/PLAN-CONCURRENCIA-SP.md §8: verifica que el mapeo por
    // lotes use SOLO las dos consultas IN (...) -- nunca findByUsuarioIdUsuario
    // (singular) -- y que cada usuario reciba exactamente sus propios roles y
    // su propio flag de 2FA, sin mezclarse entre filas de la misma pagina.

    @Test
    @DisplayName("toUserResponseList devuelve lista vacia sin consultar repositorios")
    void toUserResponseList_listaVacia_noConsulta() {
        assertThat(usuarioMapper.toUserResponseList(List.of())).isEmpty();
        verifyNoInteractions(usuarioRolRepository, autenticacionDosFactoresRepository);
    }

    @Test
    @DisplayName("toUserResponseList asigna a cada usuario solo sus propios roles y su propio flag de 2FA")
    void toUserResponseList_asignaRolesY2faPorUsuarioSinMezclar() {
        User usuario2 = User.builder().idUsuario(2L).nombres("Luis").apellidos("Perez").correo("luis@test.com").build();

        Permission permiso = Permission.builder().idPermiso(1L).nombrePermiso("CATALOGO_VER").build();
        Role rolCliente = Role.builder().idRol(1L).nombreRol("CLIENTE").permisos(Set.of(permiso)).build();
        Role rolAdmin = Role.builder().idRol(2L).nombreRol("ADMIN").permisos(Set.of()).build();

        given(usuarioRolRepository.findByUsuarioIdUsuarioIn(List.of(1L, 2L))).willReturn(List.of(
                UserRole.builder().usuario(usuario).rol(rolCliente).build(),
                UserRole.builder().usuario(usuario2).rol(rolAdmin).build()
        ));
        given(autenticacionDosFactoresRepository.findByUsuarioIdUsuarioIn(List.of(1L, 2L))).willReturn(List.of(
                TwoFactorAuthentication.builder().usuario(usuario).estaHabilitado(true).build()
                // usuario2 no tiene fila de 2FA en absoluto: debe quedar en false, no lanzar.
        ));

        List<UserResponse> respuestas = usuarioMapper.toUserResponseList(List.of(usuario, usuario2));

        UserResponse respuesta1 = respuestas.stream().filter(r -> r.getIdUsuario().equals(1L)).findFirst().orElseThrow();
        UserResponse respuesta2 = respuestas.stream().filter(r -> r.getIdUsuario().equals(2L)).findFirst().orElseThrow();

        assertThat(respuesta1.getRoles()).containsExactly("CLIENTE");
        assertThat(respuesta1.getPermisos()).containsExactly("CATALOGO_VER");
        assertThat(respuesta1.isDosFactoresHabilitado()).isTrue();

        assertThat(respuesta2.getRoles()).containsExactly("ADMIN");
        assertThat(respuesta2.getPermisos()).isEmpty();
        assertThat(respuesta2.isDosFactoresHabilitado()).isFalse();

        verify(usuarioRolRepository, never()).findByUsuarioIdUsuario(any());
        verify(autenticacionDosFactoresRepository, never()).findByUsuarioIdUsuario(any());
    }

    @Test
    @DisplayName("toUserResponseList deja roles y permisos vacios para un usuario sin filas en usuario_roles")
    void toUserResponseList_usuarioSinRoles() {
        given(usuarioRolRepository.findByUsuarioIdUsuarioIn(List.of(1L))).willReturn(List.of());
        given(autenticacionDosFactoresRepository.findByUsuarioIdUsuarioIn(List.of(1L))).willReturn(List.of());

        UserResponse respuesta = usuarioMapper.toUserResponseList(List.of(usuario)).get(0);

        assertThat(respuesta.getRoles()).isEmpty();
        assertThat(respuesta.getPermisos()).isEmpty();
        assertThat(respuesta.isDosFactoresHabilitado()).isFalse();
    }
}
