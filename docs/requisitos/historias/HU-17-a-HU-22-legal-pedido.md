# Historias de Usuario — Módulo Legal, Pedido y Finanzas

---

## HU-17 — Generación automática de contrato
**Trazabilidad:** REQ-F-017

**As a** Cliente que inicia un pedido,
**I want** que se genere automáticamente un contrato con los datos del servicio contratado,
**so that** ambas partes tengamos un documento formal que respalde el acuerdo.

```gherkin
Escenario: Generación con datos correctos
  Given que inicio un pedido de un servicio de 30 USD con 2 revisiones incluidas
  When el sistema genera el contrato
  Then el documento HTML muestra las partes, el servicio, el precio y el número de revisiones correctamente sustituidos desde la plantilla
```

---

## HU-18 — Firma electrónica del contrato
**Trazabilidad:** REQ-F-018

**As a** Cliente o Creador,
**I want** firmar electrónicamente el contrato de mi pedido,
**so that** el acuerdo quede formalizado antes de que el trabajo comience.

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

**As a** Cliente,
**I want** ver en tiempo real la etapa actual de mi pedido,
**so that** sepa en qué punto del proceso se encuentra mi encargo sin tener que preguntar al Creador.

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

**As a** Cliente,
**I want** pagar mi pedido a través de un enlace de PayPal generado automáticamente,
**so that** el pago quede en garantía (escrow) hasta que reciba el entregable aprobado.

```gherkin
Escenario: Confirmación de pago vía webhook
  Given que completé el pago en PayPal
  When PayPal envía el webhook de confirmación
  Then el estado de fondos del pedido cambia a "en garantía"
```

---

## HU-21 — Aprobación de entregable y liberación de fondos
**Trazabilidad:** REQ-F-021

**As a** Cliente,
**I want** previsualizar el entregable con marca de agua antes de aprobarlo,
**so that** verifique la calidad del trabajo antes de liberar el pago al Creador.

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

**As a** Cliente,
**I want** solicitar una revisión adicional cuando ya agoté las incluidas en el contrato,
**so that** pueda pedir ajustes pagando el cargo correspondiente sin renegociar todo el contrato.

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
