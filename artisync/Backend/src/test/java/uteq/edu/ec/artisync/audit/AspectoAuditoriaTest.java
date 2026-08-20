package uteq.edu.ec.artisync.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.auditoria.IAuditoriaServicio;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El aspecto se ejercita invocando su método @Around directamente (sin tejido
 * real de AspectJ): ProceedingJoinPoint se mockea y Auditable se obtiene por
 * reflexión de un método anotado real de Fixture, así que las expresiones
 * SpEL se evalúan exactamente igual que en producción.
 */
class AspectoAuditoriaTest {

    private IAuditoriaServicio auditoriaServicio;
    private AspectoAuditoria aspecto;

    @BeforeEach
    void setUp() {
        auditoriaServicio = mock(IAuditoriaServicio.class);
        aspecto = new AspectoAuditoria(auditoriaServicio);
        autenticarComoUsuario(7L, "actor@artisync.dev");
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
        ContextoAuditoria.limpiar();
    }

    @Test
    @DisplayName("un método que termina bien devuelve su resultado y registra EXITO con el actor de la sesión")
    void metodoExitoso_DevuelveResultadoYRegistraExito() throws Throwable {
        Auditable auditable = anotacionDe("metodoExito");
        ProceedingJoinPoint pjp = pjpQueDevuelve("metodoExito", new Class<?>[]{Long.class, String.class},
                new Object[]{5L, "hola"}, "resultado-del-negocio");

        Object resultado = aspecto.auditar(pjp, auditable);

        assertThat(resultado).isEqualTo("resultado-del-negocio");

        ArgumentCaptor<DatosEventoAuditoria> captor = ArgumentCaptor.forClass(DatosEventoAuditoria.class);
        verify(auditoriaServicio).registrar(captor.capture());
        DatosEventoAuditoria datos = captor.getValue();
        assertThat(datos.resultado()).isEqualTo(ResultadoAuditoria.EXITO);
        assertThat(datos.accion()).isEqualTo("ACCION_EXITO");
        assertThat(datos.correoActor()).isEqualTo("actor@artisync.dev");
        assertThat(datos.idUsuarioActor()).isEqualTo(7L);
        assertThat(datos.idEntidadAfectada()).isEqualTo(5L);
        assertThat(datos.detalleCambio()).containsEntry("valor", "hola");
        assertThat(datos.mensajeError()).isNull();
    }

    @Test
    @DisplayName("una regla de negocio incumplida se registra como FALLIDO y la excepción original se relanza intacta")
    void reglaDeNegocioFallida_RegistraFallidoYRelanzaLaExcepcionOriginal() throws Throwable {
        Auditable auditable = anotacionDe("metodoExito");
        ExcepcionReglaNegocio excepcion = new ExcepcionReglaNegocio("nombre duplicado");
        ProceedingJoinPoint pjp = pjpQueLanza("metodoExito", new Class<?>[]{Long.class, String.class},
                new Object[]{9L, "x"}, excepcion);

        assertThatThrownBy(() -> aspecto.auditar(pjp, auditable))
                .isSameAs(excepcion);

        ArgumentCaptor<DatosEventoAuditoria> captor = ArgumentCaptor.forClass(DatosEventoAuditoria.class);
        verify(auditoriaServicio).registrar(captor.capture());
        assertThat(captor.getValue().resultado()).isEqualTo(ResultadoAuditoria.FALLIDO);
        assertThat(captor.getValue().mensajeError()).contains("ExcepcionReglaNegocio", "nombre duplicado");
    }

    @Test
    @DisplayName("un AccessDeniedException se registra como DENEGADO, no como FALLIDO, y se relanza")
    void accesoDenegado_RegistraDenegadoYRelanza() throws Throwable {
        Auditable auditable = anotacionDe("metodoExito");
        AccessDeniedException excepcion = new AccessDeniedException("no autorizado");
        ProceedingJoinPoint pjp = pjpQueLanza("metodoExito", new Class<?>[]{Long.class, String.class},
                new Object[]{1L, "x"}, excepcion);

        assertThatThrownBy(() -> aspecto.auditar(pjp, auditable))
                .isSameAs(excepcion);

        ArgumentCaptor<DatosEventoAuditoria> captor = ArgumentCaptor.forClass(DatosEventoAuditoria.class);
        verify(auditoriaServicio).registrar(captor.capture());
        assertThat(captor.getValue().resultado()).isEqualTo(ResultadoAuditoria.DENEGADO);
    }

