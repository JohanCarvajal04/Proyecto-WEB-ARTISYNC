# Despliegue — Artisync (Entrega Final, v1.0.0)

Cumple el Bloque A.4.2 de la guía de la Entrega Final. Describe el proveedor, los recursos
consumidos, la topología de red simplificada, las variables de entorno de producción (sin valores
sensibles) y el procedimiento para reproducir el despliegue.

## Estado a la fecha de este documento (2026-08-17)

**Brecha declarada honestamente:** a la fecha de este documento el sistema **no está desplegado
en un ambiente públicamente accesible con dominio y HTTPS** (requisito A.4.1 de la guía). Existe
la infraestructura y configuración necesarias para hacerlo (`artisync/docker-compose.azure.yml`,
`ADR-005`), pero falta la decisión de equipo sobre el proveedor final y la ejecución del
despliegue. Ver `docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md`, Bloque A.4. Este documento
describe la topología y el procedimiento tal como están preparados hoy, para que el equipo pueda
ejecutar el despliegue real en el proveedor que decida sin trabajo adicional de documentación.

## Proveedor

El repositorio soporta dos topologías, seleccionables por archivo de compose:

- **Local / desarrollo** (`artisync/docker-compose.yml`): los cuatro servicios (`postgres`,
  `redis`, `backend`, `frontend`) corren en la misma máquina o VM, sin dependencias externas de
  nube.
- **Azure** (`artisync/docker-compose.azure.yml`): backend apunta a una instancia de **Azure
  Database for PostgreSQL** (variables `DB_URL`/`DB_USER`/`DB_PASSWORD` externas, no un contenedor
  local de Postgres) y opcionalmente a **Azure Blob Storage** para documentos
  (`DOCUMENTOS_PROVEEDOR=azure`, con **Azurite** como emulador local para desarrollo — perfil
  `azure` de `docker-compose.yml`, ver `AlmacenamientoAzureIntegracionTest.java`).

Cualquiera de los cinco tipos de ambiente que acepta la guía (VPS con dominio, proveedor de
contenedores gratuito, cuenta cloud gratuita/estudiante, servidor institucional UTEQ, o una
combinación) es compatible con la topología Azure ya preparada, o con la topología local si el
proveedor expone Docker Compose directamente (ej. un VPS).

## Recursos consumidos (perfil local, referencia de dimensionamiento)

| Servicio | CPU (estimado) | RAM (estimado) | Disco |
|---|---|---|---|
| `postgres` (16, imagen anclada por digest) | 0.5 vCPU | 512 MB | Volumen `pfc_postgres_data` (crece con datos) |
| `redis` (7-alpine) | 0.1 vCPU | 64 MB | Sin persistencia por diseño (cache) |
| `backend` (Spring Boot, JRE 21) | 1 vCPU | 512 MB–1 GB (JVM) | Sin estado (logs a stdout) |
| `frontend` (nginx + build Angular) | 0.1 vCPU | 64 MB | Sin estado |

Dimensionamiento de referencia para producción con tráfico bajo/medio (equivalente a los niveles
gratuitos de Oracle Cloud Free Tier, Fly.io o Render): 2 vCPU / 4 GB RAM combinados es suficiente
para los cuatro servicios con margen.

## Topología de red simplificada

```
Internet
   │  HTTPS (443)
   ▼
[frontend: nginx]  ── sirve Angular build de producción
   │  proxy interno /api/*
   ▼
[backend: Spring Boot :8080]  (NO publicado directamente al host — ver docker-compose.yml)
   │                    │
   ▼                    ▼
[postgres :5432]   [redis :6379]
(red interna Docker Compose, sin exposición externa)
```

El puerto 8080 del backend no se publica al host (decisión de seguridad, ver `OBS-AUTO-05` en
`OBSERVACIONES.md`): todo el tráfico externo entra por el frontend, que además es el único punto
donde `server.forward-headers-strategy=native` resuelve la IP real del cliente para el rate
limiting.

## Variables de entorno de producción (sin valores sensibles)

Ver `artisync/.env.example` para la lista completa con placeholders. Las variables que cambian
entre desarrollo y producción son:

| Variable | Desarrollo | Producción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://postgres:5432/...` (contenedor local) | URL del proveedor gestionado (ej. Azure PostgreSQL, con `sslmode=require`) |
| `DOCUMENTOS_PROVEEDOR` | `local` | `azure` (o el proveedor de almacenamiento elegido) |
| `JWT_SECRET` | valor de desarrollo | generado con `openssl rand -hex 32`, único por ambiente |
| `MAIL_HOST`/`MAIL_USER`/`MAIL_PASSWORD` | cuenta de pruebas | cuenta SMTP de producción |

Las credenciales reales nunca se versionan; se inyectan vía `artisync/.env` (gitignored) o el
mecanismo de secretos del proveedor elegido (ej. Azure Key Vault, variables de entorno del PaaS).

## Procedimiento paso a paso para reproducir el despliegue

1. Clonar el repositorio y copiar `artisync/.env.example` a `artisync/.env`.
2. Completar las variables marcadas `<...>` en `.env` con los valores reales del proveedor
   elegido (base de datos, JWT, correo, y `DOCUMENTOS_PROVEEDOR` según corresponda).
3. Si se usa un proveedor cloud (Azure u otro): `docker compose -f artisync/docker-compose.yml -f artisync/docker-compose.azure.yml --env-file artisync/.env up -d --build`.
   Si se usa un VPS con Docker: `make up` (equivalente al perfil local, con Postgres/Redis en el
   mismo host).
4. Configurar un proxy inverso (Caddy, nginx, o el balanceador del proveedor) delante del
   contenedor `frontend` con un certificado TLS válido (ej. Let's Encrypt vía Caddy automático).
5. Verificar `GET https://<dominio>/actuator/health` → debe responder `UP` en todos los
   componentes registrados (base de datos, Redis).
6. Publicar la URL en el `README.md` (sección de arranque) y en la portada del documento
   académico final, junto con las credenciales del usuario demo.

Ver también `docs/despliegue/RUNBOOK.md` (operación día a día), `docs/despliegue/BACKUP.md`
(estrategia de respaldo) y `docs/despliegue/RENDER.md` (procedimiento paso a paso concreto
para el proveedor Render, con el Blueprint `render.yaml` ya versionado en la raíz del repo).
