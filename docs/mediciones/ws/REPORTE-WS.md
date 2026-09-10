# Reporte de Carga WebSocket — REQ-NF-005

- Fecha: 2026-09-10 (zona horaria del entorno de prueba: UTC-05:00)
- Commit base: rama `feat/ia-verificacion-asistida`
- Entorno: local, `artisync/docker-compose.yml` (Postgres real vía perfil `postgres-it`), `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- Endpoint medido: STOMP `/ws/websocket` (transporte raw de SockJS), destino `/app/chat.enviar`, broadcast en `/topic/sala.{idSala}`
- Prueba versionada: [`ChatWebSocketLoadIT.java`](../../../artisync/Backend/src/test/java/uteq/edu/ec/artisync/service/comunicacion/impl/ChatWebSocketLoadIT.java)
- Configuración: 10 conexiones STOMP concurrentes (mismo JWT, mismo usuario — escenario de múltiples pestañas), 5 rondas de envío, cada ronda espera a que las 10 conexiones reciban el broadcast (timeout 5s por ronda)

## Comando ejecutado

```
$ docker compose -f artisync/docker-compose.yml up -d --wait postgres
$ ./mvnw test -Dtest=ChatWebSocketLoadIT -Dspring.profiles.active=postgres-it \
    -DDB_NAME=artisyncbd -DDB_USER=postgres -DDB_PASSWORD=changeme
```

## Resultado

| Métrica | Valor |
|---|---|
| Conexiones simultáneas | 10 |
| Rondas | 5 |
| Muestras totales (10×5) | 50 |
| Media | 94.8 ms |
| p50 | 32 ms |
| p95 | 346 ms |
| Máximo | 346 ms |
| Conexiones fallidas | 0 |

**Umbral esperado:** latencia extremo-a-extremo ≤500ms con ≥10 conexiones simultáneas, sin degradación — **Resultado: cumple** (p95 y máximo, 346ms, muy por debajo del umbral).

## Hallazgo colateral: defecto real corregido antes de poder medir

La primera ejecución de esta prueba no llegó a completar ninguna ronda: el servidor
rechazaba **todo** envío real por STOMP con `MessageConversionException` al intentar
deserializar el cuerpo del mensaje dentro de `CustomUserDetails` en vez del DTO
`PeticionEnviarMensaje`. Causa raíz: `ChatControlador.enviarMensajeWs` declara
`@AuthenticationPrincipal CustomUserDetails userDetails` en un método `@MessageMapping`,
pero el proyecto nunca registraba `AuthenticationPrincipalArgumentResolver` para la
mensajería (`WebSocketConfig` solo implementaba `WebSocketMessageBrokerConfigurer`, sin
`addArgumentResolvers`) — Spring, sin ese resolver, trataba el parámetro como el
`@Payload` implícito. Las pruebas unitarias existentes (`ChatControladorTest`) nunca lo
detectaron porque invocan el método del controlador directamente en código Java,
evitando por completo el pipeline real de despacho de mensajes STOMP.

**Esto significa que, antes de esta corrección, el envío de mensajes de chat por
WebSocket estaba roto para cualquier cliente real** (el fallback REST
`POST /api/v1/pedidos/{idPedido}/chat/mensajes` sí funcionaba, al no depender de este
mecanismo). Corregido agregando, en `WebSocketConfig`:

- `addArgumentResolvers(...)` con `AuthenticationPrincipalArgumentResolver`.
- `SecurityContextChannelInterceptor` en `configureClientInboundChannel` (después de
  `webSocketAuthInterceptor`), para que la `Authentication` que este último deja en
  `accessor.setUser()` llegue al `SecurityContextHolder` que el resolver anterior
  consulta.
- Nueva dependencia `org.springframework.security:spring-security-messaging` (no
  estaba en el classpath).

Confirmado sin regresiones: `./mvnw test` completo, 1133/1133 después del cambio.
