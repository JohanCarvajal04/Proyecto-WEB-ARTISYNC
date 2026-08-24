package uteq.edu.ec.artisync.repository.seguridad;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de regresión (docs de auditoría, hallazgo CR-01) para
 * fn_configurar_2fa (Fase 3, anomalía A4).
 *
 * GET DIAGNOSTICS vivía fuera del IF que envuelve el INSERT de códigos de
 * respaldo: con p_hashes NULL o vacío, capturaba el ROW_COUNT del DELETE
 * anterior en vez de 0, contradiciendo el contrato documentado de la función
 * ("Devuelve el número de códigos de respaldo insertados"). Es un caso
 * borde real: TwoFactorServiceImpl.setup2Fa siempre envía 8 hashes hoy, pero
 * cualquier llamante que reconfigure 2FA sin renovar los códigos de
 * respaldo — o que sencillamente reactive el secreto sin pasar códigos —
 * ejercitaría exactamente esta rama.
 *
 * Requiere Postgres real: PL/pgSQL no es soportado por H2 (perfil de test
 * por defecto).
 *
 * Ejecutar con:
 *   ./mvnw test -Dtest=Configurar2FaRetornoIT -Dspring.profiles.active=postgres-it
 * (requiere docker compose -f artisync/docker-compose.yml up -d postgres)
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
class Configurar2FaRetornoIT {

    private static final long ID_USUARIO = 9301L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM codigos_respaldo_2fa WHERE id_usuario = ?", ID_USUARIO);
        jdbcTemplate.update("DELETE FROM autenticacion_dos_factores WHERE id_usuario = ?", ID_USUARIO);
        jdbcTemplate.update("DELETE FROM usuarios WHERE id_usuario = ?", ID_USUARIO);
    }

    private void sembrarUsuarioConOchoCodigos() {
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Test', 'Configurar2FA', 'configurar-2fa@test.dev', 'x', true)",
                ID_USUARIO);
        for (int i = 0; i < 8; i++) {
            jdbcTemplate.update(
                    "INSERT INTO codigos_respaldo_2fa (id_usuario, codigo_hash, usado) VALUES (?, ?, false)",
                    ID_USUARIO, "hash-previo-" + i);
        }
    }

    @Test
    @DisplayName("CR-01: con p_hashes NULL devuelve 0, no el ROW_COUNT del DELETE previo")
    void configurar2Fa_hashesNulos_devuelveCero() {
        sembrarUsuarioConOchoCodigos();

        Integer total = jdbcTemplate.queryForObject(
                "SELECT fn_configurar_2fa(?, ?, NULL)", Integer.class, ID_USUARIO, "secreto-totp");

        assertThat(total).isEqualTo(0);
    }

    @Test
    @DisplayName("CR-01: con p_hashes vacío devuelve 0, no el ROW_COUNT del DELETE previo")
    void configurar2Fa_hashesVacios_devuelveCero() {
        sembrarUsuarioConOchoCodigos();

        Integer total = jdbcTemplate.queryForObject(
                "SELECT fn_configurar_2fa(?, ?, ARRAY[]::TEXT[])", Integer.class, ID_USUARIO, "secreto-totp");

        assertThat(total).isEqualTo(0);
    }

    @Test
    @DisplayName("con 8 hashes nuevos devuelve 8 (comportamiento normal, sin regresión)")
    void configurar2Fa_ochoHashes_devuelveOcho() {
        sembrarUsuarioConOchoCodigos();

        Integer total = jdbcTemplate.queryForObject(
                "SELECT fn_configurar_2fa(?, ?, ARRAY['h1','h2','h3','h4','h5','h6','h7','h8'])",
                Integer.class, ID_USUARIO, "secreto-totp");

        assertThat(total).isEqualTo(8);
    }
}
