package uteq.edu.ec.artisync.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uteq.edu.ec.artisync.service.shared.EmailService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H-05 (auditoría de estado 2026-08-26): antes de este cambio no existía
 * ningún test que verificara la cadena de filtros de {@link SecurityConfig}
 * — ni las reglas de {@code permitAll}, ni las cabeceras de seguridad.
 * Verifica en concreto:
 *   - /actuator/health sigue público (lo necesita el healthcheck de Docker).
 *   - /actuator/metrics ya NO es público (antes exponía detalle sin auth).
 *   - la CSP ya no lleva 'unsafe-eval' ni 'unsafe-inline' en script-src.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "JWT_SECRET=f98cf546c1a89c93f0b2f1559868779b76c8c4a4f89d0b676a74c431d1d8ef3f",
    // Redis y el SMTP real no están disponibles en este test (no es un IT
    // contra el stack real); sin esto /actuator/health responde 503 y el
    // test no puede distinguir "el endpoint es público" de "está DOWN".
    // application.properties de test no hereda el management.health.mail.enabled=false
    // del principal (el de test reemplaza por completo al de main en el classpath).
    "management.health.redis.enabled=false",
    "management.health.mail.enabled=false"
})
class SecurityConfigTest {

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailService emailService;

    @Test
    void actuatorHealth_esPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorMetrics_requiereAutenticacion() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorInfo_requiereAutenticacion() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cabeceraCsp_noPermiteUnsafeEvalNiUnsafeInlineEnScripts() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("script-src 'self';"),
                                org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("unsafe-eval")),
                                org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("script-src 'self' 'unsafe-inline'")),
                                // style-src conserva 'unsafe-inline' a propósito (ver comentario en SecurityConfig).
                                org.hamcrest.Matchers.containsString("style-src 'self' 'unsafe-inline'")
                        )));
    }

    @Test
    void rutaProtegidaSinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/contratos/1"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Página pública de servicios (catálogo abierto): la ficha de un creador
     * consulta GET /api/v1/perfiles/{id} sin sesión. No se afirma 200 porque el
     * id puede no existir en la base de este test — lo que importa es que la
     * cadena de filtros no la corte con 401/403 antes de llegar al controlador.
     */
    @Test
    void obtenerPerfilPorId_esPublico() throws Exception {
        int status = mockMvc.perform(get("/api/v1/perfiles/1")).andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(status).isNotIn(401, 403);
    }

    /**
     * /api/v1/perfiles/usuario/{id} expondría la correspondencia usuario->perfil
     * y queda deliberadamente fuera del permitAll de un solo segmento
     * ("/api/v1/perfiles/*"): debe seguir exigiendo sesión.
     */
    @Test
    void obtenerPerfilPorUsuario_requiereAutenticacion() throws Exception {
        mockMvc.perform(get("/api/v1/perfiles/usuario/1"))
                .andExpect(status().isUnauthorized());
    }
}
