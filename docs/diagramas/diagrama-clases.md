# Diagrama C4 Nivel 4: Código (Class Diagram) - Artisync PFC (Backend)

Este documento describe el **Nivel 4 (Código)** de la metodología **C4 Model** aplicado al componente de persistencia y dominio del backend de Artisync. Detalla la estructura interna orientada a objetos (Entidades JPA), sus atributos y multiplicidad de relaciones (agregación, composición, asociación). 

Como se definió en el **Nivel 3 (Componentes)**, estas clases pertenecen a la capa **ORM / Entidades JPA (`entity.*` + Hibernate)** del contenedor de la API REST, y representan el modelo de negocio central del sistema sobre el cual operan los Controladores y Servicios transaccionales.

## 1. Mapeo de Módulos (Namespaces) a Capas de Negocio

| Módulo / Paquete | Descripción y Responsabilidad en el Dominio |
| :--- | :--- |
| **seguridad** | Implementa la base de control de acceso (RBAC), registro de `Usuario`, 2FA, y gestión de sesiones delegadas. |
| **perfil** | Gestiona los perfiles públicos de los creadores, sus habilidades y su portafolio de trabajos verificados por IA. |
| **catalogo** | Define la estructura de oferta de los creadores mediante la categorización y configuración de un `Servicio` (Gig). |
| **pedido** | Orquesta el ciclo de vida central de un `Pedido` mediante máquinas de estado (`FlujoTrabajo`) e incidencias (`TicketRevision`). |
| **legal** | Formaliza la transacción financiera (`Contrato`), el depósito en garantía (`PagoGarantia`), chat y la entrega final de archivos (`EntregableFinal`). |
| **comunicacion** | Sistema de notificaciones asíncronas, seguimiento (Follows), mensajería, onboarding de pedidos (`Briefing`) y moderación de contenido. |
| **social** | Interacciones de comunidad como calificaciones (`ResenaServicio`) y sorteos promocionales (`Sorteo`). |

---

## 2. Visualización con Mermaid (Renderizado Nativo)

A continuación se muestra el modelo estructural detallado. Debido a la magnitud del backend completo, puede que en algunas plataformas requieras desplazar o hacer zoom sobre el gráfico.

