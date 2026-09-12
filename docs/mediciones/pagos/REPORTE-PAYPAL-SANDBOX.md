# Reporte de Verificación contra el Sandbox Real de PayPal — REQ-F-022b, REQ-F-022c, REQ-NF-019

- Fecha: Escenario 3 ejecutado el 2026-09-12 (Escenarios 1 y 2 siguen pendientes)
- Commit base: `511714d` (rama `main`) + cambio en árbol de trabajo sin commitear todavía: `recipient_type` agregado en `WithdrawalRequestServiceImpl.executePayout` (hallazgo real de esta misma corrida, ver Escenario 3)
- Entorno: sandbox de `developer.paypal.com` (`api-m.sandbox.paypal.com`), backend local expuesto con `ngrok`
- Método de verificación declarado en el SRS (`docs/requisitos/SRS.md:372,381,596`): Test unitario, con `PayPalClient` reemplazado por un mock de Mockito (`PagoServicioImplWebhookTest`, `PagoServicioImplCancelacionTest`, `PagoTicketRevisionServicioImplTest`, `ReconciliacionPayPalSchedulerTest`, `ReconciliacionPayPalEjecutorServicioTest`, `TicketRevisionExpiracionSchedulerTest`). Ese test ya está en verde; **este reporte cubre la capa que falta**: la misma lógica ejecutada contra el contrato HTTP real de PayPal, tal como exige el "Cierre" de las tres excepciones en `docs/trazabilidad/excepciones-estado.txt`.
- Script reproducible: [`scripts/verificar-paypal-sandbox.sh`](../../../scripts/verificar-paypal-sandbox.sh)

## Fase 0 — completada

Túnel `ngrok` y webhook de PayPal verificados de extremo a extremo: `POST` real contra `https://<subdominio>.ngrok-free.dev/api/webhooks/paypal` responde `200 OK` desde `PayPalWebhookControlador` real (no mockeado). Webhook registrado en la app de sandbox con `PAYPAL_WEBHOOK_ID` guardado en `.env`.

Dos hallazgos operativos que costó diagnosticar, para que quien repita esto no pierda el mismo tiempo:

1. **`ngrok` debe apuntar al backend (`8080`), no al frontend (`4200`).** El `frontend` de `docker-compose.yml` corre con live-reload (servidor de desarrollo Angular/Vite, ver `docker-compose.yml:133-149`), que por diseño devuelve `403 Blocked request` a cualquier `Host` que no reconozca (protección anti DNS-rebinding) — el dominio público de `ngrok` nunca va a estar en esa lista. `docker-compose.yml` tampoco publica el `8080` del backend al host **a propósito** (`docker-compose.yml:108-112`, OBS-AUTO-06/A07 OWASP: mantener confiable `X-Forwarded-For` para el rate-limiter de login). Se creó [`artisync/docker-compose.dev.yml`](../../../artisync/docker-compose.dev.yml) — el override que el propio comentario del archivo principal ya preveía para este caso — que solo publica ese puerto cuando se pasa explícitamente con `-f`, sin tocar la postura de seguridad del compose principal. `make up` (el default) sigue sin exponerlo; para este caso existe `make up-backend-publico` (Makefile), que aplica el override sobre un stack ya levantado.
2. Si `ngrok` se configura (authtoken) desde una herramienta que corre en un entorno distinto al de la terminal interactiva real, cada una puede terminar con su propio archivo de configuración y no reconocer el authtoken de la otra. Configúralo directamente en la terminal donde vas a dejar corriendo el túnel.
3. **El túnel de la Fase 0 se quedó corriendo en segundo plano de una sesión a otra** (proceso `ngrok`, PID 15044, iniciado 2026-09-11 y todavía activo el 2026-09-12 durante la corrida del Escenario 3). Mientras `docker-compose.yml` no publica el 8080 al host (a propósito, OBS-AUTO-06/A07 OWASP), ese túnel no tiene a quién entregarle tráfico y es inofensivo. Pero en cuanto algo publica el 8080 (p. ej. `docker-compose.dev.yml` para esta misma prueba), el túnel vuelve a funcionar sin que nadie lo reactive a propósito — ver la nota de honestidad en el Escenario 3: esto fue lo que terminó entregando un webhook real donde se esperaba que no llegara ninguno. Cierre pendiente: matar el proceso `ngrok` huérfano (o dejar documentado que solo debe levantarse dentro de la ventana de la prueba, nunca antes).

