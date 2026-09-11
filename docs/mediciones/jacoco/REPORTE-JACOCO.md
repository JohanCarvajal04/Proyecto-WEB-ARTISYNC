# Reporte de Cobertura de Código — JaCoCo

- Fecha: 2026-09-11, ronda 2 — cierre de la regresión de controladores (medición anterior el mismo
  día, ronda 1: ver "Historial de mediciones")
- Rama: `main`, commit base `e84ab50` + pruebas nuevas de esta ronda (sin commit al momento de esta
  medición)
- Comando: `./mvnw.cmd -B clean test` (plugin `jacoco-maven-plugin` 0.8.13, ya configurado en
  `pom.xml` desde la entrega anterior — ver `OBS-09`)
- Suite: 1230 pruebas, 129 clases de test, 0 fallos, 0 errores (43 pruebas nuevas desde la ronda 1
  de hoy: 5 clases de test Mockito para los controladores sin cobertura propia y una suite completa
  para `RespaldoServicioImpl`, que no tenía ninguna prueba)
- Artefactos crudos: [`report.xml`](report.xml), [`html/index.html`](html/index.html),
  [`html/jacoco.csv`](html/jacoco.csv)

## Resultado global

| Métrica | Cobertura |
|---|---|
| Lines | 6023 / 7263 = **82.93%** |
| Branches | 1548 / 2165 = **71.50%** |

## Resultado por capa (OBS-P1-01)

El criterio exige líneas Y ramas en cada una de las tres capas:

| Capa | Lines | Branches |
|---|---|---|
| Servicios (`service`) | 4778 / 5563 = **85.89%** | 1273 / 1673 = **76.09%** |
| Controladores (`controller`) | 393 / 456 = **86.18%** | 64 / 82 = **78.05%** |
| Global | 6023 / 7263 = **82.93%** | 1548 / 2165 = **71.50%** |

**OBS-P1-01 vuelve a cumplirse**: las tres capas superan el 70% de líneas y ramas, con margen
razonable en las tres. Cifras obtenidas agregando por paquete desde `html/jacoco.csv` (script
[`analyze_coverage.py`](../../../artisync/Backend/analyze_coverage.py)).

### Qué cerró la regresión de esta mañana

La medición de la ronda 1 de hoy (ver "Historial de mediciones") había detectado que la capa de
controladores cayó a 67.07% de ramas (por debajo del 70% exigido) porque cinco controladores
incorporados en V45-V48 no tenían ninguna prueba propia — solo la cobertura indirecta que dejaban
los tests de sus servicios:

- `RespaldoControlador` (21 líneas sin cubrir de 21): CRUD de respaldos manuales/programados,
  incluida la descarga con headers `Content-Disposition`/`Content-Length`
- `SubcategoriaControlador` (9/9): incluye la rama `esModerador` (moderador vs. creador autoservicio)
- `PagoControlador` (6/6): incluye ambas ramas de `cancelarPago` (con/sin cuerpo opcional)
- `PlantillaAcuerdoCreadorControlador` (5/5)
- `EtiquetaControlador` (5/5)

Se añadió una clase de test por controlador (patrón ya establecido para la inmensa mayoría de
controladores del proyecto: Mockito puro, `@ExtendWith(MockitoExtension.class)` +
`@Mock`/`@InjectMocks`, invocación directa del método — no `@WebMvcTest` con `MockMvc`, que en este
proyecto solo se usa para casos puntuales de seguridad como `SecurityConfigTest` y
`CategoriaAutorizacionTest`). El `@PreAuthorize` no se ejercita vía Spring Security en estas
pruebas nuevas, solo la lógica del método. 30 pruebas nuevas, camino feliz + las ramas
condicionales propias de cada controlador.

Adicionalmente, `RespaldoServicioImpl` (el servicio detrás de `RespaldoControlador`) tampoco tenía
ninguna prueba — 0/14 ramas cubiertas. Se añadieron 13 pruebas (`RespaldoServicioImplTest`) usando
archivos reales en un `@TempDir` para ejercitar `descargar`/`eliminar` sin mockear el filesystem
(mismo criterio que `TwoFactorServiceImpl` con TOTP real): cuota de respaldo en progreso, las dos
ramas de `descargar` (sin ruta / archivo inexistente en disco / archivo real) y de `eliminar` (en
progreso / incrementales dependientes / con y sin archivo en disco). Resultado: 14/14 ramas.