    @Test
    @DisplayName("si registrar() falla, la operación de negocio no se ve afectada: ni pierde su resultado ni lanza una excepción nueva")
    void fallaAlRegistrar_NuncaTumbaLaOperacionDeNegocio() throws Throwable {
        Auditable auditable = anotacionDe("metodoExito");
        ProceedingJoinPoint pjp = pjpQueDevuelve("metodoExito", new Class<?>[]{Long.class, String.class},
                new Object[]{2L, "y"}, "resultado-intacto");
        doThrow(new RuntimeException("la base de auditoría no responde"))
                .when(auditoriaServicio).registrar(any());

        Object resultado = aspecto.auditar(pjp, auditable);

        assertThat(resultado).isEqualTo("resultado-intacto");
    }

    @Test
    @DisplayName("un SpEL inválido en @Auditable no hace perder el evento: se registra con un detalle de error explicando el motivo")
    void spelInvalidoEnDetalle_RegistraIgualConErrorExplicito() throws Throwable {
        Auditable auditable = anotacionDe("metodoConSpelInvalido");
        ProceedingJoinPoint pjp = pjpQueDevuelve("metodoConSpelInvalido", new Class<?>[]{}, new Object[]{}, "x");

        Object resultado = aspecto.auditar(pjp, auditable);

        assertThat(resultado).isEqualTo("x");
        ArgumentCaptor<DatosEventoAuditoria> captor = ArgumentCaptor.forClass(DatosEventoAuditoria.class);
        verify(auditoriaServicio).registrar(captor.capture());
        assertThat(captor.getValue().detalleCambio()).containsKey("_error_detalle");
    }

    @Test
    @DisplayName("ContextoAuditoria se limpia siempre, incluso cuando el método anotado lanza una excepción")
    void contextoAuditoria_SeLimpiaTrasExcepcion() throws Throwable {
        Auditable auditable = anotacionDe("metodoExito");
        ContextoAuditoria.aportar("antes", Map.of("estado", "ACTIVO"));
        ProceedingJoinPoint pjp = pjpQueLanza("metodoExito", new Class<?>[]{Long.class, String.class},
                new Object[]{3L, "z"}, new RuntimeException("boom"));

        assertThatThrownBy(() -> aspecto.auditar(pjp, auditable)).isInstanceOf(RuntimeException.class);

        assertThat(ContextoAuditoria.drenar()).isEmpty();
    }

    // ------------------------------------------------------------------
    // Fixtures y helpers
    // ------------------------------------------------------------------

    private static class Fixture {

        @Auditable(accion = "ACCION_EXITO", modulo = ModuloAuditoria.SISTEMA,
                entidad = "entidad_prueba", idEntidad = "#id",
                detalle = "{valor: #valor}")
        public String metodoExito(Long id, String valor) {
            return "no invocado directamente por el test";
        }

        @Auditable(accion = "ACCION_SPEL_INVALIDO", modulo = ModuloAuditoria.SISTEMA,
                detalle = "{valor: #noExiste.campo}")
        public String metodoConSpelInvalido() {
            return "no invocado directamente por el test";
        }
    }

    private static Auditable anotacionDe(String nombreMetodo) throws NoSuchMethodException {
        Method metodo = metodoDe(nombreMetodo);
        return metodo.getAnnotation(Auditable.class);
    }

    private static Method metodoDe(String nombreMetodo) throws NoSuchMethodException {
        for (Method m : Fixture.class.getMethods()) {
            if (m.getName().equals(nombreMetodo)) {
                return m;
            }
        }
        throw new NoSuchMethodException(nombreMetodo);
    }

    private ProceedingJoinPoint pjpQueDevuelve(String nombreMetodo, Class<?>[] tipos, Object[] argumentos, Object valorRetorno) throws Throwable {
        ProceedingJoinPoint pjp = pjpBase(nombreMetodo, tipos, argumentos);
        when(pjp.proceed()).thenReturn(valorRetorno);
        return pjp;
    }

    private ProceedingJoinPoint pjpQueLanza(String nombreMetodo, Class<?>[] tipos, Object[] argumentos, Throwable excepcion) throws Throwable {
        ProceedingJoinPoint pjp = pjpBase(nombreMetodo, tipos, argumentos);
        when(pjp.proceed()).thenThrow(excepcion);
        return pjp;
    }

    private ProceedingJoinPoint pjpBase(String nombreMetodo, Class<?>[] tipos, Object[] argumentos) throws NoSuchMethodException {
        Method metodo = Fixture.class.getMethod(nombreMetodo, tipos);
        MethodSignature firma = mock(MethodSignature.class);
        when(firma.getMethod()).thenReturn(metodo);
        when(firma.toShortString()).thenReturn(nombreMetodo);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getSignature()).thenReturn(firma);
        when(pjp.getArgs()).thenReturn(argumentos);
        return pjp;
    }

    private static void autenticarComoUsuario(Long idUsuario, String correo) {
        CustomUserDetails detalles = new CustomUserDetails(
                idUsuario, correo, "x", true, true, true, true, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(detalles, null, List.of()));
    }
}
