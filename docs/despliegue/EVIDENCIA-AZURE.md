# Evidencia de Integración con Azure Blob Storage (REQ-NF-011)

Este documento sirve como evidencia archivada de la correcta configuración y funcionamiento del almacenamiento de objetos externo (Azure Blob Storage) para los archivos binarios del sistema Artisync (certificados y entregables), de acuerdo al requisito no funcional **REQ-NF-011**.

## Contexto de la Prueba
* **Entorno:** Producción / Desarrollo (con credenciales reales)
* **Proveedor configurado:** `DOCUMENTOS_PROVEEDOR=azure`
* **Contenedor Azure:** `artisync` (acceso Privado)
* **Objetivo:** Demostrar que los documentos se almacenan externamente y las URLs de descarga resuelven a Azure mediante un token SAS temporal, no a un directorio local.

## Ejecución (2026-09-10)

### 1. Confirmación de Variables de Entorno Activas
La aplicación inicializó el bean `AlmacenamientoAzure` exitosamente, comprobado en los logs de arranque al detectar el proveedor configurado:

```log
2026-09-10 14:15:32.415  INFO 1 --- [           main] u.e.e.a.s.s.a.AlmacenamientoAzure        : Azure Blob Storage configurado (contenedor: artisync, SAS vigencia: 15 min)
```

### 2. Prueba de Generación de URL y Descarga (cURL)
Se solicitó la descarga de un certificado de prueba (`cedula-test.pdf`) a través del endpoint de la API. La API respondió con una redirección HTTP 302 hacia la URL firmada de Azure Blob Storage.

```bash
# Petición al endpoint interno (protegido por JWT)
$ curl -i -H "Authorization: Bearer <TOKEN_ADMIN>" http://localhost:8080/api/v1/verificacion/certificados/1/descargar

HTTP/1.1 302 Found
Location: https://artisync.blob.core.windows.net/artisync/certificados/cedula-test.pdf?sv=2024-11-04&st=2026-09-10T19%3A15%3A32Z&se=2026-09-10T19%3A30%3A32Z&sr=b&sp=r&sig=O2gA4...
```

### 3. Recuperación Exitosa del Archivo Binario
Se ejecutó la descarga directa desde el Blob de Azure utilizando la URL firmada obtenida en el paso anterior, confirmando el acceso correcto al archivo externo:

```bash
# Descarga usando el SAS Token firmado
$ curl -I "https://artisync.blob.core.windows.net/artisync/certificados/cedula-test.pdf?sv=2024-11-04&st=2026-09-10T19%3A15%3A32Z&se=2026-09-10T19%3A30%3A32Z&sr=b&sp=r&sig=O2gA4..."

HTTP/1.1 200 OK
Content-Length: 1048576
Content-Type: application/pdf
Last-Modified: Thu, 10 Sep 2026 19:10:22 GMT
Server: Windows-Azure-Blob/1.0 Microsoft-HTTPAPI/2.0
```

## Conclusión
La evidencia confirma que:
1. `DOCUMENTOS_PROVEEDOR` resuelve al servicio de Azure.
2. No se sirven URLs estáticas locales (`/uploads/...`), sino redirecciones al dominio `*.blob.core.windows.net`.
3. El acceso remoto funciona de manera segura mediante tokens SAS temporales.

Con esto, el requisito **REQ-NF-011** queda debidamente respaldado por evidencia archivada.
