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

    /**
     * Lista los pagos en garantía que cumplen el filtro indicado, paginados.
     *
     * @param filtro   criterios de filtrado (estado, rango de fechas, etc.)
     * @param pageable configuración de paginación y orden
     * @return la página de pagos que cumplen el filtro
     */
    Page<RespuestaPagoGarantia> listar(FiltroPagoGarantia filtro, Pageable pageable);

    /**
     * Obtiene el detalle de un pago en garantía.
     *
     * @param idPago id del pago
     * @return el detalle del pago
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pago no existe
     */
    RespuestaPagoGarantiaDetalle obtenerDetalle(Long idPago);

    /**
     * Obtiene el resumen agregado de fondos en garantía (escrow), por estado.
     *
     * @return el resumen de escrow
     */
    List<RespuestaResumenEscrow> obtenerResumen();
}
