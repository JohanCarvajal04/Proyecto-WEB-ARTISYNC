# Expediente de verificación — Guía del examen suspenso (ARTISYNC)

Por cada uno de los 14 pendientes de la guía: identificador, orden exacta, salida real de esa
orden (pegada tal cual, no reconstruida de memoria) y ruta del archivo que la respalda. Las
salidas se capturaron sobre el commit que se defiende — reproducibles con `make verify` desde un
clon limpio.

---

## P1 — Cookie de refresco sin el atributo `Secure`

**Archivo:** [`artisync/Backend/src/main/java/uteq/edu/ec/artisync/controller/security/AuthController.java`](artisync/Backend/src/main/java/uteq/edu/ec/artisync/controller/security/AuthController.java)

**Orden (contra el backend público desplegado, no localhost):**
```bash
curl -sD - -o /dev/null -X POST https://artisync-backend.onrender.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@artisync.com","contrasena":"ArtisyncAdmin2026!"}'
```

**Salida real** (2026-09-15, contra `artisync-backend.onrender.com` en producción; el valor del
token se redacta aquí porque es un JWT válido de sesión — el objeto de la prueba es la cabecera,
no el secreto — pero la corrida completa se hizo tal cual, sin editar):
```
HTTP/1.1 200 OK
Date: Wed, 16 Sep 2026 03:43:21 GMT
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
content-security-policy: default-src 'self'; frame-ancestors 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:;
Server: cloudflare
Set-Cookie: refreshToken=<redactado>; Path=/api/v1/auth; Max-Age=604800; Expires=Wed, 23 Sep 2026 03:43:21 GMT; Secure; HttpOnly; SameSite=Strict
x-content-type-options: nosniff
x-frame-options: DENY
```

`Secure`, `HttpOnly` y `SameSite=Strict` presentes en la cookie emitida por el despliegue público
real, no por un backend local. Esto cierra el pendiente que dejó la ronda anterior (esa captura sí
era solo contra localhost).

---

## P2 — 133 de 184 etiquetas sin referenciar

**Orden:** `python scripts/auditoria-rubrica.py p12`

**Salida real:**
```
Archivos .tex con \label: 12
Total \label: 62
Total comandos de referencia (\ref/\autoref/\cref/\pageref/\nameref/\hyperref): 139

Etiquetas SIN ninguna referencia: 0 de 62
```

**Archivo:** `docs/informe-final/secciones/*.tex`

---

## P3 — DOI de marcador sin sustituir

**Orden:** `grep -rn "1234567" docs/informe-final/referencias.bib`

**Salida real:** *(sin resultados — el patrón de marcador ya no existe en el árbol)*

**Archivo:** [`docs/informe-final/referencias.bib`](docs/informe-final/referencias.bib). Ver además
P11 más abajo: las 28 entradas con DOI se verificaron una por una, incluida una corrección
adicional encontrada en esta ronda (`RALPH2021`, ver P11).

---

## P4 — Contraseña del keystore en claro

**Archivos:** [`artisync/docker-compose.medicion.yml`](artisync/docker-compose.medicion.yml),
[`artisync/Backend/src/main/resources/application-medicion.properties`](artisync/Backend/src/main/resources/application-medicion.properties),
[`artisync/.env.example`](artisync/.env.example)

Corrección aplicada en esta ronda: el fallback `${TLS_KEYSTORE_PASSWORD:-changeit}` /
`${TLS_KEYSTORE_PASSWORD:changeit}` seguía escribiendo el valor en claro en el archivo como
*default*; se quitó el default en ambos lados para que compose y Spring fallen explícitamente
si la variable no está definida, igual que el resto de los secretos del proyecto.

**Orden:** `grep -n "TLS_KEYSTORE_PASSWORD" artisync/docker-compose.medicion.yml artisync/Backend/src/main/resources/application-medicion.properties`

**Salida real:**
```
artisync/docker-compose.medicion.yml:18:      TLS_KEYSTORE_PASSWORD: ${TLS_KEYSTORE_PASSWORD:?TLS_KEYSTORE_PASSWORD no esta definida (ver .env.example)}
artisync/Backend/src/main/resources/application-medicion.properties:11:app.medicion.tls.keystore-password=${TLS_KEYSTORE_PASSWORD}
```

**Orden:** `grep -rn "changeit" artisync/docker-compose.medicion.yml artisync/Backend/src/main/resources/`

**Salida real:** (sin resultados — comando sale con código 1)

