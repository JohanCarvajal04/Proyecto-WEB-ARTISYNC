# Requisitos y Criterios de Aceptación del Proyecto
### PFC Entrega 1B — Módulo de Autenticación JWT + Acceso a Datos con Spring Data JPA
**UTEQ · Ingeniería de Software · Aplicaciones Web · Semana 9 · 2025-2026**

---

## 1. Objetivo del Documento

Este documento constituye la **lista oficial de verificación** para garantizar que el proyecto PFC Entrega 1B cumple el 100% de los requisitos establecidos en la guía académica de la Sumativa #8. Su propósito es servir como guía de validación antes de la entrega final al SGA en la Semana 9.

Cualquier ítem marcado como **CRÍTICO** que no sea cumplido puede resultar en anulación parcial o total del criterio correspondiente, independientemente de la calidad del informe escrito.

**Stack tecnológico obligatorio:** Java 21 LTS · Spring Boot 3.2.x · Spring Security 6.x · jjwt 0.12.x · Spring Data JPA · Hibernate 6.x · Flyway 9.x · Springdoc OpenAPI 2.x · Lombok · Bean Validation 3.x · PostgreSQL 16 · Redis 7.x · Docker Compose 2.x · Angular 17+ · Nginx 1.25 · JUnit 5 + Mockito · GitHub Actions

---

## 2. Requisitos Funcionales

### RF-001 — Registro de Usuarios
**Endpoint:** `POST /api/auth/registro`

- Debe permitir registrar un nuevo usuario proporcionando nombre, email y contraseña.
- Debe validar que el email tenga formato válido (`@Email`).
- Debe validar que los campos obligatorios no estén vacíos (`@NotBlank`).
- Debe almacenar la contraseña usando **BCrypt con costo 12** (nunca en texto plano).
- Debe impedir el registro de emails duplicados (constraint `UNIQUE` en BD).
- Debe devolver HTTP **201 Created** con el usuario creado (sin el hash de contraseña).
- El campo `password_hash` debe estar anotado con `@JsonIgnore` para no exponerse en la respuesta.

| Criterio de aceptación | Prueba requerida | Resultado esperado |
|---|---|---|
| Usuario se registra con datos válidos | `POST /api/auth/registro` con JSON válido | 201 Created + objeto usuario sin hash |
| Email duplicado es rechazado | `POST /api/auth/registro` con email ya existente | 409 Conflict |
| Campos vacíos son rechazados | `POST /api/auth/registro` con campos en blanco | 400 Bad Request |
| Email con formato inválido es rechazado | `POST /api/auth/registro` con `email: "noesmail"` | 400 Bad Request |
| Hash BCrypt visible en BD | Consulta directa a tabla `usuarios` | Campo `password_hash` inicia con `$2a$12$` |

---

### RF-002 — Login / Inicio de Sesión
**Endpoint:** `POST /api/auth/login`

- Debe aceptar `{email, password}` en el cuerpo de la solicitud.
- Debe delegar la autenticación a `AuthenticationManager.authenticate()`.
- Debe consultar `UserDetailsService.loadUserByUsername()`.
- Debe verificar el hash BCrypt con `PasswordEncoder.matches()`.
- Debe generar y retornar `{accessToken, refreshToken, expiresIn}` en caso de éxito.
- El `accessToken` debe tener una vigencia de **1 hora (3 600 000 ms)**.
- El `refreshToken` debe tener una vigencia de **7 días (604 800 000 ms)**.
- Debe retornar HTTP **200 OK** en caso exitoso.
- Debe retornar HTTP **401 Unauthorized** con credenciales incorrectas.

| Criterio de aceptación | Prueba requerida | Resultado esperado |
|---|---|---|
| Login exitoso | `POST /api/auth/login` con credenciales correctas | 200 OK + accessToken + refreshToken |
| Contraseña incorrecta | `POST /api/auth/login` con password erróneo | 401 Unauthorized |
| Usuario inexistente | `POST /api/auth/login` con email no registrado | 401 Unauthorized |
| Token tiene estructura JWT válida | Decodificar accessToken en jwt.io | Header + Payload + Signature correctos |

---

### RF-003 — Logout / Cierre de Sesión
**Endpoint:** `POST /api/auth/logout`  
**Autenticación:** JWT requerido

- Debe extraer el `jti` (JWT ID) del token recibido.
- Debe almacenar el `jti` en Redis con TTL igual al tiempo restante de expiración del token.
- Debe retornar HTTP **204 No Content** al cerrar sesión.
- Después del logout, el mismo token no debe poder usarse en ningún endpoint protegido.

| Criterio de aceptación | Prueba requerida | Resultado esperado |
|---|---|---|
| Logout exitoso | `POST /api/auth/logout` con Bearer token válido | 204 No Content |
| Token revocado no funciona | Usar el mismo token post-logout en endpoint protegido | 401 Unauthorized |
| JTI registrado en Redis | `redis-cli GET jti:[uuid]` | Valor presente con TTL > 0 |

---

### RF-004 — Refresh Token
**Endpoint:** `POST /api/auth/refresh`  
**Autenticación:** refreshToken (cookie HttpOnly o body)

- Debe permitir emitir un nuevo `accessToken` sin re-autenticar al usuario.
- Debe validar que el refreshToken no esté expirado ni en blacklist.
- Debe retornar un nuevo `accessToken` con nueva fecha de expiración.
- Debe retornar HTTP **200 OK** en caso exitoso.
- Debe retornar HTTP **401 Unauthorized** si el refreshToken es inválido o expirado.

| Criterio de aceptación | Prueba requerida | Resultado esperado |
|---|---|---|
| Refresh exitoso | `POST /api/auth/refresh` con refreshToken válido | 200 OK + nuevo accessToken |
| RefreshToken expirado rechazado | Usar refreshToken expirado | 401 Unauthorized |

---

### RF-005 — Perfil del Usuario Autenticado
**Endpoint:** `GET /api/usuarios/me`  
**Autenticación:** JWT requerido

- Debe retornar los datos del usuario autenticado extraídos del `SecurityContext`.
- No debe retornar el campo `password_hash`.
- Debe retornar HTTP **200 OK**.
- Sin token: HTTP **401 Unauthorized**.

| Criterio de aceptación | Prueba requerida | Resultado esperado |
|---|---|---|
| Perfil con token válido | `GET /api/usuarios/me` con Bearer token | 200 OK + datos del usuario |
| Sin token | `GET /api/usuarios/me` sin Authorization | 401 Unauthorized |

---

### RF-006 — CRUD Completo de Entidad Principal
**Endpoints:** `/api/[entidad]` y `/api/[entidad]/{id}`  
**Autenticación:** JWT requerido en todos

#### RF-006.1 — Crear entidad
- `POST /api/[entidad]` debe validar el cuerpo con `@Valid`.
- Debe retornar HTTP **201 Created** con la entidad creada.

#### RF-006.2 — Listar entidades con paginación
- `GET /api/[entidad]` debe soportar parámetros `?page=0&size=10&sort=id,asc`.
- Debe retornar objeto Page de Spring con `content`, `totalElements`, `totalPages`, `number`.

#### RF-006.3 — Buscar por ID
- `GET /api/[entidad]/{id}` debe retornar HTTP **200 OK** si existe.
- Debe retornar HTTP **404 Not Found** si no existe.

#### RF-006.4 — Actualizar entidad
- `PUT /api/[entidad]/{id}` debe actualizar todos los campos.
- Debe retornar HTTP **200 OK** con la entidad actualizada.

#### RF-006.5 — Eliminar entidad (Soft Delete)
- `DELETE /api/[entidad]/{id}` debe implementar **soft delete** (campo `activo = false`).
- Debe retornar HTTP **204 No Content**.
- El registro no debe ser eliminado físicamente de la BD.

