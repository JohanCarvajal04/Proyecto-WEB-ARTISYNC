package uteq.edu.ec.artisync.service.pedido.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionAvanzarEtapa;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPedido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.*;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.pedido.*;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.FlujoTrabajoRepository;
import uteq.edu.ec.artisync.repository.catalogo.ServicioRepository;
import uteq.edu.ec.artisync.repository.pedido.*;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.pedido.IPedidoServicio;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoServicioImpl implements IPedidoServicio {

    private final PedidoRepository pedidoRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final FlujoTrabajoRepository flujoTrabajoRepository;
    private final FlujoEtapaConfigRepository flujoEtapaConfigRepository;
    private final HistorialEstadoPedidoRepository historialRepository;
    private final EtapaFlujoRepository etapaFlujoRepository;

    @Override
    @Transactional
    public RespuestaPedido crearPedido(Long idCliente, PeticionCrearPedido peticion) {
        Usuario cliente = usuarioRepository.findById(idCliente)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario cliente no encontrado"));

        Servicio servicio = servicioRepository.findById(peticion.getIdServicio())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado"));

        // Verificar que el cliente no sea el mismo creador del servicio
        if (servicio.getPerfil().getUsuario().getIdUsuario().equals(idCliente)) {
            throw new ExcepcionReglaNegocio("No puedes crear un pedido para tu propio servicio");
        }

        // Obtener el primer flujo de trabajo disponible (o el asociado a la categoría)
        List<FlujoTrabajo> flujos = flujoTrabajoRepository.findAll();
        if (flujos.isEmpty()) {
            throw new ExcepcionReglaNegocio("No hay flujos de trabajo configurados en el sistema");
        }
        FlujoTrabajo flujo = flujos.get(0);

        // Verificar que el flujo tenga etapas configuradas
        List<FlujoEtapaConfig> etapas = flujoEtapaConfigRepository
                .findByFlujoIdFlujoOrderByNumeroOrdenAsc(flujo.getIdFlujo());
        if (etapas.isEmpty()) {
            throw new ExcepcionReglaNegocio("El flujo de trabajo no tiene etapas configuradas");
        }

        // Crear el pedido
        Pedido pedido = Pedido.builder()
                .usuarioCliente(cliente)
                .servicio(servicio)
                .flujo(flujo)
                .precioPactado(peticion.getPrecioOfrecido() != null
                        ? peticion.getPrecioOfrecido()
                        : servicio.getPrecioBase())
                .fechaEntregaEstimada(peticion.getFechaEntregaEstimada())
                .build();

        pedido = pedidoRepository.save(pedido);

        // Registrar estado inicial (primera etapa del flujo)
        HistorialEstadoPedido estadoInicial = HistorialEstadoPedido.builder()
                .pedido(pedido)
                .etapa(etapas.get(0).getEtapa())
                .observacion("Pedido creado")
                .build();
        historialRepository.save(estadoInicial);

        log.info("Pedido {} creado por cliente {} para servicio {}", pedido.getIdPedido(), idCliente, peticion.getIdServicio());

        return mapToRespuesta(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPedido obtenerPedidoPorId(Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado con ID: " + idPedido));
        return mapToRespuesta(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaPedidoResumido> listarMisPedidos(Long idCliente) {
        return pedidoRepository.findByUsuarioClienteIdUsuario(idCliente)
                .stream()
                .map(this::mapToResumido)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaPedidoResumido> listarMisComisiones(Long idCreador) {
        return pedidoRepository.findByServicioPerfilUsuarioIdUsuario(idCreador)
                .stream()
                .map(this::mapToResumido)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RespuestaPedido avanzarEtapa(Long idPedido, Long idCreador, PeticionAvanzarEtapa peticion) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));

        // Verificar que el creador es el dueño del servicio del pedido
        Long idCreadorServicio = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        if (!idCreadorServicio.equals(idCreador)) {
            throw new ExcepcionReglaNegocio("Solo el creador del servicio puede avanzar las etapas del pedido");
        }

        // Obtener etapa actual del historial
        HistorialEstadoPedido ultimoEstado = historialRepository
                .findTopByPedidoIdPedidoOrderByFechaTransicionDesc(idPedido)
                .orElseThrow(() -> new ExcepcionReglaNegocio("Pedido sin estado inicial"));

        // Obtener el orden actual de la etapa
        Integer ordenActual = obtenerOrdenActual(pedido, ultimoEstado);

        // Obtener siguiente etapa del flujo configurado
        List<FlujoEtapaConfig> siguientes = flujoEtapaConfigRepository
                .findByFlujoIdFlujoAndNumeroOrdenGreaterThanOrderByNumeroOrdenAsc(
                        pedido.getFlujo().getIdFlujo(), ordenActual);

        if (siguientes.isEmpty()) {
            throw new ExcepcionReglaNegocio("El pedido ya se encuentra en la etapa final");
        }

        FlujoEtapaConfig siguienteConfig = siguientes.get(0);

        // Registrar transición (INMUTABLE)
        HistorialEstadoPedido nuevoEstado = HistorialEstadoPedido.builder()
                .pedido(pedido)
                .etapa(siguienteConfig.getEtapa())
                .observacion(peticion.getObservacion())
                .build();
        historialRepository.save(nuevoEstado);

        log.info("Pedido {} avanzó a etapa '{}' (orden {})",
                idPedido, siguienteConfig.getEtapa().getNombreEtapa(), siguienteConfig.getNumeroOrden());

        // TODO M6: Notificar al cliente vía NotificacionService
        // notificacionService.notificar(pedido.getUsuarioCliente(),
        //     "PEDIDO_AVANCE", "Tu pedido ha avanzado a: " + siguienteConfig.getEtapa().getNombreEtapa());

        return mapToRespuesta(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaHistorialEstado> obtenerHistorial(Long idPedido) {
        if (!pedidoRepository.existsById(idPedido)) {
            throw new ExcepcionRecursoNoEncontrado("Pedido no encontrado con ID: " + idPedido);
        }

        return historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(idPedido)
                .stream()
                .map(this::mapHistorial)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaSeguimientoPedido obtenerSeguimiento(Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));

        List<FlujoEtapaConfig> etapasConfig = flujoEtapaConfigRepository
                .findByFlujoIdFlujoOrderByNumeroOrdenAsc(pedido.getFlujo().getIdFlujo());

        List<HistorialEstadoPedido> historial = historialRepository
                .findByPedidoIdPedidoOrderByFechaTransicionAsc(idPedido);

        HistorialEstadoPedido ultimoEstado = historial.isEmpty() ? null : historial.get(historial.size() - 1);

        Integer etapaActualOrden = 0;
        String etapaActualNombre = "Sin estado";
        if (ultimoEstado != null) {
            etapaActualNombre = ultimoEstado.getEtapa().getNombreEtapa();
            etapaActualOrden = obtenerOrdenActual(pedido, ultimoEstado);
        }

        int totalEtapas = etapasConfig.size();
        double porcentaje = totalEtapas > 0 ? ((double) etapaActualOrden / totalEtapas) * 100 : 0;

        return RespuestaSeguimientoPedido.builder()
                .idPedido(idPedido)
                .tituloServicio(pedido.getServicio().getTituloServicio())
                .etapaActual(etapaActualNombre)
                .etapaActualOrden(etapaActualOrden)
                .totalEtapas(totalEtapas)
                .porcentajeProgreso(porcentaje)
                .fechaUltimaActualizacion(ultimoEstado != null ? ultimoEstado.getFechaTransicion() : null)
                .etapasDelFlujo(etapasConfig.stream().map(this::mapEtapaConfig).collect(Collectors.toList()))
                .historial(historial.stream().map(this::mapHistorial).collect(Collectors.toList()))
                .build();
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    private Integer obtenerOrdenActual(Pedido pedido, HistorialEstadoPedido ultimoEstado) {
        List<FlujoEtapaConfig> configs = flujoEtapaConfigRepository
                .findByFlujoIdFlujoOrderByNumeroOrdenAsc(pedido.getFlujo().getIdFlujo());

        return configs.stream()
                .filter(c -> c.getEtapa().getIdEtapa().equals(ultimoEstado.getEtapa().getIdEtapa()))
                .findFirst()
                .map(FlujoEtapaConfig::getNumeroOrden)
                .orElse(0);
    }

    private String obtenerEtapaActual(Long idPedido) {
        return historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(idPedido)
                .map(h -> h.getEtapa().getNombreEtapa())
                .orElse("Sin estado");
    }

    private RespuestaPedido mapToRespuesta(Pedido pedido) {
        List<RespuestaHistorialEstado> historial = historialRepository
                .findByPedidoIdPedidoOrderByFechaTransicionAsc(pedido.getIdPedido())
                .stream()
                .map(this::mapHistorial)
                .collect(Collectors.toList());

        Usuario creador = pedido.getServicio().getPerfil().getUsuario();

        return RespuestaPedido.builder()
                .idPedido(pedido.getIdPedido())
                .idServicio(pedido.getServicio().getIdServicio())
                .tituloServicio(pedido.getServicio().getTituloServicio())
                .idCliente(pedido.getUsuarioCliente().getIdUsuario())
                .nombreCliente(pedido.getUsuarioCliente().getNombres() + " " + pedido.getUsuarioCliente().getApellidos())
                .idCreador(creador.getIdUsuario())
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
                .etapaActual(obtenerEtapaActual(pedido.getIdPedido()))
                .precioPactado(pedido.getPrecioPactado())
                .fechaInicio(pedido.getFechaInicio())
                .fechaEntregaEstimada(pedido.getFechaEntregaEstimada())
                .nombreFlujo(pedido.getFlujo().getNombreFlujo())
                .historial(historial)
                .build();
    }

    private RespuestaPedidoResumido mapToResumido(Pedido pedido) {
        Usuario creador = pedido.getServicio().getPerfil().getUsuario();

        return RespuestaPedidoResumido.builder()
                .idPedido(pedido.getIdPedido())
                .tituloServicio(pedido.getServicio().getTituloServicio())
                .etapaActual(obtenerEtapaActual(pedido.getIdPedido()))
                .precioPactado(pedido.getPrecioPactado())
                .fechaInicio(pedido.getFechaInicio())
                .fechaEntregaEstimada(pedido.getFechaEntregaEstimada())
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
                .nombreCliente(pedido.getUsuarioCliente().getNombres() + " " + pedido.getUsuarioCliente().getApellidos())
                .build();
    }

    private RespuestaHistorialEstado mapHistorial(HistorialEstadoPedido h) {
        return RespuestaHistorialEstado.builder()
                .idHistorial(h.getIdHistorialEstado())
                .nombreEtapa(h.getEtapa().getNombreEtapa())
                .fechaTransicion(h.getFechaTransicion())
                .observacion(h.getObservacion())
                .build();
    }

    private RespuestaEtapaConfig mapEtapaConfig(FlujoEtapaConfig config) {
        return RespuestaEtapaConfig.builder()
                .idFlujoEtapa(config.getIdFlujoEtapa())
                .idEtapa(config.getEtapa().getIdEtapa())
                .nombreEtapa(config.getEtapa().getNombreEtapa())
                .numeroOrden(config.getNumeroOrden())
                .esEtapaFinal(config.getEsEtapaFinal())
                .build();
    }
}
