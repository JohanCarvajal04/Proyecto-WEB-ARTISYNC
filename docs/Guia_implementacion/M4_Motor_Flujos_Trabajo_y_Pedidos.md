# Módulo 4: Motor de Flujos de Trabajo y Pedidos

## Descripción General

Este módulo implementa el **motor de flujos de trabajo** que gestiona el ciclo de vida de cada pedido en ARTISYNC. Define etapas configurables por categoría de servicio, permite transiciones de estado con registro de auditoría inmutable, y ofrece al cliente una vista de seguimiento en tiempo real.

---

## Requisitos Funcionales Cubiertos

### RF-19 — Flujo de Trabajo con Etapas Configurables

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | El sistema debe gestionar el flujo de trabajo de cada pedido mediante etapas configuradas según la categoría del servicio. Cada transición realizada por el Creador queda registrada con marca de tiempo. El Cliente debe visualizar la etapa activa en tiempo real. |
| **Criterio de Aceptación** | Al avanzar una etapa → la vista del Cliente refleja el cambio en máx. 5 segundos sin recargar página. El historial contiene un registro por cada transición y no es posible eliminar ni modificar estos registros desde la interfaz. |

**Estado actual:** ❌ No implementado (entidades JPA de todo el módulo ya existen)

**Implementación requerida:**

### 4.1 CRUD de Flujos de Trabajo (Admin/Creador)

**Endpoints:**
```
POST   /api/admin/flujos                          — Crear flujo de trabajo (ADMIN)
GET    /api/flujos                                — Listar flujos disponibles
GET    /api/flujos/{id}                           — Detalle de un flujo con etapas
PUT    /api/admin/flujos/{id}                     — Editar flujo (ADMIN)
POST   /api/admin/flujos/{id}/etapas              — Agregar etapa al flujo (ADMIN)
PUT    /api/admin/flujos/{id}/etapas/{etapaId}    — Reordenar/editar etapa (ADMIN)
DELETE /api/admin/flujos/{id}/etapas/{etapaId}    — Eliminar etapa (ADMIN)
```

**DTOs:**
```java
// CrearFlujoTrabajoRequest.java
public class CrearFlujoTrabajoRequest {
    @NotBlank private String nombreFlujo;
    private String descripcionFlujo;
    private List<EtapaConfigRequest> etapas;
}

// EtapaConfigRequest.java
public class EtapaConfigRequest {
    @NotBlank private String nombreEtapa;
    @NotNull private Integer numeroOrden;
    private boolean esEtapaFinal;
}

// FlujoTrabajoResponse.java
public class FlujoTrabajoResponse {
    private Long idFlujo;
    private String nombreFlujo;
    private String descripcionFlujo;
    private List<EtapaConfigResponse> etapas; // ordenadas por numero_orden
}
```

### 4.2 Gestión de Pedidos

**Endpoints:**
```
POST   /api/pedidos                               — Crear pedido (CLIENTE solicita servicio)
GET    /api/pedidos/{id}                          — Detalle del pedido
GET    /api/pedidos/mis-pedidos                   — Mis pedidos como cliente
GET    /api/pedidos/mis-comisiones                — Mis pedidos como creador
PUT    /api/pedidos/{id}/avanzar                  — Avanzar a siguiente etapa (CREADOR)
GET    /api/pedidos/{id}/historial                — Historial de estados (inmutable)
GET    /api/pedidos/{id}/seguimiento              — Estado actual para el cliente
```

**DTOs:**
```java
// CrearPedidoRequest.java
public class CrearPedidoRequest {
    @NotNull private Long idServicio;
    private BigDecimal precioOfrecido; // Puede negociarse o ser el precio base
    private LocalDateTime fechaEntregaEstimada;
}

// PedidoResponse.java
public class PedidoResponse {
    private Long idPedido;
    private ServicioResumidoResponse servicio;
    private UserResumidoResponse cliente;
    private UserResumidoResponse creador;
    private String etapaActual;
    private BigDecimal precioPactado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaEntregaEstimada;
    private List<HistorialEstadoResponse> historial;
}

// HistorialEstadoResponse.java
public class HistorialEstadoResponse {
    private Long idHistorial;
    private String nombreEtapa;
    private LocalDateTime fechaTransicion;
    private String observacion;
}
```

### 4.3 Lógica de Transición de Estados

```java
@Transactional
public PedidoResponse avanzarEtapa(Long idPedido, String observacion) {
    Pedido pedido = pedidoRepository.findById(idPedido)
        .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
    
    // Verificar que el creador es el dueño del pedido
    verificarPropietario(pedido);
    
    // Obtener etapa actual del historial
    HistorialEstadoPedido ultimoEstado = historialRepository
        .findTopByPedidoIdPedidoOrderByFechaTransicionDesc(idPedido)
        .orElseThrow(() -> new BusinessRuleException("Pedido sin estado inicial"));
    
    // Obtener siguiente etapa del flujo configurado
    FlujoEtapaConfig siguienteConfig = flujoEtapaConfigRepository
        .findByIdFlujoAndNumeroOrdenGreaterThan(
            pedido.getFlujo().getIdFlujo(), 
            getCurrentOrden(pedido, ultimoEstado))
        .stream().findFirst()
        .orElseThrow(() -> new BusinessRuleException("El pedido ya se encuentra en la etapa final"));
    
    // Registrar transición (INMUTABLE)
    HistorialEstadoPedido nuevoEstado = HistorialEstadoPedido.builder()
        .pedido(pedido)
        .etapa(siguienteConfig.getEtapa())
        .observacion(observacion)
        .build(); // fechaTransicion = DEFAULT NOW()
    historialRepository.save(nuevoEstado);
    
    // Notificar al cliente (Módulo 6)
    notificacionService.notificar(pedido.getUsuarioCliente(), 
        "PEDIDO_AVANCE", "Tu pedido ha avanzado a: " + siguienteConfig.getEtapa().getNombreEtapa());
    
    return mapToResponse(pedido);
}
```

