package uteq.edu.ec.artisync.repository.legal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.WithdrawalRequest;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link WithdrawalRequest}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long>,
        JpaSpecificationExecutor<WithdrawalRequest> {

    /** Solicitudes de retiro de un creador, más recientes primero. */
    List<WithdrawalRequest> findByUsuarioCreadorIdUsuarioOrderByFechaSolicitudDesc(Long idUsuario);

    /** @return {@code true} si el creador tiene alguna solicitud en uno de esos estados */
    boolean existsByUsuarioCreadorIdUsuarioAndEstadoIn(Long idUsuario, Collection<String> estados);

    /**
     * Suma de solicitudes "en curso" (no resueltas a favor del creador
     * todavía): resta del saldo disponible.
     *
     * @param idUsuario identificador del creador
     * @param estados estados considerados "en curso"
     * @return el monto total solicitado en esos estados, 0 si no hay ninguna
     */
    @Query("SELECT COALESCE(SUM(s.montoSolicitado), 0) FROM WithdrawalRequest s " +
            "WHERE s.usuarioCreador.idUsuario = :idUsuario AND s.estado IN :estados")
    BigDecimal sumMontosEnCursoPorCreador(@Param("idUsuario") Long idUsuario, @Param("estados") Collection<String> estados);

    /**
     * Igual que findById, pero con bloqueo pesimista de fila (equivalente Java
     * del SELECT ... FOR UPDATE, mismo patrón que
     * FinalDeliverableRepository.findByPedidoIdPedidoParaActualizar). Serializa
     * aprobar/rechazar/reintentar concurrentes sobre la misma solicitud: la
     * segunda transacción espera a que la primera confirme antes de leer el
     * estado, evitando una doble decisión o un doble payout.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM WithdrawalRequest s WHERE s.idSolicitud = :idSolicitud")
    Optional<WithdrawalRequest> findByIdParaActualizar(@Param("idSolicitud") Long idSolicitud);
}

