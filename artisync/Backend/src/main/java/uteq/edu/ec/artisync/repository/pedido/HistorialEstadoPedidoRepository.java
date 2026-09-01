package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.HistorialEstadoPedido;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistorialEstadoPedidoRepository extends JpaRepository<HistorialEstadoPedido, Long> {

    List<HistorialEstadoPedido> findByPedidoIdPedidoOrderByFechaTransicionAsc(Long idPedido);

    Optional<HistorialEstadoPedido> findTopByPedidoIdPedidoOrderByFechaTransicionDesc(Long idPedido);

    /**
     * Existe algún pedido de este flujo cuya transición MÁS RECIENTE apunta a
     * esta etapa — es decir, un pedido que está actualmente detenido ahí.
     * Usado para bloquear el borrado de una etapa en uso: sin este chequeo,
     * PedidoServicioImpl.obtenerOrdenActual no encuentra la etapa en la
     * configuración del flujo y el pedido "retrocede" a la primera etapa en
     * el siguiente avance (ver H-flujo-01).
     */
    @Query("""
            SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END
            FROM HistorialEstadoPedido h
            WHERE h.pedido.flujo.idFlujo = :idFlujo
              AND h.etapa.idEtapa = :idEtapa
              AND h.fechaTransicion = (
                  SELECT MAX(h2.fechaTransicion) FROM HistorialEstadoPedido h2
                  WHERE h2.pedido = h.pedido
              )
            """)
    boolean existePedidoEnEtapaActual(@Param("idFlujo") Long idFlujo, @Param("idEtapa") Long idEtapa);
}
