package uteq.edu.ec.artisync.repository.auditoria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import uteq.edu.ec.artisync.entity.auditoria.EventoAuditoria;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica el trigger de inmutabilidad de V15__modulo_auditoria.sql. Requiere
 * Postgres real: H2 (perfil de test por defecto) no ejecuta Flyway ni soporta
 * funciones/triggers PL/pgSQL, así que estas aserciones darían una falsa
 * garantía si se ejecutaran ahí — el UPDATE simplemente pasaría.
 *
 * No incluye una verificación del GRANT restringido a artisync_app: el perfil
 * postgres-it conecta como la cuenta de DDL
 * (application-postgres-it.properties), no como artisync_app, precisamente
 * "sin depender de que seed_privilegios.sh haya creado la cuenta de
 * privilegios mínimos". Esa parte se verifica manualmente (ver plan).
 *
 * Ejecutar con:
 *   ./mvnw test -Dtest=EventoAuditoriaInmutabilidadIT -Dspring.profiles.active=postgres-it
 * (requiere docker compose -f artisync/docker-compose.yml up -d postgres)
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
class EventoAuditoriaInmutabilidadIT {

    private static final String SQLSTATE_INSUFFICIENT_PRIVILEGE = "42501";

    @Autowired
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("un INSERT normal a través del repositorio funciona sin problemas")
    void insertar_FuncionaNormalmente() {
        EventoAuditoria evento = eventoDePrueba("IT_INSERT_OK");

        EventoAuditoria guardado = eventoAuditoriaRepository.save(evento);

        assertThat(guardado.getIdEventoAuditoria()).isNotNull();
    }

    @Test
    @DisplayName("un UPDATE directo por SQL es rechazado por el trigger de inmutabilidad")
    void update_EsRechazadoPorElTrigger() {
        EventoAuditoria evento = eventoAuditoriaRepository.save(eventoDePrueba("IT_UPDATE_BLOQUEADO"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE auditoria_eventos SET accion_auditoria = 'HACKEADO' WHERE id_evento_auditoria = ?",
                evento.getIdEventoAuditoria()))
                .isInstanceOf(DataAccessException.class)
                .satisfies(EventoAuditoriaInmutabilidadIT::debeVenirDelTriggerDeInmutabilidad);
    }

    @Test
    @DisplayName("un DELETE directo por SQL es rechazado por el trigger de inmutabilidad")
    void delete_EsRechazadoPorElTrigger() {
        EventoAuditoria evento = eventoAuditoriaRepository.save(eventoDePrueba("IT_DELETE_BLOQUEADO"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM auditoria_eventos WHERE id_evento_auditoria = ?", evento.getIdEventoAuditoria()))
                .isInstanceOf(DataAccessException.class)
                .satisfies(EventoAuditoriaInmutabilidadIT::debeVenirDelTriggerDeInmutabilidad);
    }

    @Test
    @DisplayName("un TRUNCATE de la tabla es rechazado por el trigger de inmutabilidad")
    void truncate_EsRechazadoPorElTrigger() {
        eventoAuditoriaRepository.save(eventoDePrueba("IT_TRUNCATE_BLOQUEADO"));

        assertThatThrownBy(() -> jdbcTemplate.execute("TRUNCATE auditoria_eventos"))
                .isInstanceOf(DataAccessException.class)
                .satisfies(EventoAuditoriaInmutabilidadIT::debeVenirDelTriggerDeInmutabilidad);
    }

    @Test
    @DisplayName("un flush de Hibernate tras modificar la entidad gestionada también choca con el trigger")
    void mergeYFlush_TambienEsRechazado() {
        EventoAuditoria evento = eventoAuditoriaRepository.saveAndFlush(eventoDePrueba("IT_MERGE_BLOQUEADO"));
        evento.setAccionAuditoria("HACKEADO_VIA_JPA");

        assertThatThrownBy(() -> eventoAuditoriaRepository.saveAndFlush(evento))
                .satisfies(EventoAuditoriaInmutabilidadIT::debeVenirDelTriggerDeInmutabilidad);
    }

    /**
     * El driver JDBC envuelve el error de PostgreSQL en varias capas
     * (BadSqlGrammarException / JpaSystemException, cuyo getMessage() de nivel
     * superior no repite el texto del servidor), así que la comprobación real
     * —tanto el mensaje como el SQLState 42501— se hace sobre la causa más
     * específica de la cadena, no sobre la excepción de más alto nivel.
     */
    private static void debeVenirDelTriggerDeInmutabilidad(Throwable excepcion) {
        Throwable causaRaiz = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(excepcion);
        assertThat(causaRaiz.getMessage()).contains("AUDITORIA_INMUTABLE");
        if (causaRaiz instanceof java.sql.SQLException sqlException) {
            assertThat(sqlException.getSQLState()).isEqualTo(SQLSTATE_INSUFFICIENT_PRIVILEGE);
        }
    }

    private EventoAuditoria eventoDePrueba(String accion) {
        return EventoAuditoria.builder()
                .fechaEvento(LocalDateTime.now())
                .correoActor("it-inmutabilidad@artisync.dev")
                .moduloAuditoria("SISTEMA")
                .accionAuditoria(accion)
                .resultadoEvento("EXITO")
                .build();
    }
}
