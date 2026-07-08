# Módulo 6: Comunicación y Notificaciones

## Descripción General

Este módulo implementa la **comunicación en tiempo real** entre Creador y Cliente a través de WebSocket, el **sistema de detección de datos de contacto** para evitar transacciones fuera de la plataforma, el **formulario de briefing interactivo**, y el **centro de notificaciones** del sistema. Es transversal a toda la plataforma.

---

## Requisitos Funcionales Cubiertos

### RF-14 — Mensajería WebSocket en Tiempo Real

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | Mensajería interna en tiempo real mediante WebSocket. Al firmarse el contrato de un pedido, se crea automáticamente una sala de chat vinculada. La sala se cierra cuando el pedido alcanza estado Entregado o Cancelado. |
| **Criterio de Aceptación** | Al firmar contrato → chat habilitado sin recargar página. Al cerrar pedido → campo de texto deshabilitado con "Esta sala ha sido cerrada". Latencia ≤ 500 ms en red local (RNF-05). |

**Estado actual:** ❌ No implementado (entidades `SalaChat`, `Mensaje`, `DocumentoAdjunto` existen)

**Implementación requerida:**

1. **Dependencia WebSocket en pom.xml:**
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-websocket</artifactId>
   </dependency>
   ```

2. **Configuración WebSocket + STOMP:**
   ```java
   // config/WebSocketConfig.java
   @Configuration
   @EnableWebSocketMessageBroker
   public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
       
       @Override
       public void configureMessageBroker(MessageBrokerRegistry config) {
           config.enableSimpleBroker("/topic", "/queue");
           config.setApplicationDestinationPrefixes("/app");
           config.setUserDestinationPrefix("/user");
       }
       
       @Override
       public void registerStompEndpoints(StompEndpointRegistry registry) {
           registry.addEndpoint("/ws/chat")
               .setAllowedOrigins("http://localhost:4200")
               .withSockJS();
       }
   }
   ```

3. **Interceptor de autenticación WebSocket:**
   ```java
   // config/WebSocketAuthInterceptor.java
   public class WebSocketAuthInterceptor implements ChannelInterceptor {
       @Override
       public Message<?> preSend(Message<?> message, MessageChannel channel) {
           StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
           if (StompCommand.CONNECT.equals(accessor.getCommand())) {
               String token = accessor.getFirstNativeHeader("Authorization");
               // Validar JWT y establecer principal
           }
           return message;
       }
   }
   ```

4. **Endpoints REST (complemento):**
   ```
   GET  /api/pedidos/{id}/chat/mensajes       — Historial de mensajes (paginado)
   POST /api/pedidos/{id}/chat/mensajes       — Enviar mensaje (REST fallback)
   GET  /api/pedidos/{id}/chat/estado         — Estado de la sala (activa/cerrada)
   ```

5. **WebSocket Topics:**
   ```
   /topic/sala.{salaId}                       — Mensajes de la sala
   /app/chat.enviar                           — Enviar mensaje via STOMP
   /user/queue/errors                         — Errores personales (infracciones)
   ```

6. **Creación automática de sala (en M5 al firmar contrato):**
   ```java
   public SalaChat crearSala(Pedido pedido) {
       SalaChat sala = SalaChat.builder()
           .pedido(pedido)
           .salaActiva(true)
           .build();
       return salaChatRepository.save(sala);
   }
   ```

7. **Cierre de sala (en M5 al aprobar/cancelar):**
   ```java
   public void cerrarSala(Long idPedido) {
       salaChatRepository.findByPedidoIdPedido(idPedido).ifPresent(sala -> {
           sala.setSalaActiva(false);
           salaChatRepository.save(sala);
           // Notificar via WebSocket que la sala está cerrada
           messagingTemplate.convertAndSend("/topic/sala." + sala.getIdSala(),
               new MensajeSistema("Esta sala ha sido cerrada"));
       });
   }
   ```

---

### RF-15 — Detección de Datos de Contacto en Mensajes

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | Analizar cada mensaje antes de entregarlo, detectando teléfonos y correos. Si se detecta un patrón, el mensaje no se entrega y el remitente recibe un aviso. 3 infracciones en 30 días → suspensión temporal de 15 días. |
| **Criterio de Aceptación** | "+593 99 123 4567" → mensaje no entregado + contador actualizado. 3ª infracción → "Tu cuenta está suspendida hasta [fecha]". Admin puede consultar historial y revertir suspensiones. |

**Estado actual:** ❌ No implementado (entidad `InfraccionMensaje` existe)

**Implementación requerida:**

1. **Servicio de filtrado de mensajes:**
   ```java
   @Service
   public class MensajeFilterService {
       
       // Patrones de detección
       private static final Pattern PATRON_TELEFONO = Pattern.compile(
           "(\\+?\\d[\\d\\-\\s]{7,})" + "|" +        // Formato internacional
           "(\\(\\d{2,4}\\)\\s?\\d{3,})" + "|" +      // (02) 123456
           "(\\d{3}[\\-.]\\d{3}[\\-.]\\d{4})"          // 123-456-7890
       );
       
       private static final Pattern PATRON_EMAIL = Pattern.compile(
           "[\\w.+-]+@[\\w-]+\\.[\\w.]+"
       );
       
       public boolean contieneContacto(String mensaje) {
           return PATRON_TELEFONO.matcher(mensaje).find() || 
                  PATRON_EMAIL.matcher(mensaje).find();
       }
   }
   ```

2. **Servicio de infracciones:**
   ```java
   @Service
   public class InfraccionService {
       
       private static final int MAX_INFRACCIONES = 3;
       private static final int PERIODO_DIAS = 30;
       private static final int SUSPENSION_DIAS = 15;
       
       @Transactional
       public void registrarInfraccion(Long idUsuario, String mensaje) {
           InfraccionMensaje infraccion = InfraccionMensaje.builder()
               .usuario(usuarioRepository.getReferenceById(idUsuario))
               .mensajeOriginal(mensaje)
               .patronDetectado(detectarPatron(mensaje))
               .build();
           infraccionRepository.save(infraccion);
           
           // Contar infracciones en los últimos 30 días
           long count = infraccionRepository.countByUsuarioIdUsuarioAndFechaAfter(
               idUsuario, LocalDateTime.now().minusDays(PERIODO_DIAS));
           
           if (count >= MAX_INFRACCIONES) {
               suspenderCuenta(idUsuario);
           }
       }
       
       private void suspenderCuenta(Long idUsuario) {
           Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow(...);
           usuario.setEstadoCuenta(false);
           // Calcular fecha de reactivación
           LocalDateTime hastaFecha = LocalDateTime.now().plusDays(SUSPENSION_DIAS);
           usuarioRepository.save(usuario);
           
           // Notificar
           notificacionService.notificar(usuario, "CUENTA_SUSPENDIDA", 
               "Tu cuenta está suspendida hasta " + hastaFecha.toLocalDate());
       }
   }
   ```

3. **Integración en el flujo de envío de mensajes:**
   ```java
   public MensajeResponse enviarMensaje(Long idSala, Long idRemitente, String cuerpo) {
       SalaChat sala = salaChatRepository.findById(idSala).orElseThrow(...);
       
       if (!sala.getSalaActiva()) {
           throw new BusinessRuleException("Esta sala ha sido cerrada");
       }
       
       // RF-15: Filtrar datos de contacto
       if (mensajeFilterService.contieneContacto(cuerpo)) {
           infraccionService.registrarInfraccion(idRemitente, cuerpo);
           throw new BusinessRuleException(
               "Tu mensaje no fue entregado porque contiene datos de contacto. " +
               "Infracción registrada.");
       }
       
       Mensaje mensaje = Mensaje.builder()
           .sala(sala)
           .remitente(usuarioRepository.getReferenceById(idRemitente))
           .cuerpoMensaje(cuerpo)
           .leido(false)
           .build();
       mensajeRepository.save(mensaje);
       
       // Enviar via WebSocket
       messagingTemplate.convertAndSend("/topic/sala." + idSala, mapToResponse(mensaje));
       
       return mapToResponse(mensaje);
   }
   ```

4. **Endpoints admin:**
   ```
   GET    /api/admin/infracciones                  — Listar todas las infracciones
   GET    /api/admin/infracciones/usuario/{id}     — Historial de un usuario
   DELETE /api/admin/suspensiones/{userId}          — Revertir suspensión
   ```

---

### RF-16 — Formulario de Briefing Interactivo

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | El Creador envía desde el chat un formulario de briefing configurable con hasta 10 preguntas de texto libre. Se renderiza como elemento interactivo en el chat del Cliente; las respuestas no pueden editarse tras el envío. |
| **Criterio de Aceptación** | Creador puede agregar/editar/eliminar preguntas sin afectar formularios ya enviados. Cliente completa y envía → respuestas visibles para ambas partes. Intentar editar respuesta → "Las respuestas del briefing no pueden modificarse una vez enviadas". |

**Implementación requerida:**

1. **Nuevas tablas (migración V4):**
   ```sql
   CREATE TABLE briefing_plantillas (
       id_briefing_plantilla SERIAL PRIMARY KEY,
       id_perfil INT NOT NULL REFERENCES perfiles_creadores(id_perfil),
       nombre_plantilla VARCHAR(150) NOT NULL,
       fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   
   CREATE TABLE briefing_preguntas (
       id_pregunta SERIAL PRIMARY KEY,
       id_briefing_plantilla INT NOT NULL REFERENCES briefing_plantillas(id_briefing_plantilla) ON DELETE CASCADE,
       texto_pregunta TEXT NOT NULL,
       numero_orden INT NOT NULL
   );
   
   CREATE TABLE briefing_enviados (
       id_briefing_enviado SERIAL PRIMARY KEY,
       id_pedido INT NOT NULL REFERENCES pedidos(id_pedido),
       id_briefing_plantilla INT NOT NULL REFERENCES briefing_plantillas(id_briefing_plantilla),
       fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       completado BOOLEAN DEFAULT FALSE
   );
   
   CREATE TABLE briefing_respuestas (
       id_respuesta SERIAL PRIMARY KEY,
       id_briefing_enviado INT NOT NULL REFERENCES briefing_enviados(id_briefing_enviado) ON DELETE CASCADE,
       id_pregunta INT NOT NULL REFERENCES briefing_preguntas(id_pregunta),
       texto_respuesta TEXT NOT NULL,
       fecha_respuesta TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   ```

2. **Endpoints:**
   ```
   # Gestión de plantillas de briefing (CREADOR)
   POST   /api/briefing/plantillas                     — Crear plantilla
   GET    /api/briefing/plantillas                     — Mis plantillas
   PUT    /api/briefing/plantillas/{id}                — Editar plantilla
   DELETE /api/briefing/plantillas/{id}                — Eliminar plantilla
   
   # Envío y respuesta
   POST   /api/pedidos/{id}/briefing                   — Enviar briefing al cliente (CREADOR)
   GET    /api/pedidos/{id}/briefing                   — Ver briefing enviado
   POST   /api/pedidos/{id}/briefing/responder         — Responder briefing (CLIENTE, inmutable)
   ```

3. **Inmutabilidad de respuestas:**
   ```java
   public void responderBriefing(Long idPedido, List<RespuestaRequest> respuestas) {
       BriefingEnviado briefing = briefingEnviadoRepository.findByPedidoIdPedido(idPedido)
           .orElseThrow(...);
       
       if (briefing.getCompletado()) {
           throw new BusinessRuleException(
               "Las respuestas del briefing no pueden modificarse una vez enviadas");
       }
       
       for (RespuestaRequest r : respuestas) {
           BriefingRespuesta respuesta = BriefingRespuesta.builder()
               .briefingEnviado(briefing)
               .pregunta(preguntaRepository.getReferenceById(r.getIdPregunta()))
               .textoRespuesta(r.getTextoRespuesta())
               .build();
           briefingRespuestaRepository.save(respuesta);
       }
       
       briefing.setCompletado(true);
       briefingEnviadoRepository.save(briefing);
       
       // Después de completar el briefing → generar contrato automáticamente (RF-17)
       contratoService.generarContrato(idPedido);
   }
   ```

---

## Notificaciones del Sistema (Transversal)

**Endpoints:**
```
GET    /api/notificaciones                          — Listar notificaciones del usuario
PUT    /api/notificaciones/{id}/leer                — Marcar como leída
PUT    /api/notificaciones/leer-todas               — Marcar todas como leídas
GET    /api/notificaciones/no-leidas/count           — Contador de no leídas
```

**Servicio transversal:**
```java
@Service
public class NotificacionService {
    
    public void notificar(Usuario destinatario, String tipoEvento, String mensaje) {
        TipoNotificacion tipo = tipoNotificacionRepository.findByNombreEvento(tipoEvento)
            .orElseGet(() -> {
                TipoNotificacion nuevo = TipoNotificacion.builder()
                    .nombreEvento(tipoEvento)
                    .formatoMensaje(mensaje)
                    .build();
                return tipoNotificacionRepository.save(nuevo);
            });
        
        NotificacionSistema notificacion = NotificacionSistema.builder()
            .usuario(destinatario)
            .tipoNotificacion(tipo)
            .estaLeida(false)
            .build();
        notificacionSistemaRepository.save(notificacion);
        
        // Enviar en tiempo real via WebSocket
        messagingTemplate.convertAndSendToUser(
            destinatario.getCorreo(), "/queue/notificaciones", 
            mapToResponse(notificacion));
    }
}
```

---

## Tablas de Base de Datos

| Tabla | Entidad JPA | Paquete | Repositorio | Estado Entidad | Estado Repo |
|---|---|---|---|---|---|
| `salas_chat` | `SalaChat` | `entity.legal` | `SalaChatRepository` | ✅ | ✅ |
| `mensajes` | `Mensaje` | `entity.legal` | `MensajeRepository` | ✅ | ✅ |
| `documentos_adjuntos` | `DocumentoAdjunto` | `entity.legal` | — | ✅ | ❌ Crear |
| `tipos_notificacion` | `TipoNotificacion` | `entity.comunicacion` | — | ✅ | ❌ Crear |
| `notificaciones_sistema` | `NotificacionSistema` | `entity.comunicacion` | — | ✅ | ❌ Crear |
| `infracciones_mensaje` | `InfraccionMensaje` | `entity.comunicacion` | `InfraccionRepository` | ✅ | ✅ |
| `briefing_plantillas` | — | — | — | ❌ Crear | ❌ Crear |
| `briefing_preguntas` | — | — | — | ❌ Crear | ❌ Crear |
| `briefing_enviados` | — | — | — | ❌ Crear | ❌ Crear |
| `briefing_respuestas` | — | — | — | ❌ Crear | ❌ Crear |

---

## Archivos a Crear

```
config/
├── WebSocketConfig.java
└── WebSocketAuthInterceptor.java

controller/
├── ChatController.java
├── BriefingController.java
└── NotificacionController.java

service/
├── ChatService.java
├── MensajeFilterService.java
├── InfraccionService.java
├── BriefingService.java
├── NotificacionService.java
└── impl/
    ├── ChatServiceImpl.java
    ├── MensajeFilterServiceImpl.java
    ├── InfraccionServiceImpl.java
    ├── BriefingServiceImpl.java
    └── NotificacionServiceImpl.java

entity/comunicacion/  (o entity/briefing/)
├── BriefingPlantilla.java
├── BriefingPregunta.java
├── BriefingEnviado.java
└── BriefingRespuesta.java

dto/request/
├── EnviarMensajeRequest.java
├── CrearBriefingPlantillaRequest.java
├── EnviarBriefingRequest.java
└── ResponderBriefingRequest.java

dto/response/
├── MensajeResponse.java
├── SalaChatResponse.java
├── BriefingResponse.java
└── NotificacionResponse.java

repository/
├── DocumentoAdjuntoRepository.java
├── TipoNotificacionRepository.java
├── NotificacionSistemaRepository.java
├── BriefingPlantillaRepository.java
├── BriefingPreguntaRepository.java
├── BriefingEnviadoRepository.java
└── BriefingRespuestaRepository.java
```

---

## Pruebas Específicas (RNF-05)

**Script de prueba de carga WebSocket:**
```javascript
// tests/websocket-load-test.js
const WebSocket = require('ws');
const connections = [];
const RTTs = [];

for (let i = 0; i < 10; i++) {
    const ws = new WebSocket('ws://localhost:8080/ws/chat');
    ws.on('open', () => {
        const start = Date.now();
        ws.send(JSON.stringify({ type: 'ping', timestamp: start }));
        ws.on('message', (data) => {
            const rtt = Date.now() - start;
            RTTs.push(rtt);
            console.log(`Conexión ${i}: RTT = ${rtt}ms`);
            if (RTTs.length === 10) {
                const avg = RTTs.reduce((a, b) => a + b) / RTTs.length;
                console.log(`\nPromedio RTT: ${avg}ms (máximo permitido: 500ms)`);
                console.log(avg <= 500 ? '✅ PASÓ' : '❌ FALLÓ');
                process.exit(avg <= 500 ? 0 : 1);
            }
        });
    });
    connections.push(ws);
}
```

---

## Checklist de Validación del Módulo

- [ ] WebSocket conecta correctamente con autenticación JWT
- [ ] Sala se crea automáticamente al firmar contrato
- [ ] Mensajes enviados aparecen en ≤ 500ms en red local
- [ ] Sala se cierra al entregar/cancelar pedido con mensaje de sistema
- [ ] Campo de texto deshabilitado en sala cerrada
- [ ] Detección de teléfonos bloquea el mensaje
- [ ] Detección de correos bloquea el mensaje
- [ ] Infracción registrada al bloquear mensaje
- [ ] 3 infracciones en 30 días → suspensión 15 días
- [ ] Admin puede consultar historial de infracciones
- [ ] Admin puede revertir suspensiones
- [ ] Briefing con hasta 10 preguntas creado por Creador
- [ ] Briefing renderizado como elemento interactivo en chat
- [ ] Respuestas inmutables post-envío
- [ ] Completar briefing genera contrato automáticamente (RF-17)
- [ ] 10 conexiones WebSocket simultáneas sin degradación (RNF-05)
- [ ] Notificaciones en tiempo real via WebSocket
- [ ] Centro de notificaciones funcional (listar, marcar leídas, contador)

---

## Dependencias con Otros Módulos

| Módulo | Relación |
|---|---|
| **M1** | Auth JWT para WebSocket y REST |
| **M4** | Salas de chat vinculadas a pedidos |
| **M5** | Firma de contrato crea sala; aprobación/cancelación cierra sala |
| **M5** | Briefing completado → genera contrato (RF-17) |
| **Todos** | NotificacionService es usado por todos los módulos |

---

## Prerrequisitos del Sistema

- [x] M1 funcional (auth JWT)
- [x] M4 funcional (pedidos)
- [ ] Dependencia `spring-boot-starter-websocket` en pom.xml
- [ ] Migración V4 con tablas de briefing ejecutada
- [ ] Entidades JPA de briefing creadas
- [ ] Frontend Angular con cliente STOMP configurado
