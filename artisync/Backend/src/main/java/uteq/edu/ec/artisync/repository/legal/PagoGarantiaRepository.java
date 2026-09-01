package uteq.edu.ec.artisync.repository.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaResumenEscrow;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoGarantiaRepository extends JpaRepository<PagoGarantia, Long>,
        JpaSpecificationExecutor<PagoGarantia> {

    Optional<PagoGarantia> findByContratoIdContrato(Long idContrato);

    Optional<PagoGarantia> findByIdOrdenPaypal(String idOrdenPaypal);

    /** Tarjetas de resumen del panel de supervisión: cuántos pagos y cuánto dinero hay en cada estado. */
    @Query("SELECT new uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaResumenEscrow(" +
            "p.estadoFondos, COUNT(p), COALESCE(SUM(p.montoRetenido), 0)) " +
            "FROM PagoGarantia p GROUP BY p.estadoFondos")
    List<RespuestaResumenEscrow> resumenPorEstado();
}
