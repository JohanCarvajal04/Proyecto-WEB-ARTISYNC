# Módulo 7: Social, Comunidad y Sorteos

## Descripción General

Este módulo implementa las funcionalidades de **interacción social** de ARTISYNC: reseñas y calificaciones de servicios (1-5 estrellas), sorteos configurables con selección automática de ganadores, y la exportación de datos para auditoría. Complementa los módulos de perfil y pedidos con métricas sociales.

---

## Requisitos Funcionales Cubiertos

### RF-23 — Sorteos Configurables

| Campo | Detalle |
|---|---|
| **Prioridad** | 🟡 Media |
| **Descripción** | El Creador puede crear sorteos configurando: título, descripción del premio, número de ganadores, fecha de inicio, fecha de cierre y requisito de ser seguidor. Al llegar la fecha de cierre, el sistema ejecuta la selección aleatoria de ganadores y notifica. |
| **Criterio de Aceptación** | Al llegar fecha de cierre → ganadores seleccionados y notificados en máx. 60 segundos. Intentar modificar número de ganadores con participantes → "No se puede modificar este campo una vez iniciadas las inscripciones". Sorteo con requisito de seguidor → no permite inscribirse a no-seguidores. |

**Estado actual:** ❌ No implementado (entidades `Sorteo`, `ParticipanteSorteo` existen)

**Implementación requerida:**

### 7.1 CRUD de Sorteos

**Endpoints:**
```
POST   /api/sorteos                                 — Crear sorteo (CREADOR)
GET    /api/sorteos/{id}                            — Detalle del sorteo (público)
PUT    /api/sorteos/{id}                            — Editar sorteo (CREADOR, con restricciones)
DELETE /api/sorteos/{id}                            — Eliminar sorteo (CREADOR, sin participantes)
GET    /api/creadores/{id}/sorteos                  — Sorteos de un creador (público)
GET    /api/sorteos/activos                         — Sorteos activos (público)
```

**DTOs:**
```java
// CrearSorteoRequest.java
public class CrearSorteoRequest {
    @NotBlank @Size(max = 150) private String tituloSorteo;
    @NotBlank private String descripcionPremios;
    @NotNull @Min(1) private Integer cantidadGanadores;
    @NotNull @Future private LocalDateTime fechaInicio;
    @NotNull @Future private LocalDateTime fechaCierre;
    private boolean requiereSeguidor; // true = solo seguidores pueden participar
}

// SorteoResponse.java
public class SorteoResponse {
    private Long idSorteo;
    private String tituloSorteo;
    private String descripcionPremios;
    private Integer cantidadGanadores;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaCierre;
    private String estadoSorteo;
    private PerfilResumidoResponse creador;
    private Long totalParticipantes;
    private boolean requiereSeguidor;
    private boolean yoParticipo; // Para el usuario actual
    private List<GanadorResponse> ganadores; // Solo si estado = 'Finalizado'
}
```

### 7.2 Participación en Sorteos

**Endpoints:**
```
POST   /api/sorteos/{id}/participar                 — Inscribirse (JWT)
DELETE /api/sorteos/{id}/participar                 — Cancelar inscripción
GET    /api/sorteos/{id}/participantes              — Listar participantes
GET    /api/sorteos/{id}/ganadores                  — Ver ganadores (post-cierre)
```