| Criterio de aceptación | Prueba requerida | Resultado esperado |
|---|---|---|
| Crear entidad válida | `POST /api/[entidad]` con JSON válido + JWT | 201 Created |
| Listar con paginación | `GET /api/[entidad]?page=0&size=10` + JWT | 200 OK + estructura Page |
| Buscar por ID existente | `GET /api/[entidad]/1` + JWT | 200 OK + entidad |
| Buscar ID inexistente | `GET /api/[entidad]/9999` + JWT | 404 Not Found |
| Actualizar entidad | `PUT /api/[entidad]/1` con JSON + JWT | 200 OK + entidad actualizada |
| Soft delete | `DELETE /api/[entidad]/1` + JWT | 204 No Content; `activo=false` en BD |

---

### RF-007 — Documentación Swagger / OpenAPI
**Endpoints:** `GET /api/docs` y `GET /api/swagger-ui.html`

- Swagger UI debe estar accesible sin autenticación.
- Debe listar **todos** los endpoints implementados (mínimo los 10 de la guía).
- Cada endpoint debe mostrar método HTTP, path, parámetros y respuestas esperadas.
- Generado automáticamente por **Springdoc OpenAPI 2.x**.

| Criterio de aceptación | Prueba requerida | Resultado esperado |
|---|---|---|
| Swagger UI accesible | `GET /api/swagger-ui.html` sin token | 200 OK + interfaz Swagger |
| Endpoints documentados | Revisar Swagger UI | Mínimo 10 endpoints visibles |

---

### RF-008 — Estructura del JWT Emitido

El payload del JWT debe contener obligatoriamente los siguientes claims:

| Claim | Descripción | Ejemplo |
|---|---|---|
| `sub` | ID del usuario | `"42"` |
| `email` | Email del usuario | `"juan@uteq.edu.ec"` |
| `rol` | Rol Spring Security | `"ROLE_USER"` |
| `iat` | Timestamp de emisión | `1735689600` |
| `exp` | Timestamp de expiración | `1735693200` |
| `jti` | JWT ID único para blacklist | `"550e8400-e29b..."` |

---

### RF-009 — Filtro JWT (JwtAuthFilter)

- Debe extender `OncePerRequestFilter`.
- Debe extraer el token del encabezado `Authorization: Bearer [token]`.
- Debe validar la firma y expiración con `JwtService.validateToken()`.
- Debe consultar Redis para verificar que el `jti` no está en la blacklist.
- Debe establecer `UsernamePasswordAuthenticationToken` en el `SecurityContextHolder`.

---

### RF-010 — Blacklist JWT con Redis

- Al hacer logout, el `jti` debe almacenarse en Redis con TTL = tiempo restante del token.
- En cada solicitud autenticada, el `JwtAuthFilter` debe consultar Redis antes de autorizar.
- Si el `jti` está en la blacklist, debe retornar **401 Unauthorized**.

---

## 3. Requisitos No Funcionales

### Rendimiento

| Métrica | Valor esperado |
|---|---|
| Tiempo promedio `POST /api/auth/login` | < 500 ms (incluye BCrypt costo 12) |
| Tiempo promedio `GET /api/[entidad]` (50 registros) | < 200 ms |
| Tiempo promedio `POST /api/[entidad]` | < 300 ms |
| Tiempo P95 login | < 800 ms |
| `GET /api/[entidad]` con Redis cache | Notablemente menor que sin cache |

> Las métricas deben medirse con Postman, JMeter o k6 y reportarse en el informe técnico con valores reales.

---

### Escalabilidad

- Uso de **Docker Compose** para orquestar todos los servicios en un solo comando.
- Separación clara entre contenedores: `backend`, `frontend`, `postgres`, `redis`.
- Redis permite escalar la blacklist de tokens sin estado compartido en el backend.
- Spring Data JPA permite cambiar de motor de BD con mínimo impacto.

---

### Disponibilidad

- `docker compose up --build -d` debe levantar **todos** los servicios sin errores manuales.
- Cada servicio debe tener `healthcheck` definido en el `docker-compose.yml`.
- El servicio `backend` debe depender de `postgres` y `redis` estando en estado `healthy`.
- `GET /actuator/health` debe retornar `{"status":"UP"}` cuando el sistema está operativo.

---

### Mantenibilidad

- Arquitectura por capas estricta: `entity` → `repository` → `service` → `controller`.
- Paquetes adicionales: `dto`, `security`, `config`.
- Uso de **DTOs** para separar la capa HTTP de las entidades JPA.
- **Inyección de dependencias por constructor** (no `@Autowired` en campo).
- Uso de **Lombok** (`@Data`, `@Builder`, `@RequiredArgsConstructor`) para reducir boilerplate.
- `ddl-auto: validate` — Flyway gestiona el DDL, Hibernate no modifica el esquema.
- `open-in-view: false` para evitar `LazyInitializationException`.

---

### Compatibilidad

| Componente | Versión mínima |
|---|---|
| Java | 21 LTS |
| Spring Boot | 3.2.x |
| Spring Security | 6.x |
| jjwt | 0.12.x |
| Angular | 17+ |
| PostgreSQL | 16 |
| Redis | 7.x |
| Docker Compose | 2.x |
| Flyway | 9.x |
| Springdoc OpenAPI | 2.x |
| JUnit | 5.x |
| Nginx | 1.25 |

---

## 4. Requisitos de Arquitectura

### 4.1 Documentos de Diseño Requeridos

- [ ] ✔ **Diagrama C4 Nivel 2 (Contenedores)** actualizado respecto a Entrega 1A, mostrando: backend Spring Boot, frontend Angular, PostgreSQL, Redis; con tecnología, protocolo y puerto de cada contenedor.
- [ ] ✔ **Diagrama de Clases UML** generado desde IntelliJ IDEA, PlantUML o Lucidchart; exportado en PNG de alta resolución.
- [ ] ✔ **Diagrama de Secuencia** del flujo JWT completo con los 11 pasos descritos en la guía.
- [ ] ✔ **Diagrama Entidad-Relación (DER)** generado desde pgAdmin 4 o DBeaver mostrando PKs, FKs, tipos y cardinalidades.
- [ ] ✔ **Diccionario de datos** con nombre de columna, tipo PostgreSQL, nulo/no nulo, valor por defecto y descripción de negocio.
- [ ] ✔ **ADR-003** documentado en `docs/adr/ADR-003-jwt-redis.md` justificando el uso de Redis para blacklist JWT.

### 4.2 Arquitectura del Backend (Spring Boot)

El diagrama de clases debe mostrar obligatoriamente:

| Elemento | Descripción |
|---|---|
| Paquete `entity` | Clases anotadas con `@Entity`; atributos tipados (`Long`, `String`, `Instant`, `boolean`, `enum`); anotaciones JPA (`@Id`, `@Column`, `@ManyToOne`, etc.) |
| Paquete `repository` | Interfaces que extienden `JpaRepository<T, Long>` con métodos custom (`findByEmail`) |
| Paquete `service` | Dependencias inyectadas como campos `final`; métodos del contrato de negocio |
| Paquete `controller` | Controladores REST anotados con `@RestController` |
| Paquete `dto` | Clases `record` o `@Data` **sin** `@Entity`; separan capa HTTP de entidades JPA |
| Paquete `security` | `JwtService`, `JwtAuthFilter`, `SecurityConfig`, `UserDetailsServiceImpl` |
| Paquete `config` | Beans de configuración (PasswordEncoder, RedisTemplate, etc.) |

### 4.3 Correspondencia Java ↔ PostgreSQL (Tabla obligatoria)