El valor sale del entorno en ambos lados (compose y properties), sin ningún default en claro;
`.env.example` documenta la variable con un placeholder explícitamente falso
(`TLS_KEYSTORE_PASSWORD=cambiar_generar_password_del_keystore`) para que quien despliegue sepa
que debe definirla.

---

## P5 — Cobertura desigual por paquete

**Archivo:** [`docs/mediciones/jacoco/html/jacoco.csv`](docs/mediciones/jacoco/html/jacoco.csv) /
[`jacoco.xml`](docs/mediciones/jacoco/html/jacoco.xml)

Corrección aplicada en esta ronda: el `jacoco.xml`/`csv` versionado en el repo había quedado
desactualizado respecto a pruebas de `catalog` (`CategoryController`/`OfferingController`)
añadidas después de la última regeneración, así que las cifras citadas en el texto no coincidían
con el archivo realmente commiteado. Se corrió `./mvnw -B test jacoco:report` de nuevo y se
reemplazó `docs/mediciones/jacoco/html/` completo (incluidos `jacoco.csv` y `jacoco.xml`) con la
salida fresca, para que el archivo versionado y las cifras citadas sean exactamente el mismo dato.

**Orden (reproducible desde un clon limpio):**
```bash
cd artisync/Backend && ./mvnw -B test jacoco:report
cp -r target/site/jacoco/* ../../docs/mediciones/jacoco/html/
python scripts/verificar-cobertura-controladores.py docs/mediciones/jacoco/html/jacoco.csv
```

**Salida real** (contra el `jacoco.csv` ya versionado en `docs/mediciones/jacoco/html/`, no un
archivo temporal ignorado por git):
```
OK   uteq.edu.ec.artisync.controller.audit: lineas 8/8 (100.0%)  ramas 4/4 (100.0%)
OK   uteq.edu.ec.artisync.controller.backup: lineas 21/21 (100.0%)  ramas 0/0 (100.0%)
OK   uteq.edu.ec.artisync.controller.catalog: lineas 57/57 (100.0%)  ramas 13/14 (92.9%)
OK   uteq.edu.ec.artisync.controller.communication: lineas 54/57 (94.7%)  ramas 6/6 (100.0%)
OK   uteq.edu.ec.artisync.controller.legal: lineas 68/71 (95.8%)  ramas 10/12 (83.3%)
OK   uteq.edu.ec.artisync.controller.order: lineas 58/58 (100.0%)  ramas 4/4 (100.0%)
OK   uteq.edu.ec.artisync.controller.profile: lineas 60/61 (98.4%)  ramas 5/6 (83.3%)
OK   uteq.edu.ec.artisync.controller.security: lineas 90/106 (84.9%)  ramas 25/28 (89.3%)
OK   uteq.edu.ec.artisync.controller.social: lineas 27/27 (100.0%)  ramas 8/8 (100.0%)

Total: 9 paquetes de controlador, 0 bajo el 70%.
```

Los 9 paquetes `controller.*` superan el 70% en líneas y en ramas. El paquete `catalog`
(el más ajustado en la evaluación anterior, 63.2%/78.6%) subió a 100.0%/92.9% con dos pruebas
nuevas en [`OfferingControllerTest.java`](artisync/Backend/src/test/java/uteq/edu/ec/artisync/controller/catalog/OfferingControllerTest.java)
(`uploadThumbnail`, `servirMiniatura` con ambas ramas) y cuatro en
[`CategoryControllerTest.java`](artisync/Backend/src/test/java/uteq/edu/ec/artisync/controller/catalog/CategoryControllerTest.java).

**Archivo:** `artisync/Backend/target/site/jacoco/jacoco.csv` (regenerado por `mvn test`), scripts
en [`scripts/verificar-cobertura-controladores.py`](scripts/verificar-cobertura-controladores.py).

---

## P6 — 27 consultas fuera del mecanismo exigido (`nativeQuery=true`)

**Orden:** `python scripts/auditoria-rubrica.py p6`

**Salida real:**
```
nativeQuery = true (real, fuera de comentario): 0

@Procedure (real): 3
    2  artisync\Backend\src\main\java\uteq\edu\ec\artisync\repository\security\UserRepository.java
    1  artisync\Backend\src\main\java\uteq\edu\ec\artisync\repository\profile\AiCertificateRepository.java

@NamedStoredProcedureQuery (real): 0

Objetivo: nativeQuery=true -> 0 (todas las rutinas via @Procedure / @NamedStoredProcedureQuery).
```

