# Historias de Usuario — Módulo Comunicación y Notificaciones

---

## HU-14 — Chat en tiempo real por pedido
**Trazabilidad:** REQ-F-014

**As a** Cliente o Creador con un contrato firmado,
**I want** comunicarme en tiempo real dentro de una sala de chat exclusiva del pedido,
**so that** podamos coordinar los detalles del trabajo sin salir de la plataforma.

```gherkin
Escenario: Apertura automática de la sala
  Given que ambas partes firmaron el contrato del pedido
  When se completa la firma
  Then el sistema crea automáticamente la sala de chat asociada al pedido

Escenario: Cierre de la sala al finalizar el pedido
  Given que el pedido cambia a estado "Entregado" o "Cancelado"
  When ocurre esa transición
  Then la sala de chat se cierra para nuevos mensajes
```

---

## HU-15 — Moderación automática de contacto externo
**Trazabilidad:** REQ-F-015

**As a** administrador de la plataforma,
**I want** que el sistema detecte automáticamente teléfonos o correos en los mensajes del chat,
**so that** se prevenga que Creador y Cliente acuerden pagos fuera de la plataforma.

```gherkin
Escenario: Mensaje con número de teléfono
  Given que un usuario escribe un mensaje que contiene un número de teléfono
  When intenta enviarlo
  Then el sistema bloquea la entrega del mensaje y muestra un aviso

Escenario: Suspensión por reincidencia
  Given que un usuario incurrió en 3 infracciones de este tipo en los últimos 30 días
  When intenta cometer una cuarta infracción
  Then el sistema suspende su cuenta por 15 días
```

---

## HU-16 — Formulario de briefing
**Trazabilidad:** REQ-F-016

**As a** Creador,
**I want** configurar un formulario de briefing con hasta 10 preguntas específicas de mi servicio,
**so that** reciba toda la información necesaria del Cliente antes de empezar a trabajar.

```gherkin
Escenario: Respuestas no editables tras el envío
  Given que el Cliente completó y envió el formulario de briefing
  When intenta modificar una respuesta ya enviada
  Then el sistema no permite la edición
```