| Atributo Java | Tipo Java | Tipo PostgreSQL | Restricciones |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | PK; mejor rendimiento en joins que UUID |
| `email` | `String` | `VARCHAR(255)` | `UNIQUE NOT NULL`; índice `CREATE UNIQUE INDEX` |
| `passwordHash` | `String` | `VARCHAR(255)` | BCrypt costo 12; `@JsonIgnore` |
| `rol` | `enum Rol` | `VARCHAR(20)` | CHECK: `ROLE_USER`, `ROLE_ADMIN`; no ENUM nativo |
| `creadoEn` | `Instant` | `TIMESTAMPTZ` | UTC; `@CreationTimestamp` |
| `activo` | `boolean` | `BOOLEAN` | Soft delete; `DEFAULT TRUE` |

> Repetir para cada entidad del sistema del PFC.

### 4.4 Estructura de Directorios del Monorepo

| Ruta | Contenido esperado |
|---|---|
| `backend/src/main/java/` | Código fuente Java organizado en paquetes por capa |
| `backend/src/main/resources/` | `application.yml`, migraciones Flyway |
| `backend/src/test/` | Pruebas JUnit 5 + MockMvc |
| `frontend/src/app/` | Módulos Angular: `auth/`, `core/`, `shared/`, `features/` |
| `frontend/src/environments/` | `environment.ts` y `environment.prod.ts` |
| `database/migrations/` | Scripts Flyway `V1__`, `V2__`, etc. |
| `docker-compose.yml` | 4 servicios: backend, postgres, redis, frontend |
| `.env.example` | Variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `REDIS_URL` |
| `docs/adr/ADR-003-jwt-redis.md` | Justificación Redis para blacklist JWT |
| `.github/workflows/ci.yml` | Pipeline GitHub Actions |

---

## 5. Requisitos de Base de Datos

### 5.1 Flyway — Migraciones

- El archivo `V1__schema_inicial.sql` debe ser ejecutable en PostgreSQL 16 **sin errores**.
- Los archivos de migración versionados (`V1__`, `V2__`, etc.) **no deben modificarse** una vez ejecutados; los cambios van en un nuevo archivo `V2__`.
- `ddl-auto` debe estar en **`validate`** en `application.yml`.
- Flyway debe ejecutarse automáticamente al iniciar la aplicación.
- La ruta de migraciones debe ser `classpath:db/migration`.

### 5.2 Esquema de la Tabla `usuarios` (mínimo obligatorio)

```sql
CREATE TABLE usuarios (
  id           BIGSERIAL PRIMARY KEY,
  nombre       VARCHAR(100) NOT NULL,
  email        VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  rol          VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
  activo       BOOLEAN      NOT NULL DEFAULT TRUE,
  creado_en    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_usuarios_email ON usuarios(email);

ALTER TABLE usuarios
  ADD CONSTRAINT chk_rol
  CHECK (rol IN ('ROLE_USER', 'ROLE_ADMIN'));
```

### 5.3 Requisitos de Esquema

| Requisito | Descripción | Estado esperado |
|---|---|---|
| Claves primarias | `BIGSERIAL PRIMARY KEY` en todas las tablas | Obligatorio |
| Claves únicas | `UNIQUE INDEX` en `email` | Obligatorio |
| Restricciones CHECK | `chk_rol` valida valores del enum | Obligatorio |
| Soft Delete | Campo `activo BOOLEAN DEFAULT TRUE` | Obligatorio |
| Timestamps | `TIMESTAMPTZ` para `creado_en` y `actualizado_en` | Obligatorio |
| Trigger `actualizado_en` | Función `set_actualizado_en()` que actualiza automáticamente | Obligatorio |
| Claves foráneas | FKs definidas entre entidades del dominio | Según el PFC |
| Convención de nombres | `snake_case` en nombres de columnas | Obligatorio |
| Sin DDL manual | Hibernate en modo `validate`; Flyway gestiona el esquema | Obligatorio |

### 5.4 Trigger Obligatorio

```sql
CREATE OR REPLACE FUNCTION set_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
  NEW.actualizado_en = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuarios_actualizado_en
  BEFORE UPDATE ON usuarios
  FOR EACH ROW EXECUTE FUNCTION set_actualizado_en();
```

---

## 6. Requisitos de Seguridad

| # | Requisito | Descripción | Cómo comprobarlo | Estado esperado |
|---|---|---|---|---|
| S-01 | BCrypt costo 12 | Contraseñas almacenadas con `BCryptPasswordEncoder(12)` | Consultar BD: hash inicia con `$2a$12$` | Obligatorio |
| S-02 | JWT firmado HS256 | Token firmado con clave secreta mínimo 256 bits | Decodificar en jwt.io; verificar alg=HS256 | Obligatorio |
| S-03 | JWT con expiración | accessToken: 1h; refreshToken: 7d | Revisar claims `exp` e `iat` | Obligatorio |
| S-04 | JTI único por token | Cada token tiene `jti` UUID v4 | Verificar claim `jti` en payload | Obligatorio |
| S-05 | Blacklist Redis | JTI revocado en Redis con TTL al hacer logout | `redis-cli GET jti:[uuid]` después del logout | Obligatorio |
| S-06 | CORS configurado | Orígenes permitidos explícitamente en `SecurityConfig` | Solicitud desde dominio no permitido → 403 | Obligatorio |
| S-07 | Headers HTTP | `X-Content-Type-Options`, `X-Frame-Options`, `CSP` activos | `curl -I http://localhost:8080` | Obligatorio |
| S-08 | Roles con `@PreAuthorize` | `ROLE_USER` y `ROLE_ADMIN` con control en endpoints | Prueba de acceso con rol incorrecto → 403 | Obligatorio |
| S-09 | Sin SQL concatenado | Cero uso de `String sql = "..." + variable` | Revisión de código; solo Spring Data JPA | Obligatorio |
| S-10 | Token NO en localStorage | accessToken en memoria (variable `AuthService`); refreshToken en cookie HttpOnly | DevTools → Application → Storage: vacío | Obligatorio |
| S-11 | Cookie HttpOnly | refreshToken enviado en cookie con flags `HttpOnly; Secure; SameSite=Strict` | DevTools → Cookies | Obligatorio |
| S-12 | OWASP A01 — Control de acceso | Endpoints protegidos retornan 401/403 sin token/rol | Prueba sin token en endpoint protegido | Obligatorio |
| S-13 | OWASP A02 — Fallas criptográficas | BCrypt costo 12; JWT HS256; HTTPS en producción | Revisar config y BD | Obligatorio |
| S-14 | OWASP A03 — Inyección | Spring Data JPA; consultas derivadas; sin JDBC raw | Revisión de código | Obligatorio |
| S-15 | OWASP A07 — Auth failures | 401 en credenciales incorrectas; sin mensajes que revelen usuarios | Pruebas de login fallido | Obligatorio |
| S-16 | Bean Validation | `@Valid` en controladores; `@NotBlank`, `@Email`, `@Size` en DTOs | Enviar campo inválido → 400 | Obligatorio |
| S-17 | Clave JWT segura | `JWT_SECRET` mínimo 256 bits, generada con `openssl rand -hex 32` | Revisar longitud en `.env` | Obligatorio |
| S-18 | Sin contraseña en texto plano | La columna `password_hash` nunca contiene texto legible | SELECT en BD post-registro | Obligatorio — **ANULACIÓN** si falla |

---

## 7. Requisitos del Backend

### 7.1 Spring Boot

