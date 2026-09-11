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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaVerificacionIntegridad;
import uteq.edu.ec.artisync.service.legal.IContratoServicio;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** REQ-NF-020: gateado en PAGO_AUDITAR, mismo criterio que ReporteContratoAutorizacionTest. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ContratoIntegridadControladorTest.ContextoDePrueba.class)
class ContratoIntegridadControladorTest {

    @Configuration
    @EnableMethodSecurity
    static class ContextoDePrueba {

        @Bean
        IContratoServicio contratoServicio() {
            IContratoServicio servicio = mock(IContratoServicio.class);
            when(servicio.verificarIntegridadHash(anyLong())).thenReturn(
                    RespuestaVerificacionIntegridad.builder().idContrato(1L).integro(true).build());
            return servicio;
        }

        @Bean
        ContratoIntegridadControlador contratoIntegridadControlador(IContratoServicio servicio) {
            return new ContratoIntegridadControlador(servicio);
        }
    }

    @Autowired
    private ContratoIntegridadControlador controlador;

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
    @DisplayName("con PAGO_AUDITAR esta autorizado")
    void conPagoAuditar_estaAutorizado() {
        autenticar("PAGO_AUDITAR");

        assertDoesNotThrow(() -> controlador.verificarIntegridad(1L));
    }

    @Test
    @DisplayName("ADMIN pasa por el bypass de rol")
    void admin_pasaPorElBypassDeRol() {
        autenticar("ROLE_ADMIN");

        assertDoesNotThrow(() -> controlador.verificarIntegridad(1L));
    }

    @Test
    @DisplayName("sin PAGO_AUDITAR ni ADMIN, se rechaza con 403")
    void sinPermiso_esRechazado() {
        autenticar("ROLE_CLIENTE");

        assertThrows(AccessDeniedException.class, () -> controlador.verificarIntegridad(1L));
    }
}
