# Módulo 5: Legal, Entregables y Finanzas (Escrow)

## Descripción General

Este módulo es el **motor financiero y legal** de ARTISYNC. Gestiona la generación automática de contratos, la firma electrónica por ambas partes, la generación de PDF, la integración con PayPal para pagos en escrow, la entrega de trabajos con marca de agua, y la liberación de fondos. Es el módulo más complejo del sistema.

---

## Requisitos Funcionales Cubiertos

### RF-17 — Generación Automática de Contrato HTML

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | El sistema genera automáticamente un contrato HTML a partir de una plantilla activa, sustituyendo variables: nombre del Creador, nombre del Cliente, descripción del servicio, precio pactado, límite de revisiones y fecha estimada de entrega. |
| **Criterio de Aceptación** | Contrato generado en máx. 5 segundos tras completar briefing. Contiene nombres, precio y fecha. Datos incompletos → indica campos faltantes y no genera contrato. |

**Estado actual:** ❌ No implementado (entidades `Contrato`, `PlantillaContrato` existen)

**Implementación requerida:**

1. **Motor de plantillas:**
   ```java
   public String generarContratoHtml(PlantillaContrato plantilla, DatosContrato datos) {
       String html = plantilla.getCuerpoHtmlPlantilla();
       html = html.replace("{{nombre_creador}}", datos.getNombreCreador());
       html = html.replace("{{nombre_cliente}}", datos.getNombreCliente());
       html = html.replace("{{descripcion_servicio}}", datos.getDescripcion());
       html = html.replace("{{precio_pactado}}", datos.getPrecio().toString());
       html = html.replace("{{limite_revisiones}}", String.valueOf(datos.getLimiteRevisiones()));
       html = html.replace("{{fecha_entrega}}", datos.getFechaEntrega().toString());
       html = html.replace("{{fecha_actual}}", LocalDate.now().toString());
       return html;
   }
   ```

2. **Datos semilla de plantilla:**
   ```sql
   INSERT INTO plantillas_contrato (version_legal, cuerpo_html_plantilla) VALUES (
     'v1.0',
     '<html><head><title>Contrato ARTISYNC</title></head><body>
       <h1>Contrato de Servicio</h1>
       <p>Entre <strong>{{nombre_creador}}</strong> (Creador) y 
       <strong>{{nombre_cliente}}</strong> (Cliente).</p>
       <p>Servicio: {{descripcion_servicio}}</p>
       <p>Precio pactado: ${{precio_pactado}} USD</p>
       <p>Límite de revisiones: {{limite_revisiones}}</p>
       <p>Fecha estimada de entrega: {{fecha_entrega}}</p>
       <p>Fecha del contrato: {{fecha_actual}}</p>
       <hr><p>Firma Creador: _______________</p>
       <p>Firma Cliente: _______________</p>
     </body></html>'
   );
   ```

---

### RF-18 — Firma Electrónica y Descarga PDF

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | La firma electrónica es la acción explícita de hacer clic en "Firmar contrato". El pedido no avanza hasta que ambas partes firmen. El contrato firmado puede descargarse en PDF. |
| **Criterio de Aceptación** | Solo Creador firmó → botón "Iniciar producción" deshabilitado + "Esperando firma del Cliente". Ambos firman → botón se habilita. PDF contiene hashes de ambas firmas en el pie. |

**Implementación requerida:**

1. **Endpoints:**
   ```
   POST /api/contratos/{id}/firmar           — Firmar contrato (CREADOR o CLIENTE)
   GET  /api/contratos/{id}                  — Ver contrato HTML renderizado
   GET  /api/contratos/{id}/pdf              — Descargar contrato como PDF
   GET  /api/contratos/{id}/estado-firma     — Estado de las firmas
   ```