- [ ] Spring Boot 3.2.x configurado correctamente.
- [ ] `application.yml` con todas las variables de entorno usando `${VAR:default}`.
- [ ] Variables de entorno: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `REDIS_HOST`, `REDIS_PORT`.
- [ ] `spring.jpa.ddl-auto: validate`.
- [ ] `spring.jpa.open-in-view: false`.
- [ ] Actuator habilitado con endpoints `health`, `info`, `metrics`.
- [ ] `springdoc.api-docs.path: /api/docs`.
- [ ] `springdoc.swagger-ui.path: /api/swagger-ui.html`.

### 7.2 Spring Security

- [ ] `SecurityFilterChain` configurado con filtros JWT.
- [ ] `JwtAuthenticationFilter` extiende `OncePerRequestFilter`.
- [ ] `AuthenticationManager` configurado como bean.
- [ ] `UserDetailsServiceImpl` implementa `UserDetailsService`.
- [ ] Rutas públicas: `/api/auth/registro`, `/api/auth/login`, `/api/docs/**`, `/api/swagger-ui/**`.
- [ ] Rutas protegidas: todo lo demás requiere JWT válido.
- [ ] CORS configurado explícitamente (no con `*`).
- [ ] Headers de seguridad habilitados.

### 7.3 Spring Data JPA / Hibernate

- [ ] Entidades anotadas con `@Entity`, `@Table`, `@Id`, `@GeneratedValue`.
- [ ] Repositorios extienden `JpaRepository<T, Long>`.
- [ ] Paginación con `Pageable` y retorno de `Page<T>`.
- [ ] `@CreationTimestamp` y `@UpdateTimestamp` en timestamps.
- [ ] `@JsonIgnore` en `passwordHash`.
- [ ] Relaciones JPA con `@ManyToOne`, `@OneToMany` según el dominio del PFC.

### 7.4 Redis

- [ ] Configuración `spring.data.redis.host` y `spring.data.redis.port` en `application.yml`.
- [ ] `RedisTemplate` configurado como bean.
- [ ] Blacklist de tokens: clave `jti:[uuid]`, TTL = tiempo restante del token.
- [ ] JwtAuthFilter consulta Redis en cada solicitud autenticada.

### 7.5 Flyway

- [ ] Ubicación: `classpath:db/migration`.
- [ ] `baseline-on-migrate: true`.
- [ ] `V1__schema_inicial.sql` ejecutable sin errores.
- [ ] Archivos de migración nombrados con convención `V{n}__{descripcion}.sql`.

---

## 8. Requisitos del Frontend (Angular)

- [ ] Angular 17+ con módulo de autenticación (`auth/`).
- [ ] `JwtInterceptor` que clona la solicitud y agrega `Authorization: Bearer [token]`.
- [ ] accessToken almacenado **en memoria** (variable de `AuthService`), **nunca en localStorage**.
- [ ] refreshToken almacenado en **cookie HttpOnly** (gestionado por el backend).
- [ ] `AuthGuard` implementa `canActivate` para proteger rutas.
- [ ] Manejo del error **401** para redirigir automáticamente al login.
- [ ] `HttpClientModule` configurado con interceptores.
- [ ] Angular Material para la interfaz de usuario.
- [ ] Rutas protegidas definidas en el Router de Angular.
- [ ] Consumo de los endpoints de autenticación y CRUD con el `HttpClient`.
- [ ] Nginx sirve el bundle compilado en producción.
- [ ] `environment.ts` y `environment.prod.ts` con la URL de la API.

---

## 9. Requisitos del API REST

### 9.1 Tabla Completa de Endpoints

| Endpoint | Método | Autenticación | HTTP esperado | Descripción |
|---|---|---|---|---|
| `/api/auth/registro` | POST | No | 201 Created | Registrar nuevo usuario |
| `/api/auth/login` | POST | No | 200 OK | Autenticar; devuelve tokens |
| `/api/auth/logout` | POST | JWT | 204 No Content | Agregar JTI a blacklist Redis |
| `/api/auth/refresh` | POST | refreshToken | 200 OK | Emitir nuevo accessToken |
| `/api/usuarios/me` | GET | JWT | 200 OK | Perfil del usuario autenticado |
| `/api/[entidad]` | GET | JWT | 200 OK | Listar con paginación |
| `/api/[entidad]/{id}` | GET | JWT | 200 OK / 404 | Buscar por ID |
| `/api/[entidad]` | POST | JWT | 201 Created | Crear entidad |
| `/api/[entidad]/{id}` | PUT | JWT | 200 OK | Actualizar entidad |
| `/api/[entidad]/{id}` | DELETE | JWT | 204 No Content | Soft delete |
| `/api/docs` | GET | No | 200 OK | Swagger JSON |
| `/api/swagger-ui.html` | GET | No | 200 OK | Swagger UI |
| `/actuator/health` | GET | No | 200 OK | Health check |

### 9.2 Estándares REST

- [ ] Uso correcto de verbos HTTP (GET, POST, PUT, DELETE).
- [ ] Respuestas con códigos HTTP semánticos (200, 201, 204, 400, 401, 403, 404, 409).
- [ ] Respuestas en formato JSON con `Content-Type: application/json`.
- [ ] Paginación con estructura `Page` de Spring: `content`, `totalElements`, `totalPages`, `number`.
- [ ] Manejo de errores con respuesta estructurada (no stack trace en producción).

---

## 10. Requisitos Docker

### 10.1 docker-compose.yml — Servicios Obligatorios

| Servicio | Imagen | Puerto | Dependencias |
|---|---|---|---|
| `postgres` | `postgres:16` | 5432 | — |
| `redis` | `redis:7` | 6379 | — |
| `backend` | imagen propia | 8080 | postgres (healthy), redis (healthy) |
| `frontend` | nginx:1.25 | 4200 | backend |

### 10.2 Checklist Docker

- [ ] `docker compose up --build -d` levanta todos los servicios sin intervención manual.
- [ ] Cada servicio tiene `healthcheck` definido.
- [ ] El servicio `backend` usa `depends_on` con condición `service_healthy` para postgres y redis.
- [ ] Variables de entorno sensibles en archivo `.env` (no hardcodeadas en `docker-compose.yml`).
- [ ] Archivo `.env.example` incluido en el repositorio.
- [ ] `.env` está en `.gitignore`.
- [ ] `docker compose ps` muestra todos los servicios en estado `healthy`.
- [ ] `GET /actuator/health` retorna `{"status":"UP"}`.
- [ ] `GET /api/swagger-ui.html` accesible en `http://localhost:8080`.
- [ ] Frontend accesible en `http://localhost:4200`.
- [ ] **README.md** con instrucciones de ejecución en exactamente 5 pasos ejecutables.

### 10.3 README.md — Instrucciones Obligatorias

```bash
# 1. Clonar el repositorio y cambiar a la rama de entrega
git clone https://github.com/[equipo]/[repo].git
cd [repo]
git checkout entrega-1b

# 2. Copiar variables de entorno
cp .env.example .env
# Editar .env con las credenciales del entorno local

# 3. Levantar todos los servicios
docker compose up --build -d

# 4. Verificar que todos los servicios están en estado healthy
docker compose ps

# 5. Acceder a la aplicación
# Frontend:        http://localhost:4200
# Swagger UI:      http://localhost:8080/api/swagger-ui.html
# Actuator health: http://localhost:8080/actuator/health
```

---

## 11. Requisitos del Repositorio Git

