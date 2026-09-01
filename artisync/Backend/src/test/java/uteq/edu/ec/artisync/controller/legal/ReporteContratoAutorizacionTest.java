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
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteContrato;
import uteq.edu.ec.artisync.service.legal.IReporteContratoServicio;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TRANSACCION_VER (ver) y REPORTE_CONTRATO_EXPORTAR (exportar) son permisos
 * deliberadamente distintos (V19__permisos_reportes.sql), mismo criterio que
 * ReporteFinancieroAutorizacionTest y AuditoriaAutorizacionTest.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ReporteContratoAutorizacionTest.ContextoDePrueba.class)
class ReporteContratoAutorizacionTest {

    @Configuration
    @EnableMethodSecurity
    static class ContextoDePrueba {

        @Bean
        IReporteContratoServicio reporteContratoServicio() {
            IReporteContratoServicio servicio = mock(IReporteContratoServicio.class);
            when(servicio.listar(any(), anyInt(), anyInt())).thenReturn(
                    PagedResponse.<uteq.edu.ec.artisync.dto.respuesta.legal.FilaReporteContrato>builder()
                            .content(List.of()).pageNumber(0).pageSize(20).totalElements(0).totalPages(0).last(true)
                            .build());
            when(servicio.exportar(any(), any(), any()))
                    .thenReturn(new DocumentoGenerado(new byte[0], "text/csv", "contratos.csv"));
            return servicio;
        }

        @Bean
        ReporteContratoControlador reporteContratoControlador(IReporteContratoServicio servicio) {
            return new ReporteContratoControlador(servicio);
        }
    }

    @Autowired
    private ReporteContratoControlador controlador;

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
    void listar_auditorFinancieroConTransaccionVer_estaAutorizado() {
        autenticar("ROLE_AUDITOR_FINANCIERO", "TRANSACCION_VER");

        assertDoesNotThrow(() -> controlador.listar(new FiltroReporteContrato(), 0, 20));
    }

    @Test
    @DisplayName("Con TRANSACCION_VER pero sin REPORTE_CONTRATO_EXPORTAR, exportar() devuelve 403")
    void conSoloTransaccionVer_noPuedeExportar() {
        autenticar("TRANSACCION_VER");

        assertDoesNotThrow(() -> controlador.listar(new FiltroReporteContrato(), 0, 20));
        assertThrows(AccessDeniedException.class, () -> controlador.exportar(
                new FiltroReporteContrato(), FormatoReporte.CSV, autenticacionActual()));
    }

    @Test
    @DisplayName("Con solo CONTRATO_VER (el permiso de cliente/creador) ambos endpoints devuelven 403")
    void conSoloContratoVer_esRechazadoEnAmbosEndpoints() {
        autenticar("CONTRATO_VER");

        assertThrows(AccessDeniedException.class, () -> controlador.listar(new FiltroReporteContrato(), 0, 20));
        assertThrows(AccessDeniedException.class, () -> controlador.exportar(
                new FiltroReporteContrato(), FormatoReporte.CSV, autenticacionActual()));
    }

    @Test
    @DisplayName("ADMIN pasa ambos endpoints por el bypass de rol")
    void admin_pasaAmbosEndpoints() {
        autenticar("ROLE_ADMIN");

        assertDoesNotThrow(() -> controlador.listar(new FiltroReporteContrato(), 0, 20));
        assertDoesNotThrow(() -> controlador.exportar(new FiltroReporteContrato(), FormatoReporte.CSV, autenticacionActual()));
    }

    @Test
    @DisplayName("un CREADOR sin ninguno de los dos permisos es rechazado en ambos endpoints")
    void creadorSinPermisos_esRechazadoEnAmbosEndpoints() {
        autenticar("ROLE_CREADOR");

        assertThrows(AccessDeniedException.class, () -> controlador.listar(new FiltroReporteContrato(), 0, 20));
        assertThrows(AccessDeniedException.class, () -> controlador.exportar(
                new FiltroReporteContrato(), FormatoReporte.CSV, autenticacionActual()));
    }
}
