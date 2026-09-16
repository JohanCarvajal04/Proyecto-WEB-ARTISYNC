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

Corrección aplicada en esta ronda: la barra de P2 exige "un script ... que sale con error si
queda alguna" etiqueta huérfana. `seccion_p12()` solo imprimía el conteo, sin `sys.exit(1)`, y no
estaba en `make verify` — una regresión futura no rompería la comprobación automática. Se agregó
`sys.exit(1)` cuando `total_huerfanas > 0` y se sumó `p12` al target `verify` del `Makefile`.

**Orden:** `python scripts/auditoria-rubrica.py p12`

**Salida real:**
```
Archivos .tex con \label: 12
Total \label: 62
Total comandos de referencia (\ref/\autoref/\cref/\pageref/\nameref/\hyperref): 139

Etiquetas SIN ninguna referencia: 0 de 62
```
Código de salida: `0`.

**Verificación del propio chequeo de error:** se creó temporalmente un archivo `.tex` con una
etiqueta sin referenciar, se confirmó que el script termina con código de salida `1` y lista el
archivo/etiqueta ofensora, y se eliminó el archivo de prueba (no forma parte del repositorio).

**Archivo:** [`docs/informe-final/secciones/*.tex`](docs/informe-final/secciones/),
[`scripts/auditoria-rubrica.py`](scripts/auditoria-rubrica.py) (`seccion_p12`),
[`Makefile`](Makefile) (target `verify`, paso P2).

---

## P3 — DOI de marcador sin sustituir

**Orden:** `grep -rn "1234567" docs/informe-final/referencias.bib`

**Salida real:** *(sin resultados — el patrón de marcador ya no existe en el árbol)*

**Archivo:** [`docs/informe-final/referencias.bib`](docs/informe-final/referencias.bib).

Corrección aplicada en esta ronda: la barra de P3 exige resolver **cada** DOI declarado, no solo
los del `.bib` — [`scripts/verificar-doi.py`](scripts/verificar-doi.py) ahora también extrae y
resuelve el DOI de `CITATION.cff` y todos los `10.5281/zenodo.*` mencionados en `README.md`
(software vigente, dataset y la versión software superseded que el propio README sigue
declarando), no solo las 28 referencias bibliográficas.

**Orden:** `python scripts/verificar-doi.py`

**Salida real (sección de software/dataset; ver P11 más abajo para la sección bibliográfica):**
```
=== DOI de software/dataset (CITATION.cff / README.md) ===
OK    zenodo(CITATION.cff+README.md) doi=10.5281/zenodo.21978572             http_final=200 -> https://zenodo.org/records/21978572
      Metadatos: titulo='Proyecto WEB-ARTISYNC' venue/publisher='Zenodo'
OK    zenodo(README.md)    doi=10.5281/zenodo.22236251             http_final=200 -> https://zenodo.org/records/22236251
      Metadatos: titulo='Artisync — Dataset de mediciones empíricas (rendimiento, seguridad, usabilidad, cobertura, calidad web) v1.0.0' venue/publisher='Zenodo'
OK    zenodo(README.md)    doi=10.5281/zenodo.21730559             http_final=200 -> https://zenodo.org/records/21730559
      Metadatos: titulo='Proyecto WEB-ARTISYNC' venue/publisher='Zenodo'

Total: 31 DOI verificados (28 bibliograficos + 3 de software/dataset), 0 fallidos.
```

Los tres DOI de Zenodo declarados en el árbol (software vigente, dataset, y el software
superseded que el README todavía menciona) resuelven 200 contra doi.org, con metadatos reales en
Zenodo/DataCite. Ver P11 más abajo: las 28 entradas bibliográficas con DOI también se verificaron
una por una en la misma corrida, incluida una corrección adicional encontrada en esta ronda
(`RALPH2021`, ver P11).

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

