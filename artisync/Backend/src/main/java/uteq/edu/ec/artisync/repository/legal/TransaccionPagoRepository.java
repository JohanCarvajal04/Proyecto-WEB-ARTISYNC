package uteq.edu.ec.artisync.repository.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.TransaccionPago;

import java.util.List;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {

    List<TransaccionPago> findByPagoIdPagoOrderByFechaEjecucionDesc(Long idPago);

    /**
     * Busca todas las transacciones de un creador navegando la cadena:
     * TransaccionPago → PagoGarantia → Contrato → Pedido → Servicio → PerfilCreador
     * Usado por el servicio de auditoría (RNF-13).
     */
    @Query("SELECT t FROM TransaccionPago t " +
           "JOIN t.pago pg " +
           "JOIN pg.contrato c " +
           "JOIN c.pedido p " +
           "JOIN p.servicio s " +
           "WHERE s.perfil.idPerfil = :idPerfil " +
           "ORDER BY t.fechaEjecucion DESC")
    List<TransaccionPago> findByCreadorPerfilId(@Param("idPerfil") Long idPerfil);
}

