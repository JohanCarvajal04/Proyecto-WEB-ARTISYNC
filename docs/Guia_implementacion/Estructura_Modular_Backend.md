# 🏗️ Guía de Arquitectura Modular del Backend ARTISYNC

## 1. Introducción y Justificación Arquitectónica

El proyecto ARTISYNC está diseñado bajo el principio de **Separación por Dominios (Package-by-Feature / Domain-Driven Modularization)**, en alineación estricta con el requisito no funcional **RNF-10 (Módulos Desacoplados)** de la guía PFC.

### Estado Actual del Repositorio
Actualmente, el paquete `uteq.edu.ec.artisync.entity` ya se encuentra correctamente dividido en subpaquetes de dominio correspondientes a los 7 módulos funcionales del sistema:
- `entity.seguridad` (M1: Autenticación, RBAC, Usuarios)
- `entity.perfil` (M2: Perfiles de Creador, Verificación, Portafolio)
- `entity.catalogo` (M3: Catálogo Dinámico de Servicios)
- `entity.pedido` (M4: Motor de Flujos de Trabajo y Pedidos)
- `entity.legal` (M5: Contratos, Entregables y Finanzas Escrow)
- `entity.comunicacion` (M6: Comunicación, Chat y Notificaciones)
- `entity.social` (M7: Social, Comunidad y Sorteos)

Sin embargo, las capas horizontales adyacentes (`repository/`, `service/`, `controller/` y `dto/`) actualmente contienen todas sus clases en un solo nivel de directorio (por ejemplo, los 21 repositorios están en `uteq.edu.ec.artisync.repository.*`).

### Objetivo de esta Guía
Esta guía define cómo **replicar la estructura de subpaquetes de dominio del paquete `entity` hacia todas las demás capas del backend (`repository`, `service`, `controller` y `dto`)**, clasificando exhaustivamente:
1. **Los archivos existentes actualmente en el proyecto**, indicando a qué subpaquete específico deben migrarse.
2. **Los nuevos archivos por crear** (según las guías M1 a M7), ubicándolos en su módulo de dominio correspondiente.

---

## 2. Árbol de Paquetes Objetivo (Estructura Estándar)

La estructura final del código fuente (`Backend/src/main/java/uteq/edu/ec/artisync/`) seguirá exactamente este esquema de subpaquetes en cada capa arquitectónica:

