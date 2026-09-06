package uteq.edu.ec.artisync.service.legal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroSolicitudRetiro;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionSolicitudRetiro;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaSaldoCreador;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaSolicitudRetiro;

import java.util.List;

public interface ISolicitudRetiroServicio {

    RespuestaSaldoCreador obtenerSaldo(Long idUsuarioCreador);

    RespuestaSolicitudRetiro solicitar(Long idUsuarioCreador, PeticionSolicitudRetiro peticion);

    List<RespuestaSolicitudRetiro> misSolicitudes(Long idUsuarioCreador);

    Page<RespuestaSolicitudRetiro> listarCola(FiltroSolicitudRetiro filtro, Pageable pageable);

    /**
     * Ejecuta el payout real a PayPal (POST /v1/payments/payouts). Según la
     * respuesta, la solicitud queda en "Pagado" (SUCCESS), sigue "Aprobado"
     * (PENDING/UNCLAIMED, PayPal la resuelve más tarde) o pasa a "Fallido"
     * (error/DENIED), con {@code mensajeError} poblado en ese último caso.
     */
    RespuestaSolicitudRetiro aprobar(Long idSolicitud, Long idAdmin);

    RespuestaSolicitudRetiro rechazar(Long idSolicitud, Long idAdmin, String notaAdmin);

    /**
     * Solo permitido desde "Fallido". Reintenta el mismo payout reusando el
     * sender_batch_id original ("retiro-" + idSolicitud): si PayPal ya había
     * procesado el intento anterior pese al error de red/respuesta, el batch
     * id idéntico hace que PayPal deduplique en vez de cobrar dos veces.
     */
    RespuestaSolicitudRetiro reintentar(Long idSolicitud, Long idAdmin);
}
