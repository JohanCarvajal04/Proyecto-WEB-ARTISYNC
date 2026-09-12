# Diagrama Entidad-Relación - Artisync PFC (Backend)

Este documento contiene el diagrama Entidad-Relación (ER) del modelo de dominio del
backend de Artisync, construido a partir de las clases @Entity reales en
rtisync/Backend/src/main/java/uteq/edu/ec/artisync/entity/.

**Nota sobre idiomas:** Las clases de dominio (Entity) en el código fuente de Java están en **inglés** (ej. User, Country, Role), por lo que los bloques del diagrama utilizan estos nombres. Sin embargo, los atributos internos y las columnas mapeadas a la base de datos están en **español** (ej. idUsuario, 
ombres, pellidos), lo cual se refleja fielmente en los campos de cada entidad.

`mermaid
erDiagram
    CreatorProfile ||--o{ Offering : "profile"
    Workflow ||--o{ Offering : "workflow"
    ContractTemplate ||--o{ Offering : "contractTemplate"
    BriefingTemplate ||--o{ Offering : "briefingTemplate"
    Offering ||--o{ OfferingAttribute : "offering"
    DynamicAttribute ||--o{ OfferingAttribute : "attribute"
    Offering ||--o{ OfferingSubcategory : "offering"
    Subcategory ||--o{ OfferingSubcategory : "subcategory"
    Offering ||--o{ OfferingTag : "offering"
    Tag ||--o{ OfferingTag : "tag"
    Category ||--o{ Subcategory : "category"
    SentBriefing ||--o{ BriefingAnswer : "sentBriefing"
    BriefingQuestion ||--o{ BriefingAnswer : "question"
    BriefingTemplate ||--o{ BriefingQuestion : "template"
    CreatorProfile ||--o{ BriefingTemplate : "creatorProfile"
    User ||--o{ Follower : "followerUser"
    CreatorProfile ||--o{ Follower : "creatorProfile"
    User ||--o{ MessageViolation : "user"
    Order ||--o{ MessageViolation : "order"
    PortfolioItem ||--o{ PortfolioComment : "portfolioItem"
    User ||--o{ PortfolioComment : "authorUser"
    PortfolioItem ||--o{ PortfolioLike : "portfolioItem"
    User ||--o{ PortfolioLike : "user"
    Order ||--o{ SentBriefing : "order"
    BriefingTemplate ||--o{ SentBriefing : "template"
    User ||--o{ SystemNotification : "user"
    NotificationType ||--o{ SystemNotification : "notificationType"
    Order ||--o{ ChatRoom : "order"
    Order ||--o{ Contract : "order"
    ContractTemplate ||--o{ Contract : "template"
    Contract ||--o{ EscrowPayment : "contract"
    Order ||--o{ FinalDeliverable : "order"
    ChatRoom ||--o{ Message : "room"
    User ||--o{ Message : "sender"
    EscrowPayment ||--o{ PaymentTransaction : "payment"
    RevisionTicket ||--o{ RevisionTicketPayment : "ticket"
    User ||--o{ WithdrawalRequest : "creatorUser"
    User ||--o{ WithdrawalRequest : "adminDecider"
    User ||--o{ Order : "clientUser"
    Offering ||--o{ Order : "offering"
    Workflow ||--o{ Order : "workflow"
    Order ||--o{ OrderStatusHistory : "order"
    WorkflowStage ||--o{ OrderStatusHistory : "stage"
    Order ||--o{ OrderTermsProposal : "order"
    User ||--o{ OrderTermsProposal : "proposedBy"
    Order ||--o{ RevisionTicket : "order"
    RejectionReason ||--o{ RevisionTicket : "reason"
    Workflow ||--o{ WorkflowStageConfig : "workflow"
    WorkflowStage ||--o{ WorkflowStageConfig : "stage"
    User ||--o{ AiCertificate : "user"
    VerificationStatus ||--o{ AiCertificate : "verificationStatus"
    User ||--o{ AiCertificate : "moderator"
    User ||--o{ CreatorPaymentDetails : "user"
    User ||--o{ CreatorProfile : "user"
    CreatorProfile ||--o{ Portfolio : "profile"
    Portfolio ||--o{ PortfolioItem : "portfolio"
    BackupSchedule ||--o{ Backup : "schedule"
    User ||--o{ TwoFactorAuthentication : "user"
    User ||--o{ TwoFactorBackupCode : "user"
    Country ||--o{ User : "country"
    User ||--o{ UserRole : "user"
    Role ||--o{ UserRole : "role"
    User ||--o{ UserSession : "user"
    Order ||--o{ OfferingReview : "order"
    CreatorProfile ||--o{ Raffle : "creatorProfile"
    Raffle ||--o{ RaffleParticipant : "raffle"
    User ||--o{ RaffleParticipant : "user"
    RafflePrize ||--o{ RaffleParticipant : "prize"
    Raffle ||--o{ RafflePrize : "raffle"
    AuditEvent {
        Long idEventoAuditoria PK
        LocalDateTime fechaEvento
        Long idUsuarioActor PK
        String correoActor
        String moduloAuditoria
        String accionAuditoria
        String resultadoEvento
        String entidadAfectada
        Long idEntidadAfectada PK
        Json detalleCambio
        String mensajeError
        String direccionIp
        String agenteUsuario
        String metodoHttp
        String rutaSolicitud
        Integer duracionMs
    }
    Category {
        Long idCategoria PK
        String nombreCategoria
    }
    DynamicAttribute {
        Long idAtributo PK
        String nombreAtributo
        String tipoDato
    }
    Offering {
        Long idServicio PK
        Long perfilId FK
        String tituloServicio
        String descripcionDetallada
        BigDecimal precioBase
        String urlMiniatura
        Long flujoId FK
        Long plantillaContratoId FK
        Long briefingPlantillaId FK
    }
    OfferingAttribute {
        Long idServicioAtributo PK
        Long servicioId FK
        Long atributoId FK
        String valorAsignado
    }
    OfferingSubcategory {
        Long idServicioSubcategoria PK
        Long servicioId FK
        Long subcategoriaId FK
    }
    OfferingTag {
        Long idServicioEtiqueta PK
        Long servicioId FK
        Long etiquetaId FK
    }
    Subcategory {
        Long idSubcategoria PK
        Long categoriaId FK
        String nombreSubcategoria
    }
    Tag {
        Long idEtiqueta PK
        String nombreEtiqueta
    }
    Workflow {
        Long idFlujo PK
        String nombreFlujo
        String descripcionFlujo
    }
    BriefingAnswer {
        Long idRespuesta PK
        Long briefingEnviadoId FK
        Long preguntaId FK
        String textoRespuesta
        LocalDateTime fechaRespuesta
    }
    BriefingQuestion {
        Long idPregunta PK
        Long plantillaId FK
        String textoPregunta
        Integer numeroOrden
    }
    BriefingTemplate {
        Long idBriefingPlantilla PK
        Long perfilCreadorId FK
        String nombrePlantilla
        LocalDateTime fechaCreacion
    }
    Follower {
        Long idSeguimiento PK
        Long usuarioSeguidorId FK
        Long perfilCreadorId FK
        LocalDateTime fechaSeguimiento
    }
    MessageViolation {
        Long idInfraccion PK
        Long usuarioId FK
        Long pedidoId FK
        String mensajeOriginal
        String patronDetectado
        LocalDateTime fechaInfraccion
    }
    NotificationType {
        Long idTipoNotificacion PK
        String nombreEvento
        String formatoMensaje
    }
    PortfolioComment {
        Long idComentario PK
        Long itemPortafolioId FK
        Long usuarioAutorId FK
        String textoComentario
        LocalDateTime fechaPublicacion
    }
    PortfolioLike {
        Long idLike PK
        Long itemPortafolioId FK
        Long usuarioId FK
        LocalDateTime fechaLike
    }
    SentBriefing {
        Long idBriefingEnviado PK
        Long pedidoId FK
        Long plantillaId FK
        LocalDateTime fechaEnvio
    }
    SystemNotification {
        Long idNotificacion PK
        Long usuarioId FK
        Long tipoNotificacionId FK
        String mensaje
        LocalDateTime fechaEmision
    }
    ChatRoom {
        Long idSala PK
        Long pedidoId FK
        LocalDateTime fechaApertura
    }
    Contract {
        Long idContrato PK
        Long pedidoId FK
        Long plantillaId FK
        String hashFirmaCliente
        String hashFirmaCreador
        LocalDateTime fechaFormalizacion
        String urlDocumentoPdf
        String contenidoCongelado
        String hashContenido
        LocalDateTime fechaHashContenido
        LocalDateTime fechaLimiteRetencion
    }
    EscrowPayment {
        Long idPago PK
        Long contratoId FK
        String idOrdenPaypal PK
        BigDecimal montoRetenido
        String mensajeError
        LocalDateTime fechaCreacion
        LocalDateTime fechaActualizacion
    }
    FinalDeliverable {
        Long idEntregable PK
        Long pedidoId FK
        String urlVersionMarcaAgua
        String urlVersionLimpia
    }
    Message {
        Long idMensaje PK
        Long salaId FK
        Long remitenteId FK
        String cuerpoMensaje
        LocalDateTime fechaHoraEnvio
    }
    PaymentTransaction {
        Long idTransaccion PK
        Long pagoId FK
        String tipoTransaccion
        BigDecimal monto
        LocalDateTime fechaEjecucion
    }
    RevisionTicketPayment {
        Long idPagoTicket PK
        Long ticketId FK
        String idOrdenPaypal PK
        String urlAprobacion
        BigDecimal monto
        String mensajeError
        LocalDateTime fechaCreacion
        LocalDateTime fechaActualizacion
    }
    WithdrawalRequest {
        Long idSolicitud PK
        Long usuarioCreadorId FK
        BigDecimal montoSolicitado
        String correoPaypalDestino
        String idPayoutPaypal PK
        String idItemPayoutPaypal PK
        String notaAdmin
        String mensajeError
        LocalDateTime fechaSolicitud
        LocalDateTime fechaDecision
        LocalDateTime fechaPago
        Long adminDecisorId FK
    }
    ContractTemplate {
        Long idPlantilla PK
        String versionLegal
        String cuerpoHtmlPlantilla
        String nombrePlantilla
        Long idCreador PK
    }
    Order {
        Long idPedido PK
        Long usuarioClienteId FK
        Long servicioId FK
        Long flujoId FK
        LocalDateTime fechaInicio
        LocalDateTime fechaEntregaEstimada
        BigDecimal precioPactado
    }
    OrderStatusHistory {
        Long idHistorialEstado PK
        Long pedidoId FK
        Long etapaId FK
        LocalDateTime fechaTransicion
        String observacion
    }
    OrderTermsProposal {
        Long idPropuesta PK
        Long pedidoId FK
        Long propuestoPorId FK
        BigDecimal precioPropuesto
        LocalDateTime fechaEntregaPropuesta
        LocalDateTime fechaCreacion
        LocalDateTime fechaResolucion
    }
    RejectionReason {
        Long idMotivo PK
        String descripcionMotivo
    }
    RevisionTicket {
        Long idTicket PK
        Long pedidoId FK
        Long motivoId FK
        String descripcionCliente
        LocalDateTime fechaCreacion
    }
    WorkflowStage {
        Long idEtapa PK
        String nombreEtapa
    }
    WorkflowStageConfig {
        Long idFlujoEtapa PK
        Long flujoId FK
        Long etapaId FK
        Integer numeroOrden
    }
    AiCertificate {
        Long idCertificado PK
        Long usuarioId FK
        Long estadoVerificacionId FK
        String urlDocumentoS3
        BigDecimal puntajeConfianzaIa
        String hashDocumento
        String veredictoIa
        String razonIa
        String datosExtraidosIa
        LocalDateTime fechaDictamenIa
        Long moderadorId FK
        LocalDateTime fechaDecision
        String notaModerador
        LocalDateTime fechaAnalisis
    }
    CreatorPaymentDetails {
        Long idDatosPago PK
        Long usuarioId FK
        String correoPaypal
        LocalDateTime fechaActualizacion
    }
    CreatorProfile {
        Long idPerfil PK
        Long usuarioId FK
        String biografia
        String urlRedSocial
        String urlPortada
        String tituloProfesional
    }
    Portfolio {
        Long idPortafolio PK
        Long perfilId FK
        LocalDateTime fechaCreacion
    }
    PortfolioItem {
        Long idItemPortafolio PK
        Long portafolioId FK
        String tituloObra
        String descripcionObra
        String urlArchivoMultimedia
        LocalDateTime fechaSubida
    }
    VerificationStatus {
        Long idEstadoVerificacion PK
        String nombreEstado
    }
    Backup {
        Long idRespaldo PK
        BackupType tipoRespaldo
        BackupOrigin origen
        Long programacionId FK
        Long idRespaldoFullBase PK
        String nombreArchivo
        String rutaArchivo
        Long tamanoBytes
        LocalDateTime fechaInicio
        LocalDateTime fechaFin
        Integer duracionMs
        String mensajeError
        String correoSolicitante
        LocalDateTime fechaDesdeIncremental
    }
    BackupSchedule {
        Long idProgramacion PK
        String nombre
        BackupType tipoRespaldo
        String expresionCron
        Integer retencionDias
        LocalDateTime proximaEjecucion
        LocalDateTime ultimaEjecucion
        String creadoPor
        LocalDateTime fechaCreacion
        LocalDateTime actualizadoEn
    }
    Country {
        Long idPais PK
        String nombrePais
    }
    Permission {
        Long idPermiso PK
        String nombrePermiso
        String moduloAplicacion
    }
    Role {
        Long idRol PK
        String nombreRol
        String descripcionRol
    }
    TwoFactorAuthentication {
        Long id2fa PK
        Long usuarioId FK
        String llaveSecreta
    }
    TwoFactorBackupCode {
        Long idCodigo PK
        Long usuarioId FK
        String codigoHash
    }
    User {
        Long idUsuario PK
        String nombres
        String apellidos
        String correo
        String contrasenaHash
        Long paisId FK
        LocalDateTime fechaRegistro
        LocalDateTime actualizadoEn
        LocalDate fechaNacimiento
        String urlFotoPerfil
    }
    UserRole {
        Long idUsuarioRol PK
        Long usuarioId FK
        Long rolId FK
    }
    UserSession {
        Long idSesion PK
        Long usuarioId FK
        String jti
        String direccionIp
        LocalDateTime fechaCreacion
        LocalDateTime fechaExpiracion
    }
    OfferingReview {
        Long idResena PK
        Long pedidoId FK
        Integer calificacionEstrellas
        String textoResena
        LocalDateTime fechaResena
    }
    Raffle {
        Long idSorteo PK
        Long perfilCreadorId FK
        String tituloSorteo
        LocalDateTime fechaInicio
        LocalDateTime fechaCierre
    }
    RaffleParticipant {
        Long idParticipacion PK
        Long sorteoId FK
        Long usuarioId FK
        LocalDateTime fechaInscripcion
        LocalDateTime fechaNotificacionPremio
        Long premioId FK
    }
    RafflePrize {
        Long idPremio PK
        Long sorteoId FK
        String descripcionPremio
        Integer orden
    }
`