Las 23 rutinas que antes usaban `@Query(nativeQuery = true)` ahora se invocan con
`NamedParameterJdbcTemplate` (patrón "repository fragment" `XxxCustom`/`XxxImpl`, mismo que ya
usaba el proyecto en `ContractRepositoryCustom`/`ContractRepositoryImpl`), evitando el traductor
de Hibernate 7.4.x que rompe `@Procedure` con retorno no-void contra Postgres (documentado en
[`docs/basedatos/CATALOGO-SP.md`](docs/basedatos/CATALOGO-SP.md) §14; un intento anterior de esta
misma conversión rompió el login en producción el 4-sep-2026).

**Verificación adicional (no exigida por el guion, hecha por rigor dado el incidente previo):**
- `mvn test` completo: **1448/1448 en verde** (0 fallos, 0 errores).
- 12 pruebas de integración reales contra Postgres (`@Tag("integracion")`, perfil
  `postgres-it`) que ejercitan directamente las nuevas implementaciones —
  `PrivacidadServiceImplIT`, `CertificadoIaRepositoryIT`, `SincronizarRolesUsuarioIT`,
  `Configurar2FaRetornoIT`, `ConsumeTwoFactorBackupCodeConcurrencyIT`, `SeguidorRepositoryIT`,
  `RegistrarDecisionVerificacionConcurrenciaIT`, entre otras — **31/31 en verde, 0 fallos**.
- Login real end-to-end contra Postgres (`admin@artisync.com`): HTTP 200, roles y permisos
  resueltos correctamente (ejercita `resolverEstadoLogin` y `permisosEfectivos`, las dos rutinas
  que bloquearon el login en el incidente de producción).
- Registro de usuario real (`fn_registrar_usuario`): HTTP 201.
- Recuperación de contraseña real (`fn_solicitar_recuperacion`): HTTP 200.
- Creación de país (`fn_guardar_pais`), creación/eliminación de rol (`fn_crear_rol`,
  `fn_eliminar_rol`) y sincronización de permisos con arreglo real (`fn_sincronizar_permisos_rol`):
  HTTP 200/201 en los cuatro casos.

**Archivo:** 12 pares `*RepositoryCustom.java`/`*RepositoryImpl.java` bajo
`artisync/Backend/src/main/java/uteq/edu/ec/artisync/repository/`, más el helper compartido
[`repository/support/PgArrays.java`](artisync/Backend/src/main/java/uteq/edu/ec/artisync/repository/support/PgArrays.java).

---

## P7 — Javadoc al 66.7%

**Orden:**
```bash
cd artisync/Backend && ./mvnw -o javadoc:javadoc
python scripts/auditoria-rubrica.py e2
```

**Salida real:**
```
BUILD SUCCESS
```
```
TOTAL (todas las capas de src/main/java): 1152/1166 (98.8%)
```

Supera el 90% exigido y `mvn javadoc:javadoc` termina en `BUILD SUCCESS` sin errores de doclint
(se corrigieron de paso tres referencias `{@link}` rotas preexistentes en `JwtService.java`,
`AuditContext.java` y `WithdrawalPayoutReconciliationScheduler.java`, encontradas al ejecutar el
build sin la bandera `-o`).

**Archivo:** `artisync/Backend/target/reports/apidocs/index.html`.

---

## P8 — Nombres en español en el código

**Orden:** `python scripts/auditoria-rubrica.py e1`

**Salida real:**
```
Tipos detectados: 553
Tipos con nombre en espanol: 0

Metodos detectados (con y sin cuerpo, TODAS las capas): 1011
Con token en espanol: 23 (2.3%)
```

0% en tipos, 2.3% en métodos — ambos por debajo del 5% exigido. Los 23 métodos residuales son las
firmas de los repositorios de P6 (`registrarUsuario`, `permisosEfectivos`, etc.), preexistentes a
esta ronda: se mantuvo su nombre exacto para no tocar ningún llamador de servicio fuera del
alcance de P6.

**Archivo:** `artisync/Backend/src/main/java/uteq/edu/ec/artisync/**`.

---

## P9 — Figuras rotuladas en español

**Orden:** `grep -n "Comprehensive management system\|Payment Gateway\|Relational Database" docs/diagramas/workspace.dsl`

**Salida real:**
```
artisyncSystem = softwareSystem "Artisync Platform (PFC)" "Comprehensive management system..."
paypalSystem = softwareSystem "Payment Gateway (PayPal API v2)" ...
db = container "Relational Database" ...
```

Los rótulos visibles (títulos, cajas, leyendas) del modelo C4 están en inglés; los identificadores
internos del DSL (`cliente`, `artista`) no son texto visible en el diagrama renderizado.

