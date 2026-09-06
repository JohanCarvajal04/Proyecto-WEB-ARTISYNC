package uteq.edu.ec.artisync.repository.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.TransaccionPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {

    List<TransaccionPago> findByPagoIdPagoOrderByFechaEjecucionDesc(Long idPago);

    /**
     * fn_reporte_comisiones_creador (db/procs/fn_reporte_comisiones_creador.sql):
     * agrega bruto/comisi??n/neto y el detalle de transacciones de un creador en
     * una sola sentencia STABLE, en vez de traer entidades crudas y sumar en
     * Java. Sustituye a la vieja findByCreadorPerfilId + agregaci??n manual que
     * usaba AuditServiceImpl (retirado: su CSV no ten??a tope, no llevaba BOM y
     * formateaba el monto con el locale por defecto de la JVM).
     */
    @Query(value = "SELECT fn_reporte_comisiones_creador(:idPerfil, :desde, :hasta, :tasa)::text",
            nativeQuery = true)
    String reporteComisionesJson(@Param("idPerfil") Long idPerfil,
                                  @Param("desde") LocalDateTime desde,
                                  @Param("hasta") LocalDateTime hasta,
                                  @Param("tasa") BigDecimal tasa);

    /**
     * Total histórico de "Egreso" (la parte del creador tras la comisión,
     * ver EntregableServicioImpl.aprobarEntrega) acumulado por todos sus
     * pedidos. Es la mitad "ingresos" del cálculo de saldo disponible para
     * retiro; la otra mitad (lo ya solicitado) vive en
     * SolicitudRetiroRepository.sumMontosEnCursoPorCreador.
     */
    @Query("SELECT COALESCE(SUM(t.monto), 0) FROM TransaccionPago t " +
            "WHERE t.tipoTransaccion = 'Egreso' " +
            "AND t.pago.contrato.pedido.servicio.perfil.usuario.idUsuario = :idUsuarioCreador")
    BigDecimal sumEgresosPorCreador(@Param("idUsuarioCreador") Long idUsuarioCreador);
}