```text
uteq.edu.ec.artisync
│
├── config/                                  # Configuraciones globales y de seguridad
│   ├── GlobalExceptionHandler.java
│   ├── OpenApiConfig.java
│   ├── RedisConfig.java
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java                 # [NUEVO - M6]
│   ├── PayPalConfig.java                    # [NUEVO - M5]
│   └── S3Config.java                        # [NUEVO - M2/M5]
│
├── security/                                # Filtros y componentes transversales de JWT/Spring Security
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtService.java
│
├── util/                                    # Paginación y utilidades generales
│   ├── PagedResponse.java
│   └── PagedResponseBuilder.java
│
├── exception/                               # Excepciones personalizadas de negocio
│   └── ...
│
├── service/
│   ├── shared/                              # Servicios transversales utilizados por múltiples módulos
│   │   ├── EmailService.java                # Envío de correos SMTP
│   │   ├── SessionRevocationService.java    # Blacklist de tokens en Redis
│   │   ├── UsuarioMapper.java               # Mapeador compartido de usuarios
│   │   ├── StorageService.java              # [NUEVO - M2/M5] S3/R2 File Upload
│   │   └── IaVerificationClient.java        # [NUEVO - M2] Cliente HTTP IA
│   │
│   ├── seguridad/                           # [M1] Autenticación, RBAC, Usuarios
│   │   ├── AuthService.java
│   │   ├── TwoFactorService.java
│   │   ├── UserService.java
│   │   ├── AdminUserService.java
│   │   ├── RolePermissionService.java
│   │   ├── PaisService.java
│   │   └── impl/
│   │       ├── AuthServiceImpl.java
│   │       ├── TwoFactorServiceImpl.java
│   │       ├── UserServiceImpl.java
│   │       ├── AdminUserServiceImpl.java
│   │       ├── RolePermissionServiceImpl.java
│   │       └── PaisServiceImpl.java
│   │
│   ├── perfil/                              # [M2] Perfiles, Verificación y Portafolio
│   │   ├── PerfilCreadorService.java        # [NUEVO]
│   │   ├── PortafolioService.java           # [NUEVO]
│   │   ├── VerificacionService.java         # [NUEVO]
│   │   └── impl/
│   │       ├── PerfilCreadorServiceImpl.java
│   │       ├── PortafolioServiceImpl.java
│   │       └── VerificacionServiceImpl.java
│   │
│   ├── catalogo/                            # [M3] Catálogo, Servicios y Categorías
│   │   ├── ServicioService.java             # [NUEVO]
│   │   ├── CategoriaService.java            # [NUEVO]
│   │   ├── EtiquetaService.java             # [NUEVO]
│   │   └── impl/
│   │       ├── ServicioServiceImpl.java
│   │       ├── CategoriaServiceImpl.java
│   │       └── EtiquetaServiceImpl.java
│   │
│   ├── pedido/                              # [M4] Pedidos y Flujos de Trabajo
│   │   ├── PedidoService.java               # [NUEVO]
│   │   ├── FlujoTrabajoService.java         # [NUEVO]
│   │   ├── TicketRevisionService.java       # [NUEVO]
│   │   └── impl/
│   │       ├── PedidoServiceImpl.java
│   │       ├── FlujoTrabajoServiceImpl.java
│   │       └── TicketRevisionServiceImpl.java
│   │
│   ├── legal/                               # [M5] Contratos, Entregables y Finanzas Escrow
│   │   ├── ContratoService.java             # [NUEVO]
│   │   ├── PagoService.java                 # [NUEVO]
│   │   ├── EntregableService.java           # [NUEVO]
│   │   ├── PdfGenerationService.java        # [NUEVO]
│   │   └── impl/
│   │       ├── ContratoServiceImpl.java
│   │       ├── PagoServiceImpl.java
│   │       ├── EntregableServiceImpl.java
│   │       └── PdfGenerationServiceImpl.java
│   │
│   ├── comunicacion/                        # [M6] Chat WebSocket, Briefing y Notificaciones
│   │   ├── ChatService.java                 # [NUEVO]
│   │   ├── MensajeFilterService.java        # [NUEVO]
│   │   ├── InfraccionService.java           # [NUEVO]
│   │   ├── BriefingService.java             # [NUEVO]
│   │   ├── NotificacionService.java         # [NUEVO]
│   │   ├── ComentarioService.java           # [NUEVO]
│   │   ├── SeguidorService.java             # [NUEVO]
│   │   └── impl/
│   │       ├── ChatServiceImpl.java
│   │       ├── MensajeFilterServiceImpl.java
│   │       ├── InfraccionServiceImpl.java
│   │       ├── BriefingServiceImpl.java
│   │       ├── NotificacionServiceImpl.java
│   │       ├── ComentarioServiceImpl.java
│   │       └── SeguidorServiceImpl.java
│   │
│   └── social/                              # [M7] Reseñas, Sorteos y Auditoría
│       ├── SorteoService.java               # [NUEVO]
│       ├── ResenaService.java               # [NUEVO]
│       ├── AuditService.java                # [NUEVO]
│       └── impl/
│           ├── SorteoServiceImpl.java
│           ├── ResenaServiceImpl.java
│           └── AuditServiceImpl.java
│
├── repository/
│   ├── seguridad/                           # [M1]
│   │   ├── UsuarioRepository.java
│   │   ├── RolRepository.java
│   │   ├── PermisoRepository.java
│   │   ├── RolPermisoRepository.java
│   │   ├── UsuarioRolRepository.java
│   │   ├── SesionUsuarioRepository.java
│   │   ├── TokenRecuperacionRepository.java
│   │   ├── AutenticacionDosFactoresRepository.java
│   │   ├── CodigoRespaldo2FaRepository.java
│   │   └── PaisRepository.java
│   │
│   ├── perfil/                              # [M2]
│   │   ├── PerfilCreadorRepository.java
│   │   ├── PortafolioRepository.java
│   │   ├── CategoriaRepository.java         # [NUEVO]
│   │   ├── CertificadoIaRepository.java     # [NUEVO]
│   │   ├── EstadoVerificacionRepository.java# [NUEVO]
│   │   ├── HabilidadRepository.java         # [NUEVO]
│   │   ├── CreadorHabilidadRepository.java  # [NUEVO]
│   │   └── PortafolioItemRepository.java    # [NUEVO]
│   │
│   ├── catalogo/                            # [M3]
│   │   ├── ServicioRepository.java
│   │   ├── SubcategoriaRepository.java      # [NUEVO]
│   │   ├── AtributoDinamicoRepository.java  # [NUEVO]
│   │   ├── ServicioAtributoRepository.java  # [NUEVO]
│   │   ├── EtiquetaRepository.java          # [NUEVO]
│   │   ├── ServicioEtiquetaRepository.java  # [NUEVO]
│   │   └── FlujoTrabajoRepository.java      # [NUEVO]
│   │
│   ├── pedido/                              # [M4]
│   │   ├── PedidoRepository.java
│   │   ├── HistorialEstadoPedidoRepository.java
│   │   ├── EtapaFlujoRepository.java        # [NUEVO]
│   │   ├── FlujoEtapaConfigRepository.java  # [NUEVO]
│   │   ├── MotivoRechazoRepository.java     # [NUEVO]
│   │   ├── TicketRevisionRepository.java    # [NUEVO]
│   │   └── PlantillaContratoRepository.java # [NUEVO]
│   │
│   ├── legal/                               # [M5]
│   │   ├── ContratoRepository.java
│   │   ├── PagoGarantiaRepository.java
│   │   ├── SalaChatRepository.java
│   │   ├── MensajeRepository.java
│   │   ├── EntregableFinalRepository.java   # [NUEVO]
│   │   ├── TransaccionPagoRepository.java   # [NUEVO]
│   │   └── DocumentoAdjuntoRepository.java  # [NUEVO]
│   │
│   ├── comunicacion/                        # [M6]
│   │   ├── InfraccionRepository.java
│   │   ├── SeguidorRepository.java          # [NUEVO]
│   │   ├── ComentarioPortafolioRepository.java # [NUEVO]
│   │   ├── LikePortafolioRepository.java    # [NUEVO]
│   │   ├── TipoNotificacionRepository.java  # [NUEVO]
│   │   ├── NotificacionSistemaRepository.java # [NUEVO]
│   │   ├── BriefingPlantillaRepository.java # [NUEVO]
│   │   ├── BriefingPreguntaRepository.java  # [NUEVO]
│   │   ├── BriefingEnviadoRepository.java   # [NUEVO]
│   │   └── BriefingRespuestaRepository.java # [NUEVO]
│   │
│   └── social/                              # [M7]
│       ├── SorteoRepository.java
│       ├── ResenaServicioRepository.java    # [NUEVO]
│       └── ParticipanteSorteoRepository.java# [NUEVO]
│
├── controller/
│   ├── seguridad/                           # [M1]
│   │   ├── AuthController.java
│   │   ├── TwoFactorController.java
│   │   ├── UserController.java
│   │   ├── AdminUserController.java
│   │   ├── RolePermissionController.java
│   │   ├── PermissionController.java
│   │   └── PaisController.java
│   │
│   ├── perfil/                              # [M2]
│   │   ├── PerfilCreadorController.java     # [NUEVO]
│   │   ├── PortafolioController.java        # [NUEVO]
│   │   └── VerificacionController.java      # [NUEVO]
│   │
│   ├── catalogo/                            # [M3]
│   │   ├── ServicioController.java          # [NUEVO]
│   │   ├── CategoriaController.java         # [NUEVO]
│   │   └── EtiquetaController.java          # [NUEVO]
│   │
│   ├── pedido/                              # [M4]
│   │   ├── PedidoController.java            # [NUEVO]
│   │   ├── FlujoTrabajoController.java      # [NUEVO]
│   │   └── TicketRevisionController.java    # [NUEVO]
│   │
│   ├── legal/                               # [M5]
│   │   ├── ContratoController.java          # [NUEVO]
│   │   ├── PagoController.java              # [NUEVO]
│   │   ├── PayPalWebhookController.java     # [NUEVO]
│   │   └── EntregableController.java        # [NUEVO]
│   │
│   ├── comunicacion/                        # [M6]
│   │   ├── ChatController.java              # [NUEVO]
│   │   ├── BriefingController.java          # [NUEVO]
│   │   ├── NotificacionController.java      # [NUEVO]
│   │   ├── ComentarioController.java        # [NUEVO]
│   │   └── SeguidorController.java          # [NUEVO]
│   │
│   └── social/                              # [M7]
│       ├── SorteoController.java            # [NUEVO]
│       ├── ResenaController.java            # [NUEVO]
│       └── AuditController.java             # [NUEVO]
│
└── dto/
    ├── shared/                              # DTOs de error o mensajes genéricos
    │   ├── MessageResponse.java
    │   └── ErrorResponse.java
    │
    ├── seguridad/                           # [M1]
    │   ├── request/
    │   │   ├── RegisterRequest.java
    │   │   ├── LoginRequest.java
    │   │   ├── TwoFactorRequest.java
    │   │   ├── TwoFactorConfirmRequest.java
    │   │   ├── RefreshTokenRequest.java
    │   │   ├── ForgotPasswordRequest.java
    │   │   ├── ResetPasswordRequest.java
    │   │   ├── ChangePasswordRequest.java
    │   │   ├── CreateUserRequest.java
    │   │   ├── UpdateUserRequest.java
    │   │   ├── AdminUpdateUserRequest.java
    │   │   ├── ChangeEstadoRequest.java
    │   │   ├── CreateRoleRequest.java
    │   │   ├── UpdateRoleRequest.java
    │   │   ├── AssignRolesRequest.java
    │   │   ├── SyncPermissionsRequest.java
    │   │   └── PaisRequest.java
    │   └── response/
    │       ├── TokenResponse.java
    │       ├── UserResponse.java
    │       ├── TwoFactorSetupResponse.java
    │       ├── RolResponse.java
    │       ├── PermisoResponse.java
    │       └── PaisResponse.java
    │
    ├── perfil/                              # [M2]
    │   ├── request/
    │   │   ├── ActualizarPerfilRequest.java # [NUEVO]
    │   │   └── CrearPortafolioItemRequest.java # [NUEVO]
    │   └── response/
    │       ├── PerfilPublicoResponse.java   # [NUEVO]
    │       ├── PortafolioItemResponse.java  # [NUEVO]
    │       └── VerificacionResponse.java    # [NUEVO]
    │
    ├── catalogo/                            # [M3]
    │   ├── request/
    │   │   ├── CrearServicioRequest.java    # [NUEVO]
    │   │   ├── ActualizarServicioRequest.java # [NUEVO]
    │   │   ├── CrearAtributoRequest.java    # [NUEVO]
    │   │   ├── BusquedaServicioRequest.java # [NUEVO]
    │   │   ├── CrearCategoriaRequest.java   # [NUEVO]
    │   │   └── CrearSubcategoriaRequest.java# [NUEVO]
    │   └── response/
    │       ├── ServicioResponse.java        # [NUEVO]
    │       ├── ServicioCatalogoResponse.java# [NUEVO]
    │       ├── CategoriaResponse.java       # [NUEVO]
    │       ├── SubcategoriaResponse.java    # [NUEVO]
    │       ├── AtributoResponse.java        # [NUEVO]
    │       └── EtiquetaResponse.java        # [NUEVO]
    │
    ├── pedido/                              # [M4]
    │   ├── request/
    │   │   ├── CrearPedidoRequest.java      # [NUEVO]
    │   │   ├── AvanzarEtapaRequest.java     # [NUEVO]
    │   │   ├── CrearFlujoTrabajoRequest.java# [NUEVO]
    │   │   ├── EtapaConfigRequest.java      # [NUEVO]
    │   │   └── CrearTicketRevisionRequest.java # [NUEVO]
    │   └── response/
    │       ├── PedidoResponse.java          # [NUEVO]
    │       ├── PedidoResumidoResponse.java  # [NUEVO]
    │       ├── SeguimientoPedidoResponse.java # [NUEVO]
    │       ├── HistorialEstadoResponse.java # [NUEVO]
    │       ├── FlujoTrabajoResponse.java    # [NUEVO]
    │       ├── EtapaConfigResponse.java     # [NUEVO]
    │       └── TicketRevisionResponse.java  # [NUEVO]
    │
    ├── legal/                               # [M5]
    │   ├── request/
    │   │   ├── FirmarContratoRequest.java   # [NUEVO]
    │   │   ├── AprobarEntregaRequest.java   # [NUEVO]
    │   │   └── CrearOrdenPagoRequest.java   # [NUEVO]
    │   └── response/
    │       ├── ContratoResponse.java        # [NUEVO]
    │       ├── PagoResponse.java            # [NUEVO]
    │       ├── EntregableResponse.java      # [NUEVO]
    │       └── EstadoFirmaResponse.java     # [NUEVO]
    │
    ├── comunicacion/                        # [M6]
    │   ├── request/
    │   │   ├── EnviarMensajeRequest.java    # [NUEVO]
    │   │   ├── CrearBriefingPlantillaRequest.java # [NUEVO]
    │   │   ├── EnviarBriefingRequest.java   # [NUEVO]
    │   │   ├── ResponderBriefingRequest.java# [NUEVO]
    │   │   └── CrearComentarioRequest.java  # [NUEVO]
    │   └── response/
    │       ├── MensajeResponse.java         # [NUEVO]
    │       ├── SalaChatResponse.java        # [NUEVO]
    │       ├── BriefingResponse.java        # [NUEVO]
    │       ├── NotificacionResponse.java    # [NUEVO]
    │       ├── ComentarioResponse.java      # [NUEVO]
    │       └── SeguidorResponse.java        # [NUEVO]
    │
    └── social/                              # [M7]
        ├── request/
        │   ├── CrearSorteoRequest.java      # [NUEVO]
        │   ├── ActualizarSorteoRequest.java # [NUEVO]
        │   └── CrearResenaRequest.java      # [NUEVO]
        └── response/
            ├── SorteoResponse.java          # [NUEVO]
            ├── ParticipanteResponse.java    # [NUEVO]
            ├── GanadorResponse.java         # [NUEVO]
            └── ResenaResponse.java          # [NUEVO]
```

