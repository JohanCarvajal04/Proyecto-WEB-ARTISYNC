package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroPagoGarantia;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPagoGarantia;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPagoGarantiaDetalle;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaResumenEscrow;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaTransaccionPago;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.legal.PagoGarantiaRepository;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;
import uteq.edu.ec.artisync.service.legal.IPagoGarantiaAuditoriaServicio;
import uteq.edu.ec.artisync.specification.legal.PagoGarantiaSpecification;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoGarantiaAuditoriaServicioImpl implements IPagoGarantiaAuditoriaServicio {

    private final PagoGarantiaRepository pagoGarantiaRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaPagoGarantia> listar(FiltroPagoGarantia filtro, Pageable pageable) {
        var spec = PagoGarantiaSpecification.conFiltros(
                filtro.getEstadoFondos(), filtro.getIdPerfilCreador(), filtro.getIdUsuarioCliente(),
                filtro.getDesde(), filtro.getHasta());

        return pagoGarantiaRepository.findAll(spec, pageable).map(this::mapearAResumen);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPagoGarantiaDetalle obtenerDetalle(Long idPago) {
        PagoGarantia pago = pagoGarantiaRepository.findById(idPago)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pago de garantía no encontrado: " + idPago));

        Pedido pedido = pago.getContrato().getPedido();
        Usuario cliente = pedido.getUsuarioCliente();
        Usuario creador = pedido.getServicio().getPerfil().getUsuario();

        List<RespuestaTransaccionPago> transacciones = transaccionPagoRepository
                .findByPagoIdPagoOrderByFechaEjecucionDesc(idPago).stream()
                .map(t -> RespuestaTransaccionPago.builder()
                        .idTransaccion(t.getIdTransaccion())
                        .tipoTransaccion(t.getTipoTransaccion())
                        .monto(t.getMonto())
                        .fechaEjecucion(t.getFechaEjecucion())
                        .build())
                .toList();

        return RespuestaPagoGarantiaDetalle.builder()
                .idPago(pago.getIdPago())
                .idContrato(pago.getContrato().getIdContrato())
                .idPedido(pedido.getIdPedido())
                .tituloServicio(pedido.getServicio().getTituloServicio())
                .idUsuarioCliente(cliente.getIdUsuario())
                .nombreCliente(cliente.getNombres() + " " + cliente.getApellidos())
                .correoCliente(cliente.getCorreo())
                .idPerfilCreador(pedido.getServicio().getPerfil().getIdPerfil())
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
                .idOrdenPaypal(pago.getIdOrdenPaypal())
                .montoRetenido(pago.getMontoRetenido())
                .estadoFondos(pago.getEstadoFondos())
                .fechaFormalizacion(pago.getContrato().getFechaFormalizacion())
                .transacciones(transacciones)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaResumenEscrow> obtenerResumen() {
        return pagoGarantiaRepository.resumenPorEstado();
    }

    private RespuestaPagoGarantia mapearAResumen(PagoGarantia pago) {
        Pedido pedido = pago.getContrato().getPedido();
        Usuario cliente = pedido.getUsuarioCliente();
        Usuario creador = pedido.getServicio().getPerfil().getUsuario();

        return RespuestaPagoGarantia.builder()
                .idPago(pago.getIdPago())
                .idContrato(pago.getContrato().getIdContrato())
                .idPedido(pedido.getIdPedido())
                .tituloServicio(pedido.getServicio().getTituloServicio())
                .idUsuarioCliente(cliente.getIdUsuario())
                .nombreCliente(cliente.getNombres() + " " + cliente.getApellidos())
                .idPerfilCreador(pedido.getServicio().getPerfil().getIdPerfil())
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
                .idOrdenPaypal(pago.getIdOrdenPaypal())
                .montoRetenido(pago.getMontoRetenido())
                .estadoFondos(pago.getEstadoFondos())
                .fechaFormalizacion(pago.getContrato().getFechaFormalizacion())
                .build();
    }
}