2. **Lógica de firma:**
   ```java
   public ContratoResponse firmarContrato(Long idContrato, Long idUsuario) {
       Contrato contrato = contratoRepository.findById(idContrato)
           .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));
       
       Pedido pedido = contrato.getPedido();
       String hash = generarHashFirma(idContrato, idUsuario);
       
       if (idUsuario.equals(pedido.getServicio().getPerfil().getUsuario().getIdUsuario())) {
           // Es el Creador
           contrato.setHashFirmaCreador(hash);
       } else if (idUsuario.equals(pedido.getUsuarioCliente().getIdUsuario())) {
           // Es el Cliente
           contrato.setHashFirmaCliente(hash);
       } else {
           throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres parte de este contrato");
       }
       
       contratoRepository.save(contrato);
       
       // Si ambos firmaron, crear sala de chat (RF-14)
       if (contrato.getHashFirmaCreador() != null && contrato.getHashFirmaCliente() != null) {
           chatService.crearSala(pedido);
       }
       
       return mapToResponse(contrato);
   }
   
   private String generarHashFirma(Long idContrato, Long idUsuario) {
       String data = idContrato + ":" + idUsuario + ":" + Instant.now().toString();
       return DigestUtils.sha256Hex(data);
   }
   ```

3. **Generación de PDF (RNF-06: ≤ 5 segundos):**

   Dependencia necesaria en `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.openhtmltopdf</groupId>
       <artifactId>openhtmltopdf-pdfbox</artifactId>
       <version>1.1.22</version>
   </dependency>
   ```

   ```java
   public byte[] generarPdf(Long idContrato) {
       long start = System.currentTimeMillis();
       Contrato contrato = contratoRepository.findById(idContrato)
           .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado"));
       
       String html = renderizarContratoCompleto(contrato); // Incluir hashes de firma en el pie
       
       try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
           PdfRendererBuilder builder = new PdfRendererBuilder();
           builder.useFastMode();
           builder.withHtmlContent(html, "/");
           builder.toStream(os);
           builder.run();
           
           long elapsed = System.currentTimeMillis() - start;
           log.info("PDF generado en {} ms (RNF-06: máx 5000ms)", elapsed);
           
           return os.toByteArray();
       }
   }
   ```

---

### RF-20 — Pagos con PayPal Orders v2 (Escrow)

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | Generar enlace de pago mediante API oficial de PayPal Orders v2. Al recibir confirmación vía webhook, actualizar estado de fondos y notificar a ambas partes. |
| **Criterio de Aceptación** | Pago confirmado en PayPal sandbox → estado de fondos actualizado en máx. 10 segundos. Ambas partes reciben notificación. |

**Implementación requerida:**

1. **Dependencia PayPal:**
   ```xml
   <dependency>
       <groupId>com.paypal.sdk</groupId>
       <artifactId>checkout-sdk</artifactId>
       <version>2.0.0</version>
   </dependency>
   ```

2. **Endpoints:**
   ```
   POST /api/pedidos/{id}/pago             — Crear orden PayPal y retornar URL
   POST /api/webhooks/paypal               — Webhook PayPal (público, verificar firma)
   GET  /api/pedidos/{id}/pago/estado      — Consultar estado de los fondos
   ```

3. **Flujo de pago:**
   ```
   1. Cliente solicita pago → backend crea orden PayPal con monto del contrato
   2. PayPal retorna approvalUrl → frontend redirige al cliente
   3. Cliente aprueba en PayPal → PayPal envía webhook al backend
   4. Backend verifica firma del webhook (RNF-14)
   5. Backend actualiza pagos_garantia.estado_fondos = 'Retenido'
   6. Backend registra transaccion_pago (tipo: 'Ingreso')
   7. Backend notifica a ambas partes
   ```

4. **Variables de entorno:**
   ```
   PAYPAL_CLIENT_ID=sandbox_client_id
   PAYPAL_CLIENT_SECRET=sandbox_client_secret
   PAYPAL_MODE=sandbox
   PAYPAL_WEBHOOK_ID=webhook_id
   ```

5. **Verificación de firma del webhook (RNF-14):**
   ```java
   public boolean verificarWebhookPayPal(HttpServletRequest request, String body) {
       // Verificar los headers de PayPal:
       // PAYPAL-TRANSMISSION-ID, PAYPAL-TRANSMISSION-TIME, 
       // PAYPAL-TRANSMISSION-SIG, PAYPAL-CERT-URL
       // Usar PayPal SDK para validar la firma
   }
   ```

---

