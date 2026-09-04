# Plan paso a paso — Camino al examen final (ARTISYNC)

**Documento fuente de las observaciones:** [`observaciones_para_el_examen.md`](observaciones_para_el_examen.md)
**Ventana de trabajo:** del 3 al 11 de septiembre de 2026 · **Examen: semana 19 (7–11 de septiembre)**
**Punto de partida:** nota de equipo 6,60 / 10 · Figueroa 7,46 · Carvajal 7,71 · Ríos 6,46

---

## §0. Cómo usar este plan

- Cada tarea lleva **ID `T-xx`**, las **observaciones que cierra**, **responsable**, **archivos exactos**, **pasos**, **comando de verificación** y **criterio de aceptación**.
- El criterio de aceptación es el único que cuenta: *«Una corrección está terminada cuando un tercero que clona su repositorio en una máquina limpia puede comprobarla sin preguntarles nada.»*
- **Nomenclatura de commits obligatoria desde hoy** (afecta a la nota individual de Carvajal y Ríos):
  `tipo(ámbito): descripción real en imperativo` — p. ej. `fix(docs): recompilar informe con bibtex y 3 pasadas`.
- **Ríos y Carvajal deben usar de aquí en adelante su correo institucional y una sola identidad Git** (T-38).

**Iniciales de responsable:** **BF** = Figueroa · **JC** = Carvajal · **JK** = Ríos · **EQ** = los tres.

### Riesgo de nota si no se hace nada

Los cuatro pisos (PISO-01…04) **no se sancionaron esta vez pero sí se aplican en el examen**. Si el 7 de septiembre siguen incumplidos, la calificación de toda la entrega es **cero**, independientemente del resto del plan. Por eso las tareas T-01 a T-07 son de máxima prioridad absoluta y van todas el lunes.

### Ganancia estimada por bloque

| Bloque | Criterios que sube | Δ nota equipo estimada |
|---|---|---|
| Pisos + PDF + despliegue (T-01…T-12) | PISO×4, P5 25→100, D1 50→75 | **+0,55** |
| SUS resuelto con honestidad (T-13…T-17) | D4 25→100, R2 75→100 | **+0,60** |
| Producto: procedimientos + cobertura + seguridad (T-18…T-26) | P1 50→100, P3 75→100 | **+0,53** |
| Mediciones k6 + Lighthouse público (T-27…T-31) | P2 50→100, P4 se conserva | **+0,30** |
| Documento: cap. 3, bibliografía, referencias (T-32…T-40) | D2 25→100, D6 75→100, D1 75→100 | **+0,60** |
| Reproducibilidad y metadatos (T-41…T-48) | R1 75→100, R4 75→100, D0R 75→100 | **+0,45** |

Techo realista con el plan completo: **9,6 / 10** de nota de equipo.

---

## §1. FASE 0 — Antes de tocar nada (3 de septiembre, 45 min, EQ)

### T-00 · Prueba del clon limpio — ✅ HECHA (2026-09-03)
**Cubre:** §11.3 de la guía — *«La comprobación más barata y la que más equipos suspende… Háganla hoy.»*
**Responsable:** BF · **Duración:** 30 min

> **Resultado:** ver [`BITACORA-CLON-LIMPIO.md`](BITACORA-CLON-LIMPIO.md). `make` no estaba disponible en la máquina de prueba, así que se ejecutaron a mano los comandos exactos de cada objetivo, en el orden de `all`. Confirmado: **rompe en `up`** por el `.env` no versionado (OBS-R1-01, tal como preveía este plan). `test` pasa limpio. Se confirmó por lectura directa que `audit-sql-dynamic` no está encadenado en ningún objetivo (OBS-R1-03). Se encontró un **hallazgo nuevo no listado antes**: `make docs` exige TeX Live local sin fallback de Docker, a diferencia de `srs`/`lighthouse` — añadido como **T-36b**.

```bash
git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC.git /tmp/artisync-clean
cd /tmp/artisync-clean && make all 2>&1 | tee /tmp/clon-limpio-$(date +%Y%m%d).log
```

**Pasos:**
1. Ejecutar el clon en un directorio **vacío**, sin reutilizar cachés locales.
2. Seguir el `README.md` **al pie de la letra**, sin aplicar conocimiento propio.
3. Anotar en `docs/observaciones/BITACORA-CLON-LIMPIO.md` **cada punto exacto donde se rompe**, con el mensaje de error literal.
4. Ese archivo es la lista de entrada de las tareas T-41 a T-46.

**Criterio de aceptación:** existe `BITACORA-CLON-LIMPIO.md` con los puntos de ruptura fechados. **Se repite esta misma prueba el 11 de septiembre (T-49) y debe terminar con código 0.**

> Predicción a confirmar: fallará en el paso 1 por el `.env` no versionado (OBS-R1-01) y en `make sus` por `python` vs `python3` (OBS-R1-02).

### T-00b · Crear la rama de trabajo y el tablero
**Responsable:** JC · **Duración:** 15 min

```bash
git checkout -b fix/camino-examen-final
```

Copiar el checklist de §13 del documento de observaciones a issues de GitHub, uno por observación, etiquetados `piso`, `eje1`, `eje2`, `eje3`, `transversal`. Esto genera además la evidencia de **revisión entre integrantes** que exige T-40.

---

## §2. LUNES — Lo urgente (los cuatro pisos)

> Bloque del docente: *«Recompilar y subir el PDF. Rotar las tres credenciales del historial. Traer el instrumento del estudio de usabilidad. Aclarar por escrito la composición del equipo.»*

### T-01 · Recompilar el PDF correctamente y volver a subirlo
**Cubre:** PISO-02, OBS-D1-01, IND-JC-03 · **Responsable:** JC · **Duración:** 20 min · **Prioridad: máxima**

El objetivo `docs:` del `Makefile` **ya hace lo correcto** (pdflatex → bibtex → pdflatex → pdflatex → copia a `Informe-Final-v1.0.0.pdf`). El PDF versionado es simplemente una compilación antigua de una sola pasada. No hay que arreglar el pipeline: hay que **ejecutarlo y comitear el resultado**.

**Pasos:**
1. Añadir `makeglossaries` al objetivo `docs:` del `Makefile`, entre la primera pasada de `pdflatex` y `bibtex` (cierra OBS-D1-06):

```make
	cd docs/informe-final && \
		pdflatex -interaction=nonstopmode main.tex && \
		bibtex main && \
		makeglossaries main && \
		pdflatex -interaction=nonstopmode main.tex && \
		pdflatex -interaction=nonstopmode main.tex
```

2. Añadir `\listoflistings` (o `\lstlistoflistings` según el paquete usado) en `docs/informe-final/main.tex`, junto a `\tableofcontents`, `\listoffigures` y `\listoftables` (cierra OBS-D1-07).
3. Ejecutar `make docs`.
4. **Verificar antes de comitear.**

**Comando de verificación:**

```bash
make docs && pdftotext docs/informe-final/Informe-Final-v1.0.0.pdf - | grep -c '\[?\]'
```

**Criterio de aceptación (guía §3.2):**
- `grep -c '\[?\]'` devuelve **0** (hoy: 90).
- El PDF versionado tiene **el mismo número de páginas que el recompilado** — esperado **76**, hoy 69 (`pdfinfo … | grep Pages`).
- **Los tres índices tienen contenido**: general, de figuras y de cuadros. Más el nuevo índice de listados y la lista de siglas no vacía.
5. Comitear: `docs(informe): recompilar PDF con bibtex, makeglossaries y tres pasadas`.

> **Esta es la corrección de mayor retorno por minuto invertido de todo el plan.** Desbloquea PISO-02 y sube D1 de 50 % a 75 %.

### T-02 · Producir la carátula PDF de una página
**Cubre:** PISO-01 · **Responsable:** JC · **Duración:** 30 min

**Pasos:**
1. Crear `docs/informe-final/caratula.tex`: **una sola página**, con la URL del repositorio **en una sola línea sin saltos** (`\url{...}` dentro de un `\sloppy` o con `\UrlBreaks` desactivado), los cuatro ORCID, el hash corto del commit que se defiende, los dos DOI de Zenodo, título, autores, asignatura y fecha.
2. Añadir un objetivo `caratula:` al `Makefile` que la compile a `docs/informe-final/Caratula-v1.1.0.pdf` y encadenarlo en `docs:`.

**Criterio de aceptación:** existe un PDF **de exactamente una página** en el repositorio, y `pdftotext caratula.pdf - | grep -c 'github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC'` devuelve 1 **en una sola línea de salida**.

> Ojo: el hash corto que declare la carátula debe ser **el commit que efectivamente se defiende**, no `d07656b`. Actualizarlo en T-48 al cerrar la semana.

