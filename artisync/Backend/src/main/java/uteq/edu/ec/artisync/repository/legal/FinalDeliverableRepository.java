package uteq.edu.ec.artisync.repository.legal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.FinalDeliverable;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link FinalDeliverable}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface FinalDeliverableRepository extends JpaRepository<FinalDeliverable, Long> {

    /** El entregable final de un pedido (relación 1:1), si existe. */
    Optional<FinalDeliverable> findByPedidoIdPedido(Long idPedido);

    /** @return {@code true} si el pedido ya tiene un entregable final subido */
    boolean existsByPedidoIdPedido(Long idPedido);

    /**
     * Igual que findByPedidoIdPedido, pero con bloqueo pesimista de fila
     * (equivalente Java del SELECT ... FOR UPDATE que ya usan
     * fn_seleccionar_ganadores_sorteo y fn_registrar_infraccion). Serializa
     * llamadas concurrentes a aprobarEntrega sobre el mismo pedido: la
     * segunda transacción espera a que la primera confirme antes de leer
     * estaLiberado, evitando liberar el escrow dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM FinalDeliverable e WHERE e.pedido.idPedido = :idPedido")
    Optional<FinalDeliverable> findByPedidoIdPedidoParaActualizar(@Param("idPedido") Long idPedido);
}