**Validaciones de inscripción:**
```java
@Transactional
public ParticipanteResponse participar(Long idSorteo, Long idUsuario) {
    Sorteo sorteo = sorteoRepository.findById(idSorteo)
        .orElseThrow(() -> new ResourceNotFoundException("Sorteo no encontrado"));
    
    // Validar estado del sorteo
    if (!"Activo".equals(sorteo.getEstadoSorteo())) {
        throw new BusinessRuleException("El sorteo no está activo");
    }
    
    // Validar que la fecha actual está dentro del rango
    LocalDateTime ahora = LocalDateTime.now();
    if (ahora.isBefore(sorteo.getFechaInicio())) {
        throw new BusinessRuleException("El sorteo aún no ha comenzado");
    }
    if (ahora.isAfter(sorteo.getFechaCierre())) {
        throw new BusinessRuleException("El periodo de inscripción ha finalizado");
    }
    
    // Validar que no está ya inscrito
    if (participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(idSorteo, idUsuario)) {
        throw new DuplicateResourceException("Ya estás inscrito en este sorteo");
    }
    
    // Validar requisito de seguidor
    if (sorteo.getRequiereSeguidor()) {
        boolean esSeguidor = seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(
            idUsuario, sorteo.getPerfilCreador().getIdPerfil());
        if (!esSeguidor) {
            throw new BusinessRuleException(
                "Este sorteo requiere que sigas al creador para poder participar");
        }
    }
    
    ParticipanteSorteo participante = ParticipanteSorteo.builder()
        .sorteo(sorteo)
        .usuario(usuarioRepository.getReferenceById(idUsuario))
        .esGanador(false)
        .build();
    participanteSorteoRepository.save(participante);
    
    return mapToResponse(participante);
}
```

### 7.3 Restricción de Edición con Participantes

```java
@Transactional
public SorteoResponse actualizarSorteo(Long idSorteo, ActualizarSorteoRequest request, Long idCreador) {
    Sorteo sorteo = verificarPropietario(idSorteo, idCreador);
    
    boolean tieneParticipantes = participanteSorteoRepository.existsBySorteoIdSorteo(idSorteo);
    
    if (tieneParticipantes) {
        // Campos NO modificables una vez hay participantes
        if (request.getCantidadGanadores() != null && 
            !request.getCantidadGanadores().equals(sorteo.getCantidadGanadores())) {
            throw new BusinessRuleException(
                "No se puede modificar este campo una vez iniciadas las inscripciones");
        }
        if (request.getFechaCierre() != null && 
            !request.getFechaCierre().equals(sorteo.getFechaCierre())) {
            throw new BusinessRuleException(
                "No se puede modificar la fecha de cierre una vez iniciadas las inscripciones");
        }
    }
    
    // Campos siempre editables: título, descripción
    if (request.getTituloSorteo() != null) sorteo.setTituloSorteo(request.getTituloSorteo());
    if (request.getDescripcionPremios() != null) sorteo.setDescripcionPremios(request.getDescripcionPremios());
    
    return mapToResponse(sorteoRepository.save(sorteo));
}
```

### 7.4 Selección Automática de Ganadores (Scheduled Task)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SorteoScheduler {
    
    private final SorteoRepository sorteoRepository;
    private final ParticipanteSorteoRepository participanteSorteoRepository;
    private final NotificacionService notificacionService;
    
    @Scheduled(fixedRate = 60000) // Ejecutar cada 60 segundos
    @Transactional
    public void procesarSorteosCerrados() {
        List<Sorteo> sorteosPendientes = sorteoRepository
            .findByEstadoSorteoAndFechaCierreBefore("Activo", LocalDateTime.now());
        
        for (Sorteo sorteo : sorteosPendientes) {
            try {
                ejecutarSorteo(sorteo);
            } catch (Exception e) {
                log.error("Error al procesar sorteo {}: {}", sorteo.getIdSorteo(), e.getMessage());
            }
        }
    }
    
    private void ejecutarSorteo(Sorteo sorteo) {
        List<ParticipanteSorteo> participantes = participanteSorteoRepository
            .findBySorteoIdSorteoAndEsGanadorFalse(sorteo.getIdSorteo());
        
        if (participantes.isEmpty()) {
            sorteo.setEstadoSorteo("Finalizado_Sin_Participantes");
            sorteoRepository.save(sorteo);
            return;
        }
        
        // Selección aleatoria
        Collections.shuffle(participantes, new SecureRandom());
        int cantidadGanadores = Math.min(sorteo.getCantidadGanadores(), participantes.size());
        
        List<ParticipanteSorteo> ganadores = participantes.subList(0, cantidadGanadores);
        
        for (ParticipanteSorteo ganador : ganadores) {
            ganador.setEsGanador(true);
            ganador.setFechaNotificacionPremio(LocalDateTime.now());
            participanteSorteoRepository.save(ganador);
            
            // Notificar al ganador
            notificacionService.notificar(ganador.getUsuario(), "SORTEO_GANADOR",
                "¡Felicidades! Has ganado el sorteo: " + sorteo.getTituloSorteo());
        }
        
        sorteo.setEstadoSorteo("Finalizado");
        sorteoRepository.save(sorteo);
        
        log.info("Sorteo {} finalizado. {} ganadores seleccionados de {} participantes",
            sorteo.getIdSorteo(), cantidadGanadores, participantes.size());
    }
}
```

---

## Reseñas y Calificaciones (Complemento de RF-09)

**Endpoints:**
```
POST   /api/pedidos/{id}/resena                    — Crear reseña (CLIENTE, 1-5 estrellas, solo post-entrega)
GET    /api/creadores/{id}/resenas                 — Listar reseñas del creador (público)
GET    /api/creadores/{id}/resenas/promedio         — Promedio de calificaciones
```

**DTOs:**
```java
// CrearResenaRequest.java
public class CrearResenaRequest {
    @NotNull @Min(1) @Max(5) private Integer calificacionEstrellas;
    @Size(max = 2000) private String textoResena;
}