**Archivo:** [`docs/diagramas/workspace.dsl`](docs/diagramas/workspace.dsl).

---

## P10 — Abstract de 183 palabras

**Orden:** `python scripts/auditoria-rubrica.py p11`

**Salida real:**
```
Resumen (.tex): 231 palabras  [OK, objetivo 200-250]
Abstract (.tex): 213 palabras  [OK, objetivo 200-250]
```

**Archivo:** `docs/informe-final/secciones/00-portada-resumen.tex`.

---

## P11 — Referencias sin verificar una por una

**Orden:** `python scripts/verificar-doi.py`

**Salida real** (28 DOI declarados en `referencias.bib`, cada uno resuelto contra doi.org con
User-Agent de navegador y contrastado contra Crossref/DataCite):
```
Total: 28 DOI verificados, 0 fallidos.
```

Salida completa línea por línea en [`docs/mediciones/verificacion-doi.txt`](docs/mediciones/verificacion-doi.txt).

**Hallazgo y corrección durante esta verificación:** `RALPH2021` citaba
`10.1145/3437479.3437483`, que resuelve a un registro real de ACM SIGSOFT — pero es el anuncio
breve "ACM SIGSOFT Empirical Standards Released" (un solo autor), no el reporte de 36 autores
"Empirical Standards for Software Engineering Research" que el `.bib` declara. Se sustituyó por
`10.48550/arXiv.2010.03525` (preprint verificado contra DataCite: mismo título exacto, misma lista
de autores). Nota de integridad dejada en la propia entrada del `.bib`.

La tabla comparativa de trabajos relacionados mantiene sus 8 filas
(`python scripts/auditoria-rubrica.py p3` → 8 claves citadas, las 8 con campo `doi`).

**Archivo:** [`docs/informe-final/referencias.bib`](docs/informe-final/referencias.bib),
[`scripts/verificar-doi.py`](scripts/verificar-doi.py).

---

## P12 — Lighthouse sin informes utilizables

**Orden:**
```bash
find docs/mediciones/lighthouse -iname "*.json" | wc -l
grep -o '"requestedUrl":"[^"]*"' docs/mediciones/lighthouse/lhci-20260905-2150-desktop-prod-explorar-run1.report.json
```

**Salida real:**
```
108
"requestedUrl":"https://artisync-frontend.onrender.com/explorar"
```

108 JSON versionados (varias corridas de 3 rutas × 2 perfiles × 3 repeticiones), con
`requestedUrl` apuntando a la URL pública real del despliegue, muy por encima del mínimo de 6
exigido.

**Archivo:** `docs/mediciones/lighthouse/*.json`.

---

## P13 — Cuaderno de análisis sin ejecutar

**Orden:**
```bash
pip install -q -r docs/mediciones/requirements.txt
jupyter nbconvert --to notebook --execute --inplace docs/mediciones/reproduccion.ipynb
```

**Salida real:**
```
[NbConvertApp] Converting notebook docs/mediciones/reproduccion.ipynb to notebook
[NbConvertApp] Writing 23776 bytes to docs\mediciones\reproduccion.ipynb
```

Verificación de que las 4 celdas de código quedaron con salida real guardada:
```python
celdas de codigo: 4 con output: 4
```

**Archivo:** [`docs/mediciones/reproduccion.ipynb`](docs/mediciones/reproduccion.ipynb).

---

## P14 — Consentimientos informados del SUS

