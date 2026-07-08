# Módulo 3: Catálogo Dinámico de Servicios

## Descripción General

Este módulo gestiona el **catálogo de servicios y productos digitales** que los creadores publican en ARTISYNC. Incluye categorías y subcategorías jerárquicas, atributos personalizados dinámicos (EAV pattern), etiquetas, y un motor de búsqueda con filtros múltiples. Es la interfaz principal de descubrimiento para los clientes.

---

## Requisitos Funcionales Cubiertos

### RF-11 — Publicación de Productos y Servicios en el Catálogo

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | El Creador puede publicar dos tipos de ítems: **Producto** (descarga inmediata tras pago) y **Servicio** (comisión con flujo de contrato). Cada ítem requiere precio (mín. 0.01 USD), al menos una imagen (JPG/PNG, máx. 10 MB) y descripción (20-2000 chars). |
| **Criterio de Aceptación** | Sin precio → "El precio es un campo obligatorio". Imagen > 10 MB → rechazada. Producto → adquirible sin contrato. Servicio → inicia flujo de contrato al ser solicitado. |

**Estado actual:** ❌ No implementado (entidad `Servicio` existe con validaciones JPA correctas)

**Implementación requerida:**

1. **Ajuste a la entidad `Servicio`:**
   - Agregar campo `tipo_item` (VARCHAR 20) → valores: `PRODUCTO` o `SERVICIO`
   - Agregar campo `estado_publicacion` (VARCHAR 20) → valores: `ACTIVO`, `PAUSADO`, `BORRADOR`
   - Requiere migración Flyway V3:
     ```sql
     ALTER TABLE servicios ADD COLUMN IF NOT EXISTS tipo_item VARCHAR(20) NOT NULL DEFAULT 'SERVICIO';
     ALTER TABLE servicios ADD COLUMN IF NOT EXISTS estado_publicacion VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';
     ```

2. **Endpoints:**
   ```
   POST   /api/servicios                    — Crear servicio/producto (CREADOR)
   PUT    /api/servicios/{id}               — Editar servicio (CREADOR dueño)
   GET    /api/servicios/{id}               — Detalle del servicio (público)
   DELETE /api/servicios/{id}               — Eliminar servicio (CREADOR dueño)
   GET    /api/creadores/{id}/servicios     — Servicios de un creador (público)
   POST   /api/servicios/{id}/imagenes      — Subir imagen del servicio (multipart)
   ```

3. **DTOs:**
   ```java
   // CrearServicioRequest.java
   public class CrearServicioRequest {
       @NotBlank private String tituloServicio;
       @NotBlank @Size(min = 20, max = 2000) private String descripcionDetallada;
       @NotNull @DecimalMin("0.01") private BigDecimal precioBase;
       @NotNull private Long idSubcategoria;
       @NotNull @Pattern(regexp = "PRODUCTO|SERVICIO") private String tipoItem;
       private List<Long> etiquetaIds;
   }
   
   // ServicioResponse.java
   public class ServicioResponse {
       private Long idServicio;
       private String tituloServicio;
       private String descripcionDetallada;
       private BigDecimal precioBase;
       private String tipoItem;
       private String estadoPublicacion;
       private String urlMiniatura;
       private String nombreCategoria;
       private String nombreSubcategoria;
       private List<AtributoResponse> atributos;
       private List<String> etiquetas;
       private PerfilResumidoResponse creador;
   }
   ```

4. **Validaciones:**
   - Precio: `@DecimalMin("0.01")` ya en la entidad ✅
   - Descripción: `@Size(min = 20, max = 2000)` ya en la entidad ✅
   - Imagen: validar Content-Type (`image/jpeg`, `image/png`) y tamaño ≤ 10 MB
   - Solo el Creador dueño puede editar/eliminar sus servicios

---

### RF-12 — Atributos Personalizados Dinámicos

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | El Creador puede añadir hasta 10 atributos personalizados por ítem del catálogo, con nombre (máx. 100 chars) y valor (máx. 255 chars). Los formularios deben adaptarse dinámicamente a la categoría sin recargar la página. |
| **Criterio de Aceptación** | Categoría "Ilustrador" muestra campos distintos a "Músico". Nombre > 100 chars → rechazado. Intentar un 11° atributo → "Se ha alcanzado el límite de 10 atributos personalizados por ítem". |

**Estado actual:** ❌ No implementado (entidades `AtributoDinamico`, `ServicioAtributo` existen)

**Implementación requerida:**