## Mapeo de Entidades

| Entidad en el diagrama | Clase Java |
| :--- | :--- |
| AiCertificate | AiCertificate.java |
| AuditEvent | AuditEvent.java |
| Backup | Backup.java |
| BackupSchedule | BackupSchedule.java |
| BriefingAnswer | BriefingAnswer.java |
| BriefingQuestion | BriefingQuestion.java |
| BriefingTemplate | BriefingTemplate.java |
| Category | Category.java |
| ChatRoom | ChatRoom.java |
| Contract | Contract.java |
| ContractTemplate | ContractTemplate.java |
| Country | Country.java |
| CreatorPaymentDetails | CreatorPaymentDetails.java |
| CreatorProfile | CreatorProfile.java |
| DynamicAttribute | DynamicAttribute.java |
| EscrowPayment | EscrowPayment.java |
| FinalDeliverable | FinalDeliverable.java |
| Follower | Follower.java |
| Message | Message.java |
| MessageViolation | MessageViolation.java |
| NotificationType | NotificationType.java |
| Offering | Offering.java |
| OfferingAttribute | OfferingAttribute.java |
| OfferingReview | OfferingReview.java |
| OfferingSubcategory | OfferingSubcategory.java |
| OfferingTag | OfferingTag.java |
| Order | Order.java |
| OrderStatusHistory | OrderStatusHistory.java |
| OrderTermsProposal | OrderTermsProposal.java |
| PaymentTransaction | PaymentTransaction.java |
| Permission | Permission.java |
| Portfolio | Portfolio.java |
| PortfolioComment | PortfolioComment.java |
| PortfolioItem | PortfolioItem.java |
| PortfolioLike | PortfolioLike.java |
| Raffle | Raffle.java |
| RaffleParticipant | RaffleParticipant.java |
| RafflePrize | RafflePrize.java |
| RejectionReason | RejectionReason.java |
| RevisionTicket | RevisionTicket.java |
| RevisionTicketPayment | RevisionTicketPayment.java |
| Role | Role.java |
| SentBriefing | SentBriefing.java |
| Subcategory | Subcategory.java |
| SystemNotification | SystemNotification.java |
| Tag | Tag.java |
| TwoFactorAuthentication | TwoFactorAuthentication.java |
| TwoFactorBackupCode | TwoFactorBackupCode.java |
| User | User.java |
| UserRole | UserRole.java |
| UserSession | UserSession.java |
| VerificationStatus | VerificationStatus.java |
| WithdrawalRequest | WithdrawalRequest.java |
| Workflow | Workflow.java |
| WorkflowStage | WorkflowStage.java |
| WorkflowStageConfig | WorkflowStageConfig.java |
