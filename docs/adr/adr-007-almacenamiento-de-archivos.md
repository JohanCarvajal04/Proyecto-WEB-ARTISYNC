# ADR-007: Almacenamiento de archivos en la nube

**Estado:** Aceptado
**Fecha:** 15 de agosto de 2026

## Contexto
La plataforma persiste archivos de varias naturalezas: documentos de verificación (cédulas, títulos), PDF de contratos y entregables, imágenes de portafolio y miniaturas de servicios, y video de portafolio. Hasta ahora el único flujo de subida implementado —documentos de verificación— escribía en un volumen del contenedor del backend (`AlmacenamientoLocal`, volumen `pfc_documentos_verificacion`).

Ese esquema tiene dos límites conocidos. El volumen es local a la instancia, de modo que escalar el backend a varias réplicas (ADR-004 ya contempla esa posibilidad) haría que un archivo subido por una réplica fuera invisible para las demás. Y el respaldo del archivo queda fuera del respaldo de la base de datos, con lo que una restauración puede dejar filas apuntando a documentos inexistentes.

## Opciones consideradas
- **A — Mantener el volumen local:** sin costo ni dependencia externa, pero impide escalar horizontalmente y deja los archivos fuera de la estrategia de respaldo.
- **B — Cloudinary para imágenes y Azure Blob para documentos:** es el esquema del proyecto de referencia consultado. Aporta CDN y transformaciones de imagen, a cambio de dos proveedores, dos facturas y dos rutas de código para un mismo concepto.
- **C — Azure Blob Storage para todos los tipos:** un único proveedor, que admite cualquier content-type y ofrece URLs firmadas (SAS) de vigencia limitada para contenedores privados.

## Decisión
Se adopta **Azure Blob Storage como único destino en la nube (opción C)**, detrás de la interfaz `AlmacenamientoDocumentos` que ya existía. Se descarta el esquema híbrido de la opción B: Blob Storage cubre imágenes, PDF y video sin necesidad de un segundo proveedor, y las transformaciones de imagen que aportaría Cloudinary no corresponden a ningún requisito actual.

La implementación queda seleccionable en configuración mediante `documentos.proveedor`, con `@ConditionalOnProperty` sobre las dos implementaciones:

| Valor | Implementación | Uso |
|---|---|---|
| `local` (por defecto) | `AlmacenamientoLocal` | Desarrollo e integración continua, que no deben requerir credenciales de Azure |
| `azure` | `AlmacenamientoAzure` | Despliegue |

Tres decisiones concretas merecen registro:

**El contenedor es privado y la referencia no es una URL.** `guardar()` devuelve el nombre del blob, no `getBlobUrl()`. El contenedor guarda cédulas y títulos, así que una URL pública —aunque lleve un UUID— es un control de acceso basado en que nadie adivine la dirección. Para entregar un archivo al frontend sin proxearlo por el backend está `generarUrlTemporal()`, que firma un SAS de solo lectura con vigencia corta (`documentos.azure.sas-minutos`, 15 por defecto).

**El transporte HTTP es el de la JDK, no Netty.** El SDK arrastra `azure-core-http-netty`, compilado contra Netty 4.1.x, mientras que el BOM de Spring Boot 4 gestiona Netty a 4.2.x. El desajuste no rompe la compilación: se manifestaría en la primera llamada real. Se excluye ese transporte y se usa `azure-core-http-jdk-httpclient`, que se apoya en el `HttpClient` de Java 21 y no añade Netty.

**El límite de subida es por caso de uso, no global.** El techo de multipart sube a 100MB para admitir video de portafolio, pero verificación conserva su límite de 5MB, ahora aplicado en `PreprocesadorImagenIa.validarFormato`. Una cédula de 100MB es un error, no un archivo legítimo. `PoliticaArchivo` concentra qué tipos y qué tamaño admite cada caso.

**Lo que se persiste es una referencia, y la API entrega una URL.** Los campos siguen llamándose `url_*` por compatibilidad con el esquema, pero guardan la referencia interna (`entregables/<uuid>.pdf`). Cada servicio la traduce al responder: con Azure entrega un SAS y el archivo viaja directo desde el blob; sin él, la ruta del endpoint que sirve los bytes. El nombre de la columna quedó desalineado con su contenido; corregirlo es una migración pendiente.

### Alcance cubierto

| Caso de uso | Prefijo | Estado |
|---|---|---|
| Verificación (cédulas, títulos) | `verificacion` | Subida, lectura, borrado y purga programada |
| Entregables (marca de agua y versión limpia) | `entregables` | Subida multipart, descarga con control de liberación de fondos, limpieza al resubir |
| Obras de portafolio (imagen y video) | `portafolio` | Subida, listado, descarga y borrado, con tope de 50 obras por portafolio |

Quedan fuera, por no tener flujo de archivo construido: la miniatura de servicio (`Servicio.urlMiniatura`, que hoy recibe un String del cliente), los adjuntos de chat (`DocumentoAdjunto`, cuya entidad existe pero nunca se instancia) y el PDF de contrato (`Contrato.urlDocumentoPdf`, que no se escribe en ningún punto del código).

## Consecuencias positivas
- Los archivos dejan de estar atados a una instancia del backend, lo que habilita escalar horizontalmente.
- El respaldo y la redundancia pasan a ser responsabilidad de Azure, alineando los archivos con la política de respaldos del proyecto.
- Los consumidores (`VerificacionServicioImpl`, `VerificacionScheduler`, `VerificacionControlador`) no se modificaron: la interfaz ya aislaba el detalle del proveedor, y cambiar de proveedor vuelve a ser un cambio de configuración.
- `ExtensionesArchivo` reemplaza el mapeo anterior, que resolvía todo content-type que no fuera PNG como `.jpg`; un PDF quedaba guardado con extensión de imagen.

## Consecuencias negativas
- Introduce una dependencia de un servicio externo de pago y una credencial más que custodiar (`AZURE_STORAGE_CONNECTION_STRING`, nunca versionada).
- Una caída de Azure deja la subida y la descarga de documentos inoperantes; no hay degradación a almacenamiento local.
- Las pruebas de integración requieren el emulador Azurite (perfil `azure` de `docker-compose.yml`). Se omiten automáticamente si no está escuchando, para no romper CI.
- Azurite rechaza la versión de API que envía el SDK, por lo que se ejecuta con `--skipApiVersionCheck`. Azure real sí la acepta; el desvío es del emulador.

## Referencias
`AlmacenamientoAzure`, `AlmacenamientoLocal`, `ExtensionesArchivo`, `PoliticaArchivo`, `PrefijoAlmacenamiento`, `AlmacenamientoProperties`; consumidores en `VerificacionServicioImpl`, `EntregableServicioImpl` y `PortafolioItemServicioImpl`; servicio `azurite` en `docker-compose.yml`; ADR-004 (escalado horizontal), ADR-005 (estrategia de despliegue).