- [ ] Commits de **todos** los integrantes del equipo (no solo uno).
- [ ] Mensajes de commit en formato **Conventional Commits**: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`.
- [ ] Rama de trabajo: `feature/autenticacion-jwt` y `feature/crud-[entidad]`, mergeadas a `develop`.
- [ ] Tag de entrega: `v0.1.0-entrega-1b` apuntando al commit de entrega.
- [ ] Pipeline **GitHub Actions** en `.github/workflows/ci.yml` que ejecuta: compile → test → docker build en cada push a `develop`.
- [ ] Repositorio actualizado antes de la fecha de cierre (Semana 9).
- [ ] Archivo PDF nombrado: `PFC_Entrega1B_[NombreEquipo].pdf`.
- [ ] Historial de Git incluido en el informe como captura de `git log --oneline --graph --all`.

---

## 12. Requisitos del Informe Técnico

### 12.1 Estructura del Informe (20-35 páginas sin anexos)

| Sección | Contenido obligatorio |
|---|---|
| 2.1 Portada | Universidad, facultad, carrera, asignatura, título, entrega 1B, integrantes con cédula, docente, fecha, URL repo, rama `entrega-1b` |
| 2.2 Resumen ejecutivo | 200-250 palabras; qué sistema, qué problema, tecnologías, resultados concretos, métricas (tiempo token, tiempo login, cobertura pruebas); tercera persona, tiempo pasado, sin listas |
| 2.3 Introducción | Contexto con datos cuantitativos; 3-4 objetivos específicos en verbos de acción; alcance; estructura del documento |
| 2.4 Marco teórico | Autenticación stateless vs stateful (RFC 7519, RFC 6749); Spring Security 6 y filtros; ORM y JPA (citar Walls 2022); Flyway; OWASP Top 10 A07 |
| 2.5 Diseño | C4 Nivel 2 actualizado; diagrama de clases UML; diagrama de secuencia JWT (11 pasos); script Flyway V1; DER; diccionario de datos |
| 2.6 Implementación | Árbol de directorios; `application.yml` completo; JwtAuthFilter; tabla de endpoints; payload JWT; interceptor Angular |
| 2.7 Pruebas | Tabla de 5+ pruebas JUnit; capturas Postman (registro, login, CRUD, paginación, Swagger); métricas de rendimiento reales |
| 2.8 Seguridad | Tabla con 7 controles OWASP con evidencia |
| 2.9 Docker | `docker-compose.yml` completo; README con 5 pasos |
| 2.10 Historial Git | Captura de `git log --oneline --graph --all` |
| 2.11 Conclusiones | 150-200 palabras por objetivo; desafío técnico; proyección Entrega 2 |
| 2.12 Referencias | Mínimo 8 referencias APA 7ma ed.; mínimo 3 artículos indexados (IEEE, ACM, Scopus) |

### 12.2 Referencias Bibliográficas Obligatorias (mínimo)

1. Walls, C. (2022). *Spring Boot in action* (2da ed.). Manning Publications.
2. Spilca, L. (2024). *Spring Security in action* (2da ed.). Manning Publications.
3. Jones, M., Bradley, J., & Sakimura, N. (2015). *JSON Web Token (JWT)* (RFC 7519). IETF.
4. OWASP Foundation. (2021). *OWASP Top 10: 2021*.
5. Fielding, R. T. (2000). *Architectural styles and the design of network-based software architectures* [Tesis doctoral].
6. PostgreSQL Global Development Group. (2024). *PostgreSQL 16 documentation*.
7. Spring Framework Team. (2024). *Spring Security reference documentation*.
8. Al menos **3 artículos indexados** (IEEE, ACM o Scopus) sobre seguridad JWT / aplicaciones web.

---

## 13. Casos de Prueba Obligatorios

### 13.1 Matriz de Pruebas JUnit 5

| ID | Caso de prueba | Método | Entrada | Resultado esperado | Prioridad |
|---|---|---|---|---|---|
| T-01 | Login con credenciales correctas | `loginExitoso()` | email+password válidos | 200 OK + JWT | CRÍTICA |
| T-02 | Login con contraseña incorrecta | `loginClaveIncorrecta()` | password erróneo | 401 Unauthorized | CRÍTICA |
| T-03 | Registro con email ya existente | `registroEmailDuplicado()` | email ya registrado | 409 Conflict | ALTA |
| T-04 | Acceso a endpoint protegido sin token | `accesoSinToken()` | sin Authorization header | 401 Unauthorized | CRÍTICA |
| T-05 | Acceso a endpoint protegido con token válido | `accesoConTokenValido()` | Bearer token válido | 200 OK | CRÍTICA |
| T-06 | Crear entidad con datos válidos | `crearEntidadValida()` | JSON completo + JWT | 201 Created | ALTA |
| T-07 | Crear entidad con datos inválidos | `crearEntidadInvalida()` | campo requerido faltante | 400 Bad Request | ALTA |
| T-08 | Buscar entidad por ID inexistente | `buscarIdInexistente()` | ID = 99999 | 404 Not Found | MEDIA |
| T-09 | Paginación de entidades | `listarConPaginacion()` | `?page=0&size=5` | 200 OK + Page structure | ALTA |
| T-10 | Soft delete de entidad | `softDeleteEntidad()` | DELETE + JWT | 204; `activo=false` en BD | ALTA |

> **Mínimo 5 pruebas en verde** son obligatorias. La captura de `mvn test` debe mostrarse en el informe.

---

### 13.2 Pruebas de Integración con Postman

| # | Caso | Pasos | Entrada | Resultado esperado |
|---|---|---|---|---|
| P-01 | Registro de usuario nuevo | Enviar POST a `/api/auth/registro` | `{nombre, email, password}` válidos | 201 + usuario sin hash |
| P-02 | Login exitoso | Enviar POST a `/api/auth/login` | `{email, password}` correctos | 200 + `accessToken` visible |
| P-03 | Acceso protegido con Bearer token | GET endpoint protegido con token | `Authorization: Bearer [token]` | 200 OK |
| P-04 | CRUD completo | POST, GET lista, GET por ID, PUT, DELETE | JSON válido + JWT | Códigos 201, 200, 200, 200, 204 |
| P-05 | Paginación | GET con `?page=0&size=10` | JWT | Objeto Page con `content`, `totalElements` |
| P-06 | Logout y reutilización de token | Logout → reintentar con mismo token | JWT revocado | 401 en el reintento |
| P-07 | Swagger UI operativo | Navegar a `/api/swagger-ui.html` | — | Interfaz Swagger con endpoints |
| P-08 | Health check Docker | GET `/actuator/health` | — | `{"status":"UP"}` |

---

## 14. Casos de Prueba de Seguridad

| ID | Prueba | Procedimiento | Resultado esperado |
|---|---|---|---|
| SEC-01 | SQL Injection en login | `email: "' OR 1=1 --"` en POST /api/auth/login | 401 o 400; nunca 200 |
| SEC-02 | JWT con firma modificada | Alterar el signature del token en base64 | 401 Unauthorized |
| SEC-03 | JWT expirado | Usar token con `exp` en el pasado | 401 Unauthorized |
| SEC-04 | JWT de otro usuario | Usar token válido de usuario A para acceder a datos de B | 403 Forbidden |
| SEC-05 | Endpoint sin token | GET endpoint protegido sin `Authorization` header | 401 Unauthorized |
| SEC-06 | Contraseña en texto plano | SELECT `password_hash` FROM usuarios | Valor inicia con `$2a$12$` |
| SEC-07 | Headers HTTP de seguridad | `curl -I http://localhost:8080/api/auth/login` | Presencia de `X-Content-Type-Options`, `X-Frame-Options` |
| SEC-08 | CORS desde dominio no permitido | Solicitud con `Origin: http://evil.com` | 403 o sin header `Access-Control-Allow-Origin` |
| SEC-09 | Refresh Token tras logout | Usar refreshToken de sesión cerrada | 401 Unauthorized |
| SEC-10 | Token en localStorage | Abrir DevTools → Application → Local Storage | Campo `token` o `accessToken`: vacío |
| SEC-11 | Blacklist Redis tras logout | `redis-cli KEYS jti:*` tras logout | JTI presente con TTL |
| SEC-12 | Rol insuficiente | Usuario `ROLE_USER` accede a endpoint `ROLE_ADMIN` | 403 Forbidden |
| SEC-13 | Inyección en parámetros CRUD | `?id=1; DROP TABLE usuarios` | 400 Bad Request; tabla intacta |

