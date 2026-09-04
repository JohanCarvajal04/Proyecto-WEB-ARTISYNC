# Historias de Usuario — Módulo Social, Comunidad y Sorteos

---

## HU-23 — Organizar un sorteo entre seguidores
**Trazabilidad:** REQ-F-023
**Prueba de aceptación:** `SorteoServiceImplTest`

**As a** Creador,
**I want** organizar un sorteo entre mis seguidores con premio, fecha de cierre y número de ganadores,
**so that** pueda fidelizar a mi comunidad y aumentar mi base de seguidores.

**INVEST:** Independiente porque solo requiere que existan seguidores (HU-09), sin depender de pedidos ni pagos; negociable en el número de ganadores y el criterio de elegibilidad; valiosa porque incentiva el crecimiento orgánico de la base de seguidores del Creador; estimable y pequeña porque se acota a configurar el sorteo y ejecutar la selección aleatoria al cierre; testable mediante el escenario de selección automática de ganadores.

```gherkin
Escenario: Selección automática de ganadores al cierre
  Given que un sorteo requiere ser seguidor del Creador para participar
  When llega la fecha de cierre configurada
  Then el sistema selecciona aleatoriamente el número de ganadores definido entre los participantes que cumplen el requisito de seguidor
```
