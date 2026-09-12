package uteq.edu.ec.artisync.repository.legal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.dto.respuesta.legal.EscrowSummaryResponse;
import uteq.edu.ec.artisync.entity.legal.EscrowPayment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link EscrowPayment}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface EscrowPaymentRepository extends JpaRepository<EscrowPayment, Long>,
        JpaSpecificationExecutor<EscrowPayment> {

    /** El pago de garantía asociado a un contrato, si existe. */
    Optional<EscrowPayment> findByContratoIdContrato(Long idContrato);

    /** El pago de garantía por el id de orden de PayPal, usado por el webhook y la reconciliación. */
    Optional<EscrowPayment> findByIdOrdenPaypal(String idOrdenPaypal);

    /**
     * Tarjetas de resumen del panel de supervisión: cuántos pagos y cuánto
     * dinero hay en cada estado de fondos.
     *
     * @return un resumen por cada estado de fondos distinto que existe
     */
    @Query("SELECT new uteq.edu.ec.artisync.dto.respuesta.legal.EscrowSummaryResponse(" +
            "p.estadoFondos, COUNT(p), COALESCE(SUM(p.montoRetenido), 0)) " +
            "FROM EscrowPayment p GROUP BY p.estadoFondos")
    List<EscrowSummaryResponse> resumenPorEstado();

    /** REQ-NF-019: pagos 'Pendiente' cuyo último intento fue antes del umbral configurable de PayPalReconciliationScheduler. */
    List<EscrowPayment> findByEstadoFondosAndFechaActualizacionBefore(String estadoFondos, LocalDateTime limite);

    /**
     * Con bloqueo pesimista (mismo patrón que WithdrawalRequestRepository.findByIdParaActualizar):
     * serializa la carrera entre el webhook de PayPal y PayPalReconciliationExecutorService sobre
     * el mismo pago.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM EscrowPayment p WHERE p.idPago = :idPago")
    Optional<EscrowPayment> findByIdParaActualizar(@Param("idPago") Long idPago);

    /** Mismo propósito que findByIdParaActualizar, para cancelarPedidoConFondosRetenidos, que solo tiene el id del contrato a mano. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM EscrowPayment p WHERE p.contrato.idContrato = :idContrato")
    Optional<EscrowPayment> findByContratoIdContratoParaActualizar(@Param("idContrato") Long idContrato);
}