// ResenaResponse.java
public class ResenaResponse {
    private Long idResena;
    private Integer calificacionEstrellas;
    private String textoResena;
    private LocalDateTime fechaResena;
    private String nombreCliente;
    private String tituloServicio;
}
```

**Validaciones:**
```java
@Transactional
public ResenaResponse crearResena(Long idPedido, CrearResenaRequest request, Long idCliente) {
    Pedido pedido = pedidoRepository.findById(idPedido).orElseThrow(...);
    
    // Solo el cliente del pedido puede dejar reseña
    if (!pedido.getUsuarioCliente().getIdUsuario().equals(idCliente)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el cliente del pedido puede dejar reseña");
    }
    
    // Solo se puede reseñar si el pedido está entregado
    EntregableFinal entregable = entregableRepository.findByPedidoIdPedido(idPedido).orElse(null);
    if (entregable == null || !entregable.getEstaLiberado()) {
        throw new BusinessRuleException("Solo puedes dejar una reseña después de recibir el entregable");
    }
    
    // Solo una reseña por pedido (UNIQUE constraint en BD)
    if (resenaServicioRepository.existsByPedidoIdPedido(idPedido)) {
        throw new DuplicateResourceException("Ya has dejado una reseña para este pedido");
    }
    
    ResenaServicio resena = ResenaServicio.builder()
        .pedido(pedido)
        .calificacionEstrellas(request.getCalificacionEstrellas())
        .textoResena(request.getTextoResena())
        .build();
    resenaServicioRepository.save(resena);
    
    return mapToResponse(resena);
}
```

---

## Exportación CSV de Transacciones (RNF-13)

**Endpoint:**
```
GET /api/admin/transacciones/{creadorId}/csv    — Exportar historial CSV (ADMIN)
```

**Implementación:**
```java
@GetMapping("/admin/transacciones/{creadorId}/csv")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<byte[]> exportarCsv(@PathVariable Long creadorId) {
    List<TransaccionPago> transacciones = transaccionPagoRepository
        .findByCreadorId(creadorId);
    
    StringBuilder csv = new StringBuilder();
    csv.append("ID,Tipo,Monto,Fecha,ID_Pedido,Estado_Fondos\n");
    
    for (TransaccionPago t : transacciones) {
        csv.append(String.format("%d,%s,%.2f,%s,%d,%s\n",
            t.getIdTransaccion(),
            t.getTipoTransaccion(),
            t.getMonto(),
            t.getFechaEjecucion(),
            t.getPago().getContrato().getPedido().getIdPedido(),
            t.getPago().getEstadoFondos()
        ));
    }
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transacciones_" + creadorId + ".csv")
        .header(HttpHeaders.CONTENT_TYPE, "text/csv")
        .body(csv.toString().getBytes(StandardCharsets.UTF_8));
}
```

---

## Tablas de Base de Datos

| Tabla | Entidad JPA | Paquete | Repositorio | Estado Entidad | Estado Repo |
|---|---|---|---|---|---|
| `resenas_servicios` | `ResenaServicio` | `entity.social` | — | ✅ | ❌ Crear |
| `sorteos` | `Sorteo` | `entity.social` | `SorteoRepository` | ✅ | ✅ (ampliar) |
| `participantes_sorteo` | `ParticipanteSorteo` | `entity.social` | — | ✅ | ❌ Crear |

**Ajustes a entidades existentes:**

- `Sorteo`: agregar campo `requiereSeguidor` (boolean) → migración V5:
  ```sql
  ALTER TABLE sorteos ADD COLUMN IF NOT EXISTS requiere_seguidor BOOLEAN DEFAULT FALSE;
  ```

---

## Archivos a Crear

```
controller/
├── SorteoController.java
├── ResenaController.java
└── AuditController.java

