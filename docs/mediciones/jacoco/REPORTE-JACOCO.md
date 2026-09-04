# Reporte de Cobertura de Código — JaCoCo

- Fecha: 2026-09-04
- Rama: `feat/ia-verificacion-asistida`
- Comando: `./mvnw.cmd -B clean test` (plugin `jacoco-maven-plugin` 0.8.13, ya configurado en
  `pom.xml` desde la entrega anterior — ver `OBS-09`)
- Suite: 907 pruebas, 0 fallos, 0 errores
- Artefactos crudos: [`report.xml`](report.xml), [`html/index.html`](html/index.html),
  [`html/jacoco.csv`](html/jacoco.csv)

## Resultado global

| Métrica | Cobertura |
|---|---|
| Lines | 4497 / 5601 = **80.29%** |
| Branches | 1091 / 1642 = **66.44%** |

## Resultado por capa (OBS-P1-01)

El criterio exige líneas Y ramas en cada una de las tres capas:

| Capa | Lines | Branches |
|---|---|---|
| Servicios (`service`) | 3523 / 4264 = **82.62%** | 887 / 1266 = **70.06%** |
| Controladores (`controller`) | 316 / 377 = **83.82%** | 36 / 50 = **72.00%** |
| Global | 4497 / 5601 = **80.29%** | 1091 / 1642 = **66.44%** |

Cifras obtenidas agregando por paquete desde `html/jacoco.csv` (script
[`analyze_coverage.py`](../../../artisync/Backend/analyze_coverage.py)). Las tres capas superan
ahora el 70% tanto en líneas como en ramas, salvo el global en ramas (66.44%), que no forma parte
del criterio de capas exigido por la guía.

## Historial de mediciones

| Fecha | Clases de test | Pruebas | Lines | Branches | Complexity |
|---|---|---|---|---|---|
| 2026-07-30 | 18 | 89 | 23.0% | 13.8% | 16.8% |
| 2026-08-16 (primera ronda) | 51 | 401 | 60.2% | 50.3% | 45.7% |
| 2026-08-16 (segunda ronda) | 58 | 522 | 72.0% | 62.5% | 56.5% |
| 2026-09-04 (esta medición) | 74 | 907 | **80.29%** | **66.44%** | — |

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