---

## 3. Plan de Refactorización y Reclasificación (Paso a Paso)

Para migrar los archivos existentes desde los paquetes planos actuales hacia la nueva estructura modular sin romper la compilación ni el funcionamiento en Spring Boot, sigue exactamente estos 4 pasos:

### Paso 1: Crear la Estructura de Directorios
Ejecuta desde la terminal en `Backend/src/main/java/uteq/edu/ec/artisync/` la creación de los subpaquetes de dominio dentro de cada capa:
```powershell
# En PowerShell (desde la raíz del proyecto)
$base = "artisync\Backend\src\main\java\uteq\edu\ec\artisync"
$capas = @("repository", "service", "controller", "dto")
$modulos = @("seguridad", "perfil", "catalogo", "pedido", "legal", "comunicacion", "social")

foreach ($capa in $capas) {
    if ($capa -eq "dto") {
        New-Item -ItemType Directory -Force -Path "$base\$capa\shared" | Out-Null
        foreach ($mod in $modulos) {
            New-Item -ItemType Directory -Force -Path "$base\$capa\$mod\request" | Out-Null
            New-Item -ItemType Directory -Force -Path "$base\$capa\$mod\response" | Out-Null
        }
    } elseif ($capa -eq "service") {
        New-Item -ItemType Directory -Force -Path "$base\$capa\shared" | Out-Null
        foreach ($mod in $modulos) {
            New-Item -ItemType Directory -Force -Path "$base\$capa\$mod\impl" | Out-Null
        }
    } else {
        foreach ($mod in $modulos) {
            New-Item -ItemType Directory -Force -Path "$base\$capa\$mod" | Out-Null
        }
    }
}
```

