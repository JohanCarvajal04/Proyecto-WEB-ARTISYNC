package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteFinanciero;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaReporteComisiones;
import uteq.edu.ec.artisync.service.legal.IReporteFinancieroServicio;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TRANSACCION_VER (ver) y REPORTE_FINANCIERO_EXPORTAR (exportar) son permisos
 * deliberadamente distintos (V19__permisos_reportes.sql), mismo criterio que
 * AuditoriaAutorizacionTest para AUDITORIA_VER/AUDITORIA_EXPORTAR.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ReporteFinancieroAutorizacionTest.ContextoDePrueba.class)
class ReporteFinancieroAutorizacionTest {

    @Configuration
    @EnableMethodSecurity
    static class ContextoDePrueba {

        @Bean
        IReporteFinancieroServicio reporteFinancieroServicio() {
            IReporteFinancieroServicio servicio = mock(IReporteFinancieroServicio.class);
            when(servicio.obtenerReporteComisiones(any())).thenReturn(
                    new RespuestaReporteComisiones(1L, null, null, null, 0, 0, null, null, null, List.of()));
            when(servicio.exportar(any(), any(), any()))
                    .thenReturn(new DocumentoGenerado(new byte[0], "text/csv", "comisiones.csv"));
            return servicio;
        }

        @Bean
        ReporteFinancieroControlador reporteFinancieroControlador(IReporteFinancieroServicio servicio) {
            return new ReporteFinancieroControlador(servicio);
        }
    }

    @Autowired
    private ReporteFinancieroControlador controlador;

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

    private Authentication autenticacionActual() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    @DisplayName("AUDITOR_FINANCIERO con TRANSACCION_VER puede ver el reporte")
    void obtener_auditorFinancieroConTransaccionVer_estaAutorizado() {
        autenticar("ROLE_AUDITOR_FINANCIERO", "TRANSACCION_VER");

        assertDoesNotThrow(() -> controlador.obtener(new FiltroReporteFinanciero()));
    }

    @Test
    @DisplayName("Con TRANSACCION_VER pero sin REPORTE_FINANCIERO_EXPORTAR, exportar() devuelve 403")
    void conSoloTransaccionVer_noPuedeExportar() {
        autenticar("TRANSACCION_VER");

        assertDoesNotThrow(() -> controlador.obtener(new FiltroReporteFinanciero()));
        assertThrows(AccessDeniedException.class, () -> controlador.exportar(
                new FiltroReporteFinanciero(), FormatoReporte.CSV, autenticacionActual()));
    }

    @Test
    @DisplayName("ADMIN pasa ambos endpoints por el bypass de rol")
    void admin_pasaAmbosEndpoints() {
        autenticar("ROLE_ADMIN");

        assertDoesNotThrow(() -> controlador.obtener(new FiltroReporteFinanciero()));
        assertDoesNotThrow(() -> controlador.exportar(new FiltroReporteFinanciero(), FormatoReporte.CSV, autenticacionActual()));
    }

    @Test
    @DisplayName("un CREADOR sin ninguno de los dos permisos es rechazado en ambos endpoints")
    void creadorSinPermisos_esRechazadoEnAmbosEndpoints() {
        autenticar("ROLE_CREADOR");

        assertThrows(AccessDeniedException.class, () -> controlador.obtener(new FiltroReporteFinanciero()));
        assertThrows(AccessDeniedException.class, () -> controlador.exportar(
                new FiltroReporteFinanciero(), FormatoReporte.CSV, autenticacionActual()));
    }
}
