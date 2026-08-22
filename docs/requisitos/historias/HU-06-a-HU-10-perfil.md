# Historias de Usuario — Módulo Perfiles, Verificación y Portafolio

---

## HU-06 — Verificación de mayoría de edad
**Trazabilidad:** REQ-F-006
**Prueba de aceptación:** `VerificacionServicioImplTest` · `VerificacionControladorTest` · `CertificadoIaRepositoryIT`

**As a** Creador que se registra por primera vez,
**I want** verificar mi mayoría de edad subiendo un documento de identidad,
**so that** pueda operar legalmente en la plataforma y obtener el sello de cuenta verificada.

```gherkin
Escenario: Verificación aprobada
  Given que subo un documento de identidad válido que confirma mayoría de edad
  When el servicio de verificación procesa el documento
  Then mi cuenta pasa a estado "verificado" en menos de 60 segundos
  And recibo una notificación de verificación exitosa
  And el documento deja de ser accesible en el almacenamiento del sistema

Escenario: Verificación rechazada por minoría de edad
  Given que el documento indica que soy menor de 18 años
  When el servicio de verificación procesa el documento
  Then mi estado de cuenta no cambia
  And recibo un mensaje de rechazo
```

---

## HU-07 — Verificación de certificados profesionales
**Trazabilidad:** REQ-F-007
**Prueba de aceptación:** `VerificacionServicioImplTest` · `VerificacionControladorTest` · `CertificadoIaRepositoryIT`

**As a** Creador con estudios o certificaciones relevantes,
**I want** subir mis certificados para que sean validados automáticamente,
**so that** obtenga un sello de verificación profesional que genere más confianza en mis Clientes.

```gherkin
Escenario: Certificado válido con puntaje suficiente
  Given que subo un certificado y el análisis de IA arroja un puntaje de 0.85
  When el puntaje supera el umbral configurado (0.75 por defecto)
  Then mi perfil muestra inmediatamente el sello de verificación profesional

Escenario: Certificado con puntaje insuficiente
  Given que el análisis de IA arroja un puntaje de 0.60
  When el puntaje no alcanza el umbral configurado
  Then mi perfil no muestra el sello
  And recibo una notificación explicando el resultado
```

---

## HU-08 — Personalización de perfil público
**Trazabilidad:** REQ-F-008
**Prueba de aceptación:** `PerfilCreadorServicioImplTest`

**As a** Creador,
**I want** personalizar mi perfil público con foto, biografía y enlaces a redes sociales,
**so that** los Clientes potenciales conozcan mi trabajo y puedan encontrarme también en otras plataformas.

```gherkin
Escenario: Biografía con datos de contacto directo
  Given que escribo en mi biografía un número de teléfono
  When intento guardar los cambios de mi perfil
  Then el sistema rechaza la actualización y me indica que no se permite contacto directo

Escenario: Imagen de perfil demasiado grande
  Given que subo una imagen de 8 MB
  When el sistema valida el archivo
  Then rechaza la imagen por exceder el límite de 5 MB

Escenario: Actualización exitosa
  Given que completo biografía, imagen válida y hasta 3 URLs de redes sociales
  When guardo los cambios
  Then mi perfil público muestra la nueva información y los enlaces son clicables
```

---

## HU-09 — Seguir a un Creador y ver su reputación
**Trazabilidad:** REQ-F-009
**Prueba de aceptación:** `SeguidorServiceImplTest` · `SeguidorRepositoryIT`

**As a** usuario autenticado,
**I want** seguir a un Creador y ver su cantidad de seguidores, servicios activos y calificación promedio,
**so that** pueda descubrir y hacer seguimiento a los Creadores cuyo trabajo me interesa.

```gherkin
Escenario: Seguir a un Creador
  Given que estoy autenticado y visito el perfil de un Creador
  When presiono "Seguir"
  Then el contador de seguidores del Creador se actualiza inmediatamente

Escenario: Visitante no autenticado intenta seguir
  Given que no he iniciado sesión
  When presiono "Seguir" en un perfil
  Then el sistema me redirige a la pantalla de login
```

---

## HU-10 — Comentar en el portafolio
**Trazabilidad:** REQ-F-010
**Prueba de aceptación:** _sin prueba automatizada: el requisito está en estado `pendiente` y su excepción se declara en `docs/trazabilidad/excepciones-estado.txt`_

**As a** usuario autenticado,
**I want** dejar comentarios en los ítems del portafolio de un Creador,
**so that** pueda expresar retroalimentación pública sobre su trabajo.

```gherkin
Escenario: El Creador elimina un comentario
  Given que existe un comentario en mi ítem de portafolio
  When lo elimino desde mi panel
  Then el comentario deja de ser visible públicamente
  And el administrador aún puede consultarlo en el panel de moderación
```
