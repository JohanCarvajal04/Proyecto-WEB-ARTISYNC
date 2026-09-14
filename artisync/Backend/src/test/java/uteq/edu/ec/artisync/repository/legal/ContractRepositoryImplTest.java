package uteq.edu.ec.artisync.repository.legal;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import uteq.edu.ec.artisync.dto.response.legal.ContractReportRow;
import uteq.edu.ec.artisync.entity.catalog.Offering;
import uteq.edu.ec.artisync.entity.catalog.Workflow;
import uteq.edu.ec.artisync.entity.legal.Contract;
import uteq.edu.ec.artisync.entity.order.ContractTemplate;
import uteq.edu.ec.artisync.entity.order.Order;
import uteq.edu.ec.artisync.entity.profile.CreatorProfile;
import uteq.edu.ec.artisync.entity.security.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica {@link ContractRepositoryImpl#findForReport} contra JPA real (H2):
 * el join de 6 tablas, la proyección directa a {@link ContractReportRow} y
 * cada rama de {@code buildPredicates} (rango de fechas, creador,
 * firmado/no firmado/sin filtrar). No requiere Postgres, solo Criteria API
 * estándar.
 */
@DataJpaTest
class ContractRepositoryImplTest {

    @Autowired
    private EntityManager entityManager;

    private ContractRepositoryImpl repositorio;

    private Long idPerfilCreador;
    private LocalDateTime fechaContratoFirmado;
    private LocalDateTime fechaContratoSinFirmar;

    @BeforeEach
    void sembrarDatos() {
        repositorio = new ContractRepositoryImpl(entityManager);

        User cliente = persistir(User.builder()
                .nombres("Carla").apellidos("Cliente").correo("carla@test.dev")
                .contrasenaHash("x").estadoCuenta(true).build());
        User creador = persistir(User.builder()
                .nombres("Diego").apellidos("Creador").correo("diego@test.dev")
                .contrasenaHash("x").estadoCuenta(true).build());

        CreatorProfile perfil = persistir(CreatorProfile.builder().usuario(creador).build());
        idPerfilCreador = perfil.getIdPerfil();

        Offering servicio = persistir(Offering.builder()
                .perfil(perfil).tituloServicio("Ilustración digital")
                .descripcionDetallada("Retrato digital a color").precioBase(BigDecimal.valueOf(50)).build());

        Workflow flujo = persistir(Workflow.builder()
                .nombreFlujo("Flujo estándar").creador(creador).build());

        ContractTemplate plantilla = persistir(ContractTemplate.builder()
                .versionLegal("v1").cuerpoHtmlPlantilla("<p>Contrato</p>").nombrePlantilla("Plantilla base").build());

        Order pedidoFirmado = persistir(Order.builder()
                .usuarioCliente(cliente).servicio(servicio).flujo(flujo)
                .precioPactado(BigDecimal.valueOf(50)).build());
        Contract contratoFirmado = persistir(Contract.builder()
                .pedido(pedidoFirmado).plantilla(plantilla).limiteRevisiones(2)
                .hashFirmaCliente("hash-cliente").hashFirmaCreador("hash-creador").build());

        Order pedidoSinFirmar = persistir(Order.builder()
                .usuarioCliente(cliente).servicio(servicio).flujo(flujo)
                .precioPactado(BigDecimal.valueOf(80)).build());
        Contract contratoSinFirmar = persistir(Contract.builder()
                .pedido(pedidoSinFirmar).plantilla(plantilla).limiteRevisiones(1)
                .hashFirmaCliente("hash-cliente").build());

        entityManager.flush();

        // fechaFormalizacion es @CreationTimestamp (updatable = false): Hibernate
        // ignora cualquier valor puesto en el builder y estampa "ahora" al
        // insertar. Para poder probar el filtro de rango con dos fechas
        // realmente distintas, se fijan directamente en la BD con SQL nativo.
        fechaContratoFirmado = LocalDateTime.now().minusDays(10);
        fechaContratoSinFirmar = LocalDateTime.now().minusDays(1);
        entityManager.createNativeQuery("UPDATE contratos SET fecha_formalizacion = ?1 WHERE id_contrato = ?2")
                .setParameter(1, fechaContratoFirmado)
                .setParameter(2, contratoFirmado.getIdContrato())
                .executeUpdate();
        entityManager.createNativeQuery("UPDATE contratos SET fecha_formalizacion = ?1 WHERE id_contrato = ?2")
                .setParameter(1, fechaContratoSinFirmar)
                .setParameter(2, contratoSinFirmar.getIdContrato())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private <T> T persistir(T entidad) {
        entityManager.persist(entidad);
        return entidad;
    }

    @Test
    @DisplayName("sin filtros: devuelve ambos contratos con los datos proyectados")
    void sinFiltros_devuelveAmbosContratos() {
        Page<ContractReportRow> pagina = repositorio.findForReport(
                null, null, null, null, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent())
                .extracting(ContractReportRow::cliente)
                .allMatch(nombre -> nombre.equals("Carla Cliente"));
        assertThat(pagina.getContent())
                .extracting(ContractReportRow::creador)
                .allMatch(nombre -> nombre.equals("Diego Creador"));
    }

    @Test
    @DisplayName("rango de fechas: solo incluye contratos formalizados dentro del rango")
    void rangoDeFechas_filtraPorFechaFormalizacion() {
        Page<ContractReportRow> pagina = repositorio.findForReport(
                fechaContratoFirmado.minusDays(1), fechaContratoFirmado.plusDays(1),
                null, null, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).limiteRevisiones()).isEqualTo(2);
    }

    @Test
    @DisplayName("idPerfilCreador: restringe a los contratos de ese creador")
    void idPerfilCreador_filtraPorCreador() {
        Page<ContractReportRow> pagina = repositorio.findForReport(
                null, null, idPerfilCreador, null, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(2);

        Page<ContractReportRow> sinCoincidencia = repositorio.findForReport(
                null, null, idPerfilCreador + 999, null, PageRequest.of(0, 10));
        assertThat(sinCoincidencia.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("soloFirmados=true: solo el contrato con ambas firmas")
    void soloFirmados_true_devuelveSoloElFirmadoPorAmbos() {
        Page<ContractReportRow> pagina = repositorio.findForReport(
                null, null, null, true, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).firmadoCliente()).isTrue();
        assertThat(pagina.getContent().get(0).firmadoCreador()).isTrue();
    }

    @Test
    @DisplayName("soloFirmados=false: solo el contrato al que le falta alguna firma")
    void soloFirmados_false_devuelveSoloElPendienteDeFirma() {
        Page<ContractReportRow> pagina = repositorio.findForReport(
                null, null, null, false, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).firmadoCreador()).isFalse();
    }
}