---

## 15. Lista de Verificación Final (Checklist)

### 📄 Informe Técnico

- [ ] El informe tiene portada con integrantes, cédulas, URL del repositorio y rama `entrega-1b`.
- [ ] El resumen ejecutivo tiene 200-250 palabras con al menos una métrica numérica.
- [ ] El resumen está en tercera persona, tiempo pasado, sin listas.
- [ ] La introducción tiene contexto con datos cuantitativos del problema.
- [ ] La introducción lista 3-4 objetivos específicos con verbos de acción.
- [ ] El marco teórico cita RFC 7519, RFC 6749 y OWASP Top 10 2021.
- [ ] El marco teórico tiene mínimo 8 referencias APA 7ma edición.
- [ ] Al menos 3 referencias son artículos indexados (IEEE, ACM o Scopus).
- [ ] El diagrama C4 Nivel 2 está actualizado mostrando backend, frontend, PostgreSQL y Redis.
- [ ] El diagrama C4 indica tecnología, protocolo y puerto de cada contenedor.
- [ ] El diagrama de clases UML muestra todos los paquetes y relaciones.
- [ ] El diagrama de secuencia del flujo JWT tiene los 11 pasos descritos.
- [ ] El DER está incluido generado desde pgAdmin 4 o DBeaver.
- [ ] El diccionario de datos describe todas las columnas de todas las tablas.
- [ ] El ADR-003 justifica el uso de Redis para la blacklist JWT.
- [ ] El árbol de directorios del monorepo está documentado.
- [ ] El `application.yml` completo está incluido en el informe.
- [ ] Se describe el funcionamiento del `JwtAuthenticationFilter`.
- [ ] La tabla de endpoints lista los 10+ endpoints con método, URL, auth y descripción.
- [ ] El payload JWT está documentado con los 6 claims obligatorios.
- [ ] Se describe el `JwtInterceptor` de Angular.
- [ ] Las conclusiones tienen 150-200 palabras por objetivo.
- [ ] Las conclusiones proyectan lo que se construirá en la Entrega 2.
- [ ] El archivo PDF se llama `PFC_Entrega1B_[NombreEquipo].pdf`.
- [ ] El PDF fue adjuntado en la Tarea del SGA antes del cierre de la Semana 9.

### 🔐 Autenticación y Seguridad

- [ ] `POST /api/auth/registro` funciona y retorna 201.
- [ ] `POST /api/auth/login` funciona y retorna accessToken + refreshToken.
- [ ] `POST /api/auth/logout` agrega JTI a Redis y retorna 204.
- [ ] `POST /api/auth/refresh` emite nuevo accessToken sin re-autenticación.
- [ ] `GET /api/usuarios/me` retorna perfil del usuario autenticado.
- [ ] La contraseña se almacena con BCrypt costo 12 (`$2a$12$...`).
- [ ] El JWT tiene los claims: `sub`, `email`, `rol`, `iat`, `exp`, `jti`.
- [ ] El `jti` es un UUID único por token.
- [ ] El accessToken expira en 1 hora.
- [ ] El refreshToken expira en 7 días.
- [ ] El accessToken NO está almacenado en `localStorage`.
- [ ] El refreshToken está en cookie con flag `HttpOnly`.
- [ ] CORS está configurado explícitamente (no con `*`).
- [ ] Headers HTTP de seguridad están activos (`X-Content-Type-Options`, etc.).
- [ ] Roles `ROLE_USER` y `ROLE_ADMIN` están implementados.
- [ ] `@PreAuthorize` protege endpoints por rol.
- [ ] La blacklist Redis invalida tokens revocados correctamente.

### 🗃️ Base de Datos y Flyway

- [ ] `V1__schema_inicial.sql` es ejecutable en PostgreSQL 16 sin errores.
- [ ] La tabla `usuarios` tiene todos los campos requeridos.
- [ ] El índice único `idx_usuarios_email` está creado.
- [ ] El constraint `chk_rol` valida los valores del rol.
- [ ] Los timestamps usan `TIMESTAMPTZ`.
- [ ] El soft delete está implementado (`activo BOOLEAN DEFAULT TRUE`).
- [ ] El trigger `set_actualizado_en` funciona correctamente.
- [ ] `ddl-auto: validate` está configurado (Flyway gestiona el DDL).
- [ ] Las tablas del dominio del PFC tienen FK a `usuarios` si aplica.
- [ ] Las columnas usan convención `snake_case`.

### ⚙️ Backend Spring Boot

- [ ] El proyecto compila sin errores con `./mvnw package`.
- [ ] La arquitectura por capas está respetada (entity → repository → service → controller).
- [ ] Los DTOs están separados de las entidades JPA.
- [ ] La inyección de dependencias es por constructor.
- [ ] Lombok está en uso (`@Data`, `@Builder`, `@RequiredArgsConstructor`).
- [ ] `@Valid` está en los controladores para validar DTOs.
- [ ] `@NotBlank`, `@Email`, `@Size` están en los DTOs.
- [ ] `open-in-view: false` está configurado.
- [ ] `show-sql: false` en producción (o `true` solo en dev local).
- [ ] Actuator expone `health`, `info`, `metrics`.
- [ ] Springdoc OpenAPI genera documentación automáticamente.

### 🎨 Frontend Angular

- [ ] Angular 17+ está configurado.
- [ ] El `JwtInterceptor` agrega `Authorization: Bearer [token]` en solicitudes protegidas.
- [ ] El accessToken está en memoria (no en localStorage).
- [ ] El `AuthGuard` protege las rutas de Angular.
- [ ] Los errores 401 redirigen al login.
- [ ] Angular Material está en uso.
- [ ] `environment.ts` y `environment.prod.ts` tienen la URL de la API.

### 🐳 Docker e Infraestructura

- [ ] `docker compose up --build -d` levanta todos los servicios sin errores.
- [ ] El servicio `postgres` tiene healthcheck.
- [ ] El servicio `redis` tiene healthcheck.
- [ ] El servicio `backend` tiene `depends_on: {postgres: {condition: service_healthy}, redis: {condition: service_healthy}}`.
- [ ] El servicio `frontend` sirve la aplicación Angular con Nginx.
- [ ] `docker compose ps` muestra todos los servicios en estado `healthy`.
- [ ] Las variables de entorno están en `.env` (no hardcodeadas).
- [ ] `.env.example` está en el repositorio.
- [ ] `.env` está en `.gitignore`.
- [ ] `GET /actuator/health` retorna `{"status":"UP"}`.
- [ ] Frontend accesible en `http://localhost:4200`.
- [ ] Swagger UI accesible en `http://localhost:8080/api/swagger-ui.html`.

### 🧪 Pruebas

- [ ] Mínimo 5 pruebas JUnit 5 en verde.
- [ ] La captura de `mvn test` muestra todos los tests en verde.
- [ ] Las capturas de Postman muestran registro exitoso.
- [ ] Las capturas muestran login exitoso con token visible.
- [ ] Las capturas muestran CRUD completo (POST, GET lista, GET ID, PUT, DELETE).
- [ ] La captura muestra paginación (`page`, `size`).
- [ ] La captura muestra Swagger UI con endpoints documentados.
- [ ] La tabla de métricas tiene tiempos reales medidos (no inventados).

### 📁 Repositorio Git

