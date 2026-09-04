# Historias de Usuario — Módulo Perfiles, Verificación y Portafolio

---

## HU-06 — Verificación de mayoría de edad
**Trazabilidad:** REQ-F-006
**Prueba de aceptación:** `VerificacionServicioImplTest` · `VerificacionControladorTest` · `CertificadoIaRepositoryIT`

**As a** Creador que se registra por primera vez,
**I want** verificar mi mayoría de edad subiendo un documento de identidad,
**so that** pueda operar legalmente en la plataforma y obtener el sello de cuenta verificada.

**INVEST:** Independiente porque ocurre una sola vez tras el registro y no bloquea el resto de historias de perfil; negociable en el umbral de tiempo de procesamiento (60 s) y en el proveedor de análisis de IA; valiosa porque es requisito legal para operar en la plataforma; estimable y pequeña porque se acota al flujo de subida + análisis + cambio de estado; testable con los escenarios de aprobación y rechazo por minoría de edad.

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

**INVEST:** Independiente de HU-06 (verifica un atributo distinto, la trayectoria profesional, no la mayoría de edad); negociable en el umbral de puntaje (0.75 por defecto); valiosa porque diferencia a los Creadores con credenciales frente al resto; estimable y pequeña porque reutiliza la misma infraestructura de análisis de IA que HU-06; testable con los escenarios de puntaje suficiente e insuficiente.

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

**INVEST:** Independiente porque el Creador la ejecuta cuando quiere, sin depender de otra historia; negociable en los límites exactos (tamaño de imagen, número de URLs); valiosa porque el perfil público es la vitrina que atrae Clientes; estimable y pequeña porque se limita a un formulario y su validación; testable con los tres escenarios de contacto directo, imagen sobredimensionada y actualización exitosa.

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

**INVEST:** Independiente porque no requiere que exista aún ningún pedido ni comisión entre las partes; negociable en qué métricas de reputación se muestran junto al conteo de seguidores; valiosa porque sostiene el descubrimiento social de Creadores; estimable y pequeña porque son dos endpoints (seguir/dejar de seguir y contador); testable con los escenarios de seguir autenticado y el redireccionamiento a login sin sesión.

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

**INVEST:** Independiente porque actúa sobre ítems de portafolio ya publicados, sin acoplarse a HU-08; negociable en si la eliminación es reversible o definitiva para el autor; valiosa porque habilita interacción pública que refuerza la reputación del Creador; estimable y pequeña porque es un CRUD simple sobre comentarios; testable mediante el escenario de eliminación y visibilidad diferenciada para moderación — **sin embargo**, al no tener aún prueba automatizada (estado `pendiente`, ver excepción declarada), su atributo Testable queda cubierto solo por el criterio de aceptación en Gherkin, no por una prueba ejecutable.

```gherkin
Escenario: El Creador elimina un comentario
  Given que existe un comentario en mi ítem de portafolio
  When lo elimino desde mi panel
  Then el comentario deja de ser visible públicamente
  And el administrador aún puede consultarlo en el panel de moderación
```