---

### Paso 2: Matriz de Migración de Clases Existentes

Mueve cada uno de los archivos existentes al nuevo subpaquete que le corresponde:

#### A) Capa `repository/` (21 repositorios existentes)
| Archivo Existente (`repository/`) | Subpaquete Destino (`repository.<modulo>`) | Módulo |
|:-----------------------------------|:--------------------------------------------|:-------|
| `UsuarioRepository.java` | `uteq.edu.ec.artisync.repository.seguridad` | M1 |
| `RolRepository.java` | `uteq.edu.ec.artisync.repository.seguridad` | M1 |
| `PermisoRepository.java` | `uteq.edu.ec.artisync.repository.seguridad` | M1 |
| `RolPermisoRepository.java` | `uteq.edu.ec.artisync.repository.seguridad` | M1 |
| `UsuarioRolRepository.java` | `uteq.edu.ec.artisync.repository.seguridad` | M1 |
| `SesionUsuarioRepository.java` | `uteq.edu.ec.artisync.repository.seguridad` | M1 |
| `TokenRecuperacionRepository.java` | `uteq.edu.ec.artisync.repository.seguridad` | M1 |
| `AutenticacionDosFactoresRepository.java` | `uteq.edu.ec.artisync.repository.seguridad` | M1 |
| `CodigoRespaldo2FaRepository.java` | `uteq.edu.ec.artisync.repository.seguridad` | M1 |
| `PaisRepository.java` | `uteq.edu.ec.artisync.repository.seguridad` | M1 |
| `PerfilCreadorRepository.java` | `uteq.edu.ec.artisync.repository.perfil` | M2 |
| `PortafolioRepository.java` | `uteq.edu.ec.artisync.repository.perfil` | M2 |
| `ServicioRepository.java` | `uteq.edu.ec.artisync.repository.catalogo` | M3 |
| `PedidoRepository.java` | `uteq.edu.ec.artisync.repository.pedido` | M4 |
| `HistorialEstadoPedidoRepository.java` | `uteq.edu.ec.artisync.repository.pedido` | M4 |
| `ContratoRepository.java` | `uteq.edu.ec.artisync.repository.legal` | M5 |
| `PagoGarantiaRepository.java` | `uteq.edu.ec.artisync.repository.legal` | M5 |
| `SalaChatRepository.java` | `uteq.edu.ec.artisync.repository.legal` | M5/M6 |
| `MensajeRepository.java` | `uteq.edu.ec.artisync.repository.legal` | M5/M6 |
| `InfraccionRepository.java` | `uteq.edu.ec.artisync.repository.comunicacion` | M6 |
| `SorteoRepository.java` | `uteq.edu.ec.artisync.repository.social` | M7 |

