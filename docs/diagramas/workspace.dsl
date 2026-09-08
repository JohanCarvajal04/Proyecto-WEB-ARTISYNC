workspace "Artisync - Plataforma para Artistas y Creadores de Contenido" "Modelo C4 (Structurizr DSL): Nivel 1 Contexto y Nivel 2 Contenedores." {

    model {
        cliente = person "Cliente / Buscador de Talento" "Persona física, marca o empresa que busca, explora y contrata servicios creativos: solicita cotizaciones, contrata proyectos, deposita en garantía, aprueba hitos y califica entregables." "Person"
        artista = person "Artista / Creador de Contenido" "Profesional creativo independiente que se registra, verifica su perfil, publica portafolio y catálogo de servicios, gestiona pedidos activos, sube entregables por hito y recibe pagos liberados." "Person"
        admin = person "Administrador de la Plataforma" "Personal técnico/administrativo con acceso privilegiado: gestiona altas y bloqueos de cuentas, roles y permisos granulares, modera catálogos y arbitra disputas." "Admin"

        smtpSystem = softwareSystem "Servicio de Correo Transaccional" "Servidor SMTP saliente para correos de verificación de cuenta, códigos 2FA, reseteo de contraseñas y alertas de hitos de pedidos." "External System"
        paypalSystem = softwareSystem "Pasarela de Pagos (PayPal API v2)" "Plataforma externa de procesamiento transaccional, retención de fondos en garantía (Escrow) y notificaciones asíncronas por Webhook." "External System"
        cloudStorageSystem = softwareSystem "Almacenamiento Cloud / CDN" "Servicio en la nube para persistencia y entrega de recursos multimedia del portafolio, avatares y entregables." "External System"

        artisyncSystem = softwareSystem "Plataforma Artisync (PFC)" "Sistema integral de gestión para artistas y clientes con catálogo, cotizaciones, hitos, contratos legales, pagos en garantía, comunidad social y comunicación en tiempo real." "System" {

            webApp = container "Aplicación Web SPA (Frontend)" "Interfaz de usuario web responsiva de página única. Permite autenticarse (2FA, JWT), explorar el catálogo, contratar servicios, gestionar hitos/tickets y participar en la comunidad." "Angular 22 / TypeScript / Nginx" "WebBrowser"

            apiServer = container "Servidor API REST (Backend)" "Núcleo transaccional. Expone endpoints RESTful (OpenAPI/Swagger), orquesta los 7 módulos funcionales (seguridad, perfil, catalogo, pedido, legal, comunicacion, social), gestiona transacciones ACID y valida seguridad sin estado con JWT." "Java 21 / Spring Boot 4.1.0 / Spring Security 6" "Backend"

            db = container "Base de Datos Relacional" "Almacén principal persistente y transaccional. Esquemas y tablas normalizadas versionadas por migraciones de Flyway." "PostgreSQL 16" "Database"

            cache = container "Almacén en Memoria / Caché" "Blacklist de tokens JWT (jti) con TTL para logout inmediato y revocación de sesiones; patrón Cache-Aside para el catálogo de servicios." "Redis 7 Alpine" "Cache"
        }

        // Interacciones a nivel de contexto (personas/sistemas externos -> sistema)
        cliente -> artisyncSystem "Explora catálogo, cotiza, paga en garantía y aprueba hitos" "HTTPS"
        artista -> artisyncSystem "Publica catálogo y portafolio, gestiona pedidos y cobra" "HTTPS"
        admin -> artisyncSystem "Modera catálogos, gestiona roles y resuelve disputas" "HTTPS"
        artisyncSystem -> smtpSystem "Envía correos transaccionales (verificación, 2FA, alertas)" "SMTP / TLS"
        artisyncSystem -> paypalSystem "Crea órdenes y gestiona depósitos de garantía Escrow" "HTTPS / REST JSON v2"
        paypalSystem -> artisyncSystem "Notifica pagos confirmados o disputados" "HTTPS / Webhooks"
        artisyncSystem -> cloudStorageSystem "Sube y sirve recursos multimedia del portafolio y entregables" "HTTPS / REST"

        // Interacciones a nivel de contenedor (dentro del sistema)
        cliente -> webApp "Accede a portafolio, cotiza pedidos, paga y aprueba hitos" "HTTPS / Puerto 4200/443"
        artista -> webApp "Gestiona catálogo, sube avances de hitos y cobra por proyectos" "HTTPS / Puerto 4200/443"
        admin -> webApp "Modera catálogos, asigna permisos y resuelve disputas" "HTTPS / Puerto 4200/443"
        webApp -> apiServer "Consume servicios REST, autentica con JWT Bearer e invoca comandos transaccionales" "HTTPS / REST JSON / Puerto 8080"
        apiServer -> db "Lee y escribe entidades de dominio transaccionales (ACID) y ejecuta migraciones Flyway" "JDBC / TCP / Puerto 5432"
        apiServer -> cache "Consulta y almacena JTI en blacklist, sesiones revocadas y caché de catálogos" "Redis Protocol (RESP) / TCP / Puerto 6379"
        apiServer -> smtpSystem "Envía correos asíncronos (@Async) con plantillas Thymeleaf" "SMTP / TLS / Puerto 587"
        apiServer -> paypalSystem "Crea órdenes y gestiona depósitos de garantía Escrow" "HTTPS / REST JSON v2"
        paypalSystem -> apiServer "Envía notificaciones de pago confirmadas o disputadas" "HTTPS / Webhooks (POST)"
        apiServer -> cloudStorageSystem "Sube y genera URLs pre-firmadas para recursos multimedia" "HTTPS / REST Azure"
    }

    views {
        systemContext artisyncSystem "C4_Context_Artisync" {
            include *
            autoLayout topBottom
            description "Diagrama C4 Nivel 1 (Contexto) de la plataforma Artisync: actores y sistemas externos."
        }

        container artisyncSystem "C4_Containers_Artisync" {
            include *
            autoLayout topBottom
            description "Diagrama C4 Nivel 2 (Contenedores) que muestra la arquitectura distribuida de la plataforma Artisync."
        }

        styles {
            element "Person" { shape Person; background #08427b; color #ffffff; fontSize 20; }
            element "Admin" { shape Person; background #990000; color #ffffff; fontSize 20; }
            element "WebBrowser" { shape WebBrowser; background #2b5c8f; color #ffffff; fontSize 18; }
            element "Backend" { shape RoundedBox; background #1168bd; color #ffffff; fontSize 18; fontStyle bold; }
            element "Database" { shape Cylinder; background #387c2b; color #ffffff; fontSize 18; }
            element "Cache" { shape Cylinder; background #c12c2c; color #ffffff; fontSize 18; }
            element "External System" { shape RoundedBox; background #999999; color #ffffff; fontSize 18; }
            element "System" { background #1168bd; color #ffffff; fontSize 20; }
        }
    }
}
