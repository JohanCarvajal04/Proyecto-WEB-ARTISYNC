# Reporte de Cobertura de Código — JaCoCo

- Fecha: 2026-08-16
- Commit: `11ac931` (rama `feat/ia-verificacion-asistida`)
- Comando: `./mvnw.cmd -B clean test` (plugin `jacoco-maven-plugin` 0.8.13, ya configurado en
  `pom.xml` desde la entrega anterior — ver `OBS-09`)
- Suite: 522 pruebas, 0 fallos, 0 errores
- Artefactos crudos: [`report.xml`](report.xml), [`html/index.html`](html/index.html),
  [`html/jacoco.csv`](html/jacoco.csv)

## Resultado global

| Métrica | Cobertura |
|---|---|
| Lines | 2867 / 3981 = **72.0%** |
| Branches | 703 / 1125 = **62.5%** |
| Instructions | 12825 / 18404 = **69.7%** |
| Complexity | 775 / 1371 = **56.5%** |

## Historial de mediciones

| Fecha | Clases de test | Pruebas | Lines | Branches | Complexity |
|---|---|---|---|---|---|
| 2026-07-30 | 18 | 89 | 23.0% | 13.8% | 16.8% |
| 2026-08-16 (primera ronda) | 51 | 401 | 60.2% | 50.3% | 45.7% |
| 2026-08-16 (esta medición) | 58 | 522 | **72.0%** | 62.5% | 56.5% |

La primera ronda de esta fecha cerró el hueco de `catalogo`, `pedido`, `legal` y parte de
`perfil`, que hasta entonces no tenían ninguna prueba de servicio. La segunda ronda amplió
`seguridad` (`AuthServiceImpl`, `AdminUserServiceImpl`, `RolePermissionServiceImpl`,
`UserServiceImpl`, `PaisServiceImpl`), completó `PedidoServicioImpl` y `PagoServicioImpl` (antes
solo cubiertos parcialmente) y `SorteoServiceImpl`, para subir del 60% al 72%.

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

Quedan sin cobertura de servicio, como trabajo pendiente para una próxima ronda:
`TwoFactorServiceImpl` (parcial), `BriefingServiceImpl` (parcial), `EntregableServicioImpl`
(parcial), `NotificacionServiceImpl`, `InfraccionServiceImpl`, `AuditServiceImpl`,
`PdfGeneracionServicioImpl`, `ServicioSpecification`, y los servicios de infraestructura
(`SessionRevocationService`, `IntentosAutenticacionService`, `EmailService`, `AlmacenamientoAzure`,
los clientes de IA) que dependen de SDKs externos y son más costosos de mockear.

## Nota de cumplimiento

El umbral de referencia de la guía (≥60% de líneas) **se cumple con margen**: 72.0%, dentro del
rango 60–72% aceptado. Se reporta el número real medido, sin ajustar el umbral ni excluir
paquetes para maquillar el resultado. Branches (62.5%) también supera el 60%; Complexity (56.5%)
queda algo por debajo, pero el compromiso original (OBS-09) se refería solo a líneas.