service/
├── SorteoService.java
├── ResenaService.java
├── AuditService.java
└── impl/
    ├── SorteoServiceImpl.java
    ├── ResenaServiceImpl.java
    └── AuditServiceImpl.java

scheduler/
└── SorteoScheduler.java              — @Scheduled para ejecutar sorteos

dto/request/
├── CrearSorteoRequest.java
├── ActualizarSorteoRequest.java
└── CrearResenaRequest.java

dto/response/
├── SorteoResponse.java
├── ParticipanteResponse.java
├── GanadorResponse.java
└── ResenaResponse.java

repository/
├── ResenaServicioRepository.java
└── ParticipanteSorteoRepository.java
```

---

## Checklist de Validación del Módulo

### Sorteos
- [ ] Creador puede crear sorteo con todos los campos obligatorios
- [ ] Fechas de inicio y cierre validadas (futuras y coherentes)
- [ ] Participación funcional con validación de seguidor
- [ ] No se puede modificar `cantidadGanadores` con participantes inscritos
- [ ] Selección automática de ganadores al llegar fecha de cierre (≤ 60 seg)
- [ ] Ganadores notificados automáticamente
- [ ] Sorteo con requisito de seguidor bloquea a no-seguidores
- [ ] Lista de ganadores visible post-cierre

### Reseñas
- [ ] Solo el cliente del pedido puede dejar reseña
- [ ] Solo post-entrega (entregable liberado)
- [ ] Una reseña por pedido (UNIQUE)
- [ ] Calificación entre 1 y 5 estrellas
- [ ] Promedio de calificaciones coincide con cálculo manual (RF-09)

### Auditoría
- [ ] Exportación CSV con todos los campos y timestamps (RNF-13)
- [ ] DELETE/PATCH a historial → HTTP 403 (RNF-13)
- [ ] CSV descargable desde panel admin

---

## Dependencias con Otros Módulos

| Módulo | Relación |
|---|---|
| **M1** | Auth + roles CREADOR/CLIENTE/ADMIN |
| **M2** | Seguidores: validar requisito de seguidor en sorteos |
| **M2** | Perfil público: promedio de calificaciones (RF-09) |
| **M4** | Reseñas vinculadas a pedidos entregados |
| **M5** | Reseñas solo post-liberación de entregable; CSV incluye transacciones |
| **M6** | Notificaciones de sorteos ganados y nuevas reseñas |

---

## Prerrequisitos del Sistema

- [x] M1 funcional (auth + roles)
- [x] M2 funcional (seguidores para validar requisito de sorteo)
- [x] M4 funcional (pedidos para vincular reseñas)
- [x] M5 funcional (entregables liberados para permitir reseñas)
- [x] M6 funcional (notificaciones para ganadores)
- [ ] `@EnableScheduling` habilitado en la aplicación principal
- [ ] Migración V5 con campo `requiere_seguidor` ejecutada
- [ ] Repositorios del módulo creados