### T-03 · Rotar el `JWT_SECRET` y las dos contraseñas de base de datos
**Cubre:** OBS-P3-04, PISO adyacente · **Responsable:** JC + JK · **Duración:** 1 h · **Prioridad: máxima**

Cifras rectificadas por la guía: `JWT_SECRET` en **358 commits desde el 20 de junio**; `DB_PASSWORD` y `DB_APP_PASSWORD` en **264 y 260 commits desde el 7 de agosto**.

**Pasos — en este orden:**
1. **Generar tres valores nuevos** (no reutilizar, no derivar de los antiguos):
```bash
openssl rand -base64 48   # JWT_SECRET
openssl rand -base64 32   # DB_PASSWORD
openssl rand -base64 32   # DB_APP_PASSWORD
```
2. **Rotar en origen**: cambiar la contraseña real del usuario de Postgres (`ALTER USER … WITH PASSWORD …`) y del usuario de aplicación, y el secreto de firma en la configuración de despliegue. Los valores viejos deben quedar **inválidos**, no solo ausentes del árbol.
3. Cargar los nuevos valores **solo** como variables de entorno de Render (T-08) y en `artisync/.env` local, que sigue en `.gitignore`.
4. **Sacar el `JWT_SECRET` del `application.properties` de pruebas.** Sustituirlo por un valor de prueba generado en tiempo de test o inyectado por `@DynamicPropertySource`:
```properties
# artisync/Backend/src/test/resources/application.properties
jwt.secret=${JWT_TEST_SECRET:ZmFrZS10ZXN0LXNlY3JldC1uby11c2FyLWVuLXByb2R1Y2Npb24=}
```
5. Verificar que `artisync/.env.example` no contiene ningún valor real (solo placeholders del tipo `CAMBIAR_ANTES_DE_DESPLEGAR`).
6. Documentar la rotación en `docs/mediciones/sec/ROTACION-CREDENCIALES.md`: qué se rotó, cuándo (UTC), quién, y **confirmación explícita de que los valores antiguos ya no autentican**.

**Comando de verificación:**
```bash
git grep -n "<fragmento-del-secreto-viejo>" $(git rev-list --all) | head
grep -rn "jwt.secret" artisync/Backend/src/test/resources/
```

**Criterio de aceptación (guía §3.4):** *«Ninguno de los valores expuestos sigue siendo válido, y no aparecen en el árbol actual ni en la configuración de pruebas.»*

> **No se exige reescribir el historial** (`git filter-repo`) y no lo recomiendo esta semana: reescribir 358 commits invalidaría todos los hashes citados en `DATA-PROVENANCE`, en el Anexo A y en la portada — 29 hashes verificados que hoy son una fortaleza reconocida. **Rotar los valores en origen satisface el criterio literal sin destruir la trazabilidad.** Dejarlo argumentado por escrito en el ADR (T-47).

### T-04 · Reunir y presentar el instrumento original del SUS
**Cubre:** OBS-D4-01, punto 1 de las exigencias del equipo y de Figueroa · **Responsable:** BF · **Duración:** 2 h · **Prioridad: máxima**

Esta es la tarea que el docente pide **antes que ninguna otra** a Figueroa, y sobre la que exige una conversación presencial.

**Pasos:**
1. Recuperar el **export íntegro y sin editar** del formulario (Google Forms / la plataforma usada), con sus marcas temporales, y versionarlo tal cual bajo `docs/mediciones/sus/export-formulario-original.csv` — nombre explícito, sin sobrescribir nada.
2. Documentar **hoja de cálculo en mano** el origen de las cinco filas divergentes (P12–P16). Solo hay dos desenlaces posibles y ambos son aceptables **si se documentan**:
   - **(a) Hay explicación** — p. ej. cinco sesiones presenciales adicionales recogidas en papel y transcritas, con su registro de sesión. Entonces: aportar las hojas firmadas, añadirlas como anexo escaneado y explicar por qué el export no las contiene.
   - **(b) No hay explicación** — entonces **`sus-raw.csv` se reemplaza por el export real**, se republica **61,25** y se reescribe la discusión.
3. **Preparar la reunión con el docente antes del 7 de septiembre.** No es opcional: *«necesito que hablemos de ello antes del examen»*.

**Criterio de aceptación:** existe en el repositorio el export original sin editar y un documento `docs/mediciones/sus/ACLARACION-SUS.md` fechado que explica, fila a fila, el origen de P12 a P16.

> Recomendación explícita, y la mantengo aunque incomode: **salvo que existan las hojas firmadas de las cinco sesiones, el camino (b) es el correcto.** El docente lo ha escrito tres veces con las mismas palabras: *«un 61,25 declarado vale infinitamente más que un 76,88 que no se sostiene»*, y en la guía §5.4 añade que ajustar datos es *«la única falta de este curso que no tiene arreglo posterior»*. La nota no baja por publicar 61,25; el criterio D4 sube de 25 % a 100 % **precisamente por publicarlo**.

### T-05 · Aclarar por escrito la composición del equipo
**Cubre:** OBS-R3-01, OBS-R3-02, punto 15 · **Responsable:** JC · **Duración:** 1 h

La cuarta identidad tiene **15 commits y el 75 % del trabajo de base de datos**, y no figura en el padrón del curso.

**Pasos:**
1. Redactar `docs/etica/COMPOSICION-EQUIPO.md`: quién es cada una de las cuatro personas, su identidad Git y correos, qué aportó, y **por qué la cuarta no está en el padrón** (¿colaboradora externa? ¿estudiante de otro paralelo? ¿cuenta secundaria de un integrante?).
2. Firmarlo los tres integrantes del padrón.
3. Alinear la respuesta en **los cuatro sitios**: `CITATION.cff`, `CONTRIBUTORS.md`, `.zenodo.json` y la portada del informe. Si la cuarta persona es autora legítima, se queda en los cuatro; si no lo es, se retira de los cuatro y se la reconoce en `CONTRIBUTORS.md` como colaboradora no autora.
4. **Asignar el rol CRediT que hoy figura sin persona** (OBS-R3-02).

**Criterio de aceptación (guía §3.9):** *«La composición del equipo es la misma en el repositorio, en los archivos de autoría, en el documento y en el padrón.»*

> Cuidado: **R3 es hoy un 100 % y el mejor criterio del curso.** Cualquier cambio en `CITATION.cff` / `.zenodo.json` debe conservar la coherencia de los cuatro ORCID validados por ISO 7064 en los cuatro archivos. Verificar tras editar:
> ```bash
> grep -h "orcid" CITATION.cff CONTRIBUTORS.md .zenodo.json | sort -u
> ```

### T-06 · Firmar el SRS
**Cubre:** PISO-04, OBS-D0R-02 · **Responsable:** BF · **Duración:** depende del docente-director — **iniciar el lunes**

La sección 8 del SRS dice literalmente «Estado de la aprobación: pendiente de firma». El `Makefile` ya avisa de ello al final de `make srs`.

**Pasos:**
1. Regenerar el SRS actualizado: `make srs`.
2. **Solicitar la firma del docente-director esta misma semana** — es un trámite con terceros y es el único piso que no depende solo del equipo. Iniciarlo el lunes.
3. Incorporar la firma (escaneada o digital) en la sección 8 y volver a generar el PDF.

**Criterio de aceptación:** `docs/requisitos/SRS-v1.0.0.pdf` sección 8 muestra la aprobación firmada y fechada, no «pendiente de firma».

### T-07 · Convención de commits e identidades desde hoy
**Cubre:** IND-JC-01, IND-JC-02, IND-JK-03, OBS-TR-08 · **Responsable:** JC + JK · **Duración:** 10 min

```bash
git config user.name "Johan Stalin Carvajal Loor"
git config user.email "<correo institucional @uteq.edu.ec>"
```

Añadir `.mailmap` en la raíz para unificar retroactivamente las identidades sin reescribir historial:

```
Johan Stalin Carvajal Loor <jcarvajall@uteq.edu.ec> <carvajalstalin.10@gmail.com>
Johan Stalin Carvajal Loor <jcarvajall@uteq.edu.ec> <91645452+johancarvajal04@users.noreply.github.com>
Jhon Kevin Rios Cuyabazo <jrriosc@uteq.edu.ec> <jhonrios_180@hotmail.com>
```

**Criterio de aceptación:** `git shortlog -sne --all` muestra **una entrada por persona** y todos los commits nuevos de la semana llevan correo institucional y mensaje convencional descriptivo (mínimo 40 caracteres). **JK debe además registrar su ORCID** (su exigencia individual n.º 4).

---

## §3. MARTES Y MIÉRCOLES — El producto

### T-08 · Ejecutar el despliegue en Render
**Cubre:** PISO-03, OBS-P5-01, punto 10 · **Responsable:** JC · **Duración:** 3 h · **Prioridad: máxima**