## Estado general: **PARCIAL — Escenario 3 con resultado real; Escenarios 1 y 2 siguen pendientes**

Este archivo es el esqueleto de evidencia, creado junto con el script de apoyo. Siguiendo el mismo criterio que ya se aplicó en REQ-NF-009 y REQ-NF-017: **ninguno de los tres requisitos se marca 'verificado' en `excepciones-estado.txt` hasta que las tres secciones de abajo tengan un resultado real**, sea éxito o hallazgo. Con un solo escenario resuelto, REQ-NF-019 se queda en `implementado` — no se sube a `verificado` todavía, ni siquiera parcialmente.

## Escenario 1 — REQ-F-022b: pago real de un ticket de revisión

- **Resultado:** pendiente
- Ticket: `idTicket=` — Orden PayPal: `orderId=`
- Cuenta sandbox Personal usada (solo el identificador, nunca la clave):
- Hora de aprobación (UTC):
- `pagos_ticket_revision.estado_pago` tras la prueba:
- Evidencia adjunta: log del webhook recibido en `/api/webhooks/paypal`, captura del dashboard sandbox (actividad de la cuenta Business)

## Escenario 2 — REQ-F-022c: expiración automática a 48h

- **Resultado:** pendiente
- Ticket: `idTicket=` — Orden PayPal: `orderId=` (nunca aprobada)
- Overrides de entorno usados: `TICKETREVISION_EXPIRACION_HORAS=0`, `TICKETREVISION_EXPIRACION_INTERVALO_MS=60000`
- `tickets_revision.estado_ticket` tras la prueba:
- `pagos_ticket_revision.estado_pago` tras la prueba:
- Estado de la orden en PayPal tras la expiración (¿sigue `CREATED` sin capturar ni anular?):
- **Nota de honestidad:** el código actual (`TicketRevisionExpiracionServicio.java:47-54`) no anula (`void`) la orden en PayPal al expirar el ticket, solo actualiza el estado local. Si la orden queda huérfana en el sandbox, se documenta aquí como hallazgo real, no se omite.

## Escenario 3 — REQ-NF-019: reconciliación con webhook retrasado + reembolso real

- **Resultado:** éxito, con un hallazgo real de infraestructura documentado abajo (no del código de negocio)
- Pedido de prueba dedicado: `idPedido=100011` (cliente `carlos.mendoza@artisync.demo`, creado para esta corrida, no forma parte del elenco narrativo de la semilla de demo) — Orden PayPal: `orderId=21L65576N8449111U`, monto `$1.00`
- Cuenta sandbox Personal usada (solo el identificador): `sb-ii4o752884478@personal.example.com`
- Hora de aprobación/captura (UTC): 2026-09-12T16:15:25Z
- **Nota de honestidad — no salió como estaba planeado, y por qué:** el plan era aprobar el pago con el túnel `ngrok` apagado para forzar el camino de reconciliación. En la práctica, el túnel de la Fase 0 llevaba corriendo en segundo plano desde el día anterior (ver hallazgo en Fase 0 arriba) y, al publicar temporalmente el puerto 8080 del backend para esta prueba, volvió a tener a quién entregarle tráfico: el webhook real de PayPal SÍ llegó, capturando el pago por esa vía y no por reconciliación. Log real del backend:
  ```
  11:15:26.851 PaymentServiceImpl: Pago 80010 confirmado y capturado. Fondos retenidos: $1.00
  11:15:39.386 PaymentServiceImpl: Webhook PayPal duplicado para la orden 21L65576N8449111U: el pago ya está en Retenido
  ```
  Esto sí es evidencia real y valiosa — confirma contra el sandbox real (no un mock) la idempotencia por reintento de webhook que exige la Aceptación de REQ-NF-019 (el segundo evento, 13s después, no generó una segunda actualización ni una segunda transacción) — pero **no** es evidencia del camino de reconciliación sin webhook, que es lo que este escenario pedía específicamente.
