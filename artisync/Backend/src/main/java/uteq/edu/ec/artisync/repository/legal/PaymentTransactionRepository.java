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
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long>, PaymentTransactionRepositoryCustom {

    /**
     * Transacciones de un pago de garantía, más recientes primero.
     * @param idPago el identificador de pago
     * @return la lista de PaymentTransaction encontrados
     */
    List<PaymentTransaction> findByPagoIdPagoOrderByFechaEjecucionDesc(Long idPago);

    /**
     * Total histórico de "Egreso" (la parte del creador tras la comisión,
     * ver DeliverableServiceImpl.aprobarEntrega) acumulado por todos sus
     * pedidos. Es la mitad "ingresos" del cálculo de saldo disponible para
     * retiro; la otra mitad (lo ya solicitado) vive en
     * WithdrawalRequestRepository.sumMontosEnCursoPorCreador.
     * @param idUsuarioCreador el identificador de usuario creador
     * @return el valor numerico calculado
     */
    @Query("SELECT COALESCE(SUM(t.monto), 0) FROM PaymentTransaction t WHERE t.tipoTransaccion = 'Egreso' AND t.pago.contrato.pedido.servicio.perfil.usuario.idUsuario = :idUsuarioCreador")
    BigDecimal sumEgresosPorCreador(@Param("idUsuarioCreador") Long idUsuarioCreador);
}