Todo el material existe: `render.yaml` en la raíz, la rama de despliegue y un `RENDER.md` de 219 líneas. **Solo falta ejecutarlo.**

**Pasos:**
1. Fusionar la rama de despliegue en `main` (o la de trabajo), resolviendo los conflictos ya identificados.
2. Crear el Blueprint en Render desde `render.yaml`.
3. Cargar las **variables de entorno con los valores rotados en T-03** — nunca los antiguos.
4. Verificar que la configuración de CORS acepta el origen público real, no `localhost`.
5. Esperar a que los tres servicios (postgres, backend, frontend) queden en verde.
6. **Declarar la URL pública** en `README.md`, en la portada del informe y en `DEPLOYMENT.md` — y **corregir en `DEPLOYMENT.md` la frase que declara que el sistema no está desplegado** (hoy es la prueba documental en contra del equipo).

**Comando de verificación:**
```bash
curl -sS -o /dev/null -w "%{http_code}\n" https://<url-publica>/
curl -sS https://<url-publica>/actuator/health
```

**Criterio de aceptación (guía §3.3):** *«Una petición a la dirección pública declarada devuelve la aplicación, y las cabeceras de seguridad están presentes en la respuesta.»* → la segunda mitad la cierra T-09.

> Este es el criterio **completo** P5, hoy en 25 %. Y desbloquea T-29 (Lighthouse público) y T-30 (ZAP autenticado), que hoy son imposibles.

### T-09 · Cabeceras de seguridad en el nginx del frontend
**Cubre:** OBS-P3-02, punto 8 · **Responsable:** JC · **Duración:** 1 h

El backend ya las configura; el frontend —**el único punto público**— no emite ninguna. Ahí es donde ZAP levanta las dos alertas medias.

**Archivos:** `artisync/Frontend/nginx.conf` **y** `artisync/Frontend/nginx.render.conf.template` (¡los dos, o el despliegue no las tendrá!).

```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' https://<backend-publico>; frame-ancestors 'none'; base-uri 'self'; form-action 'self'" always;
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

**Comando de verificación:**
```bash
curl -sI https://<url-publica>/ | grep -iE 'content-security-policy|x-frame-options|strict-transport|x-content-type'
```

**Criterio de aceptación:** las cuatro cabeceras aparecen en la respuesta pública, y **el reescaneo ZAP de T-30 ya no levanta las dos alertas medias** de CSP y anti-clickjacking.

> Verificar en el navegador que la CSP no rompe la aplicación Angular. Si `script-src 'self'` bloquea algo, ajustar con nonce, **nunca con `unsafe-inline` en `script-src`** — eso invalidaría el propósito de la cabecera.

### T-10 · Activar `secure` en la cookie de refresco
**Cubre:** OBS-P3-03, punto 8 · **Responsable:** JC · **Duración:** 30 min

**Archivo:** el emisor de la cookie de refresco en el backend (buscar con `grep -rn "refresh" --include=*.java artisync/Backend/src/main/java | grep -i cookie`).

```java
ResponseCookie.from("refreshToken", token)
    .httpOnly(true)
    .secure(true)          // hoy false
    .sameSite("Strict")
    .path("/api/v1/auth")
    .maxAge(Duration.ofDays(7))
    .build();
```

Para que el desarrollo local en HTTP siga funcionando, externalizarlo: `.secure(cookieSecure)` con `@Value("${app.cookie.secure:true}")` — **por defecto `true`**, y `false` solo en el perfil `dev`. El defecto seguro es el que se audita.

**Criterio de aceptación:** `curl -sI` sobre el login público muestra `Set-Cookie: … Secure; HttpOnly; SameSite=Strict`.

### T-11 · Convertir los procedimientos al mecanismo formal
**Cubre:** OBS-P1-02, OBS-D1-05, punto 6 · **Responsable:** JK (asignación individual explícita) + JC · **Duración:** 6 h · **Es un criterio completo**

Hoy: 28 rutinas versionadas, 26 invocadas, **ninguna de esas 26 usa el mecanismo formal**; la única anotación `@Procedure` del proyecto corresponde a una rutina que no está entre las 28.

**Pasos:**
1. Inventariar el estado real:
```bash
grep -rn "@Procedure\|@NamedStoredProcedureQuery" artisync/Backend/src/main/java --include=*.java
grep -rln "nativeQuery\s*=\s*true" artisync/Backend/src/main/java --include=*.java
ls db/procs/
```
2. **Priorizar las rutinas de negocio principales** — el criterio de la guía no exige las 26, exige *«las rutinas de negocio principales»*. Elegir entre 8 y 12: comisiones, pagos, catálogo, seguidores, notificaciones.
3. Convertir cada una. Patrón A (el más directo):
```java
@Procedure(procedureName = "sp_crear_comision")
Long crearComision(@Param("p_artista_id") Long artistaId,
                   @Param("p_cliente_id") Long clienteId,
                   @Param("p_monto") BigDecimal monto);
```
Patrón B, para rutinas con varios parámetros de salida o cursores:
```java
@NamedStoredProcedureQuery(
    name = "Comision.resumenPorArtista",
    procedureName = "sp_resumen_comisiones_artista",
    parameters = {
        @StoredProcedureParameter(mode = ParameterMode.IN,  name = "p_artista_id", type = Long.class),
        @StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class)
    })