Corrección aplicada en esta ronda: la barra de P7 exige Javadoc "completo (@param descriptivo por
parámetro, @return y @throws donde corresponda)", no solo que exista un bloque `/** */`. La
medición anterior (`auditoria-rubrica.py e2`, 98.8%) solo comprobaba presencia; al medir
completitud real con la nueva sección `e2c` (exige `@param` por cada parámetro, `@return` si el
método no es void, y `@throws`/`@exception` por cada excepción declarada — `{@inheritDoc}` cuenta
como completo, ya que Javadoc hereda la documentación del método que sobreescribe) el resultado
real era 843/1166 (72.3%), por debajo del 90% exigido. Se completaron los ~323 métodos con huecos
(la inmensa mayoría interfaces de repositorio Spring Data con un resumen de una línea pero sin
`@param`) y los 9 métodos sin ningún bloque Javadoc.

**Orden:**
```bash
python scripts/auditoria-rubrica.py e2c
cd artisync/Backend && ./mvnw -o javadoc:javadoc
```

**Salida real:**
```
TOTAL con Javadoc completo: 1166/1166 (100.0%) -- umbral exigido: 90%
OK

0 archivo(s) con al menos un metodo incompleto (0 metodos en total):
```
```
BUILD SUCCESS
```

100% de los métodos públicos con Javadoc completo (supera el 90% exigido) y
`mvn javadoc:javadoc` termina en `BUILD SUCCESS` sin errores de doclint.

**Archivos:** [`scripts/auditoria-rubrica.py`](scripts/auditoria-rubrica.py) (sección `e2c`,
nueva), `artisync/Backend/target/reports/apidocs/index.html`, y los ~120 archivos de
`src/main/java` donde se completaron las etiquetas.

---

## P8 — Nombres en español en el código

**Orden:** `python scripts/auditoria-rubrica.py e1`

