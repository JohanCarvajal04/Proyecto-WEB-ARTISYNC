# Reporte de Verificación contra el Sandbox Real de PayPal — REQ-F-022b, REQ-F-022c, REQ-NF-019

- Fecha: _pendiente de ejecución_
- Commit base: `5251132` (rama `main`) — **actualizar al commit real auditado el día de la corrida**
- Entorno: sandbox de `developer.paypal.com` (`api-m.sandbox.paypal.com`), backend local expuesto con `ngrok`
- Método de verificación declarado en el SRS (`docs/requisitos/SRS.md:372,381,596`): Test unitario, con `PayPalClient` reemplazado por un mock de Mockito (`PagoServicioImplWebhookTest`, `PagoServicioImplCancelacionTest`, `PagoTicketRevisionServicioImplTest`, `ReconciliacionPayPalSchedulerTest`, `ReconciliacionPayPalEjecutorServicioTest`, `TicketRevisionExpiracionSchedulerTest`). Ese test ya está en verde; **este reporte cubre la capa que falta**: la misma lógica ejecutada contra el contrato HTTP real de PayPal, tal como exige el "Cierre" de las tres excepciones en `docs/trazabilidad/excepciones-estado.txt`.
- Script reproducible: [`scripts/verificar-paypal-sandbox.sh`](../../../scripts/verificar-paypal-sandbox.sh)

## Fase 0 — completada

Túnel `ngrok` y webhook de PayPal verificados de extremo a extremo: `POST` real contra `https://<subdominio>.ngrok-free.dev/api/webhooks/paypal` responde `200 OK` desde `PayPalWebhookControlador` real (no mockeado). Webhook registrado en la app de sandbox con `PAYPAL_WEBHOOK_ID` guardado en `.env`.

Dos hallazgos operativos que costó diagnosticar, para que quien repita esto no pierda el mismo tiempo:

1. **`ngrok` debe apuntar al backend (`8080`), no al frontend (`4200`).** El `frontend` de `docker-compose.yml` corre con live-reload (servidor de desarrollo Angular/Vite, ver `docker-compose.yml:133-149`), que por diseño devuelve `403 Blocked request` a cualquier `Host` que no reconozca (protección anti DNS-rebinding) — el dominio público de `ngrok` nunca va a estar en esa lista. `docker-compose.yml` tampoco publica el `8080` del backend al host **a propósito** (`docker-compose.yml:108-112`, OBS-AUTO-06/A07 OWASP: mantener confiable `X-Forwarded-For` para el rate-limiter de login). Se creó [`artisync/docker-compose.dev.yml`](../../../artisync/docker-compose.dev.yml) — el override que el propio comentario del archivo principal ya preveía para este caso — que solo publica ese puerto cuando se pasa explícitamente con `-f`, sin tocar la postura de seguridad del compose principal. `make up` (el default) sigue sin exponerlo; para este caso existe `make up-backend-publico` (Makefile), que aplica el override sobre un stack ya levantado.
2. Si `ngrok` se configura (authtoken) desde una herramienta que corre en un entorno distinto al de la terminal interactiva real, cada una puede terminar con su propio archivo de configuración y no reconocer el authtoken de la otra. Configúralo directamente en la terminal donde vas a dejar corriendo el túnel.

## Estado general: **PENDIENTE DE EJECUCIÓN**

Este archivo es el esqueleto de evidencia, creado junto con el script de apoyo. Todavía no se ha ejecutado ninguna corrida real: hace falta una app de PayPal Developer, una cuenta de prueba sandbox y un túnel `ngrok`, los tres bajo el control del equipo (`scripts/verificar-paypal-sandbox.sh fase0`). Siguiendo el mismo criterio que ya se aplicó en REQ-NF-009 y REQ-NF-017: **ninguno de los tres requisitos se marca 'verificado' en `excepciones-estado.txt` hasta que las secciones de abajo tengan un resultado real**, sea éxito o hallazgo.

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

- **Resultado:** pendiente
- Pedido: `idPedido=` — Orden PayPal: `orderId=`
- Ventana en que `ngrok` estuvo caído (para simular el webhook retrasado): `desde` — `hasta`
- Overrides de entorno usados: `PAYPAL_RECONCILIACION_UMBRAL_MINUTOS=0`, `PAYPAL_RECONCILIACION_INTERVALO_MS=60000`
- `pagos_garantia.estado_fondos` tras la reconciliación (antes de recibir cualquier webhook):
- Captura reembolsada (id real de PayPal):
- `pagos_garantia.estado_fondos` tras el reembolso:
- Evidencia adjunta: log de `ReconciliacionPayPalEjecutorServicio` confirmando el pago sin webhook previo, captura del dashboard sandbox mostrando el reembolso en la cuenta Business

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