Ningún cambio de producción en esta ronda: todas las pruebas se añadieron contra el código ya
existente, sin tocar `src/main`.

## Historial de mediciones

| Fecha | Clases de test | Pruebas | Lines | Branches | Complexity |
|---|---|---|---|---|---|
| 2026-07-30 | 18 | 89 | 23.0% | 13.8% | 16.8% |
| 2026-08-16 (primera ronda) | 51 | 401 | 60.2% | 50.3% | 45.7% |
| 2026-08-16 (segunda ronda) | 58 | 522 | 72.0% | 62.5% | 56.5% |
| 2026-09-04 | 74 | 907 | 80.29% | 66.44% | — |
| 2026-09-05 (ronda 1 — cierre del 70%) | 76 | 968 | 82.88% | 70.10% | — |
| 2026-09-05 (ronda 2 — margen sobre el 75%) | 82 | 1039 | 86.75% | 75.03% | — |
| 2026-09-11 (ronda 1 — código nuevo V45-V48 sin cobertura completa, OBS-P1-01 incumplido) | — | 1187 | 81.69% | 70.44% | — |
| 2026-09-11 (ronda 2 — esta medición, cierre de la regresión) | 129 | 1230 | **82.93%** | **71.50%** | — |

## Qué se cubrió en esta medición (66.44% → 70.10% en ramas globales)

61 pruebas nuevas, todas Mockito puras salvo `PermissionControllerTest` y `BirthDateValidatorTest`
(JUnit plano, sin mocks de framework), dirigidas específicamente a las ramas condicionales que el
`jacoco.csv` de la medición anterior marcaba como no cubiertas (`BRANCH_MISSED`), no a métodos
nuevos:

- `SeguidorServicioImpl` (comunicación — módulo de seguidores): `listarSeguidores` y
  `listarCreadoresSeguidosNovedades` (0% de cobertura hasta ahora — nunca se habían invocado desde
  un test), variantes de `obtenerEstadoSeguimiento` (visitante anónimo, visitante autenticado que
  es/no es seguidor, `total` nulo), `dejarDeSeguirCreador` con perfil inexistente,
  `actualizarPortadaYTitulo` (ambos campos informados / ambos nulos).
- `LikePortafolioServiceImpl` (comunicación): sin ninguna prueba antes de esta ronda — cubiertas
  `darLike`/`quitarLike`/`obtenerEstado` completos (éxito, duplicado, recurso no encontrado,
  usuario anónimo).
- `IntentosAutenticacionService` (cuota de intentos por cuenta, Redis): sin ninguna prueba antes —
  cubiertos primer intento (fija TTL), dentro del límite, cuota excedida, y el camino fail-open
  cuando Redis no responde (`DataAccessException`), tanto en `verificarCuota` como en `limpiar`.
- `PermissionController`: sin ninguna prueba antes — 401 sin autenticación / no autenticada, y el
  filtrado de authorities `ROLE_*` al listar permisos.
- `BirthDateValidator`: sin ninguna prueba antes — las 4 ramas del validador (nulo válido, fecha
  futura, más antigua que `maxAgeYears`, no alcanza `minAge`).
- `AuditoriaServicioImpl`: las 7 ramas de `filtrosLegibles` (una por campo del filtro) y las 2 de
  `paginaSegura` (orden permitido vs. columna sin índice, que antes solo se ejercitaban con un
  `FiltroAuditoria`/`Pageable` vacíos).
- `RolePermissionServiceImpl`: `updateRole` con rol inexistente, la captura de auditoría "antes" en
  `syncPermissions` (rol con permisos / sin permisos — antes nunca se stubbeaba
  `findByNombreRol`, así que esa rama jamás se ejecutaba), y en `createRole` el caso sin permisos
  iniciales + rol sin permisos, y el `orElseThrow` de error interno tras `crearRol`.