```
4. **Mantener el test de cada rutina en verde tras la conversión** — la conversión no debe bajar cobertura.
5. **No romper la higiene de SQL** (fortaleza n.º 3): cero concatenación, parámetros nombrados siempre.
6. Cambiar el listado del documento LaTeX para que muestre **el `@Procedure` real que ahora existe**, no el `@Query` nativo (OBS-D1-05) — en `docs/informe-final/secciones/07-implementacion.tex`.

**Comando de verificación:**
```bash
grep -rc "@Procedure\|@NamedStoredProcedureQuery" artisync/Backend/src/main/java --include=*.java | grep -v ':0'
make test
```

**Criterio de aceptación (guía §3.5):** *«Las rutinas de negocio principales se invocan con la anotación formal, el catálogo declara el número real y el listado del documento muestra el código que existe.»*

> **Es la tarea individual clave de Ríos.** El docente se la asigna nominalmente: *«las dos son backend, que es lo suyo, y las dos son criterios completos»*. Es su vía más directa para subir I4 de 1/4.

### T-12 · Actualizar el catálogo de rutinas
**Cubre:** OBS-P1-03, punto 13 · **Responsable:** JK · **Duración:** 1 h

El catálogo declara **21 rutinas activas cuando son 28**. Además hay seis rutinas de seguidores desplegadas y sin documentar.

**Pasos:**
1. Contar la verdad: `ls db/procs/*.sql | wc -l` y contrastar con `R__procedimientos.sql`.
2. Actualizar el catálogo en `docs/basedatos/` con **las 28**, indicando por cada una: nombre, propósito, parámetros, repositorio Java que la invoca y **mecanismo** (`@Procedure` / `@NamedStoredProcedureQuery` / `@Query nativa`).
3. Ejecutar `make sync-procs-check` para confirmar sincronía.

**Criterio de aceptación:** el número del catálogo coincide con `ls db/procs/*.sql | wc -l`, y `make sync-procs-check` termina en 0.

### T-13 · Subir la cobertura de controladores
**Cubre:** OBS-P1-01, punto 7 · **Responsable:** JK + BF · **Duración:** 8 h · **Es medio criterio**

Estado: **controladores 29,17 % líneas (84/288) y 30,56 % ramas**; servicios 78,37 / 66,67; global 72,02 / 62,49.

**Meta:** ≥ 70 % de **líneas y ramas** en **las tres capas**. El criterio exige las dos métricas en las tres capas y hoy no lo consigue ninguna.

**Pasos:**
1. Identificar los controladores sin prueba:
```bash
awk -F, 'NR>1 && $2 ~ /controller/ {print $3, $4, $5}' docs/mediciones/jacoco/html/jacoco.csv | sort
```
2. Escribir pruebas `@WebMvcTest` por controlador — es el patrón que más líneas cubre por línea de test escrita:
```java
@WebMvcTest(ComisionController.class)
class ComisionControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ComisionServicio servicio;

    @Test @WithMockUser(roles = "ARTISTA")
    void listarComisiones_devuelve200YElCuerpoEsperado() throws Exception { … }

    @Test @WithMockUser(roles = "CLIENTE")
    void listarComisiones_sinPermiso_devuelve403() throws Exception { … }
}
```
3. **Cubrir ramas, no solo líneas:** por cada endpoint, un test del camino feliz, uno de validación fallida (400), uno de autorización denegada (403) y uno de recurso inexistente (404). La cobertura de ramas es la que está peor y es la que se olvida.
4. Regenerar la evidencia **con una sola ejecución de cierre** sobre el commit que se defiende (guía §4.2):
```bash
cd artisync/Backend && ./mvnw clean verify
cp target/site/jacoco/jacoco.csv ../../docs/mediciones/jacoco/
cp target/site/jacoco/jacoco.xml ../../docs/mediciones/jacoco/report.xml
```
5. **Actualizar en el documento y el README las cifras nuevas**, y verificar que **el umbral declarado en el texto es el mismo que fija el `pom.xml`** (guía §4.2).

**Criterio de aceptación (guía §4.2):** un tercero ejecuta `mvn clean verify`, suma las columnas del `jacoco.csv` resultante y obtiene **exactamente** la cifra publicada. Y las tres capas superan el 70 % en líneas **y** en ramas.

> Trampa a evitar: publicar la cifra de una corrida anterior. **La cifra del documento debe salir del `jacoco.csv` del commit que se defiende**, regenerado al final de la semana (T-48).

---

## §4. JUEVES — Las mediciones

### T-14 · Repetir la carga k6 contra un endpoint protegido, cinco corridas
**Cubre:** OBS-P2-01, OBS-P2-02, punto 5 · **Responsable:** BF · **Duración:** 4 h

Hoy: `/api/v1/catalogo`, declarado `permitAll` en `SecurityConfig.java`, **tres** corridas por escenario.

**Pasos:**
1. Escribir `k6/comisiones-load.js` — mismo perfil de carga (50 VUs, 30 s) pero **contra un endpoint autenticado y representativo del uso real**:
```javascript
import http from 'k6/http';
import { check } from 'k6';

export const options = { vus: 50, duration: '30s' };

export function setup() {
  const res = http.post(`${__ENV.BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: __ENV.K6_USER, password: __ENV.K6_PASS }),
    { headers: { 'Content-Type': 'application/json' } });
  return { token: res.json('accessToken') };
}

export default function (data) {
  const res = http.get(`${__ENV.BASE_URL}/api/v1/comisiones?page=0&size=20`,
    { headers: { Authorization: `Bearer ${data.token}` } });
  check(res, { 'status 200': (r) => r.status === 200 });
}
```
2. Ejecutar **cinco corridas independientes por escenario** (caliente y frío), **una por archivo NDJSON versionado**:
```bash
for i in 1 2 3 4 5; do
  k6 run --out json=docs/mediciones/perf/k6-auth-run$i.json k6/comisiones-load.js
done
```
3. Repetir para el escenario frío → `k6-auth-cold-run1..5.json`.
4. Conservar los archivos actuales del catálogo: siguen siendo evidencia válida de un escenario público, y son una fortaleza reconocida (reproducen dígito a dígito). Se **añade** el escenario protegido, no se sustituye.
5. Actualizar `k6/` y el objetivo `bench:` del `Makefile` para que ejecute el nuevo script.

**Criterio de aceptación (guía §3.6, §4.3):** *«Cinco archivos crudos por escenario contra un endpoint autenticado»* → `ls docs/mediciones/perf/k6-auth-run*.json | wc -l` devuelve **5** por escenario.

### T-15 · Añadir el test inferencial y el tamaño de efecto
**Cubre:** OBS-P2-03, OBS-D4-02, punto 5 · **Responsable:** BF · **Duración:** 3 h

El docente ya calculó **Welch t = 14,538** y **d de Cohen = 0,3065**, pero la guía §4.3 matiza: para datos de latencia corresponde el **no paramétrico** (Mann-Whitney U) y un **tamaño de efecto ordinal** (Â₁₂ de Vargha–Delaney o δ de Cliff), citando a Arcuri y Briand.

**Recomendación:** reportar **ambos** — el paramétrico porque el docente lo calculó y sirve de contraste, y el no paramétrico porque es el correcto. Eso demuestra que se entendió el matiz de la guía, no solo que se copió el número.

**Pasos:**
1. Crear `docs/mediciones/perf/analisis-inferencial.py`, **versionado y ejecutable sin edición manual** (guía §4.1):
```python
#!/usr/bin/env python3
"""Recalcula desde los NDJSON crudos: descriptivos, Mann-Whitney U, A12 y d de Cohen."""
from scipy.stats import mannwhitneyu, ttest_ind
# ... carga los k6-auth-run*.json y k6-auth-cold-run*.json
# imprime: media, mediana, desv., p90, p95, p99, tasa de error, n peticiones
# imprime: U, p en notación científica, A12 de Vargha-Delaney, d de Cohen
# aplica corrección de Holm-Bonferroni si hay más de una comparación
```
2. Ejecutarlo y volcar la salida a `docs/mediciones/perf/salida-inferencial.txt`.
3. Escribir los resultados en `docs/informe-final/secciones/08-evaluacion-resultados.tex`, **con el valor p en notación científica** y el tamaño de efecto con su interpretación.
4. **Retirar del capítulo 8 la admisión de que no hay test inferencial** — ya no será cierta.
5. Añadir un objetivo `perf-stats:` al `Makefile` y encadenarlo en `all`.

**Criterio de aceptación (guía §4.3):** un script versionado, ejecutado sobre los archivos crudos, **imprime exactamente las cifras del documento**, incluidos el estadístico del test, **el valor p en notación científica** y el tamaño de efecto.

### T-16 · Rehacer Lighthouse contra la URL pública y sobre varias rutas
**Cubre:** OBS-P4-01 · **Responsable:** JC · **Duración:** 2 h · **Depende de T-08**

P4 es hoy un **100 %**, pero con la reserva de que se auditó localhost y solo la portada. Con el despliegue vivo se convierte en un 100 % sin reservas — y, más importante, **la guía §4.5 exige `requestedUrl` apuntando a la URL pública**: si esto no se rehace, el criterio podría bajar.

**Pasos:**
1. Editar `artisync/Frontend/lighthouserc.mobile.json` y `lighthouserc.desktop.json`: cambiar `url` de `localhost:4200` a la URL pública, **y añadir al menos tres rutas** (portada, catálogo, detalle de comisión).
2. Ejecutar `make lighthouse` (tres corridas por perfil, secuenciales, como ya documenta el Makefile).
3. **Guardar el JSON completo** de cada corrida — más de 150 auditorías, no solo las cuatro puntuaciones.
4. Reportar **la media de las tres corridas por perfil y por ruta**, con la **versión exacta de la herramienta y la marca de tiempo de cada corrida**.

**Criterio de aceptación (guía §4.5):** al menos **seis JSON completos** con `requestedUrl` apuntando a la URL pública, y las puntuaciones del documento son **la media aritmética** de los archivos del repositorio.
```bash
grep -o '"requestedUrl":"[^"]*"' docs/mediciones/lighthouse/*.json | sort -u
```

### T-17 · Completar la evidencia OWASP y el reescaneo ZAP autenticado
**Cubre:** OBS-P3-01, OBS-P3-02 · **Responsable:** JC + JK · **Duración:** 3 h · **Depende de T-08, T-09**

Hoy: solo **1 de 6** controles OWASP tiene el `curl` literalmente transcrito, y el propio equipo marca **A07 como OBSOLETA**.

**Pasos:**
1. Para **cada uno de los seis controles**, transcribir en `docs/mediciones/sec/REPORTE-SEC.md` la petición real y su respuesta, con **fecha en UTC y hash del commit auditado** (guía §4.4):
```
## A01 — Broken Access Control
Commit auditado: <hash> · Fecha: 2026-09-10T14:32:00Z
$ curl -sS -i -X GET https://<url>/api/v1/comisiones/42 \
    -H "Authorization: Bearer <token-de-otro-usuario>"
HTTP/1.1 403 Forbidden
{"error":"ACCESO_DENEGADO",...}
```
2. **Rehacer la evidencia de A07** — hoy marcada OBSOLETA por el propio equipo.
3. Reescanear con ZAP **contra la aplicación desplegada y autenticada**, no la raíz sin sesión: *«un escaneo que solo alcanza la portada y el `robots.txt` no ha probado nada del sistema»* (guía §4.4). Usar un contexto ZAP con sesión autenticada.
4. **Reportar el conteo por severidad tal como lo emite la herramienta, sin reinterpretar.**

**Criterio de aceptación (guía §4.4):** los archivos de salida **abren con la herramienta que los produjo**, y el conteo por severidad del documento coincide con el del archivo **hallazgo por hallazgo**. Las dos alertas medias de CSP y anti-clickjacking han desaparecido.

---

## §5. VIERNES — El documento

### T-18 · Escribir el capítulo de trabajos relacionados
**Cubre:** OBS-D2-01 a OBS-D2-06, punto 3 · **Responsable:** BF · **Duración:** 10 h · **La tarea más costosa del plan**

Hoy vale **25 %** con peso 5 %. Bien hecho vale 100 %: **+0,375 puntos de nota de equipo**, la mayor ganancia unitaria del plan.

**Archivo:** `docs/informe-final/secciones/03-trabajos-relacionados.tex`

**Pasos — hacer la revisión de verdad, no simularla:**
1. **Declarar las bases indexadas** consultadas: IEEE Xplore, ACM Digital Library, Scopus, SpringerLink, ScienceDirect.
2. **Escribir la cadena booleana** y ejecutarla realmente en cada base, anotando la fecha de ejecución y el número de resultados:
```
("commission platform" OR "freelance marketplace" OR "creative commission")
AND ("web application" OR "web platform")
AND ("digital art" OR "creative content" OR "digital content")
```
3. **Ventana temporal:** 2015–2026, justificada.
4. **Criterios de inclusión y exclusión** explícitos (idioma, tipo de publicación, revisión por pares, disponibilidad de texto completo, relevancia temática).
5. **Diagrama de flujo PRISMA 2020 con números reales**: identificados → duplicados eliminados → cribados por título/resumen → texto completo evaluado → **incluidos**. Cada caja con su cifra y las exclusiones con su motivo.
6. **Tabla comparativa de al menos ocho filas**, y **cada trabajo citado por su clave bibliográfica** (`\cite{...}`), con columnas de dimensiones comparables: dominio, arquitectura, autenticación, modelo de pagos, evaluación empírica, disponibilidad del código.
7. **Conservar el párrafo de brecha**, que ya está bien argumentado y el docente lo reconoce — reescribirlo para que ahora se apoye en la tabla.
8. **Retirar la declaración de renuncia** («Este capítulo no presenta una revisión sistemática…»): al hacerse el capítulo deja de aplicar. Sustituirla por la descripción del protocolo seguido.
9. Añadir las nuevas referencias a `referencias.bib`, **verificando cada DOI** (ver T-19). Esto ayuda además a cerrar OBS-D6-03 (alto impacto 16/32, mínimo 20).
10. **Actualizar el checklist PRISMA** de `docs/checklists/`, que hoy se resuelve como «no aplica» — deja de ser válido (parte de T-22).

**Criterio de aceptación (guía §3.7):** el capítulo declara **bases consultadas, cadena de búsqueda, ventana temporal, criterios y diagrama de flujo con cifras**, y la tabla comparativa **cita cada trabajo por su clave bibliográfica**.

> El docente valoró expresamente la honestidad de no fabricarlo: *«Prefiero eso, con diferencia, a un capítulo inventado.»* **Eso significa que una tabla inventada ahora sería peor que la situación actual.** Hacer las búsquedas de verdad y anotar los números reales, aunque sean pocos.

### T-19 · Corregir las dos referencias bibliográficas defectuosas
**Cubre:** OBS-D6-01, OBS-D6-02, punto 4 · **Responsable:** BF · **Duración:** 2 h

Cifra rectificada por la guía: **18 de 20** DOI devuelven el título declarado, no 19. Hay **dos** entradas defectuosas.

**Pasos:**
1. `PERES2024`: su DOI resuelve a un artículo sobre estrategias de marketing en crowdfinanciación. **El DOI correcto existe y lleva al volumen y páginas que el equipo cita** — localizarlo en Crossref y sustituirlo.
2. Identificar la segunda entrada: la que **resuelve con el venue correcto pero un título distinto**.
3. Verificar los **veinte** por negociación de contenido:
```bash
for doi in $(grep -oP 'doi\s*=\s*\{\K[^}]+' docs/informe-final/referencias.bib); do
  echo -n "$doi -> "
  curl -sLH "Accept: application/vnd.citationstyles.csl+json" "https://doi.org/$doi" \
    | python -c "import sys,json; d=json.load(sys.stdin); print(d.get('title'), '|', d.get('container-title'))"
done
```
4. Guardar la salida en `docs/informe-final/verificacion-doi.txt` como evidencia versionada.

**Criterio de aceptación (guía §3.8):** *«Los veinte identificadores resuelven a la obra que el archivo bibliográfico declara.»*

> La guía §6.4 lista *«referencias bibliográficas cuyo identificador no resuelve o resuelve a otra obra»* entre las señales que **levantan sospecha de autoría**. Esto no es solo un punto de D6: toca la credibilidad general del trabajo.

### T-20 · Referenciar las 26 etiquetas huérfanas y los cuatro listados
**Cubre:** OBS-D1-04, OBS-D1-08, punto 12 · **Responsable:** BF · **Duración:** 3 h

**Pasos:**
1. Listar las etiquetas y sus referencias:
```bash
grep -rhoP '\\label\{\K[^}]+' docs/informe-final/ | sort > /tmp/labels.txt
grep -rhoP '\\(ref|autoref|cref)\{\K[^}]+' docs/informe-final/ | sort -u > /tmp/refs.txt
comm -23 /tmp/labels.txt /tmp/refs.txt   # las huérfanas
```
2. Para cada huérfana: **o se referencia desde el texto** («como muestra la figura~\ref{fig:arquitectura}…»), **o se elimina la etiqueta** si el elemento no merece ser referenciado. Ambas salidas son válidas; lo que no vale es dejarla colgando.
3. **Los cuatro listados deben referenciarse con `\ref`** desde el cuerpo del texto.

**Criterio de aceptación (guía §3.8):** *«ninguna etiqueta queda sin referencia en el texto»* → el `comm -23` devuelve vacío.

### T-21 · Ajustar los resúmenes a 200–250 palabras
**Cubre:** OBS-D1-02, punto 11 · **Responsable:** BF · **Duración:** 1,5 h

Hoy: **390 palabras el español y 335 el inglés**. Hay que recortar 140 y 85 respectivamente.

**Archivo:** `docs/informe-final/secciones/00-portada-resumen.tex`

**Comando de verificación:**
```bash
# extraer el entorno abstract y contar
pdftotext docs/informe-final/Informe-Final-v1.0.0.pdf - | sed -n '/^Resumen/,/^Palabras clave/p' | wc -w
```

**Criterio de aceptación:** ambos resúmenes entre **200 y 250 palabras**.

> Al recortar, **conservar las cifras clave** (72,02 % de cobertura, p95, la puntuación SUS ya corregida): un resumen sin números pierde más de lo que gana. Recortar los preámbulos contextuales, que es donde suele estar la grasa.

### T-22 · Actualizar los checklists de estándares de reporte
**Cubre:** OBS-R4-01 · **Responsable:** BF · **Duración:** 2 h

El checklist de Ralph está fechado el **17 de agosto** y afirma en tres ítems que el documento académico y el capítulo de amenazas **«no existen»**, cuando existen desde hace dos semanas.

**Pasos:**
1. Refechar y rehacer los tres ítems falsos del checklist de Ralph.
2. **Rehacer completo el checklist PRISMA**: hoy se resuelve como «no aplica» con justificación coherente con el capítulo 3 vacío. Al hacerse T-18, esa justificación deja de ser válida y **debe completarse ítem por ítem**.
3. Revisar FAIR e INCOSE por si alguna afirmación quedó desactualizada por los cambios de esta semana.
4. **Mantener el estilo que el docente elogió**: párrafo de evidencia concreta por ítem, y **contraejemplo real citado** en los que no se marcan. No convertirlos en plantillas.

**Criterio de aceptación:** ningún ítem de ningún checklist afirma algo que el repositorio desmienta, y todos llevan fecha posterior al 7 de septiembre.

### T-23 · Escribir el Javadoc de servicios y controladores
**Cubre:** OBS-TR-02, punto 16b · **Responsable:** JK (asignación individual) + JC · **Duración:** 8 h

Hoy: de **543 métodos públicos solo 28 tienen Javadoc**, y en todo el backend hay **8 `@param`, 0 `@return` y 1 `@throws`**.

**Alcance realista:** el criterio de aceptación de la guía §3.10 acota a *«los métodos públicos de servicios y controladores»*, no a los 543. Eso es alcanzable en una semana.

```java
/**
 * Crea una comisión entre un artista y un cliente, validando que el artista
 * acepte encargos y que el cliente no supere su límite de comisiones abiertas.
 *
 * @param solicitud datos de la comisión; no puede ser {@code null}
 * @param clienteId identificador del cliente que solicita la comisión
 * @return la comisión creada, con su identificador y estado {@code PENDIENTE}
 * @throws ArtistaNoDisponibleException si el artista no acepta encargos
 * @throws LimiteComisionesException si el cliente supera el máximo permitido
 */