```mermaid
classDiagram
  direction LR

  namespace seguridad {
    class Pais {
      - idPais : Long
      - nombrePais : String
    }
    class Usuario {
      - idUsuario : Long
      - nombres : String
      - apellidos : String
      - correo : String
      - contrasenaHash : String
      - fechaRegistro : LocalDateTime
      - actualizadoEn : LocalDateTime
      - fechaNacimiento : LocalDate
    }
    class Rol {
      - idRol : Long
      - nombreRol : String
      - descripcionRol : String
    }
    class Permiso {
      - idPermiso : Long
      - nombrePermiso : String
      - moduloAplicacion : String
    }
    class RolPermiso {
      - idRolPermiso : Long
    }
    class UsuarioRol {
      - idUsuarioRol : Long
    }
    class SesionUsuario {
      - idSesion : Long
      - jti : String
      - direccionIp : String
      - fechaCreacion : LocalDateTime
      - fechaExpiracion : LocalDateTime
    }
    class AutenticacionDosFactores {
      - id2fa : Long
      - llaveSecreta : String
    }
    class CodigoRespaldo2Fa {
      - idCodigo : Long
      - codigoHash : String
    }
    class TokenRecuperacion {
      - idToken : Long
      - hashToken : String
      - fechaGeneracion : LocalDateTime
    }
    class EventoAuditoria {
      - idEvento : Long
      - entidad : String
      - accion : String
      - idRegistro : Long
      - datosAntiguos : String
      - datosNuevos : String
      - usuarioResponsable : String
      - ipOrigen : String
      - fechaEvento : LocalDateTime
    }
  }

  namespace perfil {
    class PerfilCreador {
      - idPerfil : Long
      - biografia : String
      - urlRedSocial : String
    }
    class Habilidad {
      - idHabilidad : Long
      - nombreHabilidad : String
    }
    class CreadorHabilidad {
      - idCreadorHabilidad : Long
      - nivelDominio : String
    }
    class EstadoVerificacion {
      - idEstadoVerificacion : Long
      - nombreEstado : String
    }
    class CertificadoIa {
      - idCertificado : Long
      - urlDocumentoAzure : String
      - puntajeConfianzaIa : BigDecimal
      - fechaAnalisis : LocalDateTime
    }
    class Portafolio {
      - idPortafolio : Long
      - fechaCreacion : LocalDateTime
    }
    class PortafolioItem {
      - idItemPortafolio : Long
      - tituloObra : String
      - descripcionObra : String
      - urlArchivoMultimedia : String
      - fechaSubida : LocalDateTime
    }
  }

  namespace catalogo {
    class Categoria {
      - idCategoria : Long
      - nombreCategoria : String
    }
    class Subcategoria {
      - idSubcategoria : Long
      - nombreSubcategoria : String
    }
    class Etiqueta {
      - idEtiqueta : Long
      - nombreEtiqueta : String
    }
    class AtributoDinamico {
      - idAtributo : Long
      - nombreAtributo : String
      - tipoDato : String
    }
    class Servicio {
      - idServicio : Long
      - tituloServicio : String
      - descripcionDetallada : String
      - precioBase : BigDecimal
      - urlMiniatura : String
    }
    class ServicioAtributo {
      - idServicioAtributo : Long
      - valorAsignado : String
    }
    class ServicioEtiqueta {
      - idServicioEtiqueta : Long
    }
  }

  namespace pedido {
    class FlujoTrabajo {
      - idFlujo : Long
      - nombreFlujo : String
      - descripcionFlujo : String
    }
    class EtapaFlujo {
      - idEtapa : Long
      - nombreEtapa : String
    }
    class FlujoEtapaConfig {
      - idFlujoEtapa : Long
      - numeroOrden : Integer
    }
    class Pedido {
      - idPedido : Long
      - fechaInicio : LocalDateTime
      - fechaEntregaEstimada : LocalDateTime
      - precioPactado : BigDecimal
    }
    class HistorialEstadoPedido {
      - idHistorialEstado : Long
      - fechaTransicion : LocalDateTime
      - observacion : String
    }
    class MotivoRechazo {
      - idMotivo : Long
      - descripcionMotivo : String
    }
    class TicketRevision {
      - idTicket : Long
      - descripcionCliente : String
      - estadoTicket : String
      - fechaApertura : LocalDateTime
      - fechaResolucion : LocalDateTime
    }
    class PlantillaContrato {
      - idPlantilla : Long
      - versionLegal : String
      - cuerpoHtmlPlantilla : String
    }
  }

  namespace legal {
    class Contrato {
      - idContrato : Long
      - hashFirmaCliente : String
      - hashFirmaCreador : String
      - fechaFormalizacion : LocalDateTime
      - urlDocumentoPdf : String
    }
    class PagoGarantia {
      - idPago : Long
      - idOrdenPaypal : String
      - montoRetenido : BigDecimal
    }
    class TransaccionPago {
      - idTransaccion : Long
      - tipoTransaccion : String
      - monto : BigDecimal
      - fechaEjecucion : LocalDateTime
    }
    class SalaChat {
      - idSala : Long
      - fechaApertura : LocalDateTime
    }
    class Mensaje {
      - idMensaje : Long
      - cuerpoMensaje : String
      - fechaHoraEnvio : LocalDateTime
    }
    class DocumentoAdjunto {
      - idAdjunto : Long
      - urlArchivo : String
      - tipoMime : String
      - pesoBytes : Long
    }
    class EntregableFinal {
      - idEntregable : Long
      - urlVersionMarcaAgua : String
      - urlVersionLimpia : String
      - estadoAprobacion : String
      - fechaEntrega : LocalDateTime
    }
  }

  namespace comunicacion {
    class TipoNotificacion {
      - idTipoNotificacion : Long
      - nombreEvento : String
      - formatoMensaje : String
    }
    class NotificacionSistema {
      - idNotificacion : Long
      - fechaEmision : LocalDateTime
    }
    class Seguidor {
      - idSeguimiento : Long
      - fechaSeguimiento : LocalDateTime
    }
    class BriefingPlantilla {
      - idBriefingPlantilla : Long
      - nombrePlantilla : String
      - fechaCreacion : LocalDateTime
    }
    class BriefingPregunta {
      - idPregunta : Long
      - textoPregunta : String
      - numeroOrden : Integer
    }
    class BriefingEnviado {
      - idBriefingEnviado : Long
      - fechaEnvio : LocalDateTime
    }
    class BriefingRespuesta {
      - idRespuesta : Long
      - textoRespuesta : String
      - fechaRespuesta : LocalDateTime
    }
    class LikePortafolio {
      - idLike : Long
      - fechaLike : LocalDateTime
    }
    class ComentarioPortafolio {
      - idComentario : Long
      - textoComentario : String
      - fechaPublicacion : LocalDateTime
    }
    class InfraccionMensaje {
      - idInfraccion : Long
      - mensajeOriginal : String
      - patronDetectado : String
      - fechaInfraccion : LocalDateTime
    }
  }

  namespace social {
    class Sorteo {
      - idSorteo : Long
      - tituloSorteo : String
      - descripcionPremios : String
      - fechaInicio : LocalDateTime
      - fechaCierre : LocalDateTime
    }
    class ParticipanteSorteo {
      - idParticipacion : Long
      - fechaInscripcion : LocalDateTime
      - fechaNotificacionPremio : LocalDateTime
    }
    class ResenaServicio {
      - idResena : Long
      - calificacionEstrellas : Integer
      - textoResena : String
      - fechaResena : LocalDateTime
    }
  }

  %% ---------- seguridad ----------
  Usuario "0..*" --> "1" Pais : pais
  Usuario "1" --> "0..1" AutenticacionDosFactores : 2FA
  AutenticacionDosFactores "1" *-- "0..*" CodigoRespaldo2Fa : codigos respaldo
  Usuario "1" *-- "0..*" SesionUsuario : sesiones
  Usuario "1" *-- "0..*" TokenRecuperacion : tokens
  Usuario "1" --> "0..*" UsuarioRol : asignaciones
  Rol "1" --> "0..*" UsuarioRol : asignaciones
  Rol "1" --> "0..*" RolPermiso : permisos
  Permiso "1" --> "0..*" RolPermiso : permisos

  %% ---------- perfil ----------
  PerfilCreador "1" --> "1" Usuario : usuario
  PerfilCreador "1" --> "0..*" CreadorHabilidad : habilidades
  Habilidad "1" --> "0..*" CreadorHabilidad : habilidades
  PerfilCreador "1" *-- "0..*" CertificadoIa : certificaciones IA
  CertificadoIa "0..*" --> "1" EstadoVerificacion : estado
  PerfilCreador "1" *-- "0..1" Portafolio : portafolio
  Portafolio "1" *-- "0..*" PortafolioItem : items

  %% ---------- catalogo ----------
  Categoria "1" *-- "0..*" Subcategoria : subcategorias
  Servicio "0..*" --> "1" PerfilCreador : perfil
  Servicio "0..*" --> "1" Subcategoria : subcategoria
  Servicio "1" --> "0..*" ServicioAtributo : atributos
  AtributoDinamico "1" --> "0..*" ServicioAtributo : atributos
  Servicio "1" --> "0..*" ServicioEtiqueta : etiquetas
  Etiqueta "1" --> "0..*" ServicioEtiqueta : etiquetas

  %% ---------- pedido ----------
  FlujoTrabajo "1" *-- "0..*" FlujoEtapaConfig : etapas config
  EtapaFlujo "1" --> "0..*" FlujoEtapaConfig : etapas config
  Pedido "0..*" --> "1" Usuario : usuarioCliente
  Pedido "0..*" --> "1" Servicio : servicio
  Pedido "0..*" --> "1" FlujoTrabajo : flujo
  Pedido "1" *-- "0..*" HistorialEstadoPedido : historial
  HistorialEstadoPedido "0..*" --> "1" EtapaFlujo : etapa
  Pedido "1" --> "0..*" TicketRevision : tickets
  TicketRevision "0..*" --> "1" MotivoRechazo : motivo

  %% ---------- legal ----------
  Contrato "1" --> "1" Pedido : pedido
  Contrato "0..*" --> "1" PlantillaContrato : plantilla
  Contrato "1" *-- "0..1" PagoGarantia : garantia escrow
  PagoGarantia "1" *-- "0..*" TransaccionPago : movimientos
  SalaChat "1" --> "1" Pedido : pedido
  SalaChat "1" *-- "0..*" Mensaje : mensajes
  Mensaje "0..*" --> "1" Usuario : remitente
  Mensaje "1" *-- "0..*" DocumentoAdjunto : adjuntos
  DocumentoAdjunto "0..*" --> "1" Usuario : subidoPor
  Pedido "1" --> "0..*" EntregableFinal : entregables

  %% ---------- comunicacion ----------
  NotificacionSistema "0..*" --> "1" Usuario : usuario
  NotificacionSistema "0..*" --> "1" TipoNotificacion : tipo
  Seguidor "0..*" --> "1" Usuario : usuarioSeguidor
  Seguidor "0..*" --> "1" PerfilCreador : perfilCreador
  BriefingPlantilla "0..*" --> "1" PerfilCreador : perfilCreador
  BriefingPlantilla "1" *-- "0..*" BriefingPregunta : preguntas
  BriefingEnviado "0..*" --> "1" Pedido : pedido
  BriefingEnviado "0..*" --> "1" BriefingPlantilla : plantilla
  BriefingEnviado "1" *-- "0..*" BriefingRespuesta : respuestas
  BriefingRespuesta "0..*" --> "1" BriefingPregunta : pregunta
  LikePortafolio "0..*" --> "1" PortafolioItem : item
  LikePortafolio "0..*" --> "1" Usuario : usuario
  ComentarioPortafolio "0..*" --> "1" PortafolioItem : item
  ComentarioPortafolio "0..*" --> "1" Usuario : usuarioAutor
  InfraccionMensaje "0..*" --> "1" Usuario : usuario
  InfraccionMensaje "0..*" --> "1" Pedido : pedido

  %% ---------- social ----------
  Sorteo "0..*" --> "1" PerfilCreador : perfilCreador
  Sorteo "1" *-- "0..*" ParticipanteSorteo : participantes
  ParticipanteSorteo "0..*" --> "1" Usuario : usuario
  ResenaServicio "1" --> "1" Pedido : pedido
  ResenaServicio "0..*" --> "1" Usuario : autor
```