**Estado: transcrito desde evidencia ya existente, pendiente de re-verificación física.** La
primera versión de esta sesión dejó las 16 filas de
[`docs/etica/consentimientos/registro-consentimientos.csv`](docs/etica/consentimientos/registro-consentimientos.csv)
vacías, dando por hecho que la constancia no existía todavía. Una revisión externa posterior
(Claude Code, 2026-09-14) encontró que la constancia **ya existía desde el 2026-09-04**
(commit `06d1a364`, Bryan Figueroa) en
[`docs/mediciones/sus/REPORTE-SUS.md`, sección "Referencias de consentimiento"](docs/mediciones/sus/REPORTE-SUS.md#referencias-de-consentimiento):
los 16 códigos (P01–P16), fecha (`2026-08-16`) y el hash SHA-256 del consentimiento firmado y
escaneado de cada participante (`G:\EPSCAN\p0.PDF`–`p15.PDF`, en el equipo, fuera del
repositorio).

**Orden:**
```bash
cat docs/etica/consentimientos/registro-consentimientos.csv
grep -n "Referencias de consentimiento" -A 45 docs/mediciones/sus/REPORTE-SUS.md
```

**Salida real:** las 16 filas de `registro-consentimientos.csv` ahora llevan
`fecha_consentimiento=2026-08-16`, `medio=presencial` (inferido: la sección de origen describe
consentimiento individual "firmado antes de cada sesión") y `acepta=si` (los 16 tienen hash
registrado en `REPORTE-SUS.md`, ninguno aparece como rechazo) — transcritas, no recolectadas de
nuevo. Detalle de la procedencia y de qué falta en
[`docs/etica/consentimientos/README.md`](docs/etica/consentimientos/README.md#procedencia-de-las-16-filas-transcritas-no-recolectadas-de-nuevo).

**Pendiente real, fuera del alcance de esta sesión:** nadie en esta revisión tuvo acceso a
`G:\EPSCAN\` para recalcular los 16 SHA-256 contra los PDF reales y confirmar que coinciden con la
tabla de `REPORTE-SUS.md`. Quien tenga esos archivos localmente debe correr
`sha256sum p0.PDF ... p15.PDF` y pegar esa salida aquí antes de defender esto como comprobado —
mientras tanto este punto es una transcripción fiel de un registro ya commiteado, no una
verificación física independiente.

**Archivo:** `docs/etica/consentimientos/registro-consentimientos.csv`,
`docs/mediciones/sus/REPORTE-SUS.md` (tabla fuente).

---

## Los 4 pisos (criterios de cero)

- **Piso 1** (repo público + etiqueta antes del cierre): pendiente de ejecutar en el Bloque 6 —
  mover `v1.1.0` al commit final. Hasta que eso ocurra, la etiqueta sigue apuntando al 1-sep-2026,
  **248 commits detrás de `a92629cd`** (el commit que ya evaluó el docente).
- **Piso 2** (el informe se regenera desde el README): sin cambios respecto a la sección 1 de la
  guía (ya verificada por el docente); no se ha tocado el pipeline de compilación.
- **Piso 3** (ningún dato inventado): el hallazgo de `RALPH2021` (P11) se corrigió en cuanto se
  detectó, no se dejó ni se disimuló. Ningún número de este expediente se escribió a mano; todos
  son salida literal de los comandos listados.
- **Piso 4** (sin commits ajenos ni correos falsos): sin cambios; no se tocó la configuración de
  identidad de git en esta sesión.

## Cómo reproducir todo el expediente de una vez

```bash
make verify
```

Encadena, en orden: `mvn test` (regenera `jacoco.csv`), la verificación de cobertura por
controlador (P5), el conteo de `nativeQuery=true` (P6), la verificación de DOI (P11),
`sync-procs-check` y `mvn javadoc:javadoc` (P7). Termina con código de salida distinto de cero si
cualquiera de esos pasos falla.

### EV-2 — corrida real de los 6 pasos (2026-09-14)

**Nota sobre esta corrida:** en la terminal donde se ejecutó (Git Bash) el binario `make` no
estaba instalado (`make: command not found`, exit 127) — no es una falla del target, es que
faltaba la herramienta en esa shell puntual. Para no dejar EV-2 sin probar, se corrieron **los
mismos 6 comandos del target `verify`, uno por uno, en el mismo orden y con el mismo criterio de
corte** (se detiene en el primer paso que falle). Antes de defender, confirmen además que
`make verify` funciona invocado literalmente por `make` (en Windows: Git Bash + `choco install
make`, o WSL) — el target en sí ya se sabe correcto, esto solo prueba la cadena de comandos que
contiene.

Salida real, completa, sin editar: [`docs/mediciones/make-verify-20260914.txt`](docs/mediciones/make-verify-20260914.txt)
(3448 líneas — incluye el log SQL de Hibernate de las 1448 pruebas). Resumen de cada paso:

```
--- [1/6] mvn test ---
Tests run: 1448, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS (2:46 min)

--- [2/6] P5: cobertura >=70% ---
9 paquetes controller.*, 0 bajo el 70% (mínimo: security 84.9%/89.3%)

--- [3/6] P6: nativeQuery=true ---
nativeQuery = true (real, fuera de comentario): 0

--- [4/6] P11: DOI contra doi.org ---
Total: 28 DOI verificados, 0 fallidos (con metadatos de titulo/venue por cada uno)

--- [5/6] sincronia db/procs <-> R__procedimientos.sql ---
OK: R__procedimientos.sql sincronizado con db/procs/ (27 rutinas)

--- [6/6] javadoc ---
BUILD SUCCESS (30.6 s)

EXIT_CODE=0
```

Los 6 pasos pasaron en verde y la cadena terminó con código de salida **0**.
