package uteq.edu.ec.artisync.repository.legal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.EntregableFinal;

import java.util.Optional;

@Repository
public interface EntregableFinalRepository extends JpaRepository<EntregableFinal, Long> {

    Optional<EntregableFinal> findByPedidoIdPedido(Long idPedido);

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
    @Query("SELECT e FROM EntregableFinal e WHERE e.pedido.idPedido = :idPedido")
    Optional<EntregableFinal> findByPedidoIdPedidoParaActualizar(@Param("idPedido") Long idPedido);
}
