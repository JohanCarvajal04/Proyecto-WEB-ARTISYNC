package uteq.edu.ec.artisync.repository.legal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaResumenEscrow;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link PagoGarantia}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface PagoGarantiaRepository extends JpaRepository<PagoGarantia, Long>,
        JpaSpecificationExecutor<PagoGarantia> {

    Optional<PagoGarantia> findByContratoIdContrato(Long idContrato);

    Optional<PagoGarantia> findByIdOrdenPaypal(String idOrdenPaypal);

    /** Tarjetas de resumen del panel de supervisión: cuántos pagos y cuánto dinero hay en cada estado. */
    @Query("SELECT new uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaResumenEscrow(" +
            "p.estadoFondos, COUNT(p), COALESCE(SUM(p.montoRetenido), 0)) " +
            "FROM PagoGarantia p GROUP BY p.estadoFondos")
    List<RespuestaResumenEscrow> resumenPorEstado();

    /** REQ-NF-019: pagos 'Pendiente' cuyo último intento fue antes del umbral configurable de ReconciliacionPayPalScheduler. */
    List<PagoGarantia> findByEstadoFondosAndFechaActualizacionBefore(String estadoFondos, LocalDateTime limite);

    /**
     * Con bloqueo pesimista (mismo patrón que SolicitudRetiroRepository.findByIdParaActualizar):
     * serializa la carrera entre el webhook de PayPal y ReconciliacionPayPalEjecutorServicio sobre
     * el mismo pago.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PagoGarantia p WHERE p.idPago = :idPago")
    Optional<PagoGarantia> findByIdParaActualizar(@Param("idPago") Long idPago);

    /** Mismo propósito que findByIdParaActualizar, para cancelarPedidoConFondosRetenidos, que solo tiene el id del contrato a mano. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PagoGarantia p WHERE p.contrato.idContrato = :idContrato")
    Optional<PagoGarantia> findByContratoIdContratoParaActualizar(@Param("idContrato") Long idContrato);
}