public ComisionDTO crearComision(CrearComisionRequest solicitud, Long clienteId) { … }
```

**Comando de verificación:**
```bash
grep -rc "@param" artisync/Backend/src/main/java --include=*.java | awk -F: '{s+=$2} END {print "param:", s}'
grep -rc "@return" artisync/Backend/src/main/java --include=*.java | awk -F: '{s+=$2} END {print "return:", s}'
```

**Criterio de aceptación (guía §3.10):** *«Los métodos públicos de servicios y controladores documentan parámetros, retorno y excepciones.»*

> Es una de las **tres exigencias fijadas y reincidentes**. Que siga incumplida tras haberse señalado ya una vez pesa más que su tamaño.

### T-24 · Traducir figuras e identificadores
**Cubre:** OBS-TR-01, OBS-TR-03, punto 16a y 16c · **Responsable:** JC · **Duración:** 4 h

Hoy: **80,7 % de los tipos** y **66,2 % de los métodos** llevan token español; **tres de las cuatro figuras están en español**.

**Priorización pragmática:** las **figuras** son baratas (4 archivos) y cierran un tercio de la exigencia. La **renombrada masiva de identificadores es cara y arriesgada** a cuatro días del examen — puede romper la compilación, las pruebas y las 26 rutinas recién conectadas.

**Recomendación:** hacer las figuras (T-24a) y, en identificadores, hacer un **subconjunto acotado y verificable** (T-24b), documentando en un ADR el criterio y el porcentaje alcanzado. Un avance medido y declarado vale más que una renombrada a medias que rompe el build.

- **T-24a** — Regenerar las tres figuras en español con rótulos en inglés. Archivos en `docs/diagramas/`.
- **T-24b** — Renombrar identificadores capa por capa, ejecutando `make test` tras cada capa. Empezar por DTOs y controladores, que son los de menor acoplamiento.

**Criterio de aceptación:** las cuatro figuras en inglés; el porcentaje de tokens españoles declarado con su cifra exacta en el documento, medido con un script versionado.

---

## §6. Transversales — a lo largo de toda la semana

### T-25 · Resolver el SUS en el documento
**Cubre:** OBS-D4-01, OBS-R2-02, punto 1 · **Responsable:** BF · **Duración:** 4 h · **Depende de T-04**

Una vez tomada la decisión en T-04, ejecutarla:

**Si el camino es (b) — publicar 61,25:**
1. Sustituir `docs/mediciones/sus/sus-raw.csv` por el export real, fila a fila.
2. **Eliminar el duplicado exacto de P12 sobre P11** (señal n.º 3 de la guía §5.5).
3. Re-ejecutar `make sus` → nuevos descriptivos: media **61,25**, IC **[49,49 ; 73,01]**.
4. Regenerar `boxplot-sus.png` y `REPORTE-SUS.md`.
5. **Reescribir la discusión en el capítulo 8**: el sistema **no alcanza el umbral de aceptabilidad de 68**, con las causas plausibles y las implicaciones para trabajo futuro. **Eso es un hallazgo legítimo y se puntúa como tal.**
6. Verificar que el IC se calcula **con la t de Student y los grados de libertad correctos, no con 1,96** (guía §5.4).
7. Añadir a `docs/mediciones/sus/` una nota de rectificación fechada y firmada.
8. **Corregir `DATA-PROVENANCE`**: retirar o matizar la afirmación de que ningún archivo crudo fue editado a mano (OBS-R2-02).

**Criterio de aceptación (guía §3.1):** *«El archivo que se analiza coincide fila a fila con el export del instrumento, el recálculo reproduce la cifra publicada, y existen aprobación y consentimientos según el capítulo 5.»*

### T-26 · Aprobación ética y consentimientos del estudio SUS
**Cubre:** OBS-TR-05, guía §5.2 · **Responsable:** BF · **Duración:** variable — **empezar el lunes**

La guía §5.2 exige **cinco cosas** y el criterio de aceptación de T-25 no se cumple sin ellas:

1. **Aprobación previa** con fecha **anterior** a la recogida, número de expediente y título del estudio. *«Emitido antes, no después.»*
2. **Consentimiento informado individual firmado por participante** — *«un consentimiento colectivo, una constancia de regularización o un permiso genérico de la institución NO sustituyen al consentimiento individual»*.
3. **Registro de sesiones** con fecha, hora, duración y modalidad, **sin nombres en el archivo público** (ya existe `registro-sesiones.csv` — verificar que está anonimizado).
4. **Anonimización** que impida reidentificar — revisar `perfil-participantes.csv`: con n=16, la combinación edad + sexo + rol + dispositivo puede identificar a alguien.
5. **Declaración en el documento** de qué comité aprobó el estudio, con qué número, y que se obtuvo consentimiento de todos.

**Acción honesta si no existe la aprobación previa:** no se puede fabricar con fecha retroactiva. Declararlo como **amenaza a la validez** en el capítulo correspondiente y describir el procedimiento de consentimiento que sí se siguió. El capítulo D5 es hoy un 100 %; una amenaza declarada con precisión lo mantiene. Una aprobación retrodatada sería exactamente la falta que la guía §5.4 llama irreparable.

**Verificación previa obligatoria — las seis señales de la guía §5.5:**
```bash
python - <<'EOF'
import pandas as pd
d = pd.read_csv('docs/mediciones/sus/sus-raw.csv')
print("filas:", len(d))                       # >= 15
print("duplicados exactos:", d.duplicated().sum())   # debe ser 0
print("items con varianza 0:", (d.var(numeric_only=True) == 0).sum())  # debe ser 0
EOF
```

### T-27 · Completar `DATA-DICTIONARY` con las variables crudas
**Cubre:** OBS-R2-01, punto 14 · **Responsable:** BF · **Duración:** 3 h

Hoy documenta **21 variables agregadas** y deja fuera **~33 crudas**, incluidas **Q1–Q10 del SUS** y **las 13 columnas de `jacoco.csv`**.

**Pasos:** añadir a `docs/mediciones/DATA-DICTIONARY.md`, por cada variable cruda: nombre, tipo, unidad, rango válido, archivo de origen y significado. Las 10 del SUS con el enunciado literal del ítem y su polaridad.

**Criterio de aceptación:** toda columna de todo CSV crudo del repositorio aparece en el diccionario.

### T-28 · Verificar y completar `DATA-PROVENANCE`
**Cubre:** OBS-R2-02, guía §4.6 · **Responsable:** BF · **Duración:** 2 h

**Pasos:**
1. Corregir la afirmación de inmutabilidad (ya en T-25).
2. Añadir **una fila por cada figura y cada tabla nueva** del documento: elemento ↔ archivo crudo ↔ script ↔ **hash del commit**.
3. **Verificar cada hash antes de comitear** (obligación explícita de la guía §4.6):
```bash
grep -oP '\b[0-9a-f]{7,40}\b' docs/mediciones/DATA-PROVENANCE.md | while read h; do
  git cat-file -t "$h" >/dev/null 2>&1 && echo "OK  $h" || echo "FALTA $h"