1. **Endpoints:**
   ```
   GET    /api/categorias/{id}/atributos               — Atributos sugeridos por categoría
   POST   /api/servicios/{id}/atributos                — Añadir atributo al servicio (CREADOR)
   PUT    /api/servicios/{id}/atributos/{attrId}       — Editar atributo
   DELETE /api/servicios/{id}/atributos/{attrId}       — Eliminar atributo
   GET    /api/servicios/{id}/atributos                — Listar atributos del servicio
   ```

2. **Validación del límite de 10:**
   ```java
   public void agregarAtributo(Long idServicio, AtributoRequest request) {
       long count = servicioAtributoRepository.countByServicioIdServicio(idServicio);
       if (count >= 10) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
               "Se ha alcanzado el límite de 10 atributos personalizados por ítem");
       }
       // ... crear atributo
   }
   ```

3. **Adaptación dinámica del formulario:**
   - Crear tabla o configuración que asocie categorías con atributos sugeridos
   - En el frontend: al cambiar la categoría en el formulario, hacer `GET /api/categorias/{id}/atributos` y renderizar los campos dinámicamente sin recargar la página (SPA behavior de Angular)

---

### RF-13 — Motor de Búsqueda con Filtros

| Campo | Detalle |
|---|---|
| **Prioridad** | 🔴 Alta |
| **Descripción** | Motor de búsqueda de servicios con filtros por categoría, subcategoría, rango de precio y etiquetas, y búsqueda textual sobre título y descripción. El Creador debe poder editar cualquier campo en cualquier momento. |
| **Criterio de Aceptación** | Filtro simultáneo categoría + rango de precio → solo ítems que cumplen ambas. Búsqueda "retrato" → todos los servicios con esa cadena en título o descripción (case insensitive). Precio editado reflejado en < 5 segundos. |

**Estado actual:** ❌ No implementado

**Implementación requerida:**

1. **Endpoint de búsqueda:**
   ```
   GET /api/catalogo?
       categoria={id}&
       subcategoria={id}&
       precioMin={valor}&
       precioMax={valor}&
       etiquetas={id1,id2,id3}&
       q={texto}&
       page={n}&
       size={n}&
       sort={campo,direction}
   ```
   - Auth: **NO** — debe ser público (`/api/catalog/**` ya está en `permitAll()`)
   - Paginación con `PagedResponse`

2. **Query dinámica con Specification (JPA Criteria API):**
   ```java
   public class ServicioSpecification {
       public static Specification<Servicio> conFiltros(
               Long categoriaId, Long subcategoriaId, 
               BigDecimal precioMin, BigDecimal precioMax,
               List<Long> etiquetaIds, String textoBusqueda) {
           return (root, query, cb) -> {
               List<Predicate> predicates = new ArrayList<>();
               
               if (categoriaId != null) {
                   predicates.add(cb.equal(
                       root.get("subcategoria").get("categoria").get("idCategoria"), categoriaId));
               }
               if (subcategoriaId != null) {
                   predicates.add(cb.equal(
                       root.get("subcategoria").get("idSubcategoria"), subcategoriaId));
               }
               if (precioMin != null) {
                   predicates.add(cb.greaterThanOrEqualTo(root.get("precioBase"), precioMin));
               }
               if (precioMax != null) {
                   predicates.add(cb.lessThanOrEqualTo(root.get("precioBase"), precioMax));
               }
               if (textoBusqueda != null && !textoBusqueda.isBlank()) {
                   String like = "%" + textoBusqueda.toLowerCase() + "%";
                   predicates.add(cb.or(
                       cb.like(cb.lower(root.get("tituloServicio")), like),
                       cb.like(cb.lower(root.get("descripcionDetallada")), like)
                   ));
               }
               // Filtro de etiquetas requiere subquery o join
               
               return cb.and(predicates.toArray(new Predicate[0]));
           };
       }
   }
   ```

3. **`ServicioRepository` debe extender `JpaSpecificationExecutor<Servicio>`:**
   ```java
   public interface ServicioRepository extends JpaRepository<Servicio, Long>, 
                                               JpaSpecificationExecutor<Servicio> {
       // queries adicionales
   }
   ```

4. **Endpoints de categorías (Admin + público):**
   ```
   GET    /api/categorias                        — Listar categorías activas (público)
   POST   /api/admin/categorias                  — Crear categoría (ADMIN)
   PUT    /api/admin/categorias/{id}             — Editar categoría (ADMIN)
   GET    /api/categorias/{id}/subcategorias     — Listar subcategorías (público)
   POST   /api/admin/subcategorias               — Crear subcategoría (ADMIN)
   ```