---

## 3. Código PlantUML (Alternativa)

```plantuml
@startuml UML_Clases_Backend_Completo_Corregido
skinparam classAttributeIconSize 0
left to right direction

package "seguridad" {
  class Pais {
    - idPais : Long
    - nombrePais : String
  }
  class Usuario {
    - idUsuario : Long
    - nombres : String
    - apellidos : String
    - correo : String
    - contrasenaHash : String
    - fechaRegistro : LocalDateTime
    - actualizadoEn : LocalDateTime
    - fechaNacimiento : LocalDate
  }
  class Rol {
    - idRol : Long
    - nombreRol : String
    - descripcionRol : String
  }
  class Permiso {
    - idPermiso : Long
    - nombrePermiso : String
    - moduloAplicacion : String
  }
  class RolPermiso {
    - idRolPermiso : Long
  }
  class UsuarioRol {
    - idUsuarioRol : Long
  }
  class SesionUsuario {
    - idSesion : Long
    - jti : String
    - direccionIp : String
    - fechaCreacion : LocalDateTime
    - fechaExpiracion : LocalDateTime
  }
  class AutenticacionDosFactores {
    - id2fa : Long
    - llaveSecreta : String
  }
  class CodigoRespaldo2Fa {
    - idCodigo : Long
    - codigoHash : String
  }
  class TokenRecuperacion {
    - idToken : Long
    - hashToken : String
    - fechaGeneracion : LocalDateTime
  }
}

package "perfil" {
  class PerfilCreador {
    - idPerfil : Long
    - biografia : String
    - urlRedSocial : String
  }
  class Habilidad {
    - idHabilidad : Long
    - nombreHabilidad : String
  }
  class CreadorHabilidad {
    - idCreadorHabilidad : Long
    - nivelDominio : String
  }
  class EstadoVerificacion {
    - idEstadoVerificacion : Long
    - nombreEstado : String
  }
  class CertificadoIa {
    + Long idCertificado
    + String urlDocumentoAzure
    + BigDecimal puntajeConfianzaIa
    + LocalDateTime fechaAnalisis
  }
  class Portafolio {
    - idPortafolio : Long
    - fechaCreacion : LocalDateTime
  }
  class PortafolioItem {
    - idItemPortafolio : Long
    - tituloObra : String
    - descripcionObra : String
    - urlArchivoMultimedia : String
    - fechaSubida : LocalDateTime
  }
}

package "catalogo" {
  class Categoria {
    - idCategoria : Long
    - nombreCategoria : String
  }
  class Subcategoria {
    - idSubcategoria : Long
    - nombreSubcategoria : String
  }
  class Etiqueta {
    - idEtiqueta : Long
    - nombreEtiqueta : String
  }
  class AtributoDinamico {
    - idAtributo : Long
    - nombreAtributo : String
    - tipoDato : String
  }
  class Servicio {
    - idServicio : Long
    - tituloServicio : String
    - descripcionDetallada : String
    - precioBase : BigDecimal
    - urlMiniatura : String
  }
  class ServicioAtributo {
    - idServicioAtributo : Long
    - valorAsignado : String
  }
  class ServicioEtiqueta {
    - idServicioEtiqueta : Long
  }
}

package "pedido" {
  class FlujoTrabajo {
    - idFlujo : Long
    - nombreFlujo : String
    - descripcionFlujo : String
  }
  class EtapaFlujo {
    - idEtapa : Long
    - nombreEtapa : String
  }
  class FlujoEtapaConfig {
    - idFlujoEtapa : Long
    - numeroOrden : Integer
  }
  class Pedido {
    - idPedido : Long
    - fechaInicio : LocalDateTime
    - fechaEntregaEstimada : LocalDateTime
    - precioPactado : BigDecimal
  }
  class HistorialEstadoPedido {
    - idHistorialEstado : Long
    - fechaTransicion : LocalDateTime
    - observacion : String
  }
  class MotivoRechazo {
    - idMotivo : Long
    - descripcionMotivo : String
  }
  class TicketRevision {
    - idTicket : Long
    - descripcionCliente : String
    - estadoTicket : String
    - fechaApertura : LocalDateTime
    - fechaResolucion : LocalDateTime
  }
  class PlantillaContrato {
    - idPlantilla : Long
    - versionLegal : String
    - cuerpoHtmlPlantilla : String
  }
}

package "legal" {
  class Contrato {
    - idContrato : Long
    - hashFirmaCliente : String
    - hashFirmaCreador : String
    - fechaFormalizacion : LocalDateTime
    - urlDocumentoPdf : String
  }
  class PagoGarantia {
    - idPago : Long
    - idOrdenPaypal : String
    - montoRetenido : BigDecimal
  }
  class TransaccionPago {
    - idTransaccion : Long
    - tipoTransaccion : String
    - monto : BigDecimal
    - fechaEjecucion : LocalDateTime
  }
  class SalaChat {
    - idSala : Long
    - fechaApertura : LocalDateTime
  }
  class Mensaje {
    - idMensaje : Long
    - cuerpoMensaje : String
    - fechaHoraEnvio : LocalDateTime
  }
  class DocumentoAdjunto {
    - idAdjunto : Long
    - urlArchivo : String
    - tipoMime : String
    - pesoBytes : Long
  }
  class EntregableFinal {
    - idEntregable : Long
    - urlVersionMarcaAgua : String
    - urlVersionLimpia : String
    - estadoAprobacion : String
    - fechaEntrega : LocalDateTime
  }
}

package "comunicacion" {
  class TipoNotificacion {
    - idTipoNotificacion : Long
    - nombreEvento : String
    - formatoMensaje : String
  }
  class NotificacionSistema {
    - idNotificacion : Long
    - fechaEmision : LocalDateTime
  }
  class Seguidor {
    - idSeguimiento : Long
    - fechaSeguimiento : LocalDateTime
  }
  class BriefingPlantilla {
    - idBriefingPlantilla : Long
    - nombrePlantilla : String
    - fechaCreacion : LocalDateTime
  }
  class BriefingPregunta {
    - idPregunta : Long
    - textoPregunta : String
    - numeroOrden : Integer
  }
  class BriefingEnviado {
    - idBriefingEnviado : Long
    - fechaEnvio : LocalDateTime
  }
  class BriefingRespuesta {
    - idRespuesta : Long
    - textoRespuesta : String
    - fechaRespuesta : LocalDateTime
  }
  class LikePortafolio {
    - idLike : Long
    - fechaLike : LocalDateTime
  }
  class ComentarioPortafolio {
    - idComentario : Long
    - textoComentario : String
    - fechaPublicacion : LocalDateTime
  }
  class InfraccionMensaje {
    - idInfraccion : Long
    - mensajeOriginal : String
    - patronDetectado : String
    - fechaInfraccion : LocalDateTime
  }
}

package "social" {
  class Sorteo {
    - idSorteo : Long
    - tituloSorteo : String
    - descripcionPremios : String
    - fechaInicio : LocalDateTime
    - fechaCierre : LocalDateTime
  }
  class ParticipanteSorteo {
    - idParticipacion : Long
    - fechaInscripcion : LocalDateTime
    - fechaNotificacionPremio : LocalDateTime
  }
  class ResenaServicio {
    - idResena : Long
    - calificacionEstrellas : Integer
    - textoResena : String
    - fechaResena : LocalDateTime
  }
}

' ---------- seguridad ----------
Usuario "0..*" --> "1" Pais : pais
Usuario "1" --> "0..1" AutenticacionDosFactores : 2FA
AutenticacionDosFactores "1" *-- "0..*" CodigoRespaldo2Fa : codigos respaldo
Usuario "1" *-- "0..*" SesionUsuario : sesiones
Usuario "1" *-- "0..*" TokenRecuperacion : tokens
Usuario "1" --> "0..*" UsuarioRol : asignaciones
Rol "1" --> "0..*" UsuarioRol : asignaciones
Rol "1" --> "0..*" RolPermiso : permisos
Permiso "1" --> "0..*" RolPermiso : permisos

' ---------- perfil ----------
PerfilCreador "1" --> "1" Usuario : usuario
PerfilCreador "1" --> "0..*" CreadorHabilidad : habilidades
Habilidad "1" --> "0..*" CreadorHabilidad : habilidades
PerfilCreador "1" *-- "0..*" CertificadoIa : certificaciones IA
CertificadoIa "0..*" --> "1" EstadoVerificacion : estado
PerfilCreador "1" *-- "0..1" Portafolio : portafolio
Portafolio "1" *-- "0..*" PortafolioItem : items

' ---------- catalogo ----------
Categoria "1" *-- "0..*" Subcategoria : subcategorias
Servicio "0..*" --> "1" PerfilCreador : perfil
Servicio "0..*" --> "1" Subcategoria : subcategoria
Servicio "1" --> "0..*" ServicioAtributo : atributos
AtributoDinamico "1" --> "0..*" ServicioAtributo : atributos
Servicio "1" --> "0..*" ServicioEtiqueta : etiquetas
Etiqueta "1" --> "0..*" ServicioEtiqueta : etiquetas

' ---------- pedido ----------
FlujoTrabajo "1" *-- "0..*" FlujoEtapaConfig : etapas config
EtapaFlujo "1" --> "0..*" FlujoEtapaConfig : etapas config
Pedido "0..*" --> "1" Usuario : usuarioCliente
Pedido "0..*" --> "1" Servicio : servicio
Pedido "0..*" --> "1" FlujoTrabajo : flujo
Pedido "1" *-- "0..*" HistorialEstadoPedido : historial
HistorialEstadoPedido "0..*" --> "1" EtapaFlujo : etapa
Pedido "1" --> "0..*" TicketRevision : tickets
TicketRevision "0..*" --> "1" MotivoRechazo : motivo

' ---------- legal ----------
Contrato "1" --> "1" Pedido : pedido
Contrato "0..*" --> "1" PlantillaContrato : plantilla
Contrato "1" *-- "0..1" PagoGarantia : garantia escrow
PagoGarantia "1" *-- "0..*" TransaccionPago : movimientos
SalaChat "1" --> "1" Pedido : pedido
SalaChat "1" *-- "0..*" Mensaje : mensajes
Mensaje "0..*" --> "1" Usuario : remitente
Mensaje "1" *-- "0..*" DocumentoAdjunto : adjuntos
DocumentoAdjunto "0..*" --> "1" Usuario : subidoPor
Pedido "1" --> "0..*" EntregableFinal : entregables

' ---------- comunicacion ----------
NotificacionSistema "0..*" --> "1" Usuario : usuario
NotificacionSistema "0..*" --> "1" TipoNotificacion : tipo
Seguidor "0..*" --> "1" Usuario : usuarioSeguidor
Seguidor "0..*" --> "1" PerfilCreador : perfilCreador
BriefingPlantilla "0..*" --> "1" PerfilCreador : perfilCreador
BriefingPlantilla "1" *-- "0..*" BriefingPregunta : preguntas
BriefingEnviado "0..*" --> "1" Pedido : pedido
BriefingEnviado "0..*" --> "1" BriefingPlantilla : plantilla
BriefingEnviado "1" *-- "0..*" BriefingRespuesta : respuestas
BriefingRespuesta "0..*" --> "1" BriefingPregunta : pregunta
LikePortafolio "0..*" --> "1" PortafolioItem : item
LikePortafolio "0..*" --> "1" Usuario : usuario
ComentarioPortafolio "0..*" --> "1" PortafolioItem : item
ComentarioPortafolio "0..*" --> "1" Usuario : usuarioAutor
InfraccionMensaje "0..*" --> "1" Usuario : usuario
InfraccionMensaje "0..*" --> "1" Pedido : pedido

' ---------- social ----------
Sorteo "0..*" --> "1" PerfilCreador : perfilCreador
Sorteo "1" *-- "0..*" ParticipanteSorteo : participantes
ParticipanteSorteo "0..*" --> "1" Usuario : usuario
ResenaServicio "1" --> "1" Pedido : pedido
ResenaServicio "0..*" --> "1" Usuario : autor

@enduml
```