- `TwoFactorServiceImpl`: `confirm2Fa` completo (0% de cobertura hasta ahora: usuario no
  encontrado, 2FA no iniciado, código TOTP inválido con incremento de cuota, y **código TOTP válido
  real** generado en el propio test con la librería `GoogleAuthenticator` — no un mock — para
  ejercitar la rama de éxito genuina); `disable2Fa` con usuario no encontrado, 2FA no configurado y
  2FA no activo (antes solo se probaban los casos de código correcto/incorrecto).
- `UserServiceImpl`: `updateCurrentUser` con `apellidos`/`fechaNacimiento` informados y con
  `nombres`/`apellidos` en blanco (antes solo se probaba `nombres`+`idPais`).

Ningún cambio de producción: todas las pruebas se añadieron contra el código ya existente, sin
tocar `src/main`.

## Ronda 2 (2026-09-05, misma tarde) — margen sobre el 75% (70.10% → 75.03%)

A petición explícita de subir el número por encima de lo estrictamente pedido (para no quedar al
filo si el código se mueve), se añadieron 71 pruebas más — mismo criterio: dirigidas a ramas
condicionales concretas del `jacoco.csv`, no cobertura genérica ni cambios en `src/main`:

- `JwtService`: getters (`getExpirationMs`/`getRefreshExpirationMs`), `extraerJti`,
  `extraerTiempoRestante` (token fresco y expirado dentro/fuera de la tolerancia de reloj de 60s),
  `esAccessTokenValido`/`esRefreshTokenValido` con token malformado, cuenta bloqueada, tipo de
  token incorrecto y expiración manual — 16 ramas, todas con tokens JWT reales generados en el
  test (mismo criterio que la suite original: JwtService nunca se mockea).
- `ReporteContratoServicioImpl`: sin ninguna prueba antes de esta ronda — `listar`, `exportar`
  (tope de filas excedido, filtros completos con los 4 campos, `soloFirmados=false`, filtros
  vacíos) y la suma de `precioPactado` en el total del reporte.
- `SorteoServiceImpl`: `crearSorteo` sin perfil de creador, `actualizarSorteo` aplicando una nueva
  `fechaCierre` sin participantes (antes solo se probaba el rechazo con participantes),
  `actualizarSorteo` sin perfil de creador, y las ramas `yoParticipo` de
  `listarSorteosPorCreador`/`listarSorteosActivos` con un usuario autenticado (antes solo se
  probaba con `idUsuarioActual = null`).
- `PedidoServicioImpl`: `cancelarPropuestaTerminos` completo (0% de cobertura — éxito, no
  propietario), las 3 ramas de error de `obtenerPropuestaPendienteDelPedido` (propuesta inexistente,
  de otro pedido, ya resuelta), `obtenerPropuestaPendiente` completo (éxito, sin propuesta,
  usuario ajeno) y el auto-rechazo en `rechazarPropuestaTerminos`.
- `ServicioCatalogoServicioImpl`: valores por defecto de `crearServicio` cuando `tipoItem`/
  `cargoRevisionAdicional`/`limiteRevisionesBase` no vienen informados, `actualizarServicio` con
  todos los campos opcionales nulos o en blanco, `listarServiciosPorCreador` con estado en blanco,
  perfil sin usuario asociado (en `crearServicio` y al mapear la respuesta), y autenticación no
  autenticada (anónima) en `validarPropiedadOAdmin`.
- `AdminUserServiceImpl`: `exportar` completo (0% de cobertura — tope de filas excedido, filtros
  completos, `soloFirmados=false`/filtros vacíos), `updateUser` con `apellidos`/`fechaNacimiento`,
  la captura de auditoría "antes" en `assignRoles` con roles previos, y el `catch` de
  `StoredProcedureExceptionTranslator` cuando `sincronizarRoles` falla por rol inexistente.
- `PortafolioServicioImpl`: `actualizarPortafolio` sin ningún campo informado, y
  `obtenerPortafolioPorId` cuando el portafolio no tiene perfil asociado (`idPerfil` nulo en la
  respuesta).

