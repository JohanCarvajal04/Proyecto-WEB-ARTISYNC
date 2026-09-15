package uteq.edu.ec.artisync.repository.legal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Invocación de {@code fn_reporte_comisiones_creador} vía JDBC directo, fuera del mecanismo de
 * {@code @Query} de Spring Data JPA (ver {@link PaymentTransactionRepositoryImpl}).
 */
public interface PaymentTransactionRepositoryCustom {

    /**
     * fn_reporte_comisiones_creador: agrega bruto/comisión/neto y el detalle de transacciones
     * de un creador en una sola sentencia STABLE. Devuelve JSONB serializado como texto.
     */
    String reporteComisionesJson(Long idPerfil, LocalDateTime desde, LocalDateTime hasta, BigDecimal tasa);
}
