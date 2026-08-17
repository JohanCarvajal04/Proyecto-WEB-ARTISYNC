# Casos de Uso — Módulo Social, Comunidad y Sorteos

---

## CU-23: Organizar y cerrar un sorteo
**Trazabilidad:** REQ-F-023 / HU-23

**1. Actor principal y objetivo:** Creador — organizar un sorteo entre sus seguidores; Sistema — seleccionar los ganadores automáticamente al cierre.

**Nivel:** Meta de usuario

**Precondición:** El Creador tiene al menos un seguidor.

**Garantía de éxito:** Al llegar la fecha de cierre, el sistema selecciona aleatoriamente el número de ganadores configurado entre los participantes elegibles.

**2. Escenario principal de éxito:**
1. El Creador configura el sorteo: título, premio, número de ganadores, fecha de cierre y requisito de ser seguidor.
2. Los usuarios que cumplen el requisito se inscriben como participantes.
3. Al llegar la fecha de cierre, el sistema ejecuta la selección aleatoria entre los participantes elegibles.
4. El sistema publica los ganadores y notifica al Creador y a los ganadores.

**3. Extensiones:**
- 3a. Ningún participante cumple el requisito de seguidor al momento del cierre.

**4. Manejo de extensiones:**
- 3a1. El sistema marca el sorteo como "cerrado sin ganadores" y notifica al Creador. Termina.
