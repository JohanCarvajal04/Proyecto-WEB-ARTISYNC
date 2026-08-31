package uteq.edu.ec.artisync.service.pedido.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearFlujoTrabajo;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionEtapaConfig;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionSwapEtapas;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaEtapaConfig;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaFlujoTrabajo;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;
import uteq.edu.ec.artisync.entity.pedido.EtapaFlujo;
import uteq.edu.ec.artisync.entity.pedido.FlujoEtapaConfig;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.FlujoTrabajoRepository;
import uteq.edu.ec.artisync.repository.pedido.EtapaFlujoRepository;
import uteq.edu.ec.artisync.repository.pedido.FlujoEtapaConfigRepository;
import uteq.edu.ec.artisync.repository.pedido.HistorialEstadoPedidoRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.pedido.IFlujoTrabajoServicio;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlujoTrabajoServicioImpl implements IFlujoTrabajoServicio {

    private final FlujoTrabajoRepository flujoTrabajoRepository;
    private final EtapaFlujoRepository etapaFlujoRepository;
    private final FlujoEtapaConfigRepository flujoEtapaConfigRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialEstadoPedidoRepository historialEstadoPedidoRepository;

    @Override
    @Transactional
    public RespuestaFlujoTrabajo crearFlujoTrabajo(Long idUsuario, PeticionCrearFlujoTrabajo peticion) {
        if (flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario(peticion.getNombreFlujo(), idUsuario)) {
            throw new ExcepcionRecursoDuplicado("Ya existe un flujo de trabajo con el nombre: " + peticion.getNombreFlujo());
        }

        validarEtapasSinDuplicados(peticion.getEtapas());

        uteq.edu.ec.artisync.entity.seguridad.Usuario creador = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado"));

        FlujoTrabajo flujo = FlujoTrabajo.builder()
                .nombreFlujo(peticion.getNombreFlujo())
                .descripcionFlujo(peticion.getDescripcionFlujo())
                .creador(creador)
                .build();

        flujo = flujoTrabajoRepository.save(flujo);

        // Crear etapas si se proporcionaron
        if (peticion.getEtapas() != null && !peticion.getEtapas().isEmpty()) {
            for (PeticionEtapaConfig etapaReq : peticion.getEtapas()) {
                EtapaFlujo etapa = obtenerOCrearEtapa(etapaReq.getNombreEtapa());

                FlujoEtapaConfig config = FlujoEtapaConfig.builder()
                        .flujo(flujo)
                        .etapa(etapa)
                        .numeroOrden(etapaReq.getNumeroOrden())
                        .esEtapaFinal(etapaReq.isEsEtapaFinal())
                        .requiereEntregable(etapaReq.isRequiereEntregable())
                        .build();

                flujoEtapaConfigRepository.save(config);
            }
        }

        log.info("Flujo de trabajo '{}' creado con ID {}", flujo.getNombreFlujo(), flujo.getIdFlujo());
        return mapToRespuesta(flujo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaFlujoTrabajo> listarFlujosTrabajo(Long idUsuario, boolean puedeVerTodos) {
        List<FlujoTrabajo> flujos = puedeVerTodos
                ? flujoTrabajoRepository.findAllByOrderByIdFlujoAsc()
                : flujoTrabajoRepository.findByCreadorIdUsuario(idUsuario);
        return flujos.stream()
                .map(this::mapToRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaFlujoTrabajo obtenerFlujoPorId(Long idFlujo, Long idUsuario, boolean puedeVerTodos) {
        return mapToRespuesta(buscarFlujoAccesible(idFlujo, idUsuario, puedeVerTodos));
    }

    @Override
    @Transactional
    public RespuestaFlujoTrabajo actualizarFlujoTrabajo(Long idFlujo, Long idUsuario, boolean puedeVerTodos, PeticionCrearFlujoTrabajo peticion) {
        FlujoTrabajo flujo = buscarFlujoAccesible(idFlujo, idUsuario, puedeVerTodos);

        // La unicidad de nombre es por dueño real del flujo (V25:
        // UNIQUE(id_usuario_creador, nombre_flujo)), no por quien lo edita.
        if (flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuarioAndIdFlujoNot(
                peticion.getNombreFlujo(), flujo.getCreador().getIdUsuario(), idFlujo)) {
            throw new ExcepcionRecursoDuplicado("Ya existe un flujo de trabajo con el nombre: " + peticion.getNombreFlujo());
        }

        flujo.setNombreFlujo(peticion.getNombreFlujo());
        flujo.setDescripcionFlujo(peticion.getDescripcionFlujo());
        flujoTrabajoRepository.save(flujo);

        log.info("Flujo de trabajo '{}' actualizado", flujo.getNombreFlujo());
        return mapToRespuesta(flujo);
    }

    @Override
    @Transactional
    public RespuestaFlujoTrabajo agregarEtapa(Long idFlujo, Long idUsuario, boolean puedeVerTodos, PeticionEtapaConfig peticion) {
        FlujoTrabajo flujo = buscarFlujoAccesible(idFlujo, idUsuario, puedeVerTodos);

        EtapaFlujo etapa = obtenerOCrearEtapa(peticion.getNombreEtapa());

        if (flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(idFlujo, etapa.getIdEtapa())) {
            throw new ExcepcionRecursoDuplicado("La etapa '" + peticion.getNombreEtapa() + "' ya existe en este flujo");
        }

        if (flujoEtapaConfigRepository.existsByFlujoIdFlujoAndNumeroOrden(idFlujo, peticion.getNumeroOrden())) {
            throw new ExcepcionReglaNegocio(
                    "Ya hay una etapa con el número de orden " + peticion.getNumeroOrden() + " en este flujo.");
        }

        FlujoEtapaConfig config = FlujoEtapaConfig.builder()
                .flujo(flujo)
                .etapa(etapa)
                .numeroOrden(peticion.getNumeroOrden())
                .esEtapaFinal(peticion.isEsEtapaFinal())
                .requiereEntregable(peticion.isRequiereEntregable())
                .build();

        flujoEtapaConfigRepository.save(config);
        log.info("Etapa '{}' agregada al flujo '{}'", etapa.getNombreEtapa(), flujo.getNombreFlujo());

        return mapToRespuesta(flujo);
    }

    @Override
    @Transactional
    public RespuestaFlujoTrabajo actualizarEtapa(Long idFlujo, Long idFlujoEtapa, Long idUsuario, boolean puedeVerTodos, PeticionEtapaConfig peticion) {
        FlujoTrabajo flujo = buscarFlujoAccesible(idFlujo, idUsuario, puedeVerTodos);

        FlujoEtapaConfig config = flujoEtapaConfigRepository.findById(idFlujoEtapa)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Configuracion de etapa no encontrada"));

        if (!config.getFlujo().getIdFlujo().equals(idFlujo)) {
            throw new ExcepcionReglaNegocio("La etapa no pertenece al flujo especificado");
        }

        // Solo valida si el orden realmente cambia: alternarEtapaFinal reenvía
        // el mismo numeroOrden en cada toggle, y compararlo contra sí mismo
        // siempre "colisionaría". Reordenar de verdad usa intercambiarOrdenEtapas,
        // que hace el swap atómico — este chequeo es para llamadas directas a la
        // API que intenten mover una etapa a un orden ya ocupado por OTRA.
        if (!config.getNumeroOrden().equals(peticion.getNumeroOrden())
                && flujoEtapaConfigRepository.existsByFlujoIdFlujoAndNumeroOrden(idFlujo, peticion.getNumeroOrden())) {
            throw new ExcepcionReglaNegocio(
                    "Ya hay una etapa con el número de orden " + peticion.getNumeroOrden() + " en este flujo.");
        }

        config.setNumeroOrden(peticion.getNumeroOrden());
        config.setEsEtapaFinal(peticion.isEsEtapaFinal());
        config.setRequiereEntregable(peticion.isRequiereEntregable());

        flujoEtapaConfigRepository.save(config);
        log.info("Etapa {} actualizada en flujo {}", idFlujoEtapa, idFlujo);

        return mapToRespuesta(flujo);
    }

    @Override
    @Transactional
    public RespuestaFlujoTrabajo intercambiarOrdenEtapas(Long idFlujo, Long idUsuario, boolean puedeVerTodos, PeticionSwapEtapas peticion) {
        FlujoTrabajo flujo = buscarFlujoAccesible(idFlujo, idUsuario, puedeVerTodos);

        if (peticion.getIdFlujoEtapaA().equals(peticion.getIdFlujoEtapaB())) {
            throw new ExcepcionReglaNegocio("No se puede intercambiar una etapa consigo misma");
        }

        FlujoEtapaConfig a = flujoEtapaConfigRepository.findById(peticion.getIdFlujoEtapaA())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Configuracion de etapa no encontrada"));
        FlujoEtapaConfig b = flujoEtapaConfigRepository.findById(peticion.getIdFlujoEtapaB())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Configuracion de etapa no encontrada"));

        if (!a.getFlujo().getIdFlujo().equals(idFlujo) || !b.getFlujo().getIdFlujo().equals(idFlujo)) {
            throw new ExcepcionReglaNegocio("Las etapas no pertenecen al flujo especificado");
        }

        Integer ordenA = a.getNumeroOrden();
        a.setNumeroOrden(b.getNumeroOrden());
        b.setNumeroOrden(ordenA);
        flujoEtapaConfigRepository.save(a);
        flujoEtapaConfigRepository.save(b);

        log.info("Etapas {} y {} intercambiaron orden en flujo {}", a.getIdFlujoEtapa(), b.getIdFlujoEtapa(), idFlujo);
        return mapToRespuesta(flujo);
    }

    @Override
    @Transactional
    public void eliminarEtapa(Long idFlujo, Long idFlujoEtapa, Long idUsuario, boolean puedeVerTodos) {
        FlujoEtapaConfig config = flujoEtapaConfigRepository.findById(idFlujoEtapa)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Configuracion de etapa no encontrada"));

        boolean esDueno = config.getFlujo().getCreador().getIdUsuario().equals(idUsuario);
        if (!config.getFlujo().getIdFlujo().equals(idFlujo) || (!esDueno && !puedeVerTodos)) {
            throw new ExcepcionReglaNegocio("La etapa no pertenece al flujo especificado o no tiene permisos");
        }

        // Un pedido detenido en esta etapa dejaría de encontrarla en la
        // configuración del flujo al borrarla, y "retrocedería" a la primera
        // etapa en el siguiente avance (PedidoServicioImpl.obtenerOrdenActual).
        if (historialEstadoPedidoRepository.existePedidoEnEtapaActual(idFlujo, config.getEtapa().getIdEtapa())) {
            throw new ExcepcionReglaNegocio(
                    "No se puede eliminar la etapa '" + config.getEtapa().getNombreEtapa()
                            + "': hay pedidos actualmente detenidos en ella.");
        }

        flujoEtapaConfigRepository.delete(config);
        log.info("Etapa {} eliminada del flujo {}", idFlujoEtapa, idFlujo);
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    /** Con puedeVerTodos=true (FLUJO_MODERAR/ADMIN) accede a cualquier flujo; si no, solo a los propios. */
    private FlujoTrabajo buscarFlujoAccesible(Long idFlujo, Long idUsuario, boolean puedeVerTodos) {
        if (puedeVerTodos) {
            return flujoTrabajoRepository.findById(idFlujo)
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Flujo de trabajo no encontrado con ID: " + idFlujo));
        }
        return flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(idFlujo, idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Flujo de trabajo no encontrado con ID: " + idFlujo));
    }

    /**
     * Antes de crear un flujo con varias etapas de una vez, nada impedía
     * mandar dos con el mismo nombre o el mismo numeroOrden. Un nombre
     * repetido reventaba con un 500 crudo al chocar contra el UNIQUE
     * (id_flujo, id_etapa) de flujo_etapas_config (V25); un numeroOrden
     * repetido no tenía ninguna restricción y dejaba avanzarEtapa eligiendo
     * entre etapas empatadas sin desempate determinista.
     */
    private void validarEtapasSinDuplicados(List<PeticionEtapaConfig> etapas) {
        if (etapas == null || etapas.isEmpty()) {
            return;
        }

        java.util.Set<String> nombresVistos = new java.util.HashSet<>();
        java.util.Set<Integer> ordenesVistos = new java.util.HashSet<>();

        for (PeticionEtapaConfig etapa : etapas) {
            String nombreNormalizado = etapa.getNombreEtapa().trim().toLowerCase();
            if (!nombresVistos.add(nombreNormalizado)) {
                throw new ExcepcionReglaNegocio(
                        "Hay etapas repetidas: '" + etapa.getNombreEtapa() + "' aparece más de una vez.");
            }
            if (!ordenesVistos.add(etapa.getNumeroOrden())) {
                throw new ExcepcionReglaNegocio(
                        "Hay etapas con el mismo número de orden (" + etapa.getNumeroOrden()
                                + "): cada etapa debe tener un orden distinto.");
            }
        }
    }

    private EtapaFlujo obtenerOCrearEtapa(String nombreEtapa) {
        return etapaFlujoRepository.findByNombreEtapa(nombreEtapa)
                .orElseGet(() -> {
                    EtapaFlujo nueva = EtapaFlujo.builder()
                            .nombreEtapa(nombreEtapa)
                            .build();
                    return etapaFlujoRepository.save(nueva);
                });
    }

    private RespuestaFlujoTrabajo mapToRespuesta(FlujoTrabajo flujo) {
        List<FlujoEtapaConfig> etapas = flujoEtapaConfigRepository
                .findByFlujoIdFlujoOrderByNumeroOrdenAsc(flujo.getIdFlujo());

        return RespuestaFlujoTrabajo.builder()
                .idFlujo(flujo.getIdFlujo())
                .nombreFlujo(flujo.getNombreFlujo())
                .descripcionFlujo(flujo.getDescripcionFlujo())
                .etapas(etapas.stream().map(this::mapEtapaConfig).collect(Collectors.toList()))
                .idUsuarioCreador(flujo.getCreador().getIdUsuario())
                .nombreCreador(flujo.getCreador().getNombres() + " " + flujo.getCreador().getApellidos())
                .build();
    }

    private RespuestaEtapaConfig mapEtapaConfig(FlujoEtapaConfig config) {
        return RespuestaEtapaConfig.builder()
                .idFlujoEtapa(config.getIdFlujoEtapa())
                .idEtapa(config.getEtapa().getIdEtapa())
                .nombreEtapa(config.getEtapa().getNombreEtapa())
                .numeroOrden(config.getNumeroOrden())
                .esEtapaFinal(config.getEsEtapaFinal())
                .requiereEntregable(config.getRequiereEntregable())
                .build();
    }
}