### 4.4 Inmutabilidad del Historial (RNF-13)

**CRÍTICO:** Los registros de `historial_estados_pedido` NO pueden ser eliminados ni modificados.

```java
// En el controller, agregar endpoints que SIEMPRE retornan 403
@DeleteMapping("/pedidos/{id}/historial")
public ResponseEntity<MessageResponse> bloquearDelete() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new MessageResponse("Operación no permitida sobre registros de auditoría"));
}

@PatchMapping("/pedidos/{id}/historial")
public ResponseEntity<MessageResponse> bloquearPatch() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new MessageResponse("Operación no permitida sobre registros de auditoría"));
}
```

### 4.5 Tickets de Revisión

**Endpoints:**
```
POST   /api/pedidos/{id}/tickets-revision         — Crear ticket de revisión (CLIENTE)
GET    /api/pedidos/{id}/tickets-revision          — Listar tickets del pedido
PUT    /api/tickets-revision/{id}/estado           — Cambiar estado del ticket (CREADOR)
```

**DTOs:**
```java
// CrearTicketRevisionRequest.java
public class CrearTicketRevisionRequest {
    @NotNull private Long idMotivo;
    @NotBlank private String descripcionCliente;
}
```

---

## Tablas de Base de Datos

| Tabla | Entidad JPA | Paquete | Repositorio | Estado Entidad | Estado Repo |
|---|---|---|---|---|---|
| `etapas_flujo` | `EtapaFlujo` | `entity.pedido` | — | ✅ | ❌ Crear |
| `flujo_etapas_config` | `FlujoEtapaConfig` | `entity.pedido` | — | ✅ | ❌ Crear |
| `pedidos` | `Pedido` | `entity.pedido` | `PedidoRepository` | ✅ | ✅ (ampliar) |
| `historial_estados_pedido` | `HistorialEstadoPedido` | `entity.pedido` | `HistorialEstadoPedidoRepository` | ✅ | ✅ (ampliar) |
| `motivos_rechazo` | `MotivoRechazo` | `entity.pedido` | — | ✅ | ❌ Crear |
| `tickets_revision` | `TicketRevision` | `entity.pedido` | — | ✅ | ❌ Crear |
| `plantillas_contrato` | `PlantillaContrato` | `entity.pedido` | — | ✅ | ❌ Crear |
| `flujos_trabajo` | `FlujoTrabajo` | `entity.catalogo` | — | ✅ | ❌ Crear |

---

## Archivos a Crear

```
controller/
├── PedidoController.java
├── FlujoTrabajoController.java
└── TicketRevisionController.java

service/
├── PedidoService.java
├── FlujoTrabajoService.java
├── TicketRevisionService.java
└── impl/
    ├── PedidoServiceImpl.java
    ├── FlujoTrabajoServiceImpl.java
    └── TicketRevisionServiceImpl.java

dto/request/
├── CrearPedidoRequest.java
├── AvanzarEtapaRequest.java
├── CrearFlujoTrabajoRequest.java
├── EtapaConfigRequest.java
└── CrearTicketRevisionRequest.java

dto/response/
├── PedidoResponse.java
├── PedidoResumidoResponse.java
├── SeguimientoPedidoResponse.java
├── HistorialEstadoResponse.java
├── FlujoTrabajoResponse.java
├── EtapaConfigResponse.java
└── TicketRevisionResponse.java

repository/
├── EtapaFlujoRepository.java
├── FlujoEtapaConfigRepository.java
├── FlujoTrabajoRepository.java
├── MotivoRechazoRepository.java
├── TicketRevisionRepository.java
└── PlantillaContratoRepository.java
```

---

## Checklist de Validación del Módulo

- [ ] CRUD de flujos de trabajo con etapas ordenadas
- [ ] Crear pedido como Cliente para un Servicio específico
- [ ] Listar mis pedidos (cliente) y mis comisiones (creador)
- [ ] Creador avanza etapas con registro automático de timestamp
- [ ] Historial de estados inmutable (DELETE/PATCH → 403)
- [ ] Cliente ve actualización de etapa en < 5 segundos
- [ ] Tickets de revisión creados por el cliente
- [ ] Vista de seguimiento funcional para el cliente
- [ ] Notificación al cliente al avanzar etapa
- [ ] Un pedido no puede avanzar más allá de la etapa final

---

## Dependencias con Otros Módulos

| Módulo | Relación |
|---|---|
| **M1** | Auth + roles CREADOR/CLIENTE |
| **M3** | Pedidos se crean para un `Servicio` específico |
| **M5** | Al firmar contrato se crea sala de chat; los tickets pueden generar enlaces de pago |
| **M6** | Notificaciones al avanzar etapa; actualización en tiempo real vía WebSocket |

---

## Prerrequisitos del Sistema

- [x] M1 funcional (auth + roles)
- [x] M3 funcional (servicios publicados)
- [ ] Repositorios del módulo creados
- [ ] Datos semilla de flujos de trabajo y etapas para pruebas
