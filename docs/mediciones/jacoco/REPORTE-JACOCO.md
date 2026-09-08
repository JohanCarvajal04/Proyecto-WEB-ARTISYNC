# Reporte de Cobertura de Código — JaCoCo

- Fecha: 2026-09-05 (medición previa: 2026-09-04)
- Rama: `feat/ia-verificacion-asistida`
- Comando: `./mvnw.cmd -B clean test` (plugin `jacoco-maven-plugin` 0.8.13, ya configurado en
  `pom.xml` desde la entrega anterior — ver `OBS-09`)
- Suite: 1039 pruebas, 0 fallos, 0 errores (132 pruebas nuevas desde la medición del 09-04, en dos
  rondas: 61 para cerrar el umbral del 70% y 71 adicionales para subirlo por encima del 75% a
  pedido explícito, con margen — ver "Qué se cubrió en esta medición" más abajo)
- Artefactos crudos: [`report.xml`](report.xml), [`html/index.html`](html/index.html),
  [`html/jacoco.csv`](html/jacoco.csv)

## Resultado global

| Métrica | Cobertura |
|---|---|
| Lines | 4859 / 5601 = **86.75%** |
| Branches | 1232 / 1642 = **75.03%** |

## Resultado por capa (OBS-P1-01)

El criterio exige líneas Y ramas en cada una de las tres capas:

| Capa | Lines | Branches |
|---|---|---|
| Servicios (`service`) | 3836 / 4264 = **89.96%** | 1004 / 1266 = **79.30%** |
| Controladores (`controller`) | 326 / 377 = **86.47%** | 43 / 50 = **86.00%** |
| Global | 4859 / 5601 = **86.75%** | 1232 / 1642 = **75.03%** |

Cifras obtenidas agregando por paquete desde `html/jacoco.csv` (script
[`analyze_coverage.py`](../../../artisync/Backend/analyze_coverage.py)). Las tres capas y el
global superan ahora el 75% tanto en líneas como en ramas — la observación pendiente sobre ramas
globales (66.44%, bajo el 70% pedido) quedó cerrada el mismo día con la primera ronda (70.10%), y
se amplió a pedido explícito hasta 75.03% para dejar margen de seguridad (1232/1642 — 12 ramas de
más sobre el umbral exacto de 1232 antes de considerar el 75% comprometido) frente a cambios
futuros en el código que puedan mover el denominador.

## Historial de mediciones

| Fecha | Clases de test | Pruebas | Lines | Branches | Complexity |
|---|---|---|---|---|---|
| 2026-07-30 | 18 | 89 | 23.0% | 13.8% | 16.8% |
| 2026-08-16 (primera ronda) | 51 | 401 | 60.2% | 50.3% | 45.7% |
| 2026-08-16 (segunda ronda) | 58 | 522 | 72.0% | 62.5% | 56.5% |
| 2026-09-04 | 74 | 907 | 80.29% | 66.44% | — |
| 2026-09-05 (ronda 1 — cierre del 70%) | 76 | 968 | 82.88% | 70.10% | — |
| 2026-09-05 (ronda 2 — esta medición, margen sobre el 75%) | 82 | 1039 | **86.75%** | **75.03%** | — |

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

**OBS-P1-01 queda implementado**: el criterio exige líneas Y ramas por encima del umbral en las
tres capas (servicios, controladores, global), y las tres lo cumplen ahora — ver la tabla "Resultado
por capa" arriba. Antes de esta ronda, controladores estaba en 29.17% de líneas / 30.56% de ramas
(84 de 288 líneas cubiertas); ahora está en 83.82% / 72.00% (316 de 377). Se reporta el número
real medido desde `jacoco.csv`, sin ajustar el umbral ni excluir paquetes.

**Ramas globales al 70%**: hasta la medición del 2026-09-04 el global de ramas (66.44%) quedaba por
debajo del 70% pedido, aunque las tres capas ya lo cumplían individualmente. La ronda 1 del
2026-09-05 (ver "Qué se cubrió en esta medición" arriba) cerró esa brecha con 61 pruebas nuevas
dirigidas a ramas condicionales concretas (no cobertura genérica): 1151/1642 = 70.10%, por encima
del umbral, sin tocar `src/main`.

**Ramas globales al 75% (margen de seguridad)**: a pedido explícito de subir el número por encima
del mínimo — un cambio de código que agregue una rama sin test podría hacer bajar 70.10% de nuevo
por debajo de 70% — la ronda 2 (ver arriba) añadió 71 pruebas más dirigidas a ramas concretas
adicionales, sin tocar `src/main`: **1232/1642 = 75.03%**. Margen de 12 ramas sobre el mínimo
exacto (1232/1642 es el primer entero que redondea a ≥75.00%) — igual que antes, si una futura
medición añade clases nuevas al backend sin tests, es el primer número a revisar.
