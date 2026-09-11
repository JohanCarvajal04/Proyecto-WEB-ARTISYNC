package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uteq.edu.ec.artisync.dto.peticion.legal.WithdrawalRequestFilter;
import uteq.edu.ec.artisync.dto.peticion.legal.WithdrawalDecisionRequest;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateWithdrawalRequest;
import uteq.edu.ec.artisync.dto.respuesta.legal.CreatorBalanceResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.WithdrawalRequestResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IWithdrawalRequestService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RETIROS_SOLICITAR (creador) y RETIROS_GESTIONAR (auditor financiero) son
 * permisos deliberadamente distintos (V33__permisos_retiros.sql), mismo
 * criterio que ReporteFinancieroAutorizacionTest para
 * TRANSACCION_VER/REPORTE_FINANCIERO_EXPORTAR: cada uno abre solo su propia
 * pantalla/endpoint, y ADMIN pasa ambos por el bypass de rol aunque
 * RETIROS_GESTIONAR no se le asigne como fila de permiso (V32, mismo criterio
 * que con PAGO_AUDITAR/FONDOS_LIBERAR).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = WithdrawalRequestAuthorizationTest.ContextoDePrueba.class)
class WithdrawalRequestAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    static class ContextoDePrueba {

        @Bean
        IWithdrawalRequestService solicitudRetiroServicio() {
            IWithdrawalRequestService servicio = mock(IWithdrawalRequestService.class);
            when(servicio.obtenerSaldo(anyLong())).thenReturn(CreatorBalanceResponse.builder()
                    .saldoDisponible(BigDecimal.ZERO).montoMinimoRetiro(BigDecimal.TEN)
                    .tieneCorreoPaypalConfigurado(true).tieneSolicitudPendiente(false).build());
            when(servicio.solicitar(anyLong(), any())).thenReturn(
                    WithdrawalRequestResponse.builder().idSolicitud(1L).estado("Pendiente").build());
            when(servicio.misSolicitudes(anyLong())).thenReturn(List.of());
            when(servicio.listarCola(any(), any())).thenReturn(Page.empty());
            when(servicio.aprobar(anyLong(), anyLong())).thenReturn(
                    WithdrawalRequestResponse.builder().idSolicitud(1L).estado("Aprobado").build());
            when(servicio.rechazar(anyLong(), anyLong(), anyString())).thenReturn(
                    WithdrawalRequestResponse.builder().idSolicitud(1L).estado("Rechazado").build());
            when(servicio.reintentar(anyLong(), anyLong())).thenReturn(
                    WithdrawalRequestResponse.builder().idSolicitud(1L).estado("Pagado").build());
            return servicio;
        }

        @Bean
        WithdrawalRequestController solicitudRetiroControlador(IWithdrawalRequestService servicio) {
            return new WithdrawalRequestController(servicio);
        }

        @Bean
        WithdrawalRequestAdminController solicitudRetiroAdminControlador(IWithdrawalRequestService servicio) {
            return new WithdrawalRequestAdminController(servicio);
        }
    }

    @Autowired
    private WithdrawalRequestController controladorCreador;

    @Autowired
    private WithdrawalRequestAdminController controladorAdmin;

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

    private CustomUserDetails userDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(200L);
        return user;
    }

    private void llamarEndpointsCreador() {
        controladorCreador.obtenerSaldo(userDetails());
        controladorCreador.solicitar(userDetails(), new CreateWithdrawalRequest());
        controladorCreador.misSolicitudes(userDetails());
    }

    private void llamarEndpointsAdmin() {
        controladorAdmin.listar(new WithdrawalRequestFilter(), PageRequest.of(0, 20));
        controladorAdmin.aprobar(1L, userDetails());
        WithdrawalDecisionRequest peticion = new WithdrawalDecisionRequest();
        peticion.setNotaAdmin("motivo");
        controladorAdmin.rechazar(1L, userDetails(), peticion);
        controladorAdmin.reintentar(1L, userDetails());
    }

    @Test
    @DisplayName("CREADOR con RETIROS_SOLICITAR puede usar sus propios endpoints")
    void creadorConRetirosSolicitar_estaAutorizado() {
        autenticar("ROLE_CREADOR", "RETIROS_SOLICITAR");

        assertDoesNotThrow(this::llamarEndpointsCreador);
    }

    @Test
    @DisplayName("CREADOR con RETIROS_SOLICITAR NO puede usar los endpoints de admin")
    void creadorConRetirosSolicitar_noPuedeUsarEndpointsAdmin() {
        autenticar("ROLE_CREADOR", "RETIROS_SOLICITAR");

        assertThrows(AccessDeniedException.class,
                () -> controladorAdmin.listar(new WithdrawalRequestFilter(), PageRequest.of(0, 20)));
        assertThrows(AccessDeniedException.class, () -> controladorAdmin.aprobar(1L, userDetails()));
    }

    @Test
    @DisplayName("AUDITOR_FINANCIERO con RETIROS_GESTIONAR puede usar la cola de revisión")
    void auditorConRetirosGestionar_estaAutorizado() {
        autenticar("ROLE_AUDITOR_FINANCIERO", "RETIROS_GESTIONAR");

        assertDoesNotThrow(this::llamarEndpointsAdmin);
    }

    @Test
    @DisplayName("AUDITOR_FINANCIERO con solo RETIROS_GESTIONAR no puede solicitar su propio retiro")
    void auditorConSoloRetirosGestionar_noPuedeSolicitarRetiro() {
        autenticar("ROLE_AUDITOR_FINANCIERO", "RETIROS_GESTIONAR");

        assertThrows(AccessDeniedException.class, () -> controladorCreador.obtenerSaldo(userDetails()));
    }

    @Test
    @DisplayName("ADMIN pasa los endpoints de gestión por el bypass de rol")
    void admin_pasaEndpointsDeGestion() {
        autenticar("ROLE_ADMIN");

        assertDoesNotThrow(this::llamarEndpointsAdmin);
    }

    @Test
    @DisplayName("ADMIN sin RETIROS_SOLICITAR no puede usar los endpoints propios del creador")
    void admin_sinRetirosSolicitar_noPuedeUsarEndpointsDeCreador() {
        // Los endpoints del creador (solicitar/ver saldo/mi historial) solo llevan
        // RETIROS_SOLICITAR, sin comodín de rol: no tiene sentido de negocio que
        // un ADMIN "solicite su propio retiro" (a diferencia de la cola de
        // gestión, donde el comodín sí aplica, ver WithdrawalRequestAdminController).
        autenticar("ROLE_ADMIN");

        assertThrows(AccessDeniedException.class, () -> controladorCreador.obtenerSaldo(userDetails()));
    }

    @Test
    @DisplayName("un CLIENTE sin ninguno de los dos permisos es rechazado en todos los endpoints nuevos")
    void clienteSinPermisos_esRechazadoEnTodosLosEndpoints() {
        autenticar("ROLE_CLIENTE");

        assertThrows(AccessDeniedException.class, () -> controladorCreador.obtenerSaldo(userDetails()));
        assertThrows(AccessDeniedException.class,
                () -> controladorAdmin.listar(new WithdrawalRequestFilter(), PageRequest.of(0, 20)));
    }
}
