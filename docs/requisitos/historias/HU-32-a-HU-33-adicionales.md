# Historias de Usuario — Requisitos adicionales (v1.3.0)

Estas dos historias corresponden a REQ-F-032 y REQ-F-033, incorporados en v1.3.0 tras una segunda
auditoría del SRS contra código ya implementado. Ver `docs/requisitos/SRS.md` §3.1 y
`CHANGELOG-REQ.md` v1.3.0.

---

## HU-32 — Gestionar las obras de mi portafolio
**Trazabilidad:** REQ-F-032
**Prueba de aceptación:** `PortafolioItemControladorTest`, `PortafolioItemControladorRutasTest`, `PortafolioItemServicioImplTest`

**As a** Creador,
**I want** subir, listar, actualizar y eliminar las obras de mi portafolio, con un máximo de 50 por portafolio,
**so that** pueda mostrar mi trabajo sin que un portafolio sin límite degrade el rendimiento de la vista pública ni facilite abuso de almacenamiento.

**INVEST:** Independiente de la publicación de servicios en el catálogo (REQ-F-011), es una galería propia del perfil; negociable en el límite exacto de 50 obras; valiosa porque el portafolio es la prueba social del trabajo del Creador; estimable y pequeña (subir, listar, actualizar, eliminar); testable mediante el rechazo al superar el límite y el control de visibilidad.

```gherkin
Escenario: Rechazo al superar el límite de obras por portafolio
  Given que un portafolio ya tiene 50 obras
  When el Creador intenta subir una obra número 51
  Then el sistema rechaza la operación con un error de regla de negocio
```

---

## HU-33 — Administrar infracciones y revertir suspensiones
**Trazabilidad:** REQ-F-033
**Prueba de aceptación:** ninguna todavía para `listarInfracciones`/`revertirSuspension` — ver `docs/trazabilidad/excepciones-estado.txt`

**As a** Administrador (o titular de `INFRACCION_GESTIONAR`),
**I want** listar las infracciones registradas, consultar el historial de un usuario específico, y revertir manualmente una suspensión,
**so that** pueda corregir una suspensión automática que resultó ser un falso positivo sin necesitar acceso directo a la base de datos.

**INVEST:** Independiente de la detección automática de infracciones (REQ-F-015); negociable en si la reversión exige una nota administrativa; valiosa porque una suspensión automática sin vía de reversión deja al usuario sin recurso; estimable y pequeña (listar, consultar, revertir); testable mediante el listado de solo lectura y la reversión sobre una cuenta no suspendida.

```gherkin
Escenario: Revertir la suspensión de una cuenta no suspendida no produce un estado inconsistente
  Given que una cuenta de usuario no está actualmente suspendida
  When el Administrador intenta revertir su suspensión
  Then el sistema no produce un estado inconsistente en la cuenta
```
