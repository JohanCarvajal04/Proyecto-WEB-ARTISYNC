workspace "Artisync - Platform for Artists and Content Creators" "C4 Model (Structurizr DSL): Level 1 Context and Level 2 Containers." {

    model {
        cliente = person "Client / Talent Seeker" "Individual, brand, or company that searches for, explores, and hires creative services: requests quotes, hires projects, deposits in escrow, approves milestones, and rates deliverables." "Person"
        artista = person "Artist / Content Creator" "Independent creative professional who registers, verifies their profile, publishes their portfolio and service catalog, manages active orders, uploads deliverables per milestone, and receives released payments." "Person"
        admin = person "Platform Administrator" "Technical/administrative staff with privileged access: manages account sign-ups and bans, granular roles and permissions, moderates catalogs, and arbitrates disputes." "Admin"

        smtpSystem = softwareSystem "Transactional Email Service" "Outgoing SMTP server for account verification emails, 2FA codes, password resets, and order milestone alerts." "External System"
        paypalSystem = softwareSystem "Payment Gateway (PayPal API v2)" "External transactional processing platform, retention of funds in escrow (Escrow), and asynchronous Webhook notifications." "External System"
        cloudStorageSystem = softwareSystem "Cloud Storage / CDN" "Cloud service for persistence and delivery of multimedia resources for portfolios, avatars, and deliverables." "External System"

        artisyncSystem = softwareSystem "Artisync Platform (PFC)" "Comprehensive management system for artists and clients with catalog, quotes, milestones, legal contracts, escrow payments, social community, and real-time communication." "System" {

            webApp = container "SPA Web Application (Frontend)" "Responsive single-page web user interface. Allows authenticating (2FA, JWT), exploring the catalog, hiring services, managing milestones/tickets, and participating in the community." "Angular 22 / TypeScript / Nginx" "WebBrowser"

            apiServer = container "REST API Server (Backend)" "Transactional core. Exposes RESTful endpoints (OpenAPI/Swagger), orchestrates the 7 functional modules (security, profile, catalog, order, legal, communication, social), manages ACID transactions, and validates stateless security with JWT." "Java 21 / Spring Boot 4.1.0 / Spring Security 6" "Backend"

            db = container "Relational Database" "Main persistent and transactional store. Normalized schemas and tables versioned by Flyway migrations." "PostgreSQL 16" "Database"

            cache = container "In-Memory Store / Cache" "Blacklist of JWT tokens (jti) with TTL for immediate logout and session revocation; Cache-Aside pattern for the service catalog." "Redis 7 Alpine" "Cache"
        }

        // Interacciones a nivel de contexto (personas/sistemas externos -> sistema)
        cliente -> artisyncSystem "Explores catalog, quotes, pays in escrow, and approves milestones" "HTTPS"
        artista -> artisyncSystem "Publishes catalog and portfolio, manages orders, and gets paid" "HTTPS"
        admin -> artisyncSystem "Moderates catalogs, manages roles, and resolves disputes" "HTTPS"
        artisyncSystem -> smtpSystem "Sends transactional emails (verification, 2FA, alerts)" "SMTP / TLS"
        artisyncSystem -> paypalSystem "Creates orders and manages Escrow security deposits" "HTTPS / REST JSON v2"
        paypalSystem -> artisyncSystem "Notifies of confirmed or disputed payments" "HTTPS / Webhooks"
        artisyncSystem -> cloudStorageSystem "Uploads and serves multimedia resources for portfolios and deliverables" "HTTPS / REST"

        // Interacciones a nivel de contenedor (dentro del sistema)
        cliente -> webApp "Accesses portfolio, quotes orders, pays, and approves milestones" "HTTPS / Port 4200/443"
        artista -> webApp "Manages catalog, uploads milestone progress, and gets paid for projects" "HTTPS / Port 4200/443"
        admin -> webApp "Moderates catalogs, assigns permissions, and resolves disputes" "HTTPS / Port 4200/443"
        webApp -> apiServer "Consumes REST services, authenticates with JWT Bearer, and invokes transactional commands" "HTTPS / REST JSON / Port 8080"
        apiServer -> db "Reads and writes transactional domain entities (ACID) and executes Flyway migrations" "JDBC / TCP / Port 5432"
        apiServer -> cache "Queries and stores JTI in blacklist, revoked sessions, and catalog cache" "Redis Protocol (RESP) / TCP / Port 6379"
        apiServer -> smtpSystem "Sends asynchronous emails (@Async) with Thymeleaf templates" "SMTP / TLS / Port 587"
        apiServer -> paypalSystem "Creates orders and manages Escrow security deposits" "HTTPS / REST JSON v2"
        paypalSystem -> apiServer "Sends notifications of confirmed or disputed payments" "HTTPS / Webhooks (POST)"
        apiServer -> cloudStorageSystem "Uploads and generates pre-signed URLs for multimedia resources" "HTTPS / REST Azure"
    }

    views {
        systemContext artisyncSystem "C4_Context_Artisync" {
            include *
            autoLayout topBottom
            description "C4 Level 1 Diagram (Context) of the Artisync platform: actors and external systems."
        }

        container artisyncSystem "C4_Containers_Artisync" {
            include *
            autoLayout topBottom
            description "C4 Level 2 Diagram (Containers) showing the distributed architecture of the Artisync platform."
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