#### B) Capa `service/` y `service/impl/` (15 clases/interfaces existentes)
| Archivo Existente (`service/` o `service/impl/`) | Subpaquete Destino (`service.<modulo>` o `.impl`) | Módulo |
|:------------------------------------------------|:--------------------------------------------------|:-------|
| `AuthService.java` / `AuthServiceImpl.java` | `service.seguridad` / `service.seguridad.impl` | M1 |
| `TwoFactorService.java` / `TwoFactorServiceImpl.java` | `service.seguridad` / `service.seguridad.impl` | M1 |
| `UserService.java` / `UserServiceImpl.java` | `service.seguridad` / `service.seguridad.impl` | M1 |
| `AdminUserService.java` / `AdminUserServiceImpl.java` | `service.seguridad` / `service.seguridad.impl` | M1 |
| `RolePermissionService.java` / `RolePermissionServiceImpl.java` | `service.seguridad` / `service.seguridad.impl` | M1 |
| `PaisService.java` / `PaisServiceImpl.java` | `service.seguridad` / `service.seguridad.impl` | M1 |
| `EmailService.java` (`service/shared/`) | `service.shared` *(se mantiene en shared)* | M1+ |
| `SessionRevocationService.java` (`service/shared/`) | `service.shared` *(se mantiene en shared)* | M1+ |
| `UsuarioMapper.java` (`service/shared/`) | `service.shared` *(se mantiene en shared)* | M1+ |

