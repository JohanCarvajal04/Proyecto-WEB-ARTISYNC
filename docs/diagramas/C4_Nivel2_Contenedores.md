# Diagrama C4 Nivel 2: Contenedores (Container Diagram) - Artisync PFC

Este documento detalla el **Nivel 2 (Contenedores)** del modelo **C4** para la plataforma **Artisync**.

En este nivel se abre la caja negra del sistema para mostrar los **contenedores de software autónomos y desplegables** (servicios, bases de datos, sistemas de caché, interfaces web) que lo componen, junto con sus **tecnologías subyacentes**, **responsabilidades principales** e interacciones bajo protocolos específicos.

---

## 1. Catálogo de Contenedores y Especificaciones Técnicas

### 1.1 Contenedores Internos (Dentro del Límite de Artisync)
| Contenedor | Identificador Docker | Stack Tecnológico | Responsabilidad Principal |
| :--- | :--- | :--- | :--- |
| **Aplicación Web SPA (Frontend)** | `pfc_frontend` <br> *(Puerto 4200 / 80)* | **Angular 22**, TypeScript, HTML5/SCSS, RxJS, Nginx | Proporciona la interfaz de usuario web responsiva de página única (SPA). Permite a Clientes, Artistas y Administradores autenticarse (2FA, JWT), explorar el catálogo dinámico, contratar servicios, gestionar hitos y tickets, y participar en la comunidad social. |
| **Servidor API REST (Backend)** | `pfc_backend` <br> *(Puerto 8080)* | **Java 21**, **Spring Boot 4.1.0**, Spring Security 6, Spring Data JPA / Hibernate, Flyway, Maven | Núcleo transaccional de la plataforma. Expone los endpoints RESTful documentados con OpenAPI/Swagger (`/api/docs`). Orquesta los 7 módulos funcionales (`seguridad`, `perfil`, `catalogo`, `pedido`, `legal`, `comunicacion`, `social`), gestiona transacciones ACID (`@Transactional`) y valida la seguridad sin estado con tokens JWT y consulta a Redis. |
| **Base de Datos Relacional** | `pfc_postgres` <br> *(Puerto 5432)* | **PostgreSQL 16** <br> *(Volumen: `pfc_postgres_data`)* | Almacén principal persistente y transaccional del sistema. Contiene los esquemas y tablas normalizadas (`usuarios`, `roles`, `pedidos`, `flujo_trabajos`, `pago_garantias`, etc.) gestionados y versionados estrictamente por migraciones de **Flyway** (`db/migration`). |
| **Almacén en Memoria / Caché** | `pfc_redis` <br> *(Puerto 6379)* | **Redis 7 Alpine** <br> *(Estructuras clave-valor in-memory)* | Almacén de ultra-baja latencia O(1). Mantiene la **Blacklist de tokens JWT** (`jti:<token>` con TTL según tiempo restante de expiración) y sesiones revocadas para permitir *logout* inmediato y bloqueo ante compromisos (`SessionRevocationService`). Implementa también el patrón **Cache-Aside** para el catálogo de servicios frecuentemente consultado. |

---

### 1.2 Sistemas y Contenedores Externos Integrados
| Sistema Externo | Tecnología / Protocolo | Responsabilidad e Interacción |
| :--- | :--- | :--- |
| **Servidor SMTP Transaccional** | **SMTP / TLS** (Puerto 587) <br> *Spring Boot Starter Mail (`EmailService`)* | Envío asíncrono (`@Async`) de plantillas HTML renderizadas con **Thymeleaf**: verificación de cuenta, códigos de autenticación 2FA, reseteo de contraseñas y alertas de hitos de pedidos. |
| **Pasarela PayPal (API v2 & Webhooks)** | **HTTPS / REST JSON** <br> *(PayPal Orders v2 / `PayPalConfig`)* | Procesamiento de pagos seguros. Gestión de retención de fondos en garantía (*Escrow* en `PagoGarantia`), capturas al aprobar entregables y recepción de Webhooks asíncronos para actualizar estados transaccionales en tiempo real. |
| **Almacenamiento Cloud / CDN** | **HTTPS / REST API** <br> *(Azure Blob Storage)* | Almacenamiento externo escalable para imágenes multimedia de portafolio artístico, banners de perfiles, archivos adjuntos en el chat y entregables creativos de alta capacidad (`.zip`, `.psd`, `.mp4`). |

---

## 2. Código DSL para Structurizr Lite

El modelo formal en DSL de Structurizr (contexto y contenedores) ya **no está embebido en este Markdown**: vive como archivo fuente independiente en [`docs/diagramas/workspace.dsl`](workspace.dsl) (vista `container`), listo para cargarse directamente en Structurizr Lite (`docker run -p 8080:8080 -v ./docs/diagramas:/usr/local/structurizr structurizr/lite`). El bloque siguiente es una copia de solo lectura, a título ilustrativo, de esa misma fuente:

```groovy
workspace "Artisync - Plataforma para Artistas y Creadores de Contenido" "Modelo C4 (Structurizr DSL): Nivel 1 Contexto y Nivel 2 Contenedores." {
    model {
        cliente = person "Cliente / Buscador de Talento" "..."
        artista = person "Artista / Creador de Contenido" "..."
        admin = person "Administrador de la Plataforma" "..."

        smtpSystem = softwareSystem "Servicio de Correo Transaccional" "..." "External System"
        paypalSystem = softwareSystem "Pasarela de Pagos (PayPal API v2)" "..." "External System"
        cloudStorageSystem = softwareSystem "Almacenamiento Cloud / CDN" "..." "External System"

        artisyncSystem = softwareSystem "Plataforma Artisync (PFC)" "..." "System" {
            webApp = container "Aplicación Web SPA (Frontend)" "..." "Angular 22 / TypeScript / Nginx" "WebBrowser"
            apiServer = container "Servidor API REST (Backend)" "..." "Java 21 / Spring Boot 4.1.0 / Spring Security 6" "Backend"
            db = container "Base de Datos Relacional" "..." "PostgreSQL 16" "Database"
            cache = container "Almacén en Memoria / Caché" "..." "Redis 7 Alpine" "Cache"
        }
        // ... interacciones de contexto y de contenedor: ver workspace.dsl
    }

    views {
        systemContext artisyncSystem "C4_Context_Artisync" { include *; autoLayout topBottom }
        container artisyncSystem "C4_Containers_Artisync" { include *; autoLayout topBottom }
        styles { /* ver workspace.dsl para la definición completa de estilos */ }
    }
}
```

**Fuente completa y autoritativa:** [`docs/diagramas/workspace.dsl`](workspace.dsl).

---

## 3. Código C4-PlantUML

Código PlantUML listo para exportar a PNG/SVG de alta resolución desde VS Code o `draw.io`:

```plantuml
@startuml C4_Nivel2_Contenedores_Artisync
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

LAYOUT_WITH_LEGEND()
LAYOUT_TOP_DOWN()

title Diagrama de Contenedores (Nivel 2) - Plataforma Artisync (PFC)

Person(cliente, "Cliente / Buscador de Talento", "Explora portafolios, solicita cotizaciones, realiza pagos en garantía y aprueba hitos.")
Person(artista, "Artista / Creador de Contenido", "Publica catálogo y tarifas, gestiona pedidos activos y sube entregables creativos.")
Person(admin, "Administrador de la Plataforma", "Modera catálogos, gestiona usuarios/roles y arbitra disputas en tickets de revisión.")

System_Boundary(artisync_boundary, "Plataforma Artisync (PFC)") {
    Container(frontend, "Aplicación Web SPA", "Angular 22, TypeScript, Nginx", "Interfaz de usuario responsiva. Gestiona flujos de navegación, autenticación, formularios, carga de archivos y visualización del portafolio.")
    
    Container(backend, "Servidor API REST", "Java 21, Spring Boot 4.1.0, Spring Security 6, Hibernate", "Orquestador central. Lógica de negocio de los 7 módulos, validación de reglas, autenticación sin estado JWT y gestión de transacciones.")
    
    ContainerDb(db, "Base de Datos Relacional", "PostgreSQL 16 (pfc_postgres:5432)", "Almacén de datos persistente y relacional (ACID). Contiene usuarios, roles, pedidos, contratos y migraciones de Flyway.")
    
    ContainerDb(cache, "Caché en Memoria & Blacklist", "Redis 7 Alpine (pfc_redis:6379)", "Almacén clave-valor O(1). Gestiona la lista negra de tokens JWT (JTI) y sesiones revocadas con TTL, más caché transaccional (Cache-Aside).")
}

System_Ext(smtp, "Servicio de Correo Transaccional", "SMTP TLS (Puerto 587) - Envio de correos 2FA y plantillas de recuperación.")
System_Ext(paypal, "Pasarela de Pagos (PayPal API v2)", "HTTPS / REST JSON - Procesamiento de órdenes y retención de depósitos Escrow.")
System_Ext(storage, "Almacenamiento Cloud / CDN", "HTTPS / REST Azure - Persistencia de archivos pesados multimedia del portafolio y entregables.")

Rel(cliente, frontend, "Explora catálogo, cotiza, paga en garantía y revisa hitos", "HTTPS / Puerto 4200/443")
Rel(artista, frontend, "Publica servicios, sube avances de pedidos y cobra", "HTTPS / Puerto 4200/443")
Rel(admin, frontend, "Gestiona roles, modera catálogos y resuelve disputas", "HTTPS / Puerto 4200/443")

Rel(frontend, backend, "Consume peticiones REST transaccionales y autenticadas", "HTTPS / REST JSON Bearer JWT (Puerto 8080)")

Rel(backend, db, "Lee/Escribe entidades JPA transaccionales y ejecuta Flyway", "JDBC / TCP (Puerto 5432)")
Rel(backend, cache, "Verifica JTI en blacklist (O(1)) y gestiona caché de catálogos", "Redis Protocol RESP / TCP (Puerto 6379)")

Rel(backend, smtp, "Envía correos electrónicos asíncronos (@Async)", "SMTP / TLS (Puerto 587)")
Rel(backend, paypal, "Crea órdenes transaccionales y gestiona fondos Escrow", "HTTPS / REST JSON v2")
Rel(paypal, backend, "Confirma transacciones o disputas de forma asíncrona", "HTTPS / Webhooks (POST)")
Rel(backend, storage, "Gestiona subida y URLs seguras para archivos multimedia", "HTTPS / REST API")

@enduml
```