- **El camino de reconciliación sin webhook sí quedó demostrado, de forma limpia y no forzada, minutos antes en esta misma sesión, sobre otro pedido:** `idPedido=100010`, orden PayPal `9TC11082947767740` ($25.00). Ahí el puerto 8080 todavía NO estaba publicado (webhook físicamente inalcanzable, sin intervención manual de ningún tipo), y aun así `pagos_garantia.estado_fondos` pasó de `Pendiente` a `Retenido` a las 08:40:28 solo por `PayPalReconciliationScheduler` — confirmado contra la API real de PayPal (la orden ya estaba `APPROVED`, y quedó `COMPLETED` tras la captura) y contra `pagos_garantia.fecha_actualizacion` en la base de datos.
- `pagos_garantia.estado_fondos` (pedido 100011) tras el reembolso: `Reembolsado`
- Captura reembolsada (id real de PayPal): `9LV99180BL467994C`, confirmada `REFUNDED` consultando directamente `GET /v2/payments/captures/9LV99180BL467994C`
- **Hallazgo real adicional (fuera del alcance original de este escenario, encontrado en la misma sesión):** al reintentar un retiro de creador ($22.50, solicitud `idSolicitud=1`) durante esta corrida, PayPal devolvió `VALIDATION_ERROR — items[0].recipient_type: Required field missing`. `WithdrawalRequestServiceImpl.executePayout` nunca incluía `recipient_type` en el payload de Payouts; el mock de PayPal en el test unitario no lo detectaba porque no valida el payload real contra el esquema de PayPal. Corregido (`item.put("recipient_type", "EMAIL")`) y reverificado contra el sandbox real: el mismo retiro pasó de `Fallido` a `Aprobado`, batch PayPal `PKE9FTRNPPZ48` con `recipient_type` reflejado correctamente en la respuesta. Cambio todavía sin commitear — ver "Commit base" arriba.
- Evidencia adjunta: logs de backend citados arriba; respuestas crudas de `GET /v2/checkout/orders/{id}`, `GET /v2/payments/captures/{id}` y `GET /v1/payments/payouts/{batchId}` contra `api-m.sandbox.paypal.com`, capturadas en la conversación que originó este reporte

## Cómo reproducir

```bash
set -a; source artisync/.env; set +a
bash scripts/verificar-paypal-sandbox.sh fase0   # una sola vez
bash scripts/verificar-paypal-sandbox.sh fase1
bash scripts/verificar-paypal-sandbox.sh fase2
bash scripts/verificar-paypal-sandbox.sh fase3
```

## Cierre de las excepciones

Una vez que los tres escenarios tengan un resultado real (ver plan de ejecución completo en la conversación que originó este reporte):

- Si los tres son éxito, mover las tres líneas correspondientes de `docs/trazabilidad/excepciones-estado.txt` a comentarios `# REQ-... ya no está aquí: ... Sube a 'verificado'`, citando este reporte — mismo formato que ya usan ahí REQ-NF-005/006/020/024/010/033/018/022.
- Si alguno revela un hallazgo real (p. ej. la orden huérfana del Escenario 2), esa línea se mantiene activa pero se reescribe para describir el hallazgo concreto, igual que se hizo con REQ-NF-009 y REQ-NF-017 — nunca se fuerza un "verificado" que la evidencia no sostiene.