#### C) Capa `controller/` (7 controladores existentes)
| Archivo Existente (`controller/`) | Subpaquete Destino (`controller.<modulo>`) | Módulo |
|:-----------------------------------|:-------------------------------------------|:-------|
| `AuthController.java` | `uteq.edu.ec.artisync.controller.seguridad` | M1 |
| `TwoFactorController.java` | `uteq.edu.ec.artisync.controller.seguridad` | M1 |
| `UserController.java` | `uteq.edu.ec.artisync.controller.seguridad` | M1 |
| `AdminUserController.java` | `uteq.edu.ec.artisync.controller.seguridad` | M1 |
| `RolePermissionController.java` | `uteq.edu.ec.artisync.controller.seguridad` | M1 |
| `PermissionController.java` | `uteq.edu.ec.artisync.controller.seguridad` | M1 |
| `PaisController.java` | `uteq.edu.ec.artisync.controller.seguridad` | M1 |

#### D) Capa `dto/request/` y `dto/response/` (25 DTOs existentes)
| Archivo Existente (`dto/request/` o `response/`) | Subpaquete Destino (`dto.<modulo>.request` / `response`) | Módulo |
|:------------------------------------------------|:---------------------------------------------------------|:-------|
| `MessageResponse.java` | `uteq.edu.ec.artisync.dto.shared` | Shared |
| `ErrorResponse.java` | `uteq.edu.ec.artisync.dto.shared` | Shared |
| `RegisterRequest.java`, `LoginRequest.java` | `uteq.edu.ec.artisync.dto.seguridad.request` | M1 |
| `TwoFactorRequest.java`, `TwoFactorConfirmRequest.java` | `uteq.edu.ec.artisync.dto.seguridad.request` | M1 |
| `RefreshTokenRequest.java`, `ForgotPasswordRequest.java` | `uteq.edu.ec.artisync.dto.seguridad.request` | M1 |
| `ResetPasswordRequest.java`, `ChangePasswordRequest.java` | `uteq.edu.ec.artisync.dto.seguridad.request` | M1 |
| `CreateUserRequest.java`, `UpdateUserRequest.java` | `uteq.edu.ec.artisync.dto.seguridad.request` | M1 |
| `AdminUpdateUserRequest.java`, `ChangeEstadoRequest.java` | `uteq.edu.ec.artisync.dto.seguridad.request` | M1 |
| `CreateRoleRequest.java`, `UpdateRoleRequest.java` | `uteq.edu.ec.artisync.dto.seguridad.request` | M1 |
| `AssignRolesRequest.java`, `SyncPermissionsRequest.java` | `uteq.edu.ec.artisync.dto.seguridad.request` | M1 |
| `PaisRequest.java` | `uteq.edu.ec.artisync.dto.seguridad.request` | M1 |
| `TokenResponse.java`, `UserResponse.java` | `uteq.edu.ec.artisync.dto.seguridad.response` | M1 |
| `TwoFactorSetupResponse.java` | `uteq.edu.ec.artisync.dto.seguridad.response` | M1 |
| `RolResponse.java`, `PermisoResponse.java` | `uteq.edu.ec.artisync.dto.seguridad.response` | M1 |
| `PaisResponse.java` | `uteq.edu.ec.artisync.dto.seguridad.response` | M1 |