---

## 4. Historial de Decisiones / Refinamientos del Modelo

Las siguientes decisiones de diseño a nivel de código se tomaron para soportar la arquitectura definida en el Nivel 3 (Componentes):

| # | Decisión Arquitectónica / Modificación al Modelo Base | Justificación Técnica en el Nivel 4 (Persistencia) |
|---|---|---|
| 1 | Multiplicidad explícita (`1`, `0..1`, `0..*`) | Requerido para mapear correctamente relaciones JPA bidireccionales y unidireccionales (`@OneToMany`, `@ManyToOne`). |
| 2 | Uso de Composición (`*--`) en dependencias estrictas | Para modelar la propagación de operaciones en cascada (`CascadeType.ALL`, `orphanRemoval=true`). Ej: `SalaChat *-- Mensaje`, `Pedido *-- HistorialEstadoPedido`. |
| 3 | Auditoría y Trazabilidad inmutable | Añadidos atributos explícitos de auditoría (`autor` en Reseñas, `subidoPor` en Adjuntos) necesarios para validar permisos de acceso y roles en los Interceptores de Seguridad del Nivel 3. |
| 4 | Atributos de Estado y Temporalidad | Campos temporales y de estado (`estadoTicket`, `fechaResolucion`, `estadoAprobacion`) añadidos para posibilitar el procesamiento asíncrono y los Webhooks (p. ej. validaciones de pagos en Escrow). |
| 5 | Cambio de tipos de datos base (`Integer` a `Long`) | Ajuste en campos de metadatos (ej. `pesoBytes` en `DocumentoAdjunto`) para soportar archivos multimedia de alta calidad almacenados en Cloud Storage / CDN. |
