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
import uteq.edu.ec.artisync.audit.DatosEventoAuditoria;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.audit.ResultadoAuditoria;
import uteq.edu.ec.artisync.dto.peticion.auditoria.FiltroAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.RespuestaEventoAuditoria;
import uteq.edu.ec.artisync.entity.auditoria.EventoAuditoria;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.auditoria.EventoAuditoriaRepository;
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
class AuditoriaServicioImplTest {

    @Mock
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @Mock
    private IServicioExportacion servicioExportacion;

    @InjectMocks
    private AuditoriaServicioImpl auditoriaServicio;

    @Test
    @DisplayName("registrar() mapea el snapshot inmutable a la entidad y delega el guardado en el repositorio")
    void registrar_DelegaEnElRepositorio() {
        DatosEventoAuditoria datos = new DatosEventoAuditoria(
                LocalDateTime.now(), 3L, "ana@artisync.dev", ModuloAuditoria.SISTEMA, "PAIS_CREAR",
                ResultadoAuditoria.EXITO, "pais", 9L, Map.of("nombrePais", "Ecuador"),
                null, "127.0.0.1", "vitest", "POST", "/api/paises", 12);

        auditoriaServicio.registrar(datos);

        verify(eventoAuditoriaRepository).save(any(EventoAuditoria.class));
    }

    @Test
    @DisplayName("listar() delega en el repositorio con una Specification y mapea la página a PagedResponse")
    void listar_DelegaConSpecificationYMapea() {
        EventoAuditoria evento = eventoDe(1L, "PAIS_CREAR");
        Page<EventoAuditoria> pagina = new PageImpl<>(List.of(evento));
        when(eventoAuditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);

        PagedResponse<?> resultado = auditoriaServicio.listar(new FiltroAuditoria(), PageRequest.of(0, 20));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("obtenerPorId() lanza ExcepcionRecursoNoEncontrado cuando el id no existe")
    void obtenerPorId_Inexistente_LanzaExcepcion() {
        when(eventoAuditoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditoriaServicio.obtenerPorId(99L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerPorId() devuelve el detalle completo, incluido detalleCambio")
    void obtenerPorId_Existente_DevuelveDetalleCompleto() {
        EventoAuditoria evento = eventoDe(5L, "FONDOS_LIBERAR");
        evento.setDetalleCambio(Map.of("monto", 150));
        when(eventoAuditoriaRepository.findById(5L)).thenReturn(Optional.of(evento));

        RespuestaEventoAuditoria respuesta = auditoriaServicio.obtenerPorId(5L);

        assertThat(respuesta.getIdEventoAuditoria()).isEqualTo(5L);
        assertThat(respuesta.getDetalleCambio()).containsEntry("monto", 150);
    }

    @Test
    @DisplayName("exportar() lanza ExcepcionReglaNegocio cuando el filtro supera el tope de filas del formato")
    void exportar_ExcedeTope_LanzaExcepcion() {
        Page<EventoAuditoria> paginaEnorme = new PageImpl<>(
                List.of(eventoDe(1L, "X")), PageRequest.of(0, FormatoReporte.CSV.topeFilas()), 50_001);
        when(eventoAuditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(paginaEnorme);

        assertThatThrownBy(() -> auditoriaServicio.exportar(new FiltroAuditoria(), FormatoReporte.CSV, "admin@artisync.dev"))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("50001")
                .hasMessageContaining("Acote el rango de fechas");
    }

    @Test
    @DisplayName("exportar() construye el ModeloReporte con las columnas y filas de la bitácora y delega en el común")
    void exportar_ConstruyeModeloYDelegaEnElComun() {
        EventoAuditoria evento = eventoDe(1L, "PAIS_CREAR");
        evento.setCorreoActor("ana@artisync.dev");
        Page<EventoAuditoria> pagina = new PageImpl<>(List.of(evento));
        when(eventoAuditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);
        DocumentoGenerado esperado = new DocumentoGenerado(new byte[]{1}, "text/csv", "auditoria.csv");
        when(servicioExportacion.exportar(any(ModeloReporte.class), eq(FormatoReporte.CSV))).thenReturn(esperado);

        DocumentoGenerado resultado = auditoriaServicio.exportar(new FiltroAuditoria(), FormatoReporte.CSV, "admin@artisync.dev");

        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<ModeloReporte> captor = ArgumentCaptor.forClass(ModeloReporte.class);
        verify(servicioExportacion).exportar(captor.capture(), eq(FormatoReporte.CSV));
        ModeloReporte<EventoAuditoria> modelo = captor.getValue();
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

    private EventoAuditoria eventoDe(Long id, String accion) {
        return EventoAuditoria.builder()
                .idEventoAuditoria(id)
                .fechaEvento(LocalDateTime.now())
                .correoActor("actor@artisync.dev")
                .moduloAuditoria(ModuloAuditoria.SISTEMA.name())
                .accionAuditoria(accion)
                .resultadoEvento(ResultadoAuditoria.EXITO.name())
                .build();
    }
}
