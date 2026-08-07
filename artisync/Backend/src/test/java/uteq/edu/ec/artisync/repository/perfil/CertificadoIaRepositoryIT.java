package uteq.edu.ec.artisync.repository.perfil;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requiere Postgres real (docker compose up -d postgres): fn_listar_cola_verificacion
 * es PL/pgSQL, no soportado por el H2 usado en el resto de pruebas de integración.
 * Ejecutar con: ./mvnw test -Dtest=CertificadoIaRepositoryIT -Dspring.profiles.active=postgres-it
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
class CertificadoIaRepositoryIT {

    @Autowired
    private CertificadoIaRepository certificadoIaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Sql(statements = {
            "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                    "VALUES (9001, 'Ana', 'Creadora', 'ana.cola@test.dev', 'x', true)",
            "INSERT INTO perfiles_creadores (id_perfil, id_usuario) VALUES (9001, 9001)",
            "INSERT INTO certificados_ia (id_certificado, id_perfil, id_estado_verificacion, url_documento_s3, tipo_documento) " +
                    "SELECT 9001, 9001, id_estado_verificacion, 'ref.jpg', 'IDENTIDAD' " +
                    "FROM estados_verificacion WHERE nombre_estado = 'PENDIENTE'"
    })
    void listarCola_devuelveFilaPendienteConNombreDeCreador() {
        var cola = certificadoIaRepository.listarCola("PENDIENTE", 10, 0);

        assertThat(cola).anySatisfy(fila -> {
            assertThat(fila.getIdCertificado()).isEqualTo(9001L);
            assertThat(fila.getNombreCreador()).isEqualTo("Ana Creadora");
            assertThat(fila.getNombreEstado()).isEqualTo("PENDIENTE");
        });
    }

    @Test
    @Sql(statements = {
            "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                    "VALUES (9002, 'Beto', 'Moderador', 'beto.mod@test.dev', 'x', true), " +
                    "(9003, 'Cati', 'Creadora', 'cati.creadora@test.dev', 'x', true)",
            "INSERT INTO perfiles_creadores (id_perfil, id_usuario) VALUES (9003, 9003)",
            "INSERT INTO certificados_ia (id_certificado, id_perfil, id_estado_verificacion, url_documento_s3, tipo_documento) " +
                    "SELECT 9002, 9003, id_estado_verificacion, 'ref.jpg', 'IDENTIDAD' " +
                    "FROM estados_verificacion WHERE nombre_estado = 'PENDIENTE'"
    })
    void registrarDecision_escribeEstadoModeradorYMarcaDocumentoEliminado() {
        Long idEstadoAprobado = obtenerIdEstado("APROBADO");

        certificadoIaRepository.registrarDecision(9002L, idEstadoAprobado, 9002L, "Documento verificado a simple vista.");

        CertificadoIa actualizado = certificadoIaRepository.findById(9002L).orElseThrow();
        assertThat(actualizado.getEstadoVerificacion().getIdEstadoVerificacion()).isEqualTo(idEstadoAprobado);
        assertThat(actualizado.getModerador().getIdUsuario()).isEqualTo(9002L);
        assertThat(actualizado.isDocumentoEliminado()).isTrue();
        assertThat(actualizado.getNotaModerador()).isEqualTo("Documento verificado a simple vista.");
    }

    @Test
    @Sql(statements = {
            "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                    "VALUES (9004, 'Beto', 'Moderador', 'beto.mod2@test.dev', 'x', true), " +
                    "(9005, 'Dani', 'Creadora', 'dani.creadora@test.dev', 'x', true)",
            "INSERT INTO perfiles_creadores (id_perfil, id_usuario) VALUES (9005, 9005)",
            "INSERT INTO certificados_ia (id_certificado, id_perfil, id_estado_verificacion, url_documento_s3, tipo_documento) " +
                    "SELECT 9004, 9005, id_estado_verificacion, 'ref.jpg', 'IDENTIDAD' " +
                    "FROM estados_verificacion WHERE nombre_estado = 'PENDIENTE'"
    })
    void registrarDecision_requiereAclaracion_noMarcaDocumentoEliminado() {
        Long idEstadoRequiereAclaracion = obtenerIdEstado("REQUIERE_ACLARACION");

        certificadoIaRepository.registrarDecision(9004L, idEstadoRequiereAclaracion, 9004L, "Falta el reverso del documento.");

        CertificadoIa actualizado = certificadoIaRepository.findById(9004L).orElseThrow();
        assertThat(actualizado.getEstadoVerificacion().getIdEstadoVerificacion()).isEqualTo(idEstadoRequiereAclaracion);
        assertThat(actualizado.isDocumentoEliminado()).isFalse();
    }

    @Test
    @Sql(statements = {
            "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                    "VALUES (9006, 'Beto', 'Moderador', 'beto.mod3@test.dev', 'x', true), " +
                    "(9007, 'Eva', 'Creadora', 'eva.creadora@test.dev', 'x', true)",
            "INSERT INTO perfiles_creadores (id_perfil, id_usuario) VALUES (9007, 9007)",
            "INSERT INTO certificados_ia (id_certificado, id_perfil, id_estado_verificacion, url_documento_s3, tipo_documento) " +
                    "SELECT 9006, 9007, id_estado_verificacion, 'ref.jpg', 'IDENTIDAD' " +
                    "FROM estados_verificacion WHERE nombre_estado = 'PENDIENTE'"
    })
    void registrarDecision_certificadoYaNoPendiente_lanzaExcepcion() {
        Long idEstadoAprobado = obtenerIdEstado("APROBADO");
        Long idEstadoRechazado = obtenerIdEstado("RECHAZADO");
        certificadoIaRepository.registrarDecision(9006L, idEstadoAprobado, 9006L, "Primera decisión.");

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataAccessException.class,
                () -> certificadoIaRepository.registrarDecision(9006L, idEstadoRechazado, 9006L, "Segunda decisión, no debería aplicarse."));
    }

    private Long obtenerIdEstado(String nombre) {
        return jdbcTemplate.queryForObject(
                "SELECT id_estado_verificacion FROM estados_verificacion WHERE nombre_estado = ?",
                Long.class, nombre);
    }
}
