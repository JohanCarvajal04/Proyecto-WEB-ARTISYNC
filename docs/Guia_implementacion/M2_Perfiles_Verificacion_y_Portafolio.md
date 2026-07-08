# Módulo 2: Perfiles, Verificación y Portafolio

## Descripción General

Este módulo gestiona la **identidad profesional del Creador** dentro de ARTISYNC. Permite personalizar el perfil público, verificar la identidad y certificaciones mediante inteligencia artificial, administrar un portafolio visual, y recibir interacciones sociales (comentarios, likes). Es la vitrina del creador ante los clientes.

---

## Requisitos Funcionales Cubiertos

### RF-06 — Verificación de Identidad con IA

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | El Creador puede cargar un documento de identidad oficial para verificar su mayoría de edad mediante un servicio externo de análisis. Si el resultado es aprobatorio, el estado se actualiza a verificado; el documento se elimina del almacenamiento tras recibir la respuesta. |
| **Criterio de Aceptación** | Resultado aprobatorio → estado "verificado" en máx. 60 segundos + notificación. Minoría de edad → estado sin cambios + "Verificación rechazada: no se pudo confirmar la mayoría de edad". Documento inaccesible tras la respuesta. |

**Estado actual:** ❌ No implementado

**Implementación requerida:**

1. **Endpoint:** `POST /api/creadores/verificacion/identidad`
   - Auth: JWT + Rol CREADOR
   - Body: `multipart/form-data` con archivo del documento
   - Validar formato (JPG, PNG, PDF) y tamaño (máx. 10 MB)

2. **Flujo de negocio:**
   ```
   1. Recibir archivo → subir a S3/R2 (temporal)
   2. Enviar URL del documento al servicio de IA
   3. Recibir respuesta del servicio (aprobado/rechazado)
   4. Si aprobado → actualizar EstadoVerificacion a "APROBADO"
   5. Si rechazado → mantener estado + notificar
   6. SIEMPRE → eliminar archivo de S3/R2
   7. Registrar en certificados_ia con puntaje y estado
   ```

3. **Archivos a crear:**
   ```
   controller/VerificacionController.java
   service/VerificacionService.java
   service/impl/VerificacionServiceImpl.java
   service/shared/IaVerificationClient.java     — Cliente HTTP al servicio IA
   dto/request/VerificacionIdentidadRequest.java
   dto/response/VerificacionResponse.java
   ```

4. **Repositorios necesarios:**
   ```
   repository/CertificadoIaRepository.java      — Crear si no existe
   repository/EstadoVerificacionRepository.java  — Crear si no existe
   ```

5. **Variables de entorno:**
   ```
   IA_SERVICE_URL=https://api.example.com/verify
   IA_SERVICE_API_KEY=your_api_key
   ```

---

### RF-07 — Certificados Profesionales con Sello de Verificación

| Campo | Detalle |
|---|---|
| **Prioridad** | 🟡 Media |
| **Descripción** | El Creador puede cargar certificados profesionales para análisis por IA. Si el puntaje de confianza supera el umbral configurable (por defecto 0.75), el perfil recibe un sello de verificación visible. |
| **Criterio de Aceptación** | Puntaje ≥ umbral → sello visible inmediatamente. Puntaje inferior → sin sello + notificación al Creador. El administrador puede modificar el umbral desde el panel sin cambios en el código fuente. |

**Estado actual:** ❌ No implementado

**Implementación requerida:**

1. **Endpoints:**
   ```
   POST /api/creadores/verificacion/certificado     — Cargar certificado (CREADOR)
   GET  /api/creadores/verificacion/estado           — Consultar estado (CREADOR)
   PUT  /api/admin/configuracion/umbral-confianza    — Cambiar umbral (ADMIN)
   GET  /api/admin/configuracion/umbral-confianza    — Ver umbral actual (ADMIN)
   ```

