# Historias de Usuario — Módulo Social, Comunidad y Sorteos

---

## HU-23 — Organizar un sorteo entre seguidores
**Trazabilidad:** REQ-F-023

**As a** Creador,
**I want** organizar un sorteo entre mis seguidores con premio, fecha de cierre y número de ganadores,
**so that** pueda fidelizar a mi comunidad y aumentar mi base de seguidores.

```gherkin
Escenario: Selección automática de ganadores al cierre
  Given que un sorteo requiere ser seguidor del Creador para participar
  When llega la fecha de cierre configurada
  Then el sistema selecciona aleatoriamente el número de ganadores definido entre los participantes que cumplen el requisito de seguidor
```
