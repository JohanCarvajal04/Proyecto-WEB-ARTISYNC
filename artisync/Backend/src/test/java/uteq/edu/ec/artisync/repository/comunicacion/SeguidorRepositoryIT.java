package uteq.edu.ec.artisync.repository.comunicacion;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uteq.edu.ec.artisync.entity.comunicacion.Seguidor;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * REQ-F-009: verifica contra Postgres real el contrato de seguimiento de creadores,
 * incluida la restricción UNIQUE (id_usuario_seguidor, id_perfil_creador) de la tabla
 * seguidores, que es la que garantiza que el contador no se infle con duplicados.
 *
 * Requiere Postgres levantado (docker compose up -d postgres). Ejecutar con:
 * ./mvnw test -Dtest=SeguidorRepositoryIT -Dspring.profiles.active=postgres-it
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
class SeguidorRepositoryIT {

    private static final String SEED_USUARIOS =
            "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                    "VALUES (9101, 'Ana', 'Seguidora', 'ana.seguidora@test.dev', 'x', true), " +
                    "(9102, 'Beto', 'Creador', 'beto.creador@test.dev', 'x', true)";

    private static final String SEED_PERFIL =
            "INSERT INTO perfiles_creadores (id_perfil, id_usuario) VALUES (9102, 9102)";

    @Autowired
    private SeguidorRepository seguidorRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Seguidor nuevoSeguimiento() {
        Usuario seguidor = entityManager.getReference(Usuario.class, 9101L);
        PerfilCreador perfil = entityManager.getReference(PerfilCreador.class, 9102L);
        return Seguidor.builder()
                .usuarioSeguidor(seguidor)
                .perfilCreador(perfil)
                .build();
    }

    @Test
    @Sql(statements = {SEED_USUARIOS, SEED_PERFIL})
    void seguir_persisteYCuentaUnSeguidor() {
        seguidorRepository.saveAndFlush(nuevoSeguimiento());

        assertThat(seguidorRepository.countByPerfilCreadorIdPerfil(9102L)).isEqualTo(1L);
        assertThat(seguidorRepository
                .existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(9101L, 9102L)).isTrue();
        assertThat(seguidorRepository.findByPerfilCreadorIdPerfil(9102L)).hasSize(1);
    }

    @Test
    @Sql(statements = {SEED_USUARIOS, SEED_PERFIL})
    void seguirDosVeces_violaLaRestriccionUnique() {
        seguidorRepository.saveAndFlush(nuevoSeguimiento());

        assertThatThrownBy(() -> seguidorRepository.saveAndFlush(nuevoSeguimiento()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Sql(statements = {SEED_USUARIOS, SEED_PERFIL})
    void dejarDeSeguir_devuelveElContadorACero() {
        Seguidor seguimiento = seguidorRepository.saveAndFlush(nuevoSeguimiento());
        assertThat(seguidorRepository.countByPerfilCreadorIdPerfil(9102L)).isEqualTo(1L);

        seguidorRepository.delete(seguimiento);
        seguidorRepository.flush();

        assertThat(seguidorRepository.countByPerfilCreadorIdPerfil(9102L)).isZero();
        assertThat(seguidorRepository
                .existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(9101L, 9102L)).isFalse();
    }
}