2. **Configuración dinámica del umbral:**
   - Opción A: Tabla `configuracion_sistema` con clave-valor
   - Opción B: Variable de entorno `IA_CONFIDENCE_THRESHOLD=0.75` + endpoint admin que la actualice en cache Redis

3. **Lógica del sello:**
   - El sello se muestra en el perfil público solo si existe un `CertificadoIa` con `puntaje_confianza_ia >= umbral` y `estado_verificacion = 'APROBADO'`

---

### RF-08 — Personalización del Perfil Público

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | El Creador puede personalizar su perfil con foto (JPG/PNG, máx. 5 MB), biografía (máx. 500 chars) y hasta tres URLs de redes sociales. El sistema debe rechazar números de teléfono y correos electrónicos en la biografía. |
| **Criterio de Aceptación** | Biografía con número de teléfono → "La biografía no puede contener datos de contacto directos". Imagen > 5 MB → "El archivo supera el tamaño máximo permitido de 5 MB". URL válida → enlace funcional en el perfil. |

**Estado actual:** ❌ No implementado (solo la entidad `PerfilCreador` existe con los campos `biografia` y `urlRedSocial`)

**Implementación requerida:**

1. **Endpoints:**
   ```
   PUT  /api/creadores/perfil            — Actualizar perfil propio
   POST /api/creadores/perfil/foto       — Subir foto de perfil
   GET  /api/creadores/{id}/perfil       — Ver perfil público (sin auth)
   ```

2. **DTOs:**
   ```java
   // ActualizarPerfilRequest.java
   public class ActualizarPerfilRequest {
       @Size(max = 500, message = "La biografía no puede superar los 500 caracteres")
       private String biografia;
       
       @Size(max = 255) private String urlRedSocial1;
       @Size(max = 255) private String urlRedSocial2;
       @Size(max = 255) private String urlRedSocial3;
   }
   ```

3. **Validaciones críticas en el Service:**
   ```java
   // Regex para detectar datos de contacto en la biografía
   private static final Pattern PATRON_TELEFONO = Pattern.compile("\\+?\\d[\\d\\-\\s]{7,}");
   private static final Pattern PATRON_EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
   
   public void validarBiografia(String biografia) {
       if (PATRON_TELEFONO.matcher(biografia).find() || PATRON_EMAIL.matcher(biografia).find()) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
               "La biografía no puede contener datos de contacto directos");
       }
   }
   ```

4. **Validación de foto:**
   - Content-Type: `image/jpeg` o `image/png`
   - Tamaño: ≤ 5 MB (5 * 1024 * 1024 bytes)
   - Error: `"El archivo supera el tamaño máximo permitido de 5 MB"`

5. **Archivos a crear:**
   ```
   controller/PerfilCreadorController.java
   service/PerfilCreadorService.java
   service/impl/PerfilCreadorServiceImpl.java
   dto/request/ActualizarPerfilRequest.java
   dto/response/PerfilPublicoResponse.java
   ```

6. **Ajuste a la entidad `PerfilCreador`:**
   - El campo `urlRedSocial` es único → necesita soportar hasta 3 URLs. Opciones:
     - A) Almacenar como JSON en un campo TEXT
     - B) Agregar columnas `url_red_social_2`, `url_red_social_3` (migración V3)
     - C) Crear tabla `redes_sociales_creador` (normalizado)
   - Agregar campo `url_foto_perfil` (VARCHAR 255) si no existe

---

### RF-09 — Perfil Público con Métricas y Sistema de Seguimiento

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | El perfil público debe mostrar total de seguidores, número de servicios activos, promedio de calificaciones y estado de verificación. Cualquier usuario autenticado puede seguir o dejar de seguir. |
| **Criterio de Aceptación** | Promedio de calificaciones coincide con cálculo manual. "Seguir" incrementa contador en uno. "Dejar de seguir" lo decrementa. Usuario no autenticado que intenta seguir es redirigido al login. |

**Estado actual:** ❌ No implementado

**Implementación requerida:**

