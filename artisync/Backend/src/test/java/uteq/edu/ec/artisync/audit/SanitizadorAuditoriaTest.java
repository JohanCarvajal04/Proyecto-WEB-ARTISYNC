package uteq.edu.ec.artisync.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizadorAuditoriaTest {

    @Test
    @DisplayName("enmascara el valor de una clave sensible exacta")
    void enmascaraClaveSensibleExacta() {
        Map<String, Object> resultado = SanitizadorAuditoria.sanitizar(Map.of("contrasena", "1234"));

        assertThat(resultado).containsEntry("contrasena", "***");
    }

    @Test
    @DisplayName("la detección de claves sensibles es insensible a mayúsculas y a variaciones del nombre")
    void esInsensibleAMayusculasYVariacionesDeNombre() {
        Map<String, Object> resultado = SanitizadorAuditoria.sanitizar(Map.of(
                "Password", "abc",
                "contrasenaHash", "hash123",
                "codigoRespaldo", "999999",
                "refreshToken", "eyJ..."
        ));

        assertThat(resultado.values()).containsOnly("***");
    }

    @Test
    @DisplayName("un valor no sensible se conserva tal cual")
    void conservaValoresNoSensibles() {
        Map<String, Object> resultado = SanitizadorAuditoria.sanitizar(Map.of("nombrePais", "Ecuador"));

        assertThat(resultado).containsEntry("nombrePais", "Ecuador");
    }

    @Test
    @DisplayName("enmascara claves sensibles dentro de mapas anidados, no solo en el nivel superior")
    void enmascaraDentroDeSubmapas() {
        Map<String, Object> antes = Map.of("correo", "ana@artisync.dev", "password", "secreta");
        Map<String, Object> resultado = SanitizadorAuditoria.sanitizar(Map.of("antes", antes));

        @SuppressWarnings("unchecked")
        Map<String, Object> antesSanitizado = (Map<String, Object>) resultado.get("antes");
        assertThat(antesSanitizado).containsEntry("correo", "ana@artisync.dev");
        assertThat(antesSanitizado).containsEntry("password", "***");
    }

    @Test
    @DisplayName("convierte un LocalDateTime a String: el ObjectMapper de la aplicación no tiene JavaTimeModule registrado")
    void convierteTemporalesAString() {
        LocalDateTime fecha = LocalDateTime.of(2026, 8, 19, 10, 30);
        Map<String, Object> resultado = SanitizadorAuditoria.sanitizar(Map.of("fechaEvento", fecha));

        assertThat(resultado.get("fechaEvento")).isInstanceOf(String.class);
        assertThat(resultado.get("fechaEvento")).isEqualTo(fecha.toString());
    }

    @Test
    @DisplayName("trunca un String que excede la longitud máxima individual")
    void truncaStringsLargos() {
        String textoLargo = "x".repeat(600);
        Map<String, Object> resultado = SanitizadorAuditoria.sanitizar(Map.of("observacion", textoLargo));

        String valor = (String) resultado.get("observacion");
        assertThat(valor.length()).isLessThanOrEqualTo(501);
        assertThat(valor).endsWith("…");
    }

    @Test
    @DisplayName("un mapa vacío o nulo se normaliza a un mapa vacío, sin lanzar")
    void mapaVacioONulo_NoLanza() {
        assertThat(SanitizadorAuditoria.sanitizar(null)).isEmpty();
        assertThat(SanitizadorAuditoria.sanitizar(Map.of())).isEmpty();
    }

    @Test
    @DisplayName("normaliza listas recursivamente, enmascarando los elementos que sean mapas con claves sensibles")
    void normalizaListasRecursivamente() {
        Map<String, Object> resultado = SanitizadorAuditoria.sanitizar(
                Map.of("permisos", List.of("USUARIO_VER", "USUARIO_CREAR")));

        assertThat(resultado.get("permisos")).isEqualTo(List.of("USUARIO_VER", "USUARIO_CREAR"));
    }
}
