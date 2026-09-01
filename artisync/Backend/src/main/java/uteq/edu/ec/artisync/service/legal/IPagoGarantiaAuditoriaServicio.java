package uteq.edu.ec.artisync.service.legal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroPagoGarantia;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPagoGarantia;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPagoGarantiaDetalle;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaResumenEscrow;

import java.util.List;

/**
 * Supervisión de Pagos y Garantías (Escrow) para el Auditor Financiero
 * (PAGO_AUDITAR). Es de solo lectura a propósito: liberar fondos ya tiene su
 * propio flujo automático (aprobación del entregable por el Cliente, ver
 * EntregableServicioImpl), y este panel no lo reemplaza ni lo interviene.
 */
public interface IPagoGarantiaAuditoriaServicio {

    Page<RespuestaPagoGarantia> listar(FiltroPagoGarantia filtro, Pageable pageable);

    RespuestaPagoGarantiaDetalle obtenerDetalle(Long idPago);

    List<RespuestaResumenEscrow> obtenerResumen();
}