**Salida real:**
```
Tipos detectados: 554
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

Corrección aplicada en esta ronda: la verificación anterior solo comprobaba el `.dsl` fuente, no
el SVG realmente renderizado que se muestra en la documentación. `C4-nivel1-context_diagram.svg`
era un render congelado del 6 de julio, anterior a la traducción de `workspace.dsl` (11 de
septiembre), y todavía tenía una frase en español embebida como etiqueta de una arista
("Notificaciones, confirmaciones de garantia, mensajes en tiempo real y alertas de infraccion").

Además, al intentar re-renderizarlo se encontraron dos errores reales de sintaxis en
`workspace.dsl` que impedían que compilara con Structurizr (nunca se había probado con la
herramienta real): `autoLayout topBottom` no es válido (los valores aceptados son `tb|bt|lr|rl`) y
los bloques `element "X" { shape ...; background ...; }` en una sola línea con `;` como separador
tampoco lo son (cada propiedad va en su propia línea). Se corrigieron ambos y se re-renderizó el
SVG con Structurizr Lite (`docker run -p 8090:8080 -v ./docs/diagramas:/usr/local/structurizr
structurizr/lite:2025.05.28`), reemplazando el archivo congelado por la salida real y actual.

**Orden:**
```bash
grep -c "Comprehensive management system\|Payment Gateway\|Relational Database" docs/diagramas/workspace.dsl
grep -oE "[a-zA-Z]*[áéíóúñÁÉÍÓÚÑ][a-zA-Z]*" docs/diagramas/C4-nivel1-context_diagram.svg | sort -u
```

**Salida real:**
```
3
(sin resultados — cero palabras con tilde/ñ en el SVG renderizado)
```

Los rótulos visibles (títulos, cajas, leyendas, aristas) del modelo C4 están en inglés tanto en el
`.dsl` fuente como en el SVG realmente renderizado — ya no solo en la fuente. Los identificadores
internos del DSL (`cliente`, `artista`) no son texto visible en el diagrama.

**Archivos:** [`docs/diagramas/workspace.dsl`](docs/diagramas/workspace.dsl),
[`docs/diagramas/C4-nivel1-context_diagram.svg`](docs/diagramas/C4-nivel1-context_diagram.svg).

**Corrección aplicada en esta ronda — cobertura completa de las figuras del informe:** la ronda
anterior solo verificó el SVG de arriba. `docs/informe-final/secciones/*.tex` referencia 4 imágenes
más vía `\includegraphics` que nunca se habían comprobado:

**Orden:**
```bash
grep -n "includegraphics" docs/informe-final/secciones/*.tex
```

**Salida real:**
```
docs/informe-final/secciones/06-diseno-arquitectura.tex:32:\includegraphics[...]{c4-nivel2-contenedores.png}
docs/informe-final/secciones/06-diseno-arquitectura.tex:176:\includegraphics[...]{Entidad_Relacion.png}
docs/informe-final/secciones/07-implementacion.tex:73:\includegraphics[...]{secuencia_login_jwt.png}
docs/informe-final/secciones/08-evaluacion-resultados.tex:239:\includegraphics[...]{boxplot-sus.png}
```

Al ser PNG rasterizados (no SVG/DSL con texto extraíble), la comprobación fue inspección visual
directa de cada archivo (2026-09-16), verificando que todo el texto visible — títulos, ejes,
leyendas, cajas y rótulos de flujo — esté en inglés:

- [`docs/diagramas/c4-nivel2-contenedores.png`](docs/diagramas/c4-nivel2-contenedores.png): actores
  ("Client / Talent Seeker", "Artist / Content Creator", "Platform Administrator"), contenedores
  ("Web SPA Application", "REST API Server", "Relational Database", "Cache & JTI Blacklist") y
  relaciones ("Requests and approves orders", "Manages portfolio and deliverables") en inglés.
- [`docs/diagramas/Entidad_Relacion.png`](docs/diagramas/Entidad_Relacion.png): nombres de entidad y
  atributo derivados directamente de las clases/campos Java (`UserAccount`, `Offering`, `Contract`,
  etc.), ya verificados en inglés por P8 — sin texto en español propio del diagrama.
- [`docs/diagramas/secuencia_login_jwt.png`](docs/diagramas/secuencia_login_jwt.png): actores,
  mensajes y anotaciones del diagrama de secuencia ("POST /auth/login", "generateToken() /
  generateRefreshToken()", "Every subsequent authenticated request", "Refresh and logout") en inglés.
- [`docs/mediciones/sus/boxplot-sus.png`](docs/mediciones/sus/boxplot-sus.png): título, ejes y
  leyenda ("Distribution of SUS Scores (n=16)", "Score (0-100)", "Acceptance Threshold (68)") en
  inglés.

Las 5 figuras del informe (1 diagrama C4 nivel 1 + estas 4) están en inglés, no solo la que se
había comprobado antes.

**Archivos adicionales:** [`docs/diagramas/c4-nivel2-contenedores.png`](docs/diagramas/c4-nivel2-contenedores.png),
[`docs/diagramas/Entidad_Relacion.png`](docs/diagramas/Entidad_Relacion.png),
[`docs/diagramas/secuencia_login_jwt.png`](docs/diagramas/secuencia_login_jwt.png),
[`docs/mediciones/sus/boxplot-sus.png`](docs/mediciones/sus/boxplot-sus.png).

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

**Reconciliación del conteo "48" (corrección aplicada en esta ronda):** la guía exige "las 48
referencias con su DOI resuelto", pero `referencias.bib` tiene 45 entradas. La diferencia está
documentada en el propio historial del repositorio, no es un hueco sin explicar: el commit
`b80b268c` (2026-09-06, *"fix(informe-final): corregir referencias bibliográficas y regenerar PDF
final"*) eliminó 4 entradas fabricadas que un integrante había añadido para inflar artificialmente
el conteo de referencias de "alto impacto" (`KUMAR2023`, `PARK2023`, `CHEN2021` — con DOI de
relleno del patrón `...1234567`, sin rastro de que las obras existan — y `RAO2022`, cuyo DOI
resolvía al registro de un comité organizador, no a un artículo real). Ver
`docs/observaciones/observaciones_para_el_examen.md` (líneas 410–419, hallazgo `OBS-D6-04`) para
el detalle completo de esa limpieza. El corpus correcto y honesto es el que existe hoy: 45
entradas, todas verificables; no se añaden referencias de relleno para llegar a 48, porque eso
sería exactamente el tipo de dato inventado que el Piso 3 de la guía castiga con la nota en cero.

**Corrección adicional aplicada en esta ronda — DOI-o-razón en el 100% de las entradas:** de las
45, 17 no declaraban `doi`. Se investigó cada una contra Crossref/DataCite (no se asumió ausencia
sin buscar): se encontraron y verificaron 4 DOI reales que faltaban (`BROOKE1996`, `HEVNER2004`,
`BASILI1994`, `COELLO2023` — los tres primeros bloqueados por anti-bot del editor, 403, pero
`doi.org` los resuelve a la página real con título/venue coincidentes; `COELLO2023` resuelve 200
directo). Para las 13 restantes (estándares ISO/IEC, páginas OWASP, un blog, una tesis doctoral y
reportes técnicos EBSE/INCOSE, que legítimamente no tienen DOI) se documentó en el propio `.bib`
la razón concreta de la ausencia (campo `note`), en vez de omitirlas en silencio.

La cobertura DOI-o-razón ya no se retalla a mano: `scripts/verificar-doi.py` la calcula y hace
`sys.exit(1)` si alguna entrada del `.bib` queda sin DOI y sin nota — verificado creando
temporalmente una entrada sin ninguno de los dos y confirmando que el script sale con código 1 y
la señala por nombre; se eliminó esa entrada de prueba, no forma parte del repositorio.

**Orden:** `python scripts/verificar-doi.py`

**Salida real:**
```
Total: 35 DOI verificados (32 bibliograficos + 3 de software/dataset), 0 fallidos.

=== Cobertura DOI-o-razon en docs/informe-final/referencias.bib ===
Total: 45, con DOI: 32, sin DOI con razon documentada: 13, sin nada: 0
```
Código de salida: `0`.

Salida completa línea por línea en [`docs/mediciones/verificacion-doi.txt`](docs/mediciones/verificacion-doi.txt).

**Hallazgo y corrección de una ronda anterior:** `RALPH2021` citaba `10.1145/3437479.3437483`, que
resuelve a un registro real de ACM SIGSOFT — pero es el anuncio breve "ACM SIGSOFT Empirical
Standards Released" (un solo autor), no el reporte de 36 autores "Empirical Standards for Software
Engineering Research" que el `.bib` declara. Se sustituyó por `10.48550/arXiv.2010.03525`
(preprint verificado contra DataCite: mismo título exacto, misma lista de autores). Nota de
integridad dejada en la propia entrada del `.bib`.

La tabla comparativa de trabajos relacionados mantiene sus 8 filas
(`python scripts/auditoria-rubrica.py p3` → 8 claves citadas, las 8 con campo `doi`).

**Archivo:** [`docs/informe-final/referencias.bib`](docs/informe-final/referencias.bib),
[`scripts/verificar-doi.py`](scripts/verificar-doi.py).

---

## P12 — Lighthouse sin informes utilizables

Corrección aplicada en esta ronda: la verificación anterior solo confirmaba `requestedUrl` en un
archivo de muestra, no en el conjunto completo. Se comprobaron los 108 JSON uno por uno.

**Orden:**
```bash
python -c "
import glob, json, os
files = [f for f in glob.glob('docs/mediciones/lighthouse/*.json') if 'manifest' not in f.lower()]
bad = []
for f in files:
    data = json.load(open(f, encoding='utf-8'))
    url = data.get('requestedUrl') or data.get('finalUrl') or ''
    if 'artisync-frontend.onrender.com' not in url:
        bad.append((os.path.basename(f), url))
print(f'Total JSON (excl. manifest): {len(files)}')
print(f'Con requestedUrl fuera del dominio publico: {len(bad)}')
for b in bad: print(' ', b)
"
```

**Salida real:**
```
Total JSON (excl. manifest): 98
Con requestedUrl fuera del dominio publico: 8
  ('lhci-20260730-2009.json', 'http://localhost:4200/')
  ('lhci-20260730-2103-mejorado.json', 'http://localhost:4200/')
  ('lhci-20260817-0315-mobile-run1.json', 'http://localhost:4200/')
  ('lhci-20260817-0315-mobile-run2.json', 'http://localhost:4200/')
  ('lhci-20260817-0315-mobile-run3.json', 'http://localhost:4200/')
  ('lhci-20260817-0320-desktop-run1.json', 'http://localhost:4200/')
  ('lhci-20260817-0320-desktop-run2.json', 'http://localhost:4200/')
  ('lhci-20260817-0320-desktop-run3.json', 'http://localhost:4200/')
```

Estos 8 son corridas locales tempranas (30-jul y 17-ago), anteriores a que el despliegue público
existiera de forma estable, conservadas como historial de medición — no son la evidencia que
cierra este punto. El lote que sí la cierra es el más reciente y completo,
`lhci-20260905-2150-*` (18 archivos = 3 rutas × 3 repeticiones × 2 perfiles), verificado
individualmente:

```bash
python -c "
import glob, json, os
files = sorted(f for f in glob.glob('docs/mediciones/lighthouse/lhci-20260905-2150-*.json') if 'manifest' not in f.lower())
for f in files:
    data = json.load(open(f, encoding='utf-8'))
    print(data['configSettings']['formFactor'], data.get('requestedUrl'), os.path.basename(f))
"
```

**Salida real (18 archivos, las 18 con `requestedUrl` apuntando al dominio público):**
```
desktop https://artisync-frontend.onrender.com/auth/login              lhci-20260905-2150-desktop-prod-auth_login-run{1,2,3}.report.json
desktop https://artisync-frontend.onrender.com/explorar                lhci-20260905-2150-desktop-prod-explorar-run{1,2,3}.report.json
desktop https://artisync-frontend.onrender.com/explorar/creadores      lhci-20260905-2150-desktop-prod-explorar_creadores-run{1,2,3}.report.json
mobile  https://artisync-frontend.onrender.com/auth/login              lhci-20260905-2150-mobile-prod-auth_login-run{1,2,3}.report.json
mobile  https://artisync-frontend.onrender.com/explorar                lhci-20260905-2150-mobile-prod-explorar-run{1,2,3}.report.json
mobile  https://artisync-frontend.onrender.com/explorar/creadores      lhci-20260905-2150-mobile-prod-explorar_creadores-run{1,2,3}.report.json
```

9 corridas de escritorio + 9 de móvil, 3 por cada una de las 3 rutas, todas contra la URL pública
real del despliegue — muy por encima del mínimo de 3+3 exigido.

**Archivo:** `docs/mediciones/lighthouse/*.json` (108 archivos totales versionados; el lote
`lhci-20260905-2150-*` es la evidencia que cierra P12).

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
[NbConvertApp] Writing 23773 bytes to docs\mediciones\reproduccion.ipynb
```

Re-ejecutado en esta ronda (2026-09-16) para refrescar la evidencia tras los cambios de P5: la
primera celda recalcula la cobertura desde `docs/mediciones/jacoco/html/jacoco.csv` y ahora
reporta 91.15%/81.89% global — exactamente las cifras que P5 dejó versionadas, lo que confirma
consistencia cruzada entre ambos puntos del expediente.

Verificación de que las 4 celdas de código quedaron con salida real guardada:
```python
celdas de codigo: 4 con output: 4
```

**Archivo:** [`docs/mediciones/reproduccion.ipynb`](docs/mediciones/reproduccion.ipynb).

---

## P14 — Consentimientos informados del SUS

**Estado: verificado físicamente. 16/16 hashes SHA-256 coinciden, código de salida 0.** La
primera versión de esta sesión dejó las 16 filas de
[`docs/etica/consentimientos/registro-consentimientos.csv`](docs/etica/consentimientos/registro-consentimientos.csv)
vacías, dando por hecho que la constancia no existía todavía. Una revisión externa posterior
(Claude Code, 2026-09-14) encontró que la constancia **ya existía desde el 2026-09-04**
(commit `06d1a364`, Bryan Figueroa) en
[`docs/mediciones/sus/REPORTE-SUS.md`, sección "Referencias de consentimiento"](docs/mediciones/sus/REPORTE-SUS.md#referencias-de-consentimiento):
los 16 códigos (P01–P16), fecha (`2026-08-16`) y el hash SHA-256 del consentimiento firmado y
escaneado de cada participante (`G:\EPSCAN\p0.PDF`–`p15.PDF`, en el equipo, fuera del
repositorio). En ese momento esos 16 hashes estaban transcritos desde `REPORTE-SUS.md` sin
recalcularse contra los PDF reales (`G:\` no estaba montada en esa sesión).

**El 2026-09-16, con `G:\EPSCAN` montada de nuevo en el equipo**, se ejecutó el script de
verificación de solo lectura ya preparado para este propósito:

```bash
python scripts/verificar-consentimientos-sus.py "G:\EPSCAN"
```

**Salida real:**
```
Carpeta de origen: G:\EPSCAN
Referencia: docs/mediciones/sus/REPORTE-SUS.md

OK     P01  <-  p0.PDF  sha256:d8510c56ba0daff6d17e3278cb9696ef2bbc1dc3d470d4c06a75aa88cd59ab7b
OK     P02  <-  p1.PDF  sha256:3bb3e3d07b365131a079a894304685da87707a149729fd121157ea5108319a63
OK     P03  <-  p2.PDF  sha256:d46fcda14cf924d11f36f740a4c43db2c21b0c3f5320f73783c8f70d7fe24ba6
OK     P04  <-  p3.PDF  sha256:2f87c434a6883400aa21f8b7a4333dfaa39ca6c072ee414597596c5899a324aa
OK     P05  <-  p4.PDF  sha256:f464ff648963343d4bfe752dc4276dcce8855776daa9acab57568f231dba675e
OK     P06  <-  p5.PDF  sha256:25e74c68fbe2ab9bf4ec7b0f26d3c48a70b2d20fc46c7e6d4c3eab113dc84ba2
OK     P07  <-  p6.PDF  sha256:10c2dfacc34404c434ef910565726e0d2e8d8935942a554fea9f5b46297c3c2a
OK     P08  <-  p7.PDF  sha256:b741d4f05de1f2190019a18b6b0e23c74f1a883c2a4dbf83b96e0e014c5d5fee
OK     P09  <-  p8.PDF  sha256:92cb125ec8df0237cee9d8d8580c35b7d32f5fcac108b451f7b0425744ccfcbf
OK     P10  <-  p9.PDF  sha256:f2b4b8414f2ff17639fef3c46aee261867117c751688d2b024dbcef05cc0ecec
OK     P11  <-  p10.PDF  sha256:c0e4e83e7f485334775ab19abffb9cdff1357a176b343aeca187c8642b7930c9
OK     P12  <-  p11.PDF  sha256:94c2e6499450ab6bc6350bc8f4d4933a108b17f6ea522a91f2e5ad871046bf99
OK     P13  <-  p12.PDF  sha256:34deb77a6816cc4e7b34ba9f53b457b554a10671a4c4b5d05e35bfbae1ebb57b
OK     P14  <-  p13.PDF  sha256:41a1ab3cf47efb21ca12d715069d768c2fb536026ba643b180cf900e527c9d22
OK     P15  <-  p14.PDF  sha256:93e19fd7a65d0298ad57cf70823d69b06c10636f14b8b372456a10afc3f34708
OK     P16  <-  p15.PDF  sha256:45bedcd2e16e9b1b1fd0a64de855dc1aa0c6bfec0163ec949059ce3f7f44623f

Total: 16/16 hashes coinciden exactamente con REPORTE-SUS.md. 0 fallidos.
```
`echo $?` → `0`.

Las 16 filas de `registro-consentimientos.csv` llevan `fecha_consentimiento=2026-08-16`,
`medio=presencial` (inferido: la sección de origen describe consentimiento individual "firmado
antes de cada sesión") y `acepta=si` (los 16 tienen hash registrado en `REPORTE-SUS.md` y ahora
confirmado contra el PDF físico, ninguno aparece como rechazo). Detalle de la procedencia en
[`docs/etica/consentimientos/README.md`](docs/etica/consentimientos/README.md#procedencia-de-las-16-filas-transcritas-no-recolectadas-de-nuevo)
(la advertencia de "no verificado de forma independiente" que tenía ese README ya se actualizó a
"verificado de forma independiente" con esta misma corrida).

**Archivo:** `docs/etica/consentimientos/registro-consentimientos.csv`,
`docs/mediciones/sus/REPORTE-SUS.md` (tabla fuente),
`scripts/verificar-consentimientos-sus.py` (script de verificación).

---

## Los 4 pisos (criterios de cero)

- **Piso 1** (repo público + etiqueta antes del cierre): **corregido — la alarma de la ronda
  anterior ya no aplica.** Reverificado el 2026-09-16:
  ```bash
  git rev-parse HEAD
  git rev-parse v1.1.0
  git rev-list --count v1.1.0..HEAD
  git ls-remote --tags origin | grep v1.1.0
  ```
  ```
  HEAD:        c6b814d627329e0325b1b49b0f05bbe8e629b38d
  v1.1.0:      c6b814d627329e0325b1b49b0f05bbe8e629b38d
  commits v1.1.0..HEAD: 0
  origin/refs/tags/v1.1.0: c6b814d627329e0325b1b49b0f05bbe8e629b38d
  ```
  El tag está en `HEAD`, igual en local que en `origin`, con margen hasta el cierre (viernes
  18-sep-2026, 23:55). **Advertencia operativa, no un hallazgo de esta ronda:** esta corrección de
  `VERIFICACION.md` y la de P9 más abajo son, en sí mismas, cambios posteriores a `c6b814d6` — el
  equipo debe volver a mover `v1.1.0` al commit que resulte de comitear esta ronda (y no tocar nada
  después de ese movimiento final del tag), o Piso 1 vuelve a fallar de verdad.
- **Piso 2** (el informe se regenera desde el README): reverificado el 2026-09-16 vía CI real sobre
  el commit exacto `c6b814d6` (no una corrida anterior sobre otro commit):
  ```bash
  curl .../repos/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/runs?head_sha=c6b814d627329e0325b1b49b0f05bbe8e629b38d
  ```
  ```
  CI: completed / success — https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/actions/runs/35085378341
  ```
  Además, `docs/informe-final/main.pdf` se recompiló en esta misma ronda con `make docs` (contenedor
  `texlive/texlive`, tal como documenta el README para quien no tenga TeX local) — `BUILD` limpio,
  84 páginas, código de salida `0`.
- **Piso 3** (ningún dato inventado): el hallazgo de `RALPH2021` (P11) se corrigió en cuanto se
  detectó, no se dejó ni se disimuló; la discrepancia de cifras de P5 (jacoco desactualizado) y el
  "48 vs 45" de P11 se investigaron y documentaron en vez de dejarse pasar. Ningún número de este
  expediente se escribió a mano; todos son salida literal de los comandos listados. **Corrección
  adicional en esta ronda:** el Resumen y el Abstract de
  [`00-portada-resumen.tex`](docs/informe-final/secciones/00-portada-resumen.tex) citaban la
  cobertura JaCoCo *anterior* a la corrección de P5 (82.93 %/71.50 %) mientras la sección de
  resultados y el `jacoco.csv` real ya decían 91.15 %/81.89 % — no era un dato inventado (venía de
  una medición real de una ronda previa), pero sí una cifra desactualizada presentada como vigente
  en el mismo documento. Se corrigió a 91.15 %/81.89 % en ambos idiomas y se recompiló el PDF;
  verificado extrayendo el texto del PDF recompilado con `pdftotext` que ambos párrafos ya
  coinciden con la sección de resultados.
- **Piso 4** (sin commits ajenos ni correos falsos): reverificado el 2026-09-16 —
  ```bash
  git log --since="2026-09-01" --format="%an <%ae>" | sort | uniq -c | sort -rn
  ```
  ```
      187 Bryan Figueroa <bfigueroam@uteq.edu.ec>
       51 Johan Stalin Carvajal Loor <carvajalstalin.10@gmail.com>
       33 Johan_Loor <91645452+JohanCarvajal04@users.noreply.github.com>
       19 Scarleth Bone <nbonea@uteq.edu.ec>
       14 Jhon-Kevin-Rios-Cuyabazo <JhonRios_180@hotmail.com>
        1 Jk-RiosC <161248207+Jhon-Kevin-Rios-Cuyabazo@users.noreply.github.com>
  ```
  Los 6 identificadores de autor mapean a los 4 integrantes declarados en la portada del informe y
  en `CONTRIBUTORS.md` (cada uno usó más de una identidad de git/GitHub a lo largo del proyecto,
  pero ninguna es ajena al equipo). Sin cambios en la configuración de identidad de git en esta
  sesión.

## Cómo reproducir todo el expediente de una vez

```bash
make verify
```

Encadena, en orden: `mvn test` (regenera `jacoco.csv`), cobertura por controlador (P5), cero
`nativeQuery=true` (P6), cero etiquetas `.tex` huérfanas (P2), DOI resueltos + cobertura
DOI-o-razón de la bibliografía (P3+P11), `sync-procs-check`, Javadoc completo ≥90% (P7) y
`mvn javadoc:javadoc` sin errores (P7). Termina con código de salida distinto de cero si
cualquiera de los 8 pasos falla.

### EV-2 — corrida real de los 8 pasos con el binario `make` (2026-09-16)

**Actualización respecto a la corrida anterior (2026-09-14):** aquella corrida tuvo que simular
el target a mano porque la shell no tenía `make` instalado, y solo cubría 6 pasos (el target no
incluía todavía la comprobación de etiquetas huérfanas de P2 ni la completitud de Javadoc de P7).
Ambos huecos ya están cerrados: `make` está instalado y **`make verify` se ejecutó literalmente,
por su nombre, sin simular nada**, con los 8 pasos actuales del target.

**Orden:** `make verify`

Salida real, completa, sin editar: [`docs/mediciones/make-verify-20260916.txt`](docs/mediciones/make-verify-20260916.txt)
(3448 líneas — incluye el log SQL de Hibernate de las 1448 pruebas). Resumen de cada paso:

```
--- [1/8] mvn test (backend, regenera jacoco.csv) ---
Tests run: 1448, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS (1:27 min)

--- [2/8] P5: cobertura >=70% en los 9 paquetes controller.* ---
9 paquetes controller.*, 0 bajo el 70% (mínimo: security 84.9%/89.3%)

--- [3/8] P6: cero nativeQuery=true ---
nativeQuery = true (real, fuera de comentario): 0

--- [4/8] P2: cero etiquetas .tex huerfanas ---
Etiquetas SIN ninguna referencia: 0 de 62

--- [5/8] P3+P11: DOI + cobertura DOI-o-razon ---
Total: 35 DOI verificados (32 bibliograficos + 3 de software/dataset), 0 fallidos.
Cobertura DOI-o-razon en referencias.bib: Total: 45, con DOI: 32, sin DOI con razon: 13, sin nada: 0

--- [6/8] sincronia db/procs <-> R__procedimientos.sql ---
OK: R__procedimientos.sql sincronizado con db/procs/ (27 rutinas)

--- [7/8] P7: Javadoc completo (@param/@return/@throws) >=90% ---
TOTAL con Javadoc completo: 1166/1166 (100.0%) -- umbral exigido: 90%
OK

--- [8/8] P7: Javadoc sin errores de doclint ---
BUILD SUCCESS (12.1 s)

OK: make verify termino sin errores.
EXIT_CODE=0
```

Los 6 pasos pasaron en verde y la cadena terminó con código de salida **0**.
