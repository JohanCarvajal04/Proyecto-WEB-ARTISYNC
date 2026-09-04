# Historias de Usuario — Módulo Catálogo Dinámico de Servicios

---

## HU-11 — Publicar un producto o servicio
**Trazabilidad:** REQ-F-011
**Prueba de aceptación:** `ServicioCatalogoServicioImplTest`

**As a** Creador,
**I want** publicar un producto o servicio con precio, imagen y descripción,
**so that** los Clientes puedan descubrirlo y contratarlo desde el catálogo.

**INVEST:** Independiente porque cualquier Creador puede publicar sin depender de que otro lo haga; negociable en los límites exactos (tamaño de imagen, longitud mínima de descripción); valiosa porque es la puerta de entrada al catálogo, sin ítems no hay negocio; estimable y pequeña porque es un alta simple con validaciones de campo; testable con los escenarios de publicación exitosa y de descripción demasiado corta.

```gherkin
Escenario: Publicación exitosa
  Given que ingreso un precio de 15.00 USD, una imagen de 3 MB y una descripción de 150 caracteres
  When publico el ítem
  Then el ítem aparece en el catálogo con estado activo

Escenario: Descripción demasiado corta
  Given que ingreso una descripción de 10 caracteres
  When intento publicar el ítem
  Then el sistema rechaza la publicación por no alcanzar el mínimo de 20 caracteres
```

---

## HU-12 — Atributos personalizados por categoría
**Trazabilidad:** REQ-F-012
**Prueba de aceptación:** `ServicioCatalogoServicioImplTest`

**As a** Creador,
**I want** que el formulario de publicación muestre campos específicos según mi categoría (ej. "técnica" para ilustración, "duración" para música),
**so that** pueda describir mi producto o servicio con la precisión que su categoría requiere.

**INVEST:** Independiente de HU-11 porque extiende el formulario de publicación sin cambiar su flujo base; negociable en qué atributos exactos se definen por categoría; valiosa porque mejora la calidad y comparabilidad de la información publicada; estimable y pequeña porque se acota a un máximo de 10 atributos por categoría; testable mediante el escenario de renderizado dinámico según categoría seleccionada.

```gherkin
Escenario: Atributos dinámicos según categoría
  Given que selecciono la categoría "Ilustración digital"
  When el formulario se renderiza
  Then muestra los campos de atributos definidos para esa categoría (hasta 10)
```

---

## HU-13 — Búsqueda y filtrado en el catálogo
**Trazabilidad:** REQ-F-013
**Prueba de aceptación:** `ServicioCatalogoServicioImplTest`

**As a** Cliente,
**I want** buscar y filtrar servicios por categoría, subcategoría, rango de precio, etiquetas y texto libre,
**so that** encuentre rápidamente al Creador que se ajusta a lo que necesito.

**INVEST:** Independiente porque opera en modo lectura sobre ítems ya publicados por HU-11/HU-12, sin modificarlos; negociable en qué combinaciones de filtro se soportan; valiosa porque es el mecanismo principal de descubrimiento para el Cliente; estimable y pequeña porque se apoya en una única `Specification` de consulta parametrizada; testable con los escenarios de filtro combinado y de edición reflejada de inmediato.

```gherkin
Escenario: Filtro combinado
  Given que existen servicios de categoría "Música" con precios entre 10 y 50 USD
  When filtro por categoría "Música" y rango de precio 10-50
  Then el sistema muestra únicamente los servicios que cumplen ambos criterios

Escenario: Edición de un ítem publicado
  Given que soy el Creador dueño de un servicio publicado
  When edito su precio o descripción
  Then el catálogo refleja los cambios inmediatamente
```
