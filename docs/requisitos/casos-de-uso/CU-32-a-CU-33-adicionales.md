# Casos de Uso — Requisitos adicionales (v1.3.0)

Estos dos casos de uso corresponden a REQ-F-032 y REQ-F-033, incorporados en v1.3.0 tras una segunda
auditoría del SRS contra código ya implementado. Ver `docs/requisitos/SRS.md` §3.1 y
`CHANGELOG-REQ.md` v1.3.0.

---

## CU-32: Gestionar las obras de mi portafolio
**Trazabilidad:** REQ-F-032 / HU-32
**Prueba de integración:** `PortafolioItemControladorTest`, `PortafolioItemControladorRutasTest`, `PortafolioItemServicioImplTest`

**1. Actor principal y objetivo:** Creador — publicar, actualizar y retirar obras de su portafolio.

**Nivel:** Meta de usuario

**Precondición:** El Creador tiene un portafolio propio.

**Garantía de éxito:** La obra queda visible según la visibilidad del portafolio (público/privado) y su archivo se sirve siempre como descarga forzada.

**2. Escenario principal de éxito:**
1. El Creador sube una obra a su portafolio (por debajo del límite de 50).
2. El Creador lista, actualiza o elimina obras de su propio portafolio.
3. Un visitante consulta las obras de un portafolio público; el archivo se descarga con `Content-Disposition: attachment`.

**3. Extensiones:**
- 1a. El portafolio ya tiene 50 obras.
- 2a. Quien intenta modificar o eliminar la obra no es el dueño del portafolio.
- 3a. El portafolio consultado no es público y el solicitante no es su dueño.

**4. Manejo de extensiones:**
- 1a1. El sistema rechaza la subida con un error de regla de negocio. Termina.
- 2a1. El sistema rechaza la operación. Termina.
- 3a1. El sistema deniega el acceso a las obras del portafolio. Termina.

---

## CU-33: Administrar infracciones y revertir una suspensión
**Trazabilidad:** REQ-F-033 / HU-33
**Prueba de integración:** ninguna todavía — ver `docs/trazabilidad/excepciones-estado.txt`

**1. Actor principal y objetivo:** Administrador (o titular de `INFRACCION_GESTIONAR`) — revisar infracciones y corregir una suspensión automática incorrecta.

**Nivel:** Meta de usuario

**Precondición:** Existen infracciones registradas por REQ-F-015; opcionalmente, una cuenta suspendida automáticamente.

**Garantía de éxito:** El Administrador obtiene visibilidad completa de las infracciones y puede revertir una suspensión sin dejar la cuenta en un estado inconsistente.

**2. Escenario principal de éxito:**
1. El Administrador lista todas las infracciones registradas en el sistema.
2. El Administrador consulta el historial de infracciones de un usuario específico.
3. El Administrador revierte la suspensión de una cuenta suspendida automáticamente por error.

**3. Extensiones:**
- 3a. La cuenta objetivo no está actualmente suspendida.

**4. Manejo de extensiones:**
- 3a1. El sistema revierte la operación sin producir un estado inconsistente. Termina.
