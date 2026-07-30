# Casos de Uso — Módulo Catálogo Dinámico de Servicios

---

## CU-11: Publicar un ítem en el catálogo
**Trazabilidad:** REQ-F-011 / HU-11

**1. Actor principal y objetivo:** Creador — publicar un producto o servicio para que sea visible en el catálogo.

**Nivel:** Meta de usuario

**Precondición:** El Creador tiene sesión activa.

**Garantía de éxito:** El ítem queda publicado y visible en las búsquedas del catálogo.

**2. Escenario principal de éxito:**
1. El Creador abre el formulario de publicación y elige tipo (Producto o Servicio).
2. El sistema solicita título, precio, al menos una imagen y descripción.
3. El Creador completa los datos y envía el formulario.
4. El sistema valida precio ≥0.01 USD, imagen ≤10MB y descripción entre 20 y 2000 caracteres.
5. El sistema publica el ítem con estado activo.

**3. Extensiones:**
- 4a. La descripción no cumple la longitud mínima o máxima.
- 4b. No se adjunta ninguna imagen.

**4. Manejo de extensiones:**
- 4a1. El sistema rechaza la publicación e indica el rango permitido. Vuelve al paso 3.
- 4b1. El sistema rechaza la publicación por falta de imagen obligatoria. Vuelve al paso 3.

---

## CU-12: Configurar atributos personalizados
**Trazabilidad:** REQ-F-012 / HU-12

**1. Actor principal y objetivo:** Creador — añadir atributos personalizados a un ítem según su categoría.

**Nivel:** Subfunción (invocado desde CU-11)

**Precondición:** El Creador ha seleccionado una categoría en el formulario de publicación.

**Garantía de éxito:** El ítem queda con hasta 10 atributos personalizados asociados a su categoría.

**2. Escenario principal de éxito:**
1. El sistema detecta la categoría seleccionada y carga los campos de atributos definidos para ella.
2. El Creador completa los valores de los atributos mostrados.
3. El sistema valida que no se excedan los 10 atributos por ítem.
4. El sistema asocia los atributos al ítem al momento de publicarlo (continúa en CU-11, paso 5).

**3. Extensiones:**
- 3a. La categoría no tiene atributos configurados.

**4. Manejo de extensiones:**
- 3a1. El sistema omite la sección de atributos y continúa el flujo de publicación normalmente. Vuelve a CU-11, paso 3.

---

## CU-13: Buscar servicios en el catálogo
**Trazabilidad:** REQ-F-013 / HU-13

**1. Actor principal y objetivo:** Cliente — encontrar servicios que cumplan criterios específicos de categoría, precio o palabras clave.

**Nivel:** Meta de usuario

**Precondición:** Existen ítems publicados en el catálogo.

**Garantía de éxito:** El Cliente recibe una lista de resultados que cumple todos los filtros aplicados.

**2. Escenario principal de éxito:**
1. El Cliente abre el buscador del catálogo.
2. El Cliente introduce texto libre y/o selecciona categoría, subcategoría, rango de precio y etiquetas.
3. El sistema construye la consulta combinando todos los filtros activos.
4. El sistema devuelve los ítems que cumplen todos los criterios, paginados.

**3. Extensiones:**
- 4a. Ningún ítem cumple los criterios combinados.

**4. Manejo de extensiones:**
- 4a1. El sistema muestra un mensaje de "sin resultados" y sugiere relajar los filtros. Termina.
