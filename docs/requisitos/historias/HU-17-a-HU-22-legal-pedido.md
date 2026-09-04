# Historias de Usuario — Módulo Legal, Pedido y Finanzas

---

## HU-17 — Generación automática de contrato
**Trazabilidad:** REQ-F-017
**Prueba de aceptación:** `ContratoServicioImplTest`

**As a** Cliente que inicia un pedido,
**I want** que se genere automáticamente un contrato con los datos del servicio contratado,
**so that** ambas partes tengamos un documento formal que respalde el acuerdo.

**INVEST:** Independiente porque se dispara al iniciar el pedido, sin depender de HU-18 (la firma es un paso posterior); negociable en la plantilla exacta y en qué campos del servicio se sustituyen; valiosa porque formaliza el acuerdo entre Cliente y Creador; estimable y pequeña porque es una sustitución de plantilla sobre datos ya existentes del pedido; testable mediante el escenario de generación con datos correctos.

```gherkin
Escenario: Generación con datos correctos
  Given que inicio un pedido de un servicio de 30 USD con 2 revisiones incluidas
  When el sistema genera el contrato
  Then el documento HTML muestra las partes, el servicio, el precio y el número de revisiones correctamente sustituidos desde la plantilla
```

---

## HU-18 — Firma electrónica del contrato
**Trazabilidad:** REQ-F-018
**Prueba de aceptación:** `ContratoServicioImplTest`

**As a** Cliente o Creador,
**I want** firmar electrónicamente el contrato de mi pedido,
**so that** el acuerdo quede formalizado antes de que el trabajo comience.

**INVEST:** Independiente porque opera sobre un contrato ya generado por HU-17, sin regenerarlo; negociable en el mecanismo de firma (hash) y en el formato de descarga; valiosa porque bloquea el inicio del trabajo hasta que el compromiso es mutuo; estimable y pequeña porque son dos operaciones (firmar, descargar); testable con los escenarios de bloqueo por firma faltante y descarga con hashes de ambas firmas.

```gherkin
Escenario: El pedido no avanza sin ambas firmas
  Given que solo el Cliente ha firmado el contrato
  When se consulta el estado del pedido
  Then permanece en espera de firma del Creador y no avanza a la siguiente etapa

Escenario: Descarga del contrato firmado
  Given que ambas partes firmaron el contrato
  When solicito la descarga
  Then recibo un PDF que incluye los hashes de ambas firmas
```

---

## HU-19 — Seguimiento del flujo de trabajo del pedido
**Trazabilidad:** REQ-F-019
**Prueba de aceptación:** `FlujoTrabajoServicioImplTest` · `PedidoServicioImplFlujoTest`

**As a** Cliente,
**I want** ver en tiempo real la etapa actual de mi pedido,
**so that** sepa en qué punto del proceso se encuentra mi encargo sin tener que preguntar al Creador.

**INVEST:** Independiente porque solo requiere que el pedido ya exista (tras HU-18), sin acoplarse a pago o entrega; negociable en las etapas exactas configuradas por categoría; valiosa porque reduce la necesidad de comunicación manual de estado; estimable y pequeña porque es una máquina de estados con registro de transición; testable mediante el escenario de transición de etapa con marca de tiempo.

```gherkin
Escenario: Transición de etapa registrada
  Given que mi pedido está en la etapa "En boceto"
  When el Creador marca la etapa como completada
  Then el pedido avanza a la siguiente etapa configurada para su categoría
  And la transición queda registrada con marca de tiempo
```

---

## HU-20 — Pago del pedido vía PayPal
**Trazabilidad:** REQ-F-020
**Prueba de aceptación:** `PagoServicioImplWebhookTest`

**As a** Cliente,
**I want** pagar mi pedido a través de un enlace de PayPal generado automáticamente,
**so that** el pago quede en garantía (escrow) hasta que reciba el entregable aprobado.

**INVEST:** Independiente porque el pago se procesa vía webhook externo de PayPal, desacoplado del flujo interno de etapas (HU-19); negociable en el proveedor de pago y en el estado intermedio de garantía; valiosa porque protege económicamente a ambas partes durante la ejecución del pedido; estimable y pequeña porque se limita a recibir y procesar un único tipo de webhook; testable mediante el escenario de confirmación de pago mockeando el webhook (`PagoServicioImplWebhookTest`).

```gherkin
Escenario: Confirmación de pago vía webhook
  Given que completé el pago en PayPal
  When PayPal envía el webhook de confirmación
  Then el estado de fondos del pedido cambia a "en garantía"
```

---

## HU-21 — Aprobación de entregable y liberación de fondos
**Trazabilidad:** REQ-F-021
**Prueba de aceptación:** `EntregableServicioImplTest`

**As a** Cliente,
**I want** previsualizar el entregable con marca de agua antes de aprobarlo,
**so that** verifique la calidad del trabajo antes de liberar el pago al Creador.

**INVEST:** Independiente porque solo requiere que existan fondos en garantía (HU-20), sin acoplarse a la revisión adicional (HU-22); negociable en el mecanismo exacto de marca de agua; valiosa porque cierra el ciclo económico del pedido liberando el pago al Creador; estimable y pequeña porque combina previsualización, aprobación y liberación de fondos ya calculada; testable mediante el escenario de aprobación con liberación de fondos, descarga sin marca de agua y registro de comisión.

```gherkin
Escenario: Aprobación libera fondos
  Given que reviso el entregable con marca de agua
  When apruebo el entregable
  Then los fondos en garantía se liberan al Creador
  And puedo descargar la versión sin marca de agua
  And la comisión de la plataforma se registra automáticamente
```

---

## HU-22 — Solicitud de revisión adicional
**Trazabilidad:** REQ-F-022
**Prueba de aceptación:** `TicketRevisionServicioImplTest`

**As a** Cliente,
**I want** solicitar una revisión adicional cuando ya agoté las incluidas en el contrato,
**so that** pueda pedir ajustes pagando el cargo correspondiente sin renegociar todo el contrato.

**INVEST:** Independiente porque se activa solo después de agotar las revisiones incluidas en el contrato de HU-17, sin modificarlo; negociable en el monto del cargo adicional y en el plazo de expiración (hoy 48 h); valiosa porque monetiza trabajo extra sin fricción de renegociación; estimable y pequeña porque reutiliza el mismo mecanismo de cobro que HU-20; testable con los escenarios de nuevo cobro y rechazo automático por falta de pago.

```gherkin
Escenario: Revisión adicional genera nuevo cobro
  Given que ya usé las 2 revisiones incluidas en mi contrato
  When solicito una revisión adicional
  Then el sistema genera un nuevo enlace de pago para esa revisión

Escenario: Rechazo automático por falta de pago
  Given que se generó un ticket de revisión adicional
  When pasan 48 horas sin que se complete el pago
  Then el ticket se rechaza automáticamente
```
