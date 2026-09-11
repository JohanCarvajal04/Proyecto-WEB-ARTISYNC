package uteq.edu.ec.artisync.service.seguridad.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import uteq.edu.ec.artisync.dto.peticion.seguridad.UserFilter;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.service.shared.UserMapper;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.legal.IPdfGeneracionServicio;
import uteq.edu.ec.artisync.service.legal.impl.PdfGeneracionServicioImpl;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.TipoGraficaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.impl.GeneradorCsv;
import uteq.edu.ec.artisync.service.shared.reporte.impl.GeneradorGraficaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.impl.GeneradorPdf;
import uteq.edu.ec.artisync.service.shared.reporte.impl.GeneradorXlsx;
import uteq.edu.ec.artisync.service.shared.reporte.impl.ServicioExportacionImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserExportVisualIT {

    @Mock private UserRepository usuarioRepository;
    @Mock private UserMapper usuarioMapper;
    @Spy private GeneradorGraficaReporte generadorGraficaReporte = new GeneradorGraficaReporte();

    private AdminUserServiceImpl adminUserService;

    private static TemplateEngine crearTemplateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Test
    @DisplayName("Genera y valida reporte de usuarios completo en PDF y XLSX con branding morado, logo y gráficas")
    void exportar_PdfYXlsx_GeneranDocumentosCompletosConGraficas() throws Exception {
        // Configurar generador real de PDF y XLSX
        IPdfGeneracionServicio pdfService = new PdfGeneracionServicioImpl();
        GeneradorPdf generadorPdf = new GeneradorPdf(crearTemplateEngine(), pdfService);
        GeneradorXlsx generadorXlsx = new GeneradorXlsx();
        GeneradorCsv generadorCsv = new GeneradorCsv();

        ServicioExportacionImpl servicioExportacion = new ServicioExportacionImpl(
                List.of(generadorCsv, generadorXlsx, generadorPdf));

        adminUserService = new AdminUserServiceImpl(
                usuarioRepository, null, null, null, usuarioMapper,
                null, null, null, servicioExportacion, generadorGraficaReporte
        );

        // Sembrar 10 usuarios de prueba
        List<UserResponse> usuarios = List.of(
                UserResponse.builder().idUsuario(1L).nombres("Administrador").apellidos("Artisync").correo("admin@artisync.com").nombrePais("Chile").fechaRegistro(LocalDateTime.now().minusDays(30)).estadoCuenta(true).roles(List.of("ADMIN")).build(),
                UserResponse.builder().idUsuario(2L).nombres("Ana").apellidos("Torres").correo("moderador@artisync.com").nombrePais("Chile").fechaRegistro(LocalDateTime.now().minusDays(25)).estadoCuenta(true).roles(List.of("MODERADOR")).build(),
                UserResponse.builder().idUsuario(3L).nombres("Luis").apellidos("Vera").correo("soporte@artisync.com").nombrePais("Ecuador").fechaRegistro(LocalDateTime.now().minusDays(20)).estadoCuenta(true).roles(List.of("SOPORTE")).build(),
                UserResponse.builder().idUsuario(4L).nombres("Marta").apellidos("Ríos").correo("auditor@artisync.com").nombrePais("México").fechaRegistro(LocalDateTime.now().minusDays(18)).estadoCuenta(true).roles(List.of("AUDITOR_FINANCIERO")).build(),
                UserResponse.builder().idUsuario(5L).nombres("Valentina").apellidos("Reyes").correo("valentina@artisync.demo").nombrePais("Colombia").fechaRegistro(LocalDateTime.now().minusDays(15)).estadoCuenta(true).roles(List.of("CREADOR")).build(),
                UserResponse.builder().idUsuario(6L).nombres("Mateo").apellidos("Fernández").correo("mateo@artisync.demo").nombrePais("Argentina").fechaRegistro(LocalDateTime.now().minusDays(12)).estadoCuenta(true).roles(List.of("CREADOR")).build(),
                UserResponse.builder().idUsuario(7L).nombres("Sofía").apellidos("Navarro").correo("sofia@artisync.demo").nombrePais("México").fechaRegistro(LocalDateTime.now().minusDays(10)).estadoCuenta(true).roles(List.of("CREADOR")).build(),
                UserResponse.builder().idUsuario(8L).nombres("Carlos").apellidos("Mendoza").correo("carlos@artisync.demo").nombrePais("Perú").fechaRegistro(LocalDateTime.now().minusDays(8)).estadoCuenta(true).roles(List.of("CLIENTE")).build(),
                UserResponse.builder().idUsuario(9L).nombres("Lucía").apellidos("Paredes").correo("lucia@artisync.demo").nombrePais("Chile").fechaRegistro(LocalDateTime.now().minusDays(5)).estadoCuenta(true).roles(List.of("CLIENTE")).build(),
                UserResponse.builder().idUsuario(10L).nombres("Diego").apellidos("Salinas").correo("diego@artisync.demo").nombrePais("Ecuador").fechaRegistro(LocalDateTime.now().minusDays(2)).estadoCuenta(true).roles(List.of("CLIENTE")).build()
        );

        when(usuarioRepository.count(org.mockito.ArgumentMatchers.<Specification<User>>any())).thenReturn(10L);
        when(usuarioRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<User>>any(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(new ArrayList<>()));
        when(usuarioMapper.toUserResponseList(any())).thenReturn(usuarios);

        UserFilter filtro = new UserFilter();

        // 1. Exportar en PDF
        DocumentoGenerado docPdf = adminUserService.exportar(filtro, FormatoReporte.PDF, TipoGraficaReporte.AMBAS, "admin@artisync.com");
        assertThat(docPdf).isNotNull();
        assertThat(docPdf.contenido()).isNotEmpty();
        assertThat(docPdf.contentType()).isEqualTo("application/pdf");

        // 2. Exportar en XLSX
        DocumentoGenerado docXlsx = adminUserService.exportar(filtro, FormatoReporte.XLSX, TipoGraficaReporte.AMBAS, "admin@artisync.com");
        assertThat(docXlsx).isNotNull();
        assertThat(docXlsx.contenido()).isNotEmpty();
        assertThat(docXlsx.contentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }
}