---

### Paso 3: Refactorización Automática en IDE (o mediante declaración de paquete)
Al mover las clases a sus nuevos subdirectorios:
1. Actualiza la primera línea de cada archivo `.java` para reflejar su nuevo subpaquete:
   - Ejemplo en `AuthController.java`: cambiar `package uteq.edu.ec.artisync.controller;` por `package uteq.edu.ec.artisync.controller.seguridad;`.
   - Ejemplo en `UsuarioRepository.java`: cambiar `package uteq.edu.ec.artisync.repository;` por `package uteq.edu.ec.artisync.repository.seguridad;`.
2. Si realizas el movimiento usando la opción **"Refactor -> Move"** en un IDE como IntelliJ IDEA o Eclipse, el IDE actualizará automáticamente los `import` en todos los archivos del proyecto que dependan de estas clases.

---

### Paso 4: Verificación y Escaneo de Spring Boot
En Spring Boot 4.x / 3.x, la anotación `@SpringBootApplication` ubicada en `ArtisyncApplication.java` (paquete raíz `uteq.edu.ec.artisync`) escanea de forma recursiva **todos los subpaquetes** descendientes:
- **`@RestController` y `@Service`**: Seguirán siendo descubiertos e inyectados automáticamente sin importar en qué subpaquete de `controller.<modulo>` o `service.<modulo>` se encuentren.
- **`@EntityScan` y `@EnableJpaRepositories`**: Por defecto, Spring Data JPA escanea todo lo que esté bajo el paquete raíz de la aplicación.
  > [!TIP]
  > Si en algún momento defines configuraciones personalizadas en una clase `@Configuration` con `@EntityScan` o `@EnableJpaRepositories`, asegúrate de apuntar a los paquetes raíz intermedios (`uteq.edu.ec.artisync.entity` y `uteq.edu.ec.artisync.repository`) para que abarquen todos los subpaquetes de los 7 módulos.

---

## 4. Beneficios para el Trabajo en Equipo (PFC)

1. **Desacoplamiento Estricto (RNF-10)**: Cada módulo (M1 a M7) tiene sus propias entidades, repositorios, servicios y controladores agrupados vertical y horizontalmente.
2. **Minimización de Conflictos en Git**: Dos desarrolladores pueden trabajar en paralelo (por ejemplo, el Desarrollador A en el módulo `catalogo/` y el Desarrollador B en `pedido/`) sin tocar las mismas carpetas de repositorios o servicios ni generar conflictos en merge/rebase.
3. **Cohesión Alta y Bajo Acoplamiento**: Facilita la comprensión del código por parte del tribunal en la defensa oral, demostrando una arquitectura limpia, modular de escala profesional y lista para microservicios si fuese requerido a futuro.