---

## Tablas de Base de Datos

| Tabla | Entidad JPA | Paquete | Repositorio | Estado Entidad | Estado Repo |
|---|---|---|---|---|---|
| `categorias` | `Categoria` | `entity.perfil` | — | ✅ | ❌ Crear |
| `subcategorias` | `Subcategoria` | `entity.catalogo` | — | ✅ | ❌ Crear |
| `servicios` | `Servicio` | `entity.catalogo` | `ServicioRepository` | ✅ (ampliar) | ✅ (ampliar) |
| `atributos_dinamicos` | `AtributoDinamico` | `entity.catalogo` | — | ✅ | ❌ Crear |
| `servicio_atributos` | `ServicioAtributo` | `entity.catalogo` | — | ✅ | ❌ Crear |
| `etiquetas` | `Etiqueta` | `entity.catalogo` | — | ✅ | ❌ Crear |
| `servicio_etiquetas` | `ServicioEtiqueta` | `entity.catalogo` | — | ✅ | ❌ Crear |
| `flujos_trabajo` | `FlujoTrabajo` | `entity.catalogo` | — | ✅ | ❌ Crear |

---

## Archivos a Crear

```
controller/
├── ServicioController.java
├── CategoriaController.java
└── EtiquetaController.java

service/
├── ServicioService.java
├── CategoriaService.java
├── EtiquetaService.java
└── impl/
    ├── ServicioServiceImpl.java
    ├── CategoriaServiceImpl.java
    └── EtiquetaServiceImpl.java

dto/request/
├── CrearServicioRequest.java
├── ActualizarServicioRequest.java
├── CrearAtributoRequest.java
├── BusquedaServicioRequest.java
├── CrearCategoriaRequest.java
└── CrearSubcategoriaRequest.java

dto/response/
├── ServicioResponse.java
├── ServicioCatalogoResponse.java    — Vista resumida para la grilla
├── CategoriaResponse.java
├── SubcategoriaResponse.java
├── AtributoResponse.java
└── EtiquetaResponse.java

repository/
├── CategoriaRepository.java
├── SubcategoriaRepository.java
├── ServicioAtributoRepository.java
├── AtributoDinamicoRepository.java
├── EtiquetaRepository.java
└── ServicioEtiquetaRepository.java

specification/
└── ServicioSpecification.java       — Filtros dinámicos con Criteria API
```

---

## Migración de BD (V3)

```sql
-- V3__catalogo_ajustes.sql
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS tipo_item VARCHAR(20) NOT NULL DEFAULT 'SERVICIO';
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS estado_publicacion VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS cargo_revision_adicional DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS limite_revisiones_base INT DEFAULT 0;
```

---

## Checklist de Validación del Módulo

- [ ] CRUD completo de categorías y subcategorías (admin)
- [ ] CRUD completo de servicios (creador)
- [ ] Diferenciar tipo PRODUCTO vs SERVICIO
- [ ] Precio mínimo 0.01 USD validado
- [ ] Imagen obligatoria, JPG/PNG, máx. 10 MB
- [ ] Descripción entre 20 y 2000 caracteres
- [ ] Atributos dinámicos: máx. 10 por servicio
- [ ] Error exacto al intentar 11° atributo
- [ ] Formularios dinámicos por categoría (frontend sin recarga)
- [ ] Motor de búsqueda con filtros combinados funcionando
- [ ] Búsqueda textual case-insensitive
- [ ] Precio editado reflejado en < 5 segundos
- [ ] Etiquetas asignables a servicios
- [ ] Paginación funcional en resultados

---

## Dependencias con Otros Módulos

| Módulo | Relación |
|---|---|
| **M1 (Seguridad)** | Auth JWT + rol CREADOR para publicar, ADMIN para gestionar categorías |
| **M2 (Perfiles)** | Servicios se asocian al `PerfilCreador` |
| **M4 (Pedidos)** | Los servicios de tipo SERVICIO inician flujo de contrato al ser solicitados |
| **M5 (Legal)** | Integración con S3/R2 para almacenar imágenes de servicios |

---

## Prerrequisitos del Sistema

- [x] M1 completamente funcional (auth + RBAC)
- [x] M2 parcialmente funcional (PerfilCreador existe al registrar CREADOR)
- [ ] Servicio de almacenamiento S3/R2 para imágenes (RNF-11)
- [ ] Migración V3 ejecutada para nuevos campos