Quedan sin cubrir (candidatos para una futura ronda, ninguno bloquea el 75% ya alcanzado):
`EventoAuditoriaSpecification`/`ServicioSpecification`/`PagoGarantiaSpecification` (Criteria API,
requieren `@DataJpaTest` en vez de Mockito puro), `PagoServicioImpl` (integración PayPal —
`RestTemplate`, verificación de firma de webhook), `ContratoRepositoryImpl` (`JdbcTemplate` a
medida), `AspectoAuditoria` (AOP, requiere un `ProceedingJoinPoint` de prueba), y los clientes de
IA (`GeminiIaService`/`NvidiaIaService`/`AbstractIaService`, ya señalados como costosos de
mockear).

La primera ronda de agosto cerró el hueco de `catalogo`, `pedido`, `legal` y parte de `perfil`,
que hasta entonces no tenían ninguna prueba de servicio. La segunda ronda amplió `seguridad`
(`AuthServiceImpl`, `AdminUserServiceImpl`, `RolePermissionServiceImpl`, `UserServiceImpl`,
`PaisServiceImpl`), completó `PedidoServicioImpl` y `PagoServicioImpl` (antes solo cubiertos
parcialmente) y `SorteoServiceImpl`, para subir del 60% al 72%. La ronda de septiembre cerró la
capa de controladores (hasta entonces prácticamente sin pruebas propias, dependía solo de la
cobertura indirecta de los servicios) añadiendo pruebas `@WebMvcTest` para `catalogo`,
`comunicacion`, `legal`, `pedido`, `perfil` y `social`, y amplió servicios puntuales
(`SessionRevocationService`, `ComentarioPortafolioServiceImpl`, `EntregableServicioImpl`).

## Qué se cubrió en esta medición

Pruebas Mockito puras (mismo patrón que las ya existentes: `@ExtendWith(MockitoExtension.class)`,
`@Mock`/`@InjectMocks`, AssertJ, casos de camino feliz + cada excepción de negocio), añadidas o
ampliadas en:

- `seguridad`: `AuthServiceImpl` (register/login/verify2Fa/refreshToken/logout/forgotPassword/resetPassword
  completos), `AdminUserServiceImpl` (createUser/updateUser/assignRoles/deleteUser/revokeUserSessions),
  `RolePermissionServiceImpl` (getAllRoles/getAllPermisos/getPermissionsByRole/syncPermissions/createRole),
  `UserServiceImpl` (changePassword/revokeAllMySessions), `PaisServiceImpl` (getPaisById/updatePais)
- `pedido`: `PedidoServicioImpl` (obtener/listar/avanzarEtapa/historial/seguimiento, complementario
  al test de flujo RF-19 ya existente)
- `legal`: `PagoServicioImpl` (crearOrdenPayPal/obtenerEstadoPago, complementario al test de webhook
  ya existente), `ContratoServicioImpl`
- `catalogo`: `ServicioCatalogoServicioImpl`, `CategoriaServicioImpl`, `EtiquetaServicioImpl`
- `social`: `SorteoServiceImpl` (obtenerSorteo/actualizarSorteo/eliminarSorteo/cancelarParticipacion/
  listarGanadores, complementario a los tests ya existentes)
- `perfil`: `PortafolioServicioImpl`, `PerfilCreadorServicioImpl`, `CertificadoIaServicioImpl`
- `shared`: `UsuarioMapper`

La ronda de septiembre (esta medición) cerró la capa de controladores, que hasta entonces solo
tenía la cobertura indirecta que dejaban los tests de servicio, y algunos servicios puntuales que
quedaban pendientes:

- `controller.catalogo`: `ServicioControlador`
- `controller.comunicacion`: `BriefingControlador`, `ChatControlador`, `ComentarioPortafolioControlador`,
  `LikePortafolioControlador`, `NotificacionControlador`, `SeguidorControlador`