- [ ] Commits de todos los integrantes del equipo.
- [ ] Mensajes en formato Conventional Commits (`feat:`, `fix:`, `test:`, etc.).
- [ ] Rama `feature/autenticacion-jwt` mergeada a `develop`.
- [ ] Rama `feature/crud-[entidad]` mergeada a `develop`.
- [ ] Tag `v0.1.0-entrega-1b` creado.
- [ ] GitHub Actions pipeline configurado en `.github/workflows/ci.yml`.
- [ ] Repositorio actualizado antes del cierre de la Semana 9.
- [ ] README.md con instrucciones en 5 pasos ejecutables.

---

## 16. Riesgos de Incumplimiento

| Requisito | Consecuencia técnica | Consecuencia funcional | Impacto en nota | Criticidad |
|---|---|---|---|---|
| Sistema no arranca con `docker compose up` | El docente no puede verificar ningún módulo en tiempo real | Criterios funcionales irrecuperables | 0 en autenticación (25%) + 0 en CRUD (20%) + 0 en Docker (10%) | **CRÍTICA — ANULACIÓN** |
| Contraseña en texto plano | Vulnerabilidad de seguridad total (OWASP A02) | Cualquier acceso a la BD expone todas las contraseñas | 0 en seguridad (15%) + penalización en autenticación | **CRÍTICA — ANULACIÓN** |
| JWT en localStorage | Vulnerable a XSS; cualquier script malicioso roba todos los tokens | Sesiones robadas sin detección posible | Penalización grave en seguridad | **ALTA** |
| SQL concatenado | SQL Injection potencial (OWASP A03) | Datos de todos los usuarios comprometibles | Penalización grave en CRUD y seguridad | **ALTA** |
| Sin Redis / Blacklist | Tokens revocados siguen siendo válidos; logout no funciona realmente | Sesiones no pueden invalidarse tras logout o robo | Penalización en Docker y autenticación | **ALTA** |
| Sin pruebas JUnit (< 3) | Sin validación automatizada del comportamiento del sistema | Bugs no detectados llegan a producción | 0 en pruebas automatizadas (15%) | **ALTA** |
| Sin Flyway o DDL manual | El esquema no es reproducible; el docente puede tener diferente BD | El sistema falla en entornos limpios | Penalización grave en CRUD | **ALTA** |
| Sin Swagger UI | El docente no puede explorar la API sin herramientas externas | Los endpoints no están autodocumentados | Penalización en informe y API | **MEDIA** |
| CRUD incompleto | Solo parte del módulo de datos funciona | El sistema no cumple requisitos del módulo de datos | Penalización proporcional en CRUD (20%) | **ALTA** |
| Informe sin marco teórico | Sin sustento académico de las decisiones técnicas | El informe no cumple estándares universitarios | Reducción en criterio de informe (10%) | **MEDIA** |
| Sin referencias APA (< 8) | No se cumplen los estándares académicos de citación | El informe pierde credibilidad académica | Reducción en criterio de informe | **MEDIA** |
| Commits solo de un integrante | El trabajo en equipo no es verificable | Evaluación individual inequitativa | Penalización en repositorio Git (5%) | **MEDIA** |
| Sin tag de entrega | No se puede identificar el commit exacto de entrega | El docente no sabe cuál versión evaluar | Penalización en repositorio Git | **MEDIA** |
| Sin GitHub Actions | Sin CI/CD pipeline | No hay verificación automática de compilación y pruebas | Penalización en repositorio Git | **BAJA** |

---

## 17. Errores Críticos que Provocan Pérdida de Puntos

| Error | Impacto | Cómo detectarlo | Cómo evitarlo |
|---|---|---|---|
| **Sistema no arranca con `docker compose up`** | **ANULACIÓN** de criterios funcionales (autenticación 25% + CRUD 20% + Docker 10% = 55%) | El docente ejecuta `docker compose up --build` en un entorno limpio | Probar en una máquina diferente con `docker compose down -v && docker compose up --build` antes de entregar |
| **Contraseñas en texto plano** | **ANULACIÓN** del criterio de seguridad completo (15%) + penalización en autenticación | `SELECT email, password_hash FROM usuarios LIMIT 5;` | Usar siempre `BCryptPasswordEncoder(12)`; nunca `passwordEncoder.encode()` con otro encoder |
| **JWT en localStorage** | Penalización grave en seguridad | DevTools → Application → Local Storage → buscar `token` o `jwt` | Almacenar accessToken en variable de `AuthService` en memoria; refreshToken en cookie HttpOnly |
| **SQL concatenado** | Penalización grave en CRUD y seguridad | Buscar en código Java: `String sql = "..." + variable` o `"WHERE id = " + id` | Usar siempre Spring Data JPA con métodos derivados o `@Query` con `:param` |
| **Flyway incorrectamente configurado** | El esquema no se aplica al iniciar; la app falla al arrancar | Revisar logs al iniciar el backend; verificar `spring.flyway.enabled: true` | Ejecutar `docker compose up` en entorno limpio y verificar que las tablas se crean |
| **Sin pruebas JUnit** | 0 en criterio de pruebas (15%) | `./mvnw test` no produce output de pruebas | Escribir mínimo 5 pruebas que cubran los casos de la tabla de pruebas obligatorias |
| **Sin Redis en Docker Compose** | Logout no funciona; blacklist JWT inoperativa | `docker compose ps` no muestra servicio `redis` | Incluir el servicio `redis:7` en `docker-compose.yml` con healthcheck |
| **Swagger UI inaccesible** | Penalización en documentación y criterio de informe | `GET http://localhost:8080/api/swagger-ui.html` retorna 404 | Verificar dependencia `springdoc-openapi-starter-webmvc-ui` en `pom.xml` y configuración en `application.yml` |
| **CRUD incompleto** | Penalización proporcional en el 20% del CRUD | Probar los 5 métodos: POST, GET lista, GET ID, PUT, DELETE | Implementar todos los métodos en el controlador y servicio antes de entregar |
| **Sin paginación en GET lista** | Penalización en CRUD | `GET /api/[entidad]?page=0&size=10` retorna lista sin estructura Page | Usar `Pageable` en el repositorio y `Page<T>` como tipo de retorno |
| **Informe sin resumen ejecutivo** | Penalización en criterio de informe | Revisar si la sección 2.2 existe y tiene 200-250 palabras con métricas | Escribir el resumen al final, cuando ya se tienen las métricas reales del sistema |
| **Commits de un solo integrante** | Penalización en criterio de repositorio (5%) | `git log --all --oneline --author="[nombre]"` para cada integrante | Distribuir el trabajo y hacer commits desde las cuentas de cada integrante del equipo |

---

## 18. Matriz de Cumplimiento

