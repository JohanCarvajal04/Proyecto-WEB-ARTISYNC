package uteq.edu.ec.artisync.controller.auditoria;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uteq.edu.ec.artisync.dto.peticion.auditoria.AuditFilter;
import uteq.edu.ec.artisync.service.auditoria.IAuditService;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AUDITORIA_VER y AUDITORIA_EXPORTAR son dos permisos deliberadamente
 * distintos (V15__modulo_auditoria.sql): consultar la bitácora no implica
 * poder sacar sus datos personales del sistema en un CSV. Este test es el que
 * justifica esa separación — SOPORTE tiene el primero pero no el segundo.
 *
 * Mismo patrón que CategoriaAutorizacionTest: contexto mínimo con
 * @EnableMethodSecurity, porque @PreAuthorize se aplica por AOP y no se
 * evalúa invocando el controlador a mano sin este contexto.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AuditAuthorizationTest.ContextoDePrueba.class)
class AuditAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    static class ContextoDePrueba {

        @Bean
        IAuditService auditoriaServicio() {
            IAuditService servicio = mock(IAuditService.class);
            // Un mock de export() sin stub devuelve null y DocumentResponse.de(null)
            // reventaría con NPE en documento.contentType() — se stubea un documento no
            // nulo para que los casos "autorizado" del test puedan afirmar
            // assertDoesNotThrow sin que el propio mock rompa la aserción.
            when(servicio.export(any(), any(), any()))
                    .thenReturn(new GeneratedDocument(new byte[0], "text/csv", "auditoria.csv"));
            return servicio;
        }

        @Bean
        AuditController auditoriaControlador(IAuditService servicio) {
            return new AuditController(servicio);
        }
    }

    @Autowired
    private AuditController controlador;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String... authorities) {
        List<GrantedAuthority> concedidas = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario", "x", concedidas));
    }

    @Test
    @DisplayName("AUDITOR_FINANCIERO con AUDITORIA_VER puede list la bitácora")
    void listar_auditorFinancieroConAuditoriaVer_estaAutorizado() {
        autenticar("ROLE_AUDITOR_FINANCIERO", "AUDITORIA_VER");

        assertDoesNotThrow(() -> controlador.list(new AuditFilter(), PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("SOPORTE con AUDITORIA_VER puede list, pero export le devuelve 403 porque no tiene AUDITORIA_EXPORTAR")
    void soporte_puedeListarPeroNoExportar() {
        autenticar("ROLE_SOPORTE", "AUDITORIA_VER");

        assertDoesNotThrow(() -> controlador.list(new AuditFilter(), PageRequest.of(0, 20)));
        assertThrows(AccessDeniedException.class, () -> controlador.export(
                new AuditFilter(), ReportFormat.CSV, autenticacionActual()));
    }

    @Test
    @DisplayName("ADMIN pasa los cuatro endpoints por el bypass de rol, sin authorities granulares")
    void admin_sinAuthoritiesGranulares_pasaTodosLosEndpoints() {
        autenticar("ROLE_ADMIN");

        assertDoesNotThrow(() -> controlador.list(new AuditFilter(), PageRequest.of(0, 20)));
        assertDoesNotThrow(() -> controlador.getById(1L));
        assertDoesNotThrow(() -> controlador.listActions());
        assertDoesNotThrow(() -> controlador.export(new AuditFilter(), ReportFormat.CSV, autenticacionActual()));
    }

    @Test
    @DisplayName("un CREADOR sin ninguno de los dos permisos es rechazado en los cuatro endpoints")
    void creadorSinPermisos_esRechazadoEnLosCuatroEndpoints() {
        autenticar("ROLE_CREADOR");

        assertThrows(AccessDeniedException.class, () -> controlador.list(new AuditFilter(), PageRequest.of(0, 20)));
        assertThrows(AccessDeniedException.class, () -> controlador.getById(1L));
        assertThrows(AccessDeniedException.class, () -> controlador.listActions());
        assertThrows(AccessDeniedException.class, () -> controlador.export(
                new AuditFilter(), ReportFormat.CSV, autenticacionActual()));
    }

    private org.springframework.security.core.Authentication autenticacionActual() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