- `controller.legal`: `ContratoControlador`, `EntregableControlador`
- `controller.pedido`: `FlujoTrabajoControlador`, `PedidoControlador`, `TicketRevisionControlador`
- `controller.perfil`: `PerfilCreadorControlador`, `PortafolioControlador` (y ampliación de
  `VerificacionControladorTest`)
- `controller.social`: `ResenaControlador`, `SorteoControlador`
- `controller.seguridad`: ampliación de `AuthControllerTest` y `UserControllerTest`
- `service.comunicacion`: `ComentarioPortafolioServiceImpl`
- `service.legal`: ampliación de `EntregableServicioImplTest`
- `service.shared`: `SessionRevocationService`

Pruebas `@WebMvcTest` con `MockMvc` + `@MockitoBean` para los controladores (mismo patrón: camino
feliz + validación + errores de negocio mapeados a códigos HTTP), y Mockito puro para los
servicios, siguiendo la convención ya establecida.

Quedan sin cobertura de servicio, como trabajo pendiente para una próxima ronda:
`TwoFactorServiceImpl` (parcial), `BriefingServiceImpl` (parcial), `NotificacionServiceImpl`,
`InfraccionServiceImpl`, `AuditServiceImpl`, `PdfGeneracionServicioImpl`, `ServicioSpecification`,
y los servicios de infraestructura (`IntentosAutenticacionService`, `EmailService`,
`AlmacenamientoAzure`, los clientes de IA) que dependen de SDKs externos y son más costosos de
mockear.

## Nota de cumplimiento

**OBS-P1-01 vuelve a cumplirse (2026-09-11, ronda 2)**: el criterio exige líneas Y ramas por encima
del 70% en cada una de las tres capas. Las tres lo cumplen ahora: servicios 85.89%/76.09%,
controladores 86.18%/78.05%, global 82.93%/71.50%. La regresión detectada en la ronda 1 de hoy
(controladores en 67.07% de ramas, por debajo del umbral, por cinco controladores V45-V48 sin
prueba propia — ver "Qué cerró la regresión de esta mañana" arriba) quedó resuelta con 43 pruebas
nuevas dirigidas específicamente a esos controladores y a `RespaldoServicioImpl` (0% → 100% de
ramas), sin tocar `src/main`. Se reporta el número real medido desde `jacoco.csv`, sin ajustar el
umbral ni excluir paquetes.

El margen sobre el 70% en controladores (78.05% de ramas) es más ajustado que en servicios: los
próximos candidatos con mayor déficit de ramas a nivel de servicio (`GeneradorXlsx` 39,
`ServicioCatalogoServicioImpl` 35, `AdminUserServiceImpl` 21, `PagoServicioImpl` 17,
`PedidoServicioImpl` 15 — ver `analyze_coverage.py`) quedan como trabajo pendiente para una próxima
ronda; ninguno bloquea el cumplimiento actual de OBS-P1-01.

**Ramas globales al 70%**: hasta la medición del 2026-09-04 el global de ramas (66.44%) quedaba por
debajo del 70% pedido, aunque las tres capas ya lo cumplían individualmente. La ronda 1 del
2026-09-05 (ver "Qué se cubrió en esta medición" arriba) cerró esa brecha con 61 pruebas nuevas
dirigidas a ramas condicionales concretas (no cobertura genérica): 1151/1642 = 70.10%, por encima
del umbral, sin tocar `src/main`.

**Ramas globales al 75% (margen de seguridad, 2026-09-05)**: a pedido explícito de subir el número
por encima del mínimo, la ronda 2 del 2026-09-05 añadió 71 pruebas más dirigidas a ramas concretas
adicionales, sin tocar `src/main`: 1232/1642 = 75.03%. Ese margen se consumió por completo con el
código nuevo incorporado después (V45-V48): la ronda 1 del 2026-09-11 quedó en 1525/2165 = 70.44%
global, y la capa de controladores cayó por debajo del umbral (ver arriba). La ronda 2 del mismo día
recuperó margen en controladores (78.05% de ramas) pero el global (71.50%) sigue sin el colchón que
tenía el 2026-09-05: si una futura medición añade clases nuevas al backend sin tests equivalentes,
este es el primer número a revisar.