| Requisito | Sección del informe | Archivo de código | Prueba que lo valida | Estado | Observaciones |
|---|---|---|---|---|---|
| Registro de usuarios | 2.6.4 + Postman P-01 | `AuthController.java`, `AuthService.java` | T-03, P-01 | [ ] Pendiente | |
| Login JWT | 2.6.4 + Postman P-02 | `AuthController.java`, `JwtService.java` | T-01, T-02, P-02 | [ ] Pendiente | |
| Logout + Redis blacklist | 2.6.3 + 2.8 | `AuthController.java`, `RedisService.java` | P-06, SEC-09 | [ ] Pendiente | |
| Refresh Token | 2.6.4 | `AuthController.java`, `JwtService.java` | — | [ ] Pendiente | |
| Perfil `/usuarios/me` | 2.6.4 | `UsuarioController.java` | T-05 | [ ] Pendiente | |
| CRUD completo | 2.6.4 + Postman P-04 | `[Entidad]Controller.java`, `[Entidad]Service.java` | T-06 a T-10, P-04 | [ ] Pendiente | |
| Paginación | 2.6.4 + Postman P-05 | `[Entidad]Repository.java` | T-09, P-05 | [ ] Pendiente | |
| Swagger UI | 2.6.4 + Postman P-07 | `application.yml` | P-07 | [ ] Pendiente | |
| BCrypt costo 12 | 2.8 | `SecurityConfig.java` | SEC-06 | [ ] Pendiente | |
| JWT estructura correcta | 2.6.5 | `JwtService.java` | T-01 | [ ] Pendiente | |
| Blacklist Redis | 2.8 | `JwtAuthFilter.java`, `RedisService.java` | SEC-11 | [ ] Pendiente | |
| CORS configurado | 2.8 | `SecurityConfig.java` | SEC-08 | [ ] Pendiente | |
| Headers HTTP | 2.8 | `SecurityConfig.java` | SEC-07 | [ ] Pendiente | |
| Roles `@PreAuthorize` | 2.8 | `[Entidad]Controller.java` | SEC-12 | [ ] Pendiente | |
| Bean Validation | 2.6.4 | DTOs | T-07 | [ ] Pendiente | |
| Flyway `V1__schema_inicial.sql` | 2.5.4 | `database/migrations/V1__schema_inicial.sql` | Docker compose up | [ ] Pendiente | |
| Trigger `actualizado_en` | 2.5.4 | `V1__schema_inicial.sql` | UPDATE en BD | [ ] Pendiente | |
| Soft delete | 2.6.4 | `[Entidad]Service.java` | T-10 | [ ] Pendiente | |
| JwtInterceptor Angular | 2.6.6 | `jwt.interceptor.ts` | P-03 | [ ] Pendiente | |
| AuthGuard Angular | 2.6.6 | `auth.guard.ts` | Navegar a ruta protegida | [ ] Pendiente | |
| 5+ pruebas JUnit en verde | 2.7.1 | `src/test/java/` | `./mvnw test` | [ ] Pendiente | |
| docker compose up funcional | 2.9 | `docker-compose.yml` | Ejecución en entorno limpio | [ ] Pendiente | |
| Health checks en compose | 2.9 | `docker-compose.yml` | `docker compose ps` | [ ] Pendiente | |
| README con 5 pasos | 2.9 | `README.md` | Ejecutar pasos manualmente | [ ] Pendiente | |
| Conventional Commits | 2.10 | `.git/` | `git log --oneline` | [ ] Pendiente | |
| Tag `v0.1.0-entrega-1b` | 2.10 | `.git/refs/tags/` | `git tag -l` | [ ] Pendiente | |
| GitHub Actions CI | 2.10 | `.github/workflows/ci.yml` | Push a `develop` | [ ] Pendiente | |
| 8+ referencias APA | 2.12 | Informe PDF | Revisión manual | [ ] Pendiente | |
| Diagrama C4 Nivel 2 | 2.5.1 | `docs/` o informe PDF | Revisión manual | [ ] Pendiente | |
| Diagrama de secuencia JWT | 2.5.3 | `docs/` o informe PDF | Revisión manual | [ ] Pendiente | |
| DER desde pgAdmin/DBeaver | 2.5 | Informe PDF | Revisión manual | [ ] Pendiente | |
| Métricas de rendimiento reales | 2.7.3 | Informe PDF | Revisión manual | [ ] Pendiente | |

---

## 19. Criterios de Aprobación

### 19.1 Rúbrica Oficial de Evaluación

| Criterio | Peso | Qué debe cumplirse para puntuación máxima | Errores que reducen la nota |
|---|---|---|---|
| **Autenticación JWT funcional** | 25% | Login, logout y refresh funcionan correctamente; token tiene estructura correcta (`sub`, `email`, `rol`, `iat`, `exp`, `jti`); blacklist Redis operativa; accessToken en memoria; refreshToken en cookie HttpOnly | Solo login funciona (sin logout ni refresh); token en localStorage; sin blacklist Redis |
| **CRUD con Spring Data JPA** | 20% | CRUD completo (POST, GET lista paginada, GET ID, PUT, DELETE) funcional; Flyway gestiona el esquema (`V1__schema_inicial.sql` ejecutable); cero concatenación SQL; soft delete implementado | CRUD parcial (falta algún método); DDL manual sin Flyway; SQL concatenado; sin paginación |
| **Seguridad OWASP** | 15% | BCrypt costo 12; cabeceras HTTP de seguridad; CORS correctamente configurado (no `*`); roles con `@PreAuthorize`; sin SQL injection | Solo BCrypt sin el resto de controles; sin cabeceras ni CORS; roles no implementados |
| **Pruebas automatizadas** | 15% | 5+ pruebas JUnit en verde; captura de `mvn test` incluida en el informe; pruebas cubren login exitoso, fallido, email duplicado, acceso sin token, acceso con token | Menos de 3 pruebas; pruebas fallan; captura no incluida |
| **Docker Compose** | 10% | `docker compose up --build -d` levanta los 4 servicios; health checks definidos; `docker compose ps` muestra todos `healthy`; README con 5 pasos ejecutables | Solo backend funciona; sin servicio Redis; sin health checks; sin README funcional |
| **Informe técnico** | 10% | Resumen ejecutivo con métricas reales; marco teórico con 8+ referencias APA (3+ indexadas); diagrama C4 actualizado; diagrama de secuencia JWT con 11 pasos; tabla de seguridad OWASP; métricas de rendimiento reales | Sin marco teórico; referencias insuficientes; diagramas desactualizados; resumen sin métricas |
| **Repositorio Git** | 5% | Commits de **todos** los integrantes; mensajes Conventional Commits; tag `v0.1.0-entrega-1b`; ramas feature mergeadas a develop; GitHub Actions configurado | Solo un integrante con commits; sin tag; sin Conventional Commits |
| **TOTAL** | **100%** | Todos los criterios en nivel alto | Ver sección 17 para errores de anulación |

### 19.2 Condiciones de Anulación (Nota = 0 en criterios afectados)

> Estos errores anulan el criterio correspondiente independientemente de la calidad del resto del proyecto:

1. **[ANULACIÓN TOTAL FUNCIONAL]** El sistema no arranca con `docker compose up`. Equivale a 0 en autenticación (25%) + CRUD (20%) + Docker (10%).

2. **[ANULACIÓN SEGURIDAD]** Contraseñas almacenadas en texto plano. Anula completamente el criterio de seguridad (15%) con penalización adicional en autenticación.

### 19.3 Penalizaciones Graves (Reducción significativa de puntos)

1. **[PENALIZACIÓN GRAVE]** Token JWT almacenado en `localStorage`. Reduce el criterio de seguridad significativamente. Solo se acepta con alternativa documentada y evidencia.

2. **[PENALIZACIÓN GRAVE]** Consultas SQL concatenadas (`String sql = "..." + variable`). Reduce los criterios de seguridad y CRUD.

---

## Apéndice: application.yml de Referencia

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/pfc_db}
    username: ${DB_USER:pfc_user}
    password: ${DB_PASSWORD:changeme}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate          # Flyway gestiona el DDL
    show-sql: false               # true solo en desarrollo local
    open-in-view: false           # evitar LazyInitializationException
    properties:
      hibernate:
        format_sql: true
        default_schema: public
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

security:
  jwt:
    secret: ${JWT_SECRET}         # min. 256 bits (openssl rand -hex 32)
    expiration-ms: 3600000        # 1 hora
    refresh-expiration-ms: 604800000  # 7 días

springdoc:
  api-docs:
    path: /api/docs
  swagger-ui:
    path: /api/swagger-ui.html
    operationsSorter: method

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

*Documento generado a partir de la Guía Académica PFC Entrega 1B — UTEQ Ingeniería de Software 2025-2026.*  
*Última revisión: Semana 9 · Sumativa #8 del Segundo Corte.*
