package uteq.edu.ec.artisync.service.auditoria.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.audit.AuditEventData;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.audit.AuditResult;
import uteq.edu.ec.artisync.dto.peticion.auditoria.AuditFilter;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.AuditEventResponse;
import uteq.edu.ec.artisync.entity.auditoria.AuditEvent;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.auditoria.AuditEventRepository;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.IServicioExportacion;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditEventRepository eventoAuditoriaRepository;

    @Mock
    private IServicioExportacion servicioExportacion;

    @InjectMocks
    private AuditServiceImpl auditoriaServicio;

    @Test
    @DisplayName("registrar() mapea el snapshot inmutable a la entidad y delega el guardado en el repositorio")
    void registrar_DelegaEnElRepositorio() {
        AuditEventData datos = new AuditEventData(
                LocalDateTime.now(), 3L, "ana@artisync.dev", AuditModule.SISTEMA, "PAIS_CREAR",
                AuditResult.EXITO, "pais", 9L, Map.of("nombrePais", "Ecuador"),
                null, "127.0.0.1", "vitest", "POST", "/api/paises", 12);

        auditoriaServicio.registrar(datos);

        verify(eventoAuditoriaRepository).save(any(AuditEvent.class));
    }

    @Test
    @DisplayName("listar() delega en el repositorio con una Specification y mapea la página a PagedResponse")
    void listar_DelegaConSpecificationYMapea() {
        AuditEvent evento = eventoDe(1L, "PAIS_CREAR");
        Page<AuditEvent> pagina = new PageImpl<>(List.of(evento));
        when(eventoAuditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);

        PagedResponse<?> resultado = auditoriaServicio.listar(new AuditFilter(), PageRequest.of(0, 20));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("obtenerPorId() lanza ResourceNotFoundException cuando el id no existe")
    void obtenerPorId_Inexistente_LanzaExcepcion() {
        when(eventoAuditoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditoriaServicio.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("obtenerPorId() devuelve el detalle completo, incluido detalleCambio")
    void obtenerPorId_Existente_DevuelveDetalleCompleto() {
        AuditEvent evento = eventoDe(5L, "FONDOS_LIBERAR");
        evento.setDetalleCambio(Map.of("monto", 150));
        when(eventoAuditoriaRepository.findById(5L)).thenReturn(Optional.of(evento));

        AuditEventResponse respuesta = auditoriaServicio.obtenerPorId(5L);

        assertThat(respuesta.getIdEventoAuditoria()).isEqualTo(5L);
        assertThat(respuesta.getDetalleCambio()).containsEntry("monto", 150);
    }

    @Test
    @DisplayName("exportar() lanza BusinessRuleException cuando el filtro supera el tope de filas del formato")
    void exportar_ExcedeTope_LanzaExcepcion() {
        Page<AuditEvent> paginaEnorme = new PageImpl<>(
                List.of(eventoDe(1L, "X")), PageRequest.of(0, FormatoReporte.CSV.topeFilas()), 50_001);
        when(eventoAuditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(paginaEnorme);

        assertThatThrownBy(() -> auditoriaServicio.exportar(new AuditFilter(), FormatoReporte.CSV, "admin@artisync.dev"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("50001")
                .hasMessageContaining("Acote el rango de fechas");
    }

    @Test
    @DisplayName("exportar() construye el ModeloReporte con las columnas y filas de la bitácora y delega en el común")
    void exportar_ConstruyeModeloYDelegaEnElComun() {
        AuditEvent evento = eventoDe(1L, "PAIS_CREAR");
        evento.setCorreoActor("ana@artisync.dev");
        Page<AuditEvent> pagina = new PageImpl<>(List.of(evento));
        when(eventoAuditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);
        DocumentoGenerado esperado = new DocumentoGenerado(new byte[]{1}, "text/csv", "auditoria.csv");
        when(servicioExportacion.exportar(any(ModeloReporte.class), eq(FormatoReporte.CSV))).thenReturn(esperado);

        DocumentoGenerado resultado = auditoriaServicio.exportar(new AuditFilter(), FormatoReporte.CSV, "admin@artisync.dev");

        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<ModeloReporte> captor = ArgumentCaptor.forClass(ModeloReporte.class);
        verify(servicioExportacion).exportar(captor.capture(), eq(FormatoReporte.CSV));
        ModeloReporte<AuditEvent> modelo = captor.getValue();
        assertThat(modelo.getFilas()).containsExactly(evento);
        assertThat(modelo.getGeneradoPor()).isEqualTo("admin@artisync.dev");
        assertThat(modelo.getColumnas()).hasSize(9);
        assertThat(modelo.getColumnas().get(1).extractor().apply(evento)).isEqualTo("ana@artisync.dev");
    }

    @Test
    @DisplayName("listarAccionesDisponibles() delega directamente en el repositorio")
    void listarAccionesDisponibles_Delega() {
        when(eventoAuditoriaRepository.listarAccionesDistintas()).thenReturn(List.of("PAIS_CREAR", "USUARIO_CREAR"));

        assertThat(auditoriaServicio.listarAccionesDisponibles()).containsExactly("PAIS_CREAR", "USUARIO_CREAR");
    }

    @Test
    @DisplayName("exportar() incluye en filtrosAplicados todos los campos del filtro cuando vienen informados")
    void exportar_FiltroCompleto_IncluyeTodosLosFiltrosLegibles() {
        Page<AuditEvent> pagina = new PageImpl<>(List.of(eventoDe(1L, "PAIS_CREAR")));
        when(eventoAuditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);
        when(servicioExportacion.exportar(any(ModeloReporte.class), eq(FormatoReporte.CSV)))
                .thenReturn(new DocumentoGenerado(new byte[]{1}, "text/csv", "auditoria.csv"));

        AuditFilter filtro = new AuditFilter();
        filtro.setCorreoActor("ana@artisync.dev");
        filtro.setAccion("PAIS_CREAR");
        filtro.setModulo("SISTEMA");
        filtro.setResultado("EXITO");
        filtro.setEntidad("pais");
        filtro.setDesde(LocalDateTime.of(2026, 1, 1, 0, 0));
        filtro.setHasta(LocalDateTime.of(2026, 1, 31, 23, 59));

        auditoriaServicio.exportar(filtro, FormatoReporte.CSV, "admin@artisync.dev");

        ArgumentCaptor<ModeloReporte> captor = ArgumentCaptor.forClass(ModeloReporte.class);
        verify(servicioExportacion).exportar(captor.capture(), eq(FormatoReporte.CSV));
        Map<String, String> filtrosAplicados = captor.getValue().getFiltrosAplicados();
        assertThat(filtrosAplicados)
                .containsEntry("Actor", "ana@artisync.dev")
                .containsEntry("Acción", "PAIS_CREAR")
                .containsEntry("Módulo", "SISTEMA")
                .containsEntry("Resultado", "EXITO")
                .containsEntry("Entidad", "pais")
                .containsKey("Desde")
                .containsKey("Hasta");
    }

    @Test
    @DisplayName("listar() ignora un orden por una columna sin índice y usa fechaEvento DESC")
    void listar_OrdenNoPermitido_UsaOrdenSeguroPorDefecto() {
        Page<AuditEvent> pagina = new PageImpl<>(List.of(eventoDe(1L, "PAIS_CREAR")));
        when(eventoAuditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);
        Pageable pageableInseguro = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("detalleCambio"));

        auditoriaServicio.listar(new AuditFilter(), pageableInseguro);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventoAuditoriaRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("fechaEvento")).isNotNull();
        assertThat(captor.getValue().getSort().getOrderFor("fechaEvento").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }

    @Test
    @DisplayName("listar() respeta un orden permitido explícito")
    void listar_OrdenPermitido_SeRespeta() {
        Page<AuditEvent> pagina = new PageImpl<>(List.of(eventoDe(1L, "PAIS_CREAR")));
        when(eventoAuditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);
        Pageable pageableSeguro = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("correoActor"));

        auditoriaServicio.listar(new AuditFilter(), pageableSeguro);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventoAuditoriaRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("correoActor")).isNotNull();
    }

    @Test
    @DisplayName("exportar() con page y size permite exportar por lotes sin exceder el tope")
    void exportar_conPaginacion_permiteExportarPorLotes() {
        Page<AuditEvent> pagina = new PageImpl<>(List.of(eventoDe(1L, "TEST")), PageRequest.of(0, 5000), 50_000);
        when(eventoAuditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);
        DocumentoGenerado esperado = new DocumentoGenerado(new byte[]{1}, "application/pdf", "auditoria_parte_1.pdf");
        when(servicioExportacion.exportar(any(ModeloReporte.class), eq(FormatoReporte.PDF))).thenReturn(esperado);

        DocumentoGenerado resultado = auditoriaServicio.exportar(new AuditFilter(), FormatoReporte.PDF, 0, 5000, "admin@artisync.dev");

        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<ModeloReporte> captor = ArgumentCaptor.forClass(ModeloReporte.class);
        verify(servicioExportacion).exportar(captor.capture(), eq(FormatoReporte.PDF));
        assertThat(captor.getValue().getTitulo()).isEqualTo("Auditoría - Parte 1");
        assertThat(captor.getValue().getSubtitulo()).contains("Parte 1 de 10");
    }

    private AuditEvent eventoDe(Long id, String accion) {
        return AuditEvent.builder()
                .idEventoAuditoria(id)
                .fechaEvento(LocalDateTime.now())
                .correoActor("actor@artisync.dev")
                .moduloAuditoria(AuditModule.SISTEMA.name())
                .accionAuditoria(accion)
                .resultadoEvento(AuditResult.EXITO.name())
                .build();
    }
}