### RF-21 — Entregables con Marca de Agua

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | El Creador sube versión con marca de agua para previsualización. Al aprobar, el Cliente desencadena liberación de fondos y descarga de versión limpia. La comisión de la plataforma se registra automáticamente. |
| **Criterio de Aceptación** | Aprobación → botón de descarga habilitado inmediatamente. Descarga antes de aprobar → "El entregable no está disponible hasta que el pago sea liberado". Comisión registrada con monto correcto. |

**Endpoints:**
```
POST /api/pedidos/{id}/entregable                — Subir entregable (CREADOR, 2 archivos: marcaAgua + limpio)
GET  /api/pedidos/{id}/entregable                — Ver entregable (marca de agua si no aprobado)
POST /api/pedidos/{id}/aprobar                   — Aprobar trabajo (CLIENTE → libera fondos)
GET  /api/pedidos/{id}/entregable/descargar      — Descargar versión limpia (solo post-aprobación)
```

**Lógica de aprobación:**
```java
@Transactional
public void aprobarEntrega(Long idPedido, Long idCliente) {
    Pedido pedido = verificarPedidoCliente(idPedido, idCliente);
    EntregableFinal entregable = entregableRepository.findByPedidoIdPedido(idPedido)
        .orElseThrow(() -> new ResourceNotFoundException("No hay entregable para este pedido"));
    
    if (entregable.getEstaLiberado()) {
        throw new BusinessRuleException("El entregable ya fue aprobado");
    }
    
    // Liberar fondos
    PagoGarantia pago = pagoGarantiaRepository.findByContratoIdPedido(idPedido)
        .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
    pago.setEstadoFondos("Liberado");
    pagoGarantiaRepository.save(pago);
    
    // Registrar transacciones
    BigDecimal comision = pago.getMontoRetenido().multiply(BigDecimal.valueOf(0.10)); // 10% plataforma
    BigDecimal pagoCreador = pago.getMontoRetenido().subtract(comision);
    
    transaccionPagoRepository.save(TransaccionPago.builder()
        .pago(pago).tipoTransaccion("Egreso").monto(pagoCreador).build());
    transaccionPagoRepository.save(TransaccionPago.builder()
        .pago(pago).tipoTransaccion("Comision").monto(comision).build());
    
    // Habilitar descarga
    entregable.setEstaLiberado(true);
    entregableRepository.save(entregable);
    
    // Cerrar sala de chat
    chatService.cerrarSala(idPedido);
    
    // Notificar
    notificacionService.notificar(pedido.getServicio().getPerfil().getUsuario(), 
        "PAGO_LIBERADO", "El pago ha sido liberado por el cliente");
}
```

---

### RF-22 — Cargo por Revisión Adicional

| Campo | Detalle |
|---|---|
| **Prioridad** | 🟡 Media |
| **Descripción** | El Creador configura un cargo por revisión adicional. Si el Cliente supera el límite del contrato, se crea automáticamente un nuevo enlace de pago PayPal. Si no se paga en 48h, el ticket se rechaza. |
| **Criterio de Aceptación** | Ticket que supera límite → enlace de pago visible en máx. 5 segundos. 48h sin pago → estado "rechazado" + notificación al Creador. Ticket dentro del límite → sin enlace de pago adicional. |

**Implementación:**
```java
public TicketRevisionResponse crearTicketRevision(Long idPedido, CrearTicketRevisionRequest request) {
    Pedido pedido = pedidoRepository.findById(idPedido).orElseThrow(...);
    Contrato contrato = contratoRepository.findByPedidoIdPedido(idPedido).orElseThrow(...);
    
    long revisionesActuales = ticketRevisionRepository.countByPedidoIdPedido(idPedido);
    
    TicketRevision ticket = TicketRevision.builder()
        .pedido(pedido)
        .motivo(motivoRechazoRepository.findById(request.getIdMotivo()).orElseThrow(...))
        .descripcionCliente(request.getDescripcionCliente())
        .estadoTicket("Abierto")
        .build();
    
    if (revisionesActuales >= contrato.getLimiteRevisiones()) {
        // Supera el límite → generar cargo adicional
        BigDecimal cargoExtra = pedido.getServicio().getCargoRevisionAdicional();
        ticket.setCostoAdicionalGenerado(cargoExtra);
        
        // Crear orden PayPal para el cargo extra
        String paymentUrl = pagoService.crearOrdenPayPal(idPedido, cargoExtra);
        ticket.setUrlPagoAdicional(paymentUrl);
        
        // Programar timeout de 48h
        schedulerService.programarTimeoutRevision(ticket.getIdTicket(), Duration.ofHours(48));
    }
    
    return mapToResponse(ticketRevisionRepository.save(ticket));
}
```