done
```

**Criterio de aceptación:** todos los hashes citados existen (`git cat-file -t` devuelve `commit`), y no hay figura ni tabla del documento sin fila de procedencia.

### T-29 · Unificar las tres cifras de observaciones
**Cubre:** OBS-P0-02, punto 13 · **Responsable:** BF · **Duración:** 1 h

Hoy conviven tres cifras del mismo dato: **29/26 con 89,7 %**, **86,2 %** en una nota interna, y **27/23 con 85,2 %** en el Anexo A del PDF.

**Pasos:** fijar la cifra verdadera contando en `OBSERVACIONES.md`, y propagarla a **los tres sitios** más al README. Al recompilar el PDF (T-01), el Anexo A queda alineado.

**Criterio de aceptación:** `grep -rn "89,7\|86,2\|85,2" docs/` devuelve una sola cifra coherente.

### T-30 · Cerrar las tres observaciones acumuladas pendientes
**Cubre:** OBS-P0-01 · **Responsable:** EQ · **Duración:** variable

Hoy 26 resueltas de 29 (una parcial, dos pendientes). Cerrarlas lleva P0 de 75 % a 100 %.

### T-31 · Deshacer el empate de etiquetas
**Cubre:** OBS-P0-03 · **Responsable:** JC · **Duración:** 15 min

`v0.7.1` y `v0.9.0-rc` apuntan al mismo commit. Reasignar `v0.9.0-rc` al commit que efectivamente corresponde a esa fase, o documentar en `CHANGELOG.md` por qué coinciden.

### T-32 · Corregir el nombre de la clase de prueba en la matriz
**Cubre:** OBS-D0R-01 · **Responsable:** BF · **Duración:** 10 min

`SeguidorServiceImplTest` → `SeguidorServicioImplTest` en la matriz de trazabilidad. Y verificar las **34** referenciadas (cifra rectificada):
```bash
bash scripts/validate-traceability.sh
```

### T-33 · Aplicar INVEST a las 23 historias
**Cubre:** OBS-D0R-03 · **Responsable:** BF · **Duración:** 3 h

Hoy INVEST aparece en **1 de 23**. Añadir la evaluación INVEST (Independent, Negotiable, Valuable, Estimable, Small, Testable) a las 22 restantes en `docs/requisitos/`.

### T-34 · Ampliar los diagramas de secuencia
**Cubre:** OBS-D0R-04 · **Responsable:** JC · **Duración:** 4 h

Hoy solo **un** caso de uso se traza a diagrama de secuencia. Añadir al menos cinco más, priorizando los casos de uso de los requisitos Must.

### T-35 · Extraer el DSL de Structurizr a archivos `.dsl`
**Cubre:** OBS-D1-03 · **Responsable:** JC · **Duración:** 1 h

Hoy el DSL está embebido en Markdown. Extraerlo a `docs/diagramas/workspace.dsl` y dejar el Markdown referenciándolo.

---

## §7. Reproducibilidad — el criterio que se comprueba clonando

> *«En el examen final voy a clonar su repositorio en una máquina limpia y ejecutar lo que su documentación diga que hay que ejecutar. Lo que no funcione desde ese clon, no cuenta.»*

### T-36 · Hacer que `make all` funcione desde un clon limpio — ✅ HECHA (2026-09-03)
**Cubre:** OBS-R1-01 · **Responsable:** BF · **Duración:** 2 h

> **Resultado:** implementado y verificado con `make` real sobre un clon limpio, con Docker Desktop activo. `up:` genera `.env` desde `.env.example` y sustituye `JWT_SECRET` por uno real (`openssl rand -hex 32` — el placeholder original tenía 31 bytes, no 32, y hacía que el backend rechazara arrancar; corregido también en `.env.example`). Los cuatro contenedores terminan sanos y el frontend responde 200. Detalle completo en la tercera pasada de [`BITACORA-CLON-LIMPIO.md`](BITACORA-CLON-LIMPIO.md).

Hoy exige un `.env` no versionado → **falla sin un paso manual**.

**Pasos:**
1. Añadir al principio del objetivo `up:` una copia automática si falta:
```make
up:
	@test -f artisync/.env || cp artisync/.env.example artisync/.env
	$(COMPOSE) up -d --build
