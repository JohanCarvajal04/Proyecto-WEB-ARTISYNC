# Casos de Uso — Módulo Perfiles, Verificación y Portafolio

---

## CU-06: Verificar mayoría de edad
**Trazabilidad:** REQ-F-006 / HU-06

**1. Actor principal y objetivo:** Creador — obtener el estado de cuenta verificada mediante validación de su documento de identidad.

**Nivel:** Meta de usuario

**Precondición:** El Creador se ha registrado y aún no está verificado.

**Garantía de éxito:** El documento se procesa, el resultado se refleja en el estado de la cuenta, y el documento se elimina del almacenamiento.

**2. Escenario principal de éxito:**
1. El Creador sube una imagen de su documento de identidad.
2. El sistema envía el documento al servicio externo de verificación por IA.
3. El servicio confirma que la persona es mayor de edad.
4. El sistema marca la cuenta como "verificada" y notifica al Creador.
5. El sistema elimina el documento del almacenamiento temporal.

**3. Extensiones:**
- 3a. El servicio determina que la persona es menor de edad.
- 2a. El servicio externo no responde en el tiempo esperado.

**4. Manejo de extensiones:**
- 3a1. El sistema no modifica el estado de la cuenta y notifica el rechazo al Creador. Termina.
- 2a1. El sistema reintenta una vez; si falla de nuevo, notifica error temporal e invita a reintentar más tarde. Termina.

---

## CU-07: Verificar certificado profesional
**Trazabilidad:** REQ-F-007 / HU-07

**1. Actor principal y objetivo:** Creador — obtener el sello de verificación profesional mediante análisis automático de un certificado.

**Nivel:** Meta de usuario

**Precondición:** El Creador tiene cuenta verificada (CU-06 completado).

**Garantía de éxito:** El sello se otorga u omite según el puntaje devuelto por el análisis.

**2. Escenario principal de éxito:**
1. El Creador sube el archivo del certificado.
2. El sistema lo envía al servicio de análisis por IA.
3. El servicio devuelve un puntaje de confianza.
4. El sistema compara el puntaje contra el umbral configurado (0.75 por defecto).
5. Si el puntaje es igual o mayor, el sistema activa el sello de verificación profesional en el perfil.

**3. Extensiones:**
- 5a. El puntaje es menor al umbral.

**4. Manejo de extensiones:**
- 5a1. El sistema no activa el sello y notifica el resultado al Creador, indicando que puede volver a intentarlo con otro documento. Termina.

---

## CU-08: Editar perfil público
**Trazabilidad:** REQ-F-008 / HU-08

**1. Actor principal y objetivo:** Creador — actualizar su foto, biografía y enlaces de redes sociales.

**Nivel:** Meta de usuario

**Precondición:** El Creador tiene sesión activa.

**Garantía de éxito:** El perfil público refleja los cambios validados.

**2. Escenario principal de éxito:**
1. El Creador abre el editor de perfil.
2. El sistema muestra los campos actuales (foto, biografía, hasta 3 URLs).
3. El Creador modifica uno o más campos y guarda.
4. El sistema valida tamaño de imagen, longitud de biografía y ausencia de datos de contacto directo.
5. El sistema persiste los cambios y actualiza el perfil público.

**3. Extensiones:**
- 4a. La biografía contiene un teléfono o correo.
- 4b. La imagen excede 5 MB.

**4. Manejo de extensiones:**
- 4a1. El sistema rechaza el guardado y resalta el fragmento de texto conflictivo. Vuelve al paso 3.
- 4b1. El sistema rechaza la imagen e indica el límite permitido. Vuelve al paso 3.

---

## CU-09: Seguir a un Creador
**Trazabilidad:** REQ-F-009 / HU-09

**1. Actor principal y objetivo:** Usuario autenticado — seguir a un Creador para ver su actividad y reputación.

**Nivel:** Meta de usuario

**Precondición:** El usuario tiene sesión activa y visita un perfil de Creador.

**Garantía de éxito:** El usuario queda registrado como seguidor y el contador se actualiza.

**2. Escenario principal de éxito:**
1. El usuario visita el perfil público de un Creador.
2. El sistema muestra el botón "Seguir" junto con seguidores, servicios activos y calificación promedio.
3. El usuario presiona "Seguir".
4. El sistema registra la relación de seguimiento y actualiza el contador en tiempo real.

**3. Extensiones:**
- 1a. El usuario no tiene sesión activa.

**4. Manejo de extensiones:**
- 1a1. El sistema redirige al usuario a la pantalla de login; tras autenticarse, retorna al perfil del Creador. Vuelve al paso 3.

---

## CU-10: Comentar en un ítem de portafolio
**Trazabilidad:** REQ-F-010 / HU-10

**1. Actor principal y objetivo:** Usuario autenticado — publicar un comentario en un ítem de portafolio de un Creador.

**Nivel:** Meta de usuario

**Precondición:** El usuario tiene sesión activa; el ítem de portafolio es público.

**Garantía de éxito:** El comentario queda visible públicamente o eliminado lógicamente si el Creador lo decide.

**2. Escenario principal de éxito:**
1. El usuario visita un ítem del portafolio.
2. El usuario escribe y envía un comentario.
3. El sistema persiste el comentario y lo muestra públicamente.

**3. Extensiones:**
- 3a. El Creador dueño del ítem decide eliminar el comentario.

**4. Manejo de extensiones:**
- 3a1. El sistema marca el comentario como eliminado lógicamente (no visible al público, consultable por el administrador). Termina.