---

## Tablas de Base de Datos

| Tabla | Entidad JPA | Paquete | Repositorio | Estado Entidad | Estado Repo |
|---|---|---|---|---|---|
| `contratos` | `Contrato` | `entity.legal` | `ContratoRepository` | ✅ | ✅ |
| `entregables_finales` | `EntregableFinal` | `entity.legal` | — | ✅ | ❌ Crear |
| `pagos_garantia` | `PagoGarantia` | `entity.legal` | `PagoGarantiaRepository` | ✅ | ✅ |
| `transacciones_pago` | `TransaccionPago` | `entity.legal` | — | ✅ | ❌ Crear |
| `plantillas_contrato` | `PlantillaContrato` | `entity.pedido` | — | ✅ | ❌ Crear |

---

## Archivos a Crear

```
controller/
├── ContratoController.java
├── PagoController.java
├── PayPalWebhookController.java
└── EntregableController.java

service/
├── ContratoService.java
├── PagoService.java
├── PdfGenerationService.java
├── EntregableService.java
└── impl/
    ├── ContratoServiceImpl.java
    ├── PagoServiceImpl.java
    ├── PdfGenerationServiceImpl.java
    └── EntregableServiceImpl.java

config/
└── PayPalConfig.java

dto/request/
├── FirmarContratoRequest.java
├── AprobarEntregaRequest.java
└── CrearOrdenPagoRequest.java

dto/response/
├── ContratoResponse.java
├── PagoResponse.java
├── EntregableResponse.java
└── EstadoFirmaResponse.java

repository/
├── EntregableFinalRepository.java
├── TransaccionPagoRepository.java
└── PlantillaContratoRepository.java
```

---

## Dependencias a Agregar al pom.xml

```xml
<!-- PDF Generation (RF-18, RNF-06) -->
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-pdfbox</artifactId>
    <version>1.1.22</version>
</dependency>

<!-- PayPal SDK (RF-20, RNF-14) -->
<dependency>
    <groupId>com.paypal.sdk</groupId>
    <artifactId>checkout-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

---

## Checklist de Validación del Módulo

- [ ] Contrato HTML generado automáticamente desde plantilla con variables sustituidas
- [ ] Contrato contiene nombres, precio, fecha, límite de revisiones
- [ ] Firma electrónica funcional (click explícito por cada parte)
- [ ] Pedido no avanza sin ambas firmas
- [ ] PDF descargable con hashes de firma en pie del documento
- [ ] PDF generado en ≤ 5 segundos (RNF-06)
- [ ] Orden PayPal creada correctamente (sandbox)
- [ ] Webhook PayPal verificado con firma
- [ ] Estado de fondos actualizado en ≤ 10 segundos post-pago
- [ ] Entregable con marca de agua visible antes de aprobar
- [ ] Descarga de versión limpia solo después de aprobación
- [ ] Comisión de plataforma registrada automáticamente
- [ ] Cargo por revisión adicional genera enlace PayPal
- [ ] Timeout de 48h rechaza ticket no pagado
- [ ] Credenciales PayPal solo en variables de entorno (RNF-14)

---

## Dependencias con Otros Módulos

| Módulo | Relación |
|---|---|
| **M1** | Auth + roles |
| **M4** | Contratos asociados a Pedidos; firma desbloquea avance de etapas |
| **M6** | Firma de contrato crea sala de chat; aprobación cierra la sala |
| **M6** | Notificaciones de pago y liberación |

---

## Prerrequisitos del Sistema

- [x] M1, M3, M4 funcionales
- [ ] Cuenta PayPal Sandbox configurada
- [ ] Webhook PayPal registrado y URL accesible (ngrok para desarrollo)
- [ ] Dependencias PDF y PayPal en pom.xml
- [ ] Servicio S3/R2 para entregables