```
2. Garantizar que `artisync/.env.example` tiene **valores por defecto funcionales para desarrollo local** (nunca los de producción, ya rotados en T-03) — de modo que un clon limpio arranque sin intervención.
3. Documentarlo en el `README.md`.

**Criterio de aceptación:** T-49 (`make all` desde clon vacío) termina con código 0 **sin ningún paso manual**.

### T-36b · Contenedorizar `make docs` — ✅ HECHA (2026-09-03)
**Cubre:** hallazgo nuevo (bitácora T-00, 2026-09-03) · **Responsable:** JC · **Duración:** 2 h

> **Resultado:** implementado y verificado. Además del fallback Docker (`texlive/texlive:latest`), aparecieron y se corrigieron dos problemas que el diseño original de esta tarea no anticipaba: (1) `docker run -w /data` se rompe en Windows/Git Bash por traducción automática de rutas (MSYS) — se antepuso `MSYS_NO_PATHCONV=1` a las seis invocaciones `docker run` del Makefile; (2) el mount solo incluía `docs/informe-final`, pero `main.tex` referencia imágenes vía `\graphicspath` en `../diagramas/` y `../mediciones/`, fuera de esa carpeta — se corrigió montando el repositorio completo. Con ambos arreglos, `make docs` produce **76 páginas, cero `[?]`**, igual que la compilación local de referencia. Detalle en la tercera pasada de [`BITACORA-CLON-LIMPIO.md`](BITACORA-CLON-LIMPIO.md).

La prueba de clon limpio (`docs/observaciones/BITACORA-CLON-LIMPIO.md`) confirmó que el objetivo `docs:` — el que compila **precisamente el PDF que el docente marcó como PISO-02**— exige `pdflatex` y `bibtex` **instalados localmente**, sin ningún fallback en contenedor. Esto contradice el propio encabezado del `Makefile`, que promete reproducción «desde una clonación limpia» con **Docker + Docker Compose** como único requisito previo declarado. En una máquina que solo tiene Docker (el escenario que el Makefile dice soportar), `make all` se detiene aquí sin remedio — y sin ese fallback, T-01 (recompilar el PDF) tampoco es reproducible por un tercero que solo tenga Docker.

**Pasos:**
1. Añadir un fallback en contenedor al objetivo `docs:`, análogo al de `srs:` (que ya usa `pandoc/latex:3.1`) o a `lighthouse:` (que usa `docker run` con una imagen efímera). Opción más directa: una imagen con TeX Live completo, p. ej. `texlive/texlive:latest`, que trae `pdflatex`, `bibtex` y `makeglossaries`:
```make
docs:
	@if [ ! -f docs/informe-final/main.tex ]; then \
		echo "ERROR: docs/informe-final/main.tex no existe todavia."; exit 1; \
	fi
	@if command -v pdflatex >/dev/null 2>&1 && command -v bibtex >/dev/null 2>&1; then \
		cd docs/informe-final && \
			pdflatex -interaction=nonstopmode main.tex && bibtex main && \
			makeglossaries main && \
			pdflatex -interaction=nonstopmode main.tex && \
			pdflatex -interaction=nonstopmode main.tex; \
	else \
		command -v docker >/dev/null 2>&1 || { echo "ERROR: se necesita Docker o una instalacion local de TeX Live."; exit 1; }; \
		docker run --rm -v "$(CURDIR)/docs/informe-final:/data" -w /data texlive/texlive:latest \
			bash -c "pdflatex -interaction=nonstopmode main.tex && bibtex main && makeglossaries main && pdflatex -interaction=nonstopmode main.tex && pdflatex -interaction=nonstopmode main.tex"; \
	fi
	cp docs/informe-final/main.pdf docs/informe-final/Informe-Final-v1.0.0.pdf
	@echo "OK: docs/informe-final/Informe-Final-v1.0.0.pdf generado."
