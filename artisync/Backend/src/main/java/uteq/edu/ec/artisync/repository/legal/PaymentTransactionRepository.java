package uteq.edu.ec.artisync.repository.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.PaymentTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link PaymentTransaction}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /** Transacciones de un pago de garantía, más recientes primero. */
    List<PaymentTransaction> findByPagoIdPagoOrderByFechaEjecucionDesc(Long idPago);

    /**
     * fn_reporte_comisiones_creador (db/procs/fn_reporte_comisiones_creador.sql):
     * agrega bruto/comisión/neto y el detalle de transacciones de un creador en
     * una sola sentencia STABLE, en vez de traer entidades crudas y sumar en
     * Java. Sustituye a la vieja findByCreadorPerfilId + agregación manual que
     * usaba AuditServiceImpl (retirado: su CSV no tenía tope, no llevaba BOM y
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
     * ver DeliverableServiceImpl.aprobarEntrega) acumulado por todos sus
     * pedidos. Es la mitad "ingresos" del cálculo de saldo disponible para
     * retiro; la otra mitad (lo ya solicitado) vive en
     * WithdrawalRequestRepository.sumMontosEnCursoPorCreador.
     */
    @Query("SELECT COALESCE(SUM(t.monto), 0) FROM PaymentTransaction t WHERE t.tipoTransaccion = 'Egreso' AND t.pago.contrato.pedido.servicio.perfil.usuario.idUsuario = :idUsuarioCreador")
    BigDecimal sumEgresosPorCreador(@Param("idUsuarioCreador") Long idUsuarioCreador);
}



