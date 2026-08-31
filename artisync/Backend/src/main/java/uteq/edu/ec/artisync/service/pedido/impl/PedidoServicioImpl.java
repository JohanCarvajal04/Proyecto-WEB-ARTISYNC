package uteq.edu.ec.artisync.service.pedido.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionActualizarTerminosPedido;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionAvanzarEtapa;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPedido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.*;
import uteq.edu.ec.artisync.entity.catalogo.Categoria;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.pedido.*;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.FlujoTrabajoRepository;
import uteq.edu.ec.artisync.repository.catalogo.ServicioRepository;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.pedido.*;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;
import uteq.edu.ec.artisync.service.pedido.IPedidoServicio;
import uteq.edu.ec.artisync.service.perfil.IVerificacionServicio;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.IServicioExportacion;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
import uteq.edu.ec.artisync.util.ValidadorPertenenciaPedido;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ContratoRepository contratoRepository;
    private final NotificacionService notificacionService;
    private final ChatService chatService;
    private final IServicioExportacion servicioExportacion;
    private final IVerificacionServicio verificacionServicio;

    @Override
    @Transactional
    @Auditable(accion = "PEDIDO_CREAR", modulo = ModuloAuditoria.PEDIDOS,
            entidad = "pedidos", idEntidad = "#resultado.idPedido",
            detalle = "{idServicio: #peticion.idServicio}")
    public RespuestaPedido crearPedido(Long idCliente, PeticionCrearPedido peticion) {
        Usuario cliente = usuarioRepository.findById(idCliente)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario cliente no encontrado"));

        if (!verificacionServicio.estaIdentidadVerificada(idCliente)) {
            throw new ExcepcionReglaNegocio(
                    "Debes verificar tu identidad antes de crear un pedido. Sube tu documento de identidad desde tu perfil.");
        }

        Servicio servicio = servicioRepository.findById(peticion.getIdServicio())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado"));

        // Verificar que el cliente no sea el mismo creador del servicio
        if (servicio.getPerfil().getUsuario().getIdUsuario().equals(idCliente)) {
            throw new ExcepcionReglaNegocio("No puedes crear un pedido para tu propio servicio");
        }

        FlujoTrabajo flujo = resolverFlujoDelServicio(servicio);

        // Verificar que el flujo tenga etapas configuradas
        List<FlujoEtapaConfig> etapas = flujoEtapaConfigRepository
                .findByFlujoIdFlujoOrderByNumeroOrdenAsc(flujo.getIdFlujo());
        if (etapas.isEmpty()) {
            throw new ExcepcionReglaNegocio(
                    "El flujo '" + flujo.getNombreFlujo() + "' no tiene etapas configuradas");
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

        log.info("Pedido {} creado por cliente {} para servicio {} con flujo '{}'",
                pedido.getIdPedido(), idCliente, peticion.getIdServicio(), flujo.getNombreFlujo());

        // La sala se abre desde ya, antes de cualquier firma: así cliente y
        // creador pueden negociar precio/alcance por chat antes de
        // comprometerse con un contrato (ver actualizarTerminos). Antes solo
        // se abría cuando ambas partes ya habían firmado.
        chatService.crearSala(pedido);

        return mapToRespuesta(pedido);
    }

    @Override
    @Transactional
    public RespuestaPedido actualizarTerminos(Long idPedido, Long idUsuario, PeticionActualizarTerminosPedido peticion) {
        if (peticion.getPrecioPactado() == null && peticion.getFechaEntregaEstimada() == null) {
            throw new ExcepcionReglaNegocio("Debes indicar al menos un término a actualizar");
        }

        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));

        Long idCliente = pedido.getUsuarioCliente().getIdUsuario();
        Long idCreador = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        boolean esCliente = idCliente.equals(idUsuario);
        boolean esCreador = idCreador.equals(idUsuario);
        if (!esCliente && !esCreador) {
            throw new ExcepcionReglaNegocio("No tienes permiso para modificar los términos de este pedido");
        }

        // Los términos quedan congelados apenas hay una firma: el contrato ya
        // renderiza precio/fecha en vivo desde el pedido (ver
        // ContratoServicioImpl#generarContratoHtml), así que cambiarlos
        // después de que alguien firmó reescribiría en silencio lo que esa
        // persona ya aceptó.
        contratoRepository.findByPedidoIdPedido(idPedido).ifPresent(contrato -> {
            if (contrato.getHashFirmaCreador() != null || contrato.getHashFirmaCliente() != null) {
                throw new ExcepcionReglaNegocio(
                        "No se pueden modificar los términos: el contrato ya tiene al menos una firma");
            }
        });

        if (peticion.getPrecioPactado() != null) {
            pedido.setPrecioPactado(peticion.getPrecioPactado());
        }
        if (peticion.getFechaEntregaEstimada() != null) {
            pedido.setFechaEntregaEstimada(peticion.getFechaEntregaEstimada());
        }
        pedido = pedidoRepository.save(pedido);

        log.info("Pedido {} actualizó términos (usuario {}): precio={}, entrega={}",
                idPedido, idUsuario, pedido.getPrecioPactado(), pedido.getFechaEntregaEstimada());

        Usuario otraParte = esCliente
                ? pedido.getServicio().getPerfil().getUsuario()
                : pedido.getUsuarioCliente();
        notificacionService.notificar(otraParte, "PEDIDO_TERMINOS_ACTUALIZADOS",
                "Se actualizaron los términos del pedido \"" + pedido.getServicio().getTituloServicio() + "\".");

        return mapToRespuesta(pedido);
    }

    /**
     * Flujo que le corresponde al pedido (RF-19): el configurado en la
     * categoría del servicio, siguiendo servicio → subcategoría → categoría.
     *
     * <p>Antes se tomaba {@code findAll().get(0)}, es decir el primer flujo que
     * devolviese Postgres sin ORDER BY: todos los pedidos compartían flujo y
     * cuál era dependía del plan de ejecución.
     *
     * <p>Si la categoría no tiene flujo asignado se cae al flujo por defecto en
     * lugar de rechazar el pedido: la columna es nullable y un catálogo a medio
     * configurar no debe impedir vender.
     */
    private FlujoTrabajo resolverFlujoDelServicio(Servicio servicio) {
        Categoria categoria = servicio.getSubcategoria().getCategoria();

        if (categoria.getFlujo() != null) {
            return categoria.getFlujo();
        }

        log.warn("La categoria '{}' no tiene flujo asignado; se usa el flujo por defecto",
                categoria.getNombreCategoria());
        return obtenerFlujoPorDefecto();
    }

    /**
     * Flujo de respaldo: el de menor id.
     *
     * <p>No se busca ningún flujo por nombre a propósito. Los nombres los fija
     * el seed de fixtures (`database/seed-medicion-referencia.sql`), no el
     * esquema, y acoplar el servicio a una cadena concreta lo rompería en
     * cualquier instalación que sembrase otros datos. Lo que sí se garantiza
     * frente al {@code findAll().get(0)} anterior es que la elección sea
     * determinista.
     */
    private FlujoTrabajo obtenerFlujoPorDefecto() {
        return flujoTrabajoRepository.findFirstByOrderByIdFlujoAsc()
                .orElseThrow(() -> new ExcepcionReglaNegocio(
                        "No hay flujos de trabajo configurados en el sistema"));
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPedido obtenerPedidoPorId(Long idPedido, Long idUsuarioSolicitante) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado con ID: " + idPedido));
        // OBS-08 / H-02: evita el acceso indebido (IDOR) a pedidos ajenos.
        ValidadorPertenenciaPedido.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);
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
    @Transactional(readOnly = true)
    public DocumentoGenerado exportarMisPedidos(Long idCliente, FormatoReporte formato, String correoSolicitante) {
        return exportarResumen(listarMisPedidos(idCliente), "Mis pedidos", "Pedidos como cliente",
                formato, correoSolicitante);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoGenerado exportarMisComisiones(Long idCreador, List<Long> idsPedido, FormatoReporte formato,
                                                     String correoSolicitante) {
        List<RespuestaPedidoResumido> comisiones = listarMisComisiones(idCreador);
        if (idsPedido != null && !idsPedido.isEmpty()) {
            // 1.4: filtra sobre el propio listado del creador, así que un id
            // ajeno enviado por el cliente simplemente no matchea — no es una
            // vía de IDOR, solo se restringe el subconjunto ya autorizado.
            Set<Long> idsSolicitados = new HashSet<>(idsPedido);
            comisiones = comisiones.stream()
                    .filter(c -> idsSolicitados.contains(c.getIdPedido()))
                    .collect(Collectors.toList());
        }
        return exportarResumen(comisiones, "Mis comisiones", "Pedidos como creador",
                formato, correoSolicitante);
    }

    private DocumentoGenerado exportarResumen(List<RespuestaPedidoResumido> filas, String titulo, String subtitulo,
                                               FormatoReporte formato, String correoSolicitante) {
        if (filas.size() > formato.topeFilas()) {
            throw new ExcepcionReglaNegocio(
                    "El listado tiene " + filas.size() + " pedidos, más de los " + formato.topeFilas()
                            + " que admite una exportación en " + formato + ".");
        }

        ModeloReporte<RespuestaPedidoResumido> modelo = ModeloReporte.<RespuestaPedidoResumido>builder()
                .titulo(titulo)
                .subtitulo(subtitulo)
                .filtrosAplicados(Map.of())
                .columnas(List.of(
                        ColumnaReporte.entero("Id. pedido", RespuestaPedidoResumido::getIdPedido),
                        ColumnaReporte.texto("Servicio", RespuestaPedidoResumido::getTituloServicio),
                        ColumnaReporte.texto("Etapa", RespuestaPedidoResumido::getEtapaActual),
                        ColumnaReporte.moneda("Precio pactado", RespuestaPedidoResumido::getPrecioPactado),
                        ColumnaReporte.fechaHora("Inicio", RespuestaPedidoResumido::getFechaInicio),
                        ColumnaReporte.fechaHora("Entrega estimada", RespuestaPedidoResumido::getFechaEntregaEstimada),
                        ColumnaReporte.texto("Creador", RespuestaPedidoResumido::getNombreCreador),
                        ColumnaReporte.texto("Cliente", RespuestaPedidoResumido::getNombreCliente)))
                .filas(filas)
                .generadoPor(correoSolicitante)
                .build();

        return servicioExportacion.exportar(modelo, formato);
    }

    @Override
    @Transactional
    // Las transiciones de flujo: incluye los intentos FALLIDOS, que
    // historial_estados_pedido (tabla de dominio) nunca registra.
    @Auditable(accion = "PEDIDO_AVANZAR_ETAPA", modulo = ModuloAuditoria.PEDIDOS,
            entidad = "pedidos", idEntidad = "#idPedido",
            detalle = "{observacion: #peticion.observacion}")
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

        notificacionService.notificar(pedido.getUsuarioCliente(), "PEDIDO_AVANCE",
                "Tu pedido ha avanzado a: " + siguienteConfig.getEtapa().getNombreEtapa());

        return mapToRespuesta(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaHistorialEstado> obtenerHistorial(Long idPedido, Long idUsuarioSolicitante) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado con ID: " + idPedido));
        // Evita que cualquier autenticado lea el historial de un pedido ajeno.
        ValidadorPertenenciaPedido.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);

        return historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(idPedido)
                .stream()
                .map(this::mapHistorial)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaSeguimientoPedido obtenerSeguimiento(Long idPedido, Long idUsuarioSolicitante) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));
        // Evita que cualquier autenticado lea el seguimiento de un pedido ajeno.
        ValidadorPertenenciaPedido.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);

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
                // 1.2: dato ya presente en `historial` (ordenado ASC), evita repetir
                // la consulta que obtenerEtapaActual(idPedido) haría por separado.
                .etapaActual(historial.isEmpty() ? "Sin estado" : historial.get(historial.size() - 1).getNombreEtapa())
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