```
2. Probar ambas rutas: con TeX local instalado y sin él (solo Docker).
3. Actualizar el README para declarar que **Docker basta** también para este objetivo, quitando la exigencia de TeX Live/MiKTeX local como único camino.

**Criterio de aceptación:** `make docs` termina en 0 en una máquina que **solo tiene Docker instalado**, sin TeX Live ni MiKTeX locales — la misma condición bajo la que ya funcionan `srs:` y `lighthouse:`.

### T-37 · `python3` en el objetivo `sus` — ✅ HECHA (2026-09-03)
**Cubre:** OBS-R1-02 · **Responsable:** BF · **Duración:** 5 min

Aplicado y verificado con `make sus` real, código 0.

### T-38 · Encadenar `audit-sql-dynamic` en `make all` — ✅ HECHA (2026-09-03)
**Cubre:** OBS-R1-03 · **Responsable:** BF · **Duración:** 20 min

> Implementado como dependencia de Make (`audit: audit-sql-dynamic`) en vez de encadenarlo por separado en `all`, para que `make audit` sola también lo corra y `make all` no lo ejecute dos veces. Verificado con `make audit-sql-dynamic`: código 0, "AUDITORIA SUPERADA... Rutinas auditadas: 29".

### T-39 · Automatizar el renombrado de los informes de Lighthouse — ✅ HECHA (2026-09-03)
**Cubre:** OBS-R1-04 · **Responsable:** JC · **Duración:** 30 min

> **Resultado:** la primera versión de este arreglo tenía un bug propio — recorría `.json` y `.html` en un solo bucle con un contador compartido, así que el par de una misma corrida quedaba como `run1.json`/`run2.html` en vez de compartir número. Corregido: el bucle itera solo sobre los `.json` y mueve el `.html` hermano (mismo nombre base) con el mismo número de corrida. Verificado con archivos simulados: tres corridas quedan como `run1`/`run2`/`run3`, cada una con su par correcto. El pipeline completo de `lighthouse` (instala Chromium, tarda varios minutos) no se corrió de punta a punta en esta pasada — pendiente para T-16/T-49.

### T-40 · Añadir el cuaderno Jupyter de reproducción
**Cubre:** OBS-R1-05 · **Responsable:** BF · **Duración:** 3 h

Crear `docs/mediciones/reproduccion.ipynb` que, ejecutado de arriba abajo, **recalcule desde los archivos crudos versionados** todas las cifras del documento: cobertura desde `jacoco.csv`, percentiles y test inferencial desde los NDJSON, SUS desde el CSV, medias de Lighthouse desde los JSON. Es la materialización directa de la regla de la guía §4.1.

### T-41 · Publicar el digest `sha256` en los tres sitios
**Cubre:** OBS-R1-06 · **Responsable:** JC · **Duración:** 30 min

```bash
sha256sum docs/informe-final/Informe-Final-v1.0.0.pdf
```
Publicarlo en **README.md**, en la **portada del informe** y en el **release de GitHub** (los tres sitios exigidos).

---

## §8. Evidencia de autoría — para la defensa (guía cap. 6)

### T-42 · Escribir la declaración de uso de asistencia
**Cubre:** OBS-TR-04, guía §6.2.6 · **Responsable:** EQ · **Duración:** 2 h

*«En esta asignatura la voy a exigir. Redáctenla en positivo: describe cómo trabajaron, no es una confesión.»*

Ya existe `docs/etica/ai-disclosure.md` (con cambios sin comitear en el árbol actual). Completarlo y **añadirlo como sección del documento LaTeX**, no solo como archivo suelto: **qué herramientas, en qué partes concretas, con qué grado de supervisión, y qué verificó cada integrante**.

### T-43 · Producir la revisión entre integrantes
**Cubre:** OBS-TR-06, guía §6.2.2 · **Responsable:** EQ · **Duración:** continua

*«Una revisión con observaciones sustantivas es de las evidencias más difíciles de simular y de las más valoradas.»*

**Forma de conseguirla sin esfuerzo extra:** que **todo el trabajo de esta semana entre por pull request**, y que cada PR lo revise **otro integrante** con observaciones sustantivas, respuesta y cambios derivados de la conversación. Ríos revisa el despliegue de Carvajal; Carvajal revisa el capítulo 3 de Figueroa; Figueroa revisa las conversiones a `@Procedure` de Ríos. Eso produce la evidencia **mientras se trabaja**, que es exactamente lo que la guía pide.

### T-44 · Completar los ADR con las alternativas descartadas
**Cubre:** OBS-TR-07, guía §6.2.3 · **Responsable:** JC · **Duración:** 3 h

*«Un registro que solo describe la decisión final no acredita nada; uno que muestra el camino, sí.»*

Revisar `docs/adr/` y añadir a cada ADR: **alternativas consideradas, por qué se descartó cada una**, y cuando aplique **la fecha del cambio de opinión y el commit que lo materializó**. Añadir un ADR nuevo para la decisión de T-03 (rotar en origen en vez de reescribir el historial).

### T-45 · Subir los artefactos previos al código
**Cubre:** guía §6.2.4 · **Responsable:** EQ · **Duración:** 1 h

Bocetos de interfaz, esquemas de datos a mano, notas de reunión fechadas, fotos de pizarra. **Con su fecha.** *«Son la parte del trabajo que ninguna herramienta produce por ustedes.»* Si existen en teléfonos o cuadernos, subirlos a `docs/proceso/` esta semana.

### T-46 · Preparación individual para la defensa
**Cubre:** guía §6.2.7 · **Responsable:** EQ · **Duración:** 4 h — **el fin de semana previo**

Cada integrante debe poder, **sin buscador**:
- Explicar por qué una decisión concreta del código es como es y qué alternativa se descartó.
- Localizar dónde vive una funcionalidad determinada.
- **Modificar algo en vivo:** añadir una validación, cambiar una consulta, hacer pasar una prueba que falla.
- Justificar una cifra del documento **y decir de qué archivo sale**.
- **Responder por partes del sistema que no escribió.**

**Sesiones cruzadas obligatorias** (el docente las asigna nominalmente a Ríos):
| Sesión | Quien explica | Quien recibe | Contenido |
|---|---|---|---|
| 1 | BF | JK, JC | Capítulo de mediciones: de dónde sale cada cifra y con qué comando se recalcula |
| 2 | JC | JK, BF | Despliegue: Render, `render.yaml`, nginx, cabeceras |
| 3 | JK | BF, JC | Backend: procedimientos, repositorios, capa de servicio |

---

## §9. Cierre — 11 de septiembre

### T-47 · Regenerar toda la evidencia sobre el commit que se defiende
**Responsable:** BF · **Duración:** 2 h

La guía §4.2 lo exige explícitamente: *«La cifra que publiquen debe ser la de ese archivo, no la de una corrida anterior.»* Tras todos los cambios de la semana, **todas** las métricas cambiaron.

```bash
make clean && make all
```
Y actualizar en el documento y el README: cobertura, percentiles, test inferencial, SUS, Lighthouse, conteos de ZAP y SpotBugs. **Cada cifra publicada debe salir del archivo crudo de esta última corrida.**

### T-48 · Alinear versión, etiqueta, hash y DOI
**Responsable:** JC · **Duración:** 1 h

1. Crear la etiqueta final: `git tag -a v1.1.0 -m "Entrega para examen final"`.
2. Actualizar el hash corto en **la portada y la carátula** (hoy `d07656b`, que ya no es el commit que se defiende).
3. Verificar que el `Makefile`, el `CHANGELOG.md`, el `CITATION.cff` y el nombre del PDF declaran la **misma versión**.
4. Publicar la nueva versión en Zenodo si corresponde, manteniendo la separación software / dataset y las licencias (MIT / CC BY 4.0), que son una fortaleza reconocida.

### T-49 · Prueba final del clon limpio
**Responsable:** EQ · **Duración:** 1 h · **Bloqueante para la entrega**

```bash
git clone <url> /tmp/artisync-final && cd /tmp/artisync-final && make all; echo "EXIT: $?"
```

**Criterio de aceptación:** `EXIT: 0` **sin ningún paso manual**. Si falla, se corrige y se repite. Este es el comando exacto que el docente ejecutará.

### T-50 · Verificación final contra los diez criterios de aceptación de la guía
**Responsable:** EQ · **Duración:** 1 h

Recorrer la tabla de §11.4 del documento de observaciones y marcar los diez uno por uno, con la evidencia de cada verificación. Actualizar el checklist de §13 de `observaciones_para_el_examen.md`.

---

## §10. Cronograma consolidado

| Día | BF (Figueroa) | JC (Carvajal) | JK (Ríos) |
|---|---|---|---|
| **Mié 3** | T-00 clon limpio · T-04 SUS instrumento · T-06 iniciar firma SRS · T-26 iniciar ética | T-00b rama · T-01 **recompilar PDF** · T-02 carátula | T-07 identidad · T-03 rotar credenciales |
| **Jue 4** | T-04 cerrar decisión SUS · T-25 aplicar | T-05 composición equipo · T-08 **desplegar** | T-11 procedimientos (inicio) |
| **Vie 5** | T-18 cap. 3 (búsquedas y PRISMA) | T-08 desplegar · T-09 cabeceras · T-10 cookie | T-11 procedimientos · T-12 catálogo |
| **Sáb 6 – Dom 7** | T-18 cap. 3 (tabla y redacción) | T-16 Lighthouse público · T-17 OWASP/ZAP | T-13 cobertura controladores |
| **Lun 8** | T-14 k6 protegido · T-15 inferencial | T-17 OWASP/ZAP · T-44 ADR | T-13 cobertura · T-23 Javadoc |
| **Mar 9** | T-19 DOI · T-20 etiquetas · T-21 resúmenes | T-24 figuras · T-35 DSL · T-34 secuencia | T-23 Javadoc · T-32 matriz |
| **Mié 10** | T-22 checklists · T-27 diccionario · T-28 provenance · T-40 notebook | T-31 etiquetas · T-39 lighthouse auto · T-41 sha256 | T-33 INVEST · T-30 pendientes |
| **Jue 11** | T-47 regenerar evidencia | T-48 versión/tag/DOI | T-46 defensa |
| **Todos** | T-49 clon final · T-50 verificación · T-42 declaración IA · T-43 PR cruzados · T-45 artefactos previos · T-46 sesiones cruzadas | | |

**Todo el trabajo entra por pull request revisado por otro integrante** (T-43) — esa es la vía más barata de producir la evidencia de autoría del capítulo 6.

---

## §11. Las cinco cosas que no hay que romper

Las fortalezas reconocidas son puntos ya ganados. Antes de cada commit, verificar que siguen intactas:

1. **Los cuatro ORCID coherentes en los cuatro archivos** — T-05 los toca. `grep -h orcid CITATION.cff CONTRIBUTORS.md .zenodo.json`
2. **Los dos DOI de Zenodo separados con licencias correctas** — T-48 los toca.
3. **La higiene de SQL: cero concatenación, Criteria API tipada** — T-11 la toca. `make audit`
4. **Los 29 hashes verificables de `OBSERVACIONES.md`** — razón por la que T-03 **no** reescribe el historial.
5. **Los artefactos de herramienta genuinos** — nunca editar a mano un `jacoco.csv`, un NDJSON de k6, un JSON de Lighthouse ni un XML de SpotBugs. Regenerarlos siempre (T-47).

> Y la regla que gobierna todo lo demás, de la guía §4.1: **«Si un número no se puede recalcular, no es un resultado: es una afirmación.»** Antes de escribir cualquier cifra nueva esta semana, asegurarse de que existe el archivo crudo versionado y el comando documentado que la reproduce.
