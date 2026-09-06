package uteq.edu.ec.artisync.repository.seguridad;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.perfil.PortafolioRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica contra Postgres real que fn_sincronizar_roles_usuario, ademas del
 * perfil de creador, da de alta el portafolio inicial cuando el rol CREADOR
 * queda entre los asignados -- el mismo par de altas que fn_registrar_usuario
 * ya hacia en el auto-registro, pero que faltaba en la ruta administrativa
 * (fn_crear_usuario_admin / asignacion de roles), dejando creadores dados de
 * alta por un administrador sin portafolio.
 *
 * Requiere Postgres levantado (docker compose up -d postgres). Ejecutar con:
 * ./mvnw test -Dtest=SincronizarRolesUsuarioIT -Dspring.profiles.active=postgres-it
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
class SincronizarRolesUsuarioIT {

    private static final String SEED_USUARIO =
            "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                    "VALUES (9201, 'Carla', 'Ascendida', 'carla.ascendida@test.dev', 'x', true)";

    @Autowired
    private UsuarioRolRepository usuarioRolRepository;

    @Autowired
    private PerfilCreadorRepository perfilCreadorRepository;

    @Autowired
    private PortafolioRepository portafolioRepository;

    @Test
    @Sql(statements = SEED_USUARIO)
    void asignarRolCreador_creaPerfilYPortafolio() {
        usuarioRolRepository.sincronizarRoles(9201L, new String[]{"CREADOR"});

        Long idPerfil = perfilCreadorRepository.findByUsuarioIdUsuario(9201L)
                .orElseThrow(() -> new AssertionError("Se esperaba un perfil de creador para el usuario 9201"))
                .getIdPerfil();

        assertThat(portafolioRepository.findByPerfilIdPerfil(idPerfil)).isPresent();
    }

    @Test
    @Sql(statements = SEED_USUARIO)
    void asignarRolCreadorDosVeces_esIdempotenteYNoDuplicaPortafolio() {
        usuarioRolRepository.sincronizarRoles(9201L, new String[]{"CREADOR"});
        usuarioRolRepository.sincronizarRoles(9201L, new String[]{"CREADOR"});

        Long idPerfil = perfilCreadorRepository.findByUsuarioIdUsuario(9201L)
                .orElseThrow(() -> new AssertionError("Se esperaba un perfil de creador para el usuario 9201"))
                .getIdPerfil();

        assertThat(portafolioRepository.findByPerfilIdPerfil(idPerfil)).isPresent();
    }

    @Test
    @Sql(statements = SEED_USUARIO)
    void asignarSoloRolCliente_noCreaPerfilNiPortafolio() {
        usuarioRolRepository.sincronizarRoles(9201L, new String[]{"CLIENTE"});

        assertThat(perfilCreadorRepository.findByUsuarioIdUsuario(9201L)).isEmpty();
    }
}
