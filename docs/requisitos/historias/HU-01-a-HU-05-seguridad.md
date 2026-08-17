# Historias de Usuario — Módulo Seguridad y Control de Acceso

Formato Connextra (*As a ⟨rol⟩, I want ⟨objetivo⟩, so that ⟨beneficio⟩*), evaluado contra los criterios INVEST (Independent, Negotiable, Valuable, Estimable, Small, Testable) de Cohn. Criterios de aceptación en Gherkin (Given/When/Then). Trazabilidad: cada historia soporta exactamente un requisito funcional del SRS (`docs/requisitos/SRS.md`).

---

## HU-01 — Registro con selección de rol
**Trazabilidad:** REQ-F-001

**As a** visitante que quiere ofrecer o contratar servicios creativos,
**I want** registrarme eligiendo si seré Creador o Cliente,
**so that** el sistema me muestre desde el inicio las vistas y acciones que corresponden a mi rol.

**INVEST:** Independiente del resto del flujo de auth; negociable en los campos exactos del formulario; valiosa (es la puerta de entrada al sistema); estimable y pequeña (un solo formulario + persistencia); testable mediante el criterio de aceptación.

```gherkin
Escenario: Registro exitoso como Creador
  Given que soy un visitante no autenticado
  When completo el formulario de registro y selecciono el rol "Creador"
  Then el sistema crea mi cuenta con ese rol
  And al iniciar sesión veo únicamente las vistas y acciones habilitadas para Creadores

Escenario: Un Cliente no puede acceder a rutas de Creador
  Given que estoy autenticado con rol "Cliente"
  When intento acceder a una ruta exclusiva de Creador
  Then el sistema responde 403 y me redirige a una página de acceso no autorizado
```

---

## HU-02 — Control de acceso basado en roles (RBAC)
**Trazabilidad:** REQ-F-002

**As a** administrador de la plataforma,
**I want** asignar y revocar permisos específicos a cada rol,
**so that** pueda ajustar con precisión qué puede hacer cada tipo de usuario sin modificar código.

```gherkin
Escenario: Revocación de permiso surte efecto inmediato
  Given que el rol "Creador" tiene el permiso "publicar_servicio"
  When el administrador revoca ese permiso al rol "Creador"
  Then la siguiente solicitud de un Creador para publicar un servicio recibe 403
  And no requiere que el Creador cierre e inicie sesión nuevamente
```

---

## HU-03 — Autenticación con sesión JWT
**Trazabilidad:** REQ-F-003

**As a** usuario registrado,
**I want** iniciar sesión y mantenerla activa de forma segura durante 24 horas,
**so that** no tenga que volver a autenticarme en cada acción dentro de ese período.

```gherkin
Escenario: Acceso con token válido
  Given que tengo un token JWT vigente
  When realizo una solicitud a una ruta protegida
  Then el sistema responde 200 y ejecuta la acción solicitada

Escenario: Acceso con token expirado
  Given que mi token JWT expiró
  When realizo una solicitud a una ruta protegida
  Then el sistema responde 401 con el mensaje "Token expirado"

Escenario: Acceso sin token
  Given que no envío cabecera de autorización
  When realizo una solicitud a una ruta protegida
  Then el sistema responde 401 con el mensaje "Autenticación requerida"
```

---

## HU-04 — Recuperación de contraseña
**Trazabilidad:** REQ-F-004

**As a** usuario que olvidó su contraseña,
**I want** recibir un enlace de un solo uso para restablecerla,
**so that** pueda recuperar el acceso a mi cuenta sin intervención del administrador.

```gherkin
Escenario: Enlace usado dos veces
  Given que ya usé mi enlace de recuperación para cambiar la contraseña
  When intento usar el mismo enlace nuevamente
  Then el sistema muestra un mensaje de enlace inválido

Escenario: Enlace expirado
  Given que ha pasado más de 60 minutos desde que se generó el enlace
  When intento usarlo
  Then el sistema muestra un mensaje de enlace expirado

Escenario: Flujo completo exitoso
  Given que recibí un enlace de recuperación vigente
  When establezco una nueva contraseña
  Then puedo iniciar sesión inmediatamente con la nueva contraseña
```

---

## HU-05 — Segundo factor de autenticación (2FA)
**Trazabilidad:** REQ-F-005

**As a** Creador con identidad verificada,
**I want** activar un segundo factor de autenticación basado en TOTP,
**so that** mi cuenta tenga una capa adicional de protección frente a robo de contraseña.

```gherkin
Escenario: Código TOTP incorrecto
  Given que tengo 2FA activado
  When ingreso un código incorrecto al iniciar sesión
  Then el sistema responde "Código inválido o expirado" y no me deja continuar

Escenario: Usuario no verificado no ve la opción de 2FA
  Given que mi identidad aún no ha sido verificada
  When accedo a la configuración de seguridad de mi cuenta
  Then no veo la opción de activar 2FA
```