---

## 4. Visualización con Mermaid (Renderizado Nativo en Markdown)

```mermaid
flowchart TB
    subgraph Users["👤 System Actors"]
        C["👨‍💻 Client / Talent Seeker"]
        A["🎨 Artist / Content Creator"]
        ADM["🛡️ Platform Administrator"]
    end

    subgraph Artisync["🏢 Artisync Platform (System Boundary - Docker Compose)"]
        direction TB
        FE["🌐 Web SPA Application (Frontend)<br>----------------------------------------<br>Angular 22 / TypeScript / Nginx<br>Port: 4200 / 80"]
        
        BE["⚙️ REST API Server (Backend)<br>----------------------------------------<br>Java 21 / Spring Boot 4.1.0 / Security 6<br>Port: 8080 (REST JSON)"]
        
        subgraph DataTier["High-Speed Persistence & Cache"]
            DB[("🗄️ Relational Database<br>-------------------------<br>PostgreSQL 16 (pfc_postgres)<br>Port: 5432 / Flyway")]
            REDIS[("⚡ Cache & JTI Blacklist<br>-------------------------<br>Redis 7 Alpine (pfc_redis)<br>Port: 6379 / TTL O(1)")]
        end
    end

    subgraph External["🌐 External Systems"]
        SMTP["📧 Transactional Email (SMTP TLS 587)"]
        PAY["💳 PayPal API v2 & Webhooks (Escrow)"]
        CLOUD["☁️ Cloud Storage / Media CDN"]
    end

    %% User -> Frontend relationships
    C -- "HTTPS / 4200<br>Requests and approves orders" --> FE
    A -- "HTTPS / 4200<br>Manages portfolio and deliverables" --> FE
    ADM -- "HTTPS / 4200<br>Moderates and resolves tickets" --> FE

    %% Frontend -> Backend relationship
    FE -- "HTTPS / REST JSON<br>Authorization: Bearer <JWT>" --> BE

    %% Backend -> Data relationships
    BE -- "JDBC / Hibernate JPA<br>ACID transactions (@Transactional)" --> DB
    BE -- "RESP Protocol / RedisTemplate<br>Checks blacklist and cache-aside" --> REDIS

    %% Backend -> External relationships
    BE -- "SMTP / TLS 587<br>Sends emails (@Async)" --> SMTP
    BE -- "HTTPS / REST API v2<br>Creates escrow orders" --> PAY
    PAY -- "HTTPS Webhook POST<br>Payment notification" --> BE
    BE -- "HTTPS / REST API<br>Uploads media files" --> CLOUD

    style FE fill:#2b5c8f,stroke:#1b3d5f,stroke-width:2px,color:#fff
    style BE fill:#1168bd,stroke:#08427b,stroke-width:3px,color:#fff
    style DB fill:#387c2b,stroke:#24521c,stroke-width:2px,color:#fff
    style REDIS fill:#c12c2c,stroke:#851c1c,stroke-width:2px,color:#fff
    style C fill:#08427b,stroke:#052b52,color:#fff
    style A fill:#08427b,stroke:#052b52,color:#fff
    style ADM fill:#990000,stroke:#660000,color:#fff
    style SMTP fill:#666,stroke:#444,color:#fff
    style PAY fill:#666,stroke:#444,color:#fff
    style CLOUD fill:#666,stroke:#444,color:#fff
```

---

## 5. Mapeo con Docker Compose (`docker-compose.yml`)

El diseño de contenedores se refleja 1:1 con la orquestación implementada en el archivo `docker-compose.yml` del repositorio:

| Contenedor C4 | Servicio Docker | Imagen / Build Context | Puertos Mapeados | Dependencias (`depends_on`) | Healthcheck |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Base de Datos** | `postgres` | `postgres:16` | `5432:5432` | N/A | `pg_isready -U pfc_user -d pfc_db` |
| **Caché en Memoria** | `redis` | `redis:7-alpine` | `6379:6379` | N/A | `redis-cli ping` |
| **Backend API REST** | `backend` | `./Backend (Dockerfile)` | `expose: 8080` — **no publicado al host** (OBS-AUTO-05 / A07 OWASP): solo alcanzable desde la red interna, es decir a través de las reglas de proxy `/api` y `/actuator` del frontend | `postgres` (service_healthy), <br> `redis` (service_healthy) | `wget -qO- http://localhost:8080/actuator/health` |
| **Frontend Web SPA** | `frontend` | `./Frontend (Dockerfile)` | `4200:4200` | `backend` (service_healthy) | Servidor web Nginx |