1. **Endpoints de perfil público:**
   ```
   GET /api/creadores/{id}/perfil    — Retorna PerfilPublicoResponse
   ```
   
   ```java
   // PerfilPublicoResponse.java
   public class PerfilPublicoResponse {
       private Long idPerfil;
       private String nombres;
       private String apellidos;
       private String biografia;
       private String urlFotoPerfil;
       private List<String> urlsRedesSociales;
       private Long totalSeguidores;
       private Long serviciosActivos;
       private Double promedioCalificaciones;
       private boolean identidadVerificada;
       private boolean selloVerificado;
       private boolean siguiendo; // Si el usuario actual lo sigue
   }
   ```

2. **Endpoints de seguimiento:**
   ```
   POST   /api/creadores/{id}/seguir       — Seguir (JWT requerido)
   DELETE /api/creadores/{id}/seguir       — Dejar de seguir (JWT requerido)
   GET    /api/creadores/{id}/seguidores   — Listar seguidores
   GET    /api/usuarios/me/siguiendo       — Listar a quiénes sigo
   ```

3. **Archivos a crear:**
   ```
   controller/SeguidorController.java
   service/SeguidorService.java
   service/impl/SeguidorServiceImpl.java
   dto/response/SeguidorResponse.java
   ```

4. **Queries necesarias en repositorios:**
   ```java
   // SeguidorRepository
   long countByPerfilCreadorIdPerfil(Long idPerfil);
   boolean existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(Long idUsuario, Long idPerfil);
   
   // ServicioRepository
   long countByPerfilIdPerfil(Long idPerfil);
   
   // ResenaServicioRepository (agregar)
   @Query("SELECT AVG(r.calificacionEstrellas) FROM ResenaServicio r WHERE r.pedido.servicio.perfil.idPerfil = :idPerfil")
   Double promedioCalificacionesPorCreador(@Param("idPerfil") Long idPerfil);
   ```

---

### RF-10 — Comentarios en Portafolio (Soft Delete)

| Campo | Detalle |
|---|---|
| **Prioridad** | 🟡 Media |
| **Descripción** | Cualquier usuario autenticado puede publicar comentarios en los ítems del portafolio de los Creadores. El Creador puede eliminar comentarios (soft delete, no visible públicamente). Los eliminados son visibles para el administrador. |
| **Criterio de Aceptación** | Comentario publicado → visible públicamente. Creador lo elimina → deja de ser visible. Admin puede consultar eliminados. |

**Estado actual:** ❌ No implementado (entidad `ComentarioPortafolio` existe con campo `estado_moderacion`)

**Implementación requerida:**

1. **Endpoints:**
   ```
   POST   /api/portafolio/items/{id}/comentarios               — Crear comentario (JWT)
   GET    /api/portafolio/items/{id}/comentarios               — Listar comentarios activos
   DELETE /api/portafolio/items/{id}/comentarios/{commentId}    — Soft delete (CREADOR dueño)
   GET    /api/admin/comentarios/eliminados                     — Admin: ver eliminados
   ```

2. **Lógica de soft delete:**
   ```java
   // No eliminar de la BD, solo cambiar estado
   comentario.setEstadoModeracion("Eliminado");
   comentarioRepository.save(comentario);
   ```

3. **Archivos a crear:**
   ```
   controller/ComentarioController.java
   service/ComentarioService.java
   service/impl/ComentarioServiceImpl.java
   dto/request/CrearComentarioRequest.java
   dto/response/ComentarioResponse.java
   repository/ComentarioPortafolioRepository.java
   ```

---

## Tablas de Base de Datos

| Tabla | Entidad JPA | Paquete | Repositorio | Estado Entidad | Estado Repo |
|---|---|---|---|---|---|
| `perfiles_creadores` | `PerfilCreador` | `entity.perfil` | `PerfilCreadorRepository` | ✅ | ✅ |
| `estados_verificacion` | `EstadoVerificacion` | `entity.perfil` | — | ✅ | ❌ Crear |
| `certificados_ia` | `CertificadoIa` | `entity.perfil` | — | ✅ | ❌ Crear |
| `habilidades` | `Habilidad` | `entity.perfil` | — | ✅ | ❌ Crear |
| `creador_habilidades` | `CreadorHabilidad` | `entity.perfil` | — | ✅ | ❌ Crear |
| `portafolios` | `Portafolio` | `entity.perfil` | `PortafolioRepository` | ✅ | ✅ |
| `portafolio_items` | `PortafolioItem` | `entity.perfil` | — | ✅ | ❌ Crear |
| `categorias` | `Categoria` | `entity.perfil` | — | ✅ | ❌ Crear |
| `seguidores` | `Seguidor` | `entity.comunicacion` | — | ✅ | ❌ Crear |
| `comentarios_portafolio` | `ComentarioPortafolio` | `entity.comunicacion` | — | ✅ | ❌ Crear |
| `likes_portafolio` | `LikePortafolio` | `entity.comunicacion` | — | ✅ | ❌ Crear |

---

## Archivos a Crear (Resumen)

```
controller/
├── PerfilCreadorController.java
├── PortafolioController.java
├── ComentarioController.java
├── SeguidorController.java
└── VerificacionController.java

service/
├── PerfilCreadorService.java
├── PortafolioService.java
├── ComentarioService.java
├── SeguidorService.java
├── VerificacionService.java
└── impl/
    ├── PerfilCreadorServiceImpl.java
    ├── PortafolioServiceImpl.java
    ├── ComentarioServiceImpl.java
    ├── SeguidorServiceImpl.java
    └── VerificacionServiceImpl.java

service/shared/
└── IaVerificationClient.java

dto/request/
├── ActualizarPerfilRequest.java
├── CrearPortafolioItemRequest.java
└── CrearComentarioRequest.java

dto/response/
├── PerfilPublicoResponse.java
├── PortafolioItemResponse.java
├── ComentarioResponse.java
├── SeguidorResponse.java
└── VerificacionResponse.java

repository/
├── EstadoVerificacionRepository.java
├── CertificadoIaRepository.java
├── HabilidadRepository.java
├── CreadorHabilidadRepository.java
├── PortafolioItemRepository.java
├── CategoriaRepository.java
├── SeguidorRepository.java
├── ComentarioPortafolioRepository.java
└── LikePortafolioRepository.java
```

---

## Checklist de Validación del Módulo

- [ ] Creador puede actualizar biografía, foto y redes sociales
- [ ] Biografía con teléfono/email es rechazada con mensaje exacto
- [ ] Foto > 5 MB es rechazada con mensaje exacto
- [ ] Perfil público muestra: seguidores, servicios activos, calificaciones, verificación
- [ ] Seguir/dejar de seguir funcional con conteo inmediato
- [ ] Usuario no autenticado redirigido al login al intentar seguir
- [ ] Comentarios creados por usuarios autenticados visibles públicamente
- [ ] Creador puede soft-delete comentarios de su portafolio
- [ ] Admin puede ver comentarios eliminados
- [ ] Verificación de identidad con IA actualiza estado
- [ ] Documento eliminado de S3 después de verificación
- [ ] Sello de verificación visible si puntaje ≥ umbral
- [ ] Admin puede cambiar umbral sin código

---

## Dependencias con Otros Módulos

| Módulo | Relación |
|---|---|
| **M1 (Seguridad)** | Requiere autenticación JWT y roles CREADOR/CLIENTE/ADMIN |
| **M3 (Catálogo)** | RF-09 cuenta servicios activos del creador |
| **M7 (Social)** | RF-09 calcula promedio de calificaciones desde `resenas_servicios` |
| **M5 (Legal)** | Almacenamiento S3/R2 para fotos y documentos |

---

## Prerrequisitos del Sistema

- [x] M1 completamente funcional (auth + RBAC)
- [ ] Servicio de almacenamiento S3/R2 configurado (RNF-11)
- [ ] Servicio de IA externo disponible (o mock para desarrollo)
- [ ] Variables de entorno de IA configuradas
