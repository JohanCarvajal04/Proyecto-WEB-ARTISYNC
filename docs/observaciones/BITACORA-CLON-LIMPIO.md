# Bitácora — Prueba de clon limpio (T-00)

**Fecha:** 2026-09-03
**Ejecutor:** Bryan Figueroa (con Claude Code)
**Método:** `git clone` a un directorio nuevo y vacío, siguiendo el `README.md` al pie de la letra, sin aplicar conocimiento propio del repositorio.
**Commit clonado:** `HEAD` de `main` en el momento de la prueba (posterior a `09f6221`, incluye los cambios de esta sesión).

```bash
git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC.git /tmp/artisync-clean
cd /tmp/artisync-clean && make all
```

> **Actualización — misma fecha, segunda pasada con `make` real.** La primera pasada de esta prueba se hizo sin `make` instalado en la máquina, así que los objetivos se ejecutaron simulándolos comando por comando. Se instaló GNU Make 4.4.1 (`winget install ezwinports.make`) y se repitió la prueba con **`make` de verdad**, sobre un segundo clon limpio independiente. **Los resultados coinciden exactamente con la simulación** — se dejan ambas secciones abajo, la segunda como confirmación definitiva.
>
> El daemon de Docker **no estaba corriendo** en esta máquina durante la segunda pasada (Docker Desktop apagado), así que `up`, `bench`, `audit` (parte SpotBugs), `audit-zap` y `lighthouse` no se pudieron ejecutar hasta el final en esta pasada — eso es una limitación de esta máquina de prueba, no un hallazgo del repositorio. Sí se pudo confirmar `up` hasta el punto exacto de ruptura (el `.env`), que ocurre **antes** de que se intente conectar con el daemon.

> **Actualización — misma fecha, tercera pasada: se corrigió el `Makefile` y se reverificó todo con Docker Desktop activo.** Con base en los hallazgos de las dos pasadas anteriores se aplicaron seis correcciones al `Makefile` (ver detalle en §"Correcciones aplicadas" al final de este documento) y se reverificó cada una sobre un tercer clon limpio, esta vez con el stack de Docker completo funcionando de punta a punta. **Tres bugs adicionales, no detectados en las pasadas anteriores porque nunca se había llegado a ejecutar el pipeline completo con Docker activo, aparecieron durante esta verificación y también se corrigieron** — están documentados abajo con su causa raíz.

---

## Resultado por objetivo

### 1. `up` — ❌ ROMPE (confirma OBS-R1-01)

```bash
$ docker compose -f artisync/docker-compose.yml --env-file artisync/.env up -d --build
couldn't find env file: C:\...\artisync-clean\artisync\.env
```

**Causa:** `artisync/.env` no está versionado (correcto, no debe estarlo — contiene secretos) pero **tampoco se genera automáticamente** desde `artisync/.env.example`. El objetivo `up:` del `Makefile` invoca `docker compose` directamente, sin el paso `cp .env.example .env` que el plan propone en T-36.

**Esto es exactamente el primer punto de ruptura que predijo el plan.** Un clon limpio se detiene aquí sin ninguna pista de qué hacer más allá del comentario del propio `Makefile` («Requisitos previos: … `cp artisync/.env.example artisync/.env` con las variables editadas»), que está en un comentario, no en un mensaje de error accionable.

**Corrección aplicada para poder seguir probando (no comiteada — es la de T-36):**
```bash
cp artisync/.env.example artisync/.env
```
Con esto el `docker compose up -d --build` sí llega a construir imágenes (no se dejó terminar completo por tiempo — el objetivo de esta prueba era mapear rupturas de *build/config*, no verificar el arranque completo del stack, que ya está cubierto por evidencia previa).

---

### 2. `test` — ✅ NO ROMPE

```bash
cd artisync/Backend && ./mvnw -B -q test
[exited with code 0]
```

El wrapper de Maven se descarga y ejecuta solo, no requiere Maven instalado en el sistema (`mvn` faltaba en esta máquina y no hizo falta). Las pruebas corren sin necesidad de que el stack de `up` esté levantado (usan una base de datos embebida/de prueba, no Postgres real) y **terminan en verde**. Este objetivo es reproducible tal cual está.

---

### 3. `bench` — ⚠️ NO PROBADO A FONDO (depende de `up`)

`k6` **sí está instalado** en esta máquina y `k6/catalogo-load.js` existe. El objetivo depende de que el stack de `up` esté sano y respondiendo en el puerto del backend — no se dejó el stack completo arriba el tiempo suficiente para ejecutar la carga real en esta pasada. Sin cambios respecto a lo ya documentado en el plan (OBS-P2-01: mide un endpoint público, no protegido).

---

### 4. `audit` — ⚠️ NO EJECUTADO COMPLETO (requiere pull de imagen Docker pesada)

El objetivo corre SpotBugs dentro de `maven:3.9-eclipse-temurin-21` vía `docker run`, lo que implica descargar una imagen grande la primera vez. No se ejecutó completo en esta pasada por tiempo. La parte de grep de SQL dinámico (líneas 1–15 del objetivo) es estática y no depende de Docker; se verificó por separado que sigue devolviendo cero coincidencias (higiene de SQL intacta).

**Hallazgo confirmado por lectura directa del Makefile:** `audit-sql-dynamic` (el script `scripts/audit-sql-dynamic.sh`) **no aparece referenciado en ningún objetivo del `Makefile`**, ni en `audit:` ni en `all:`. Confirma OBS-R1-03 tal cual.

```bash
$ grep -n "audit-sql-dynamic" Makefile
(sin resultados)
```

---

### 5. `audit-zap`, `lighthouse` — ⚠️ NO EJECUTADOS (dependen de `up` con nombres de contenedor específicos)

`lighthouse:` depende de que exista un contenedor llamado exactamente `pfc_frontend` (`--network container:pfc_frontend`, hardcodeado en el `Makefile`, líneas 154 y 162). Esto es frágil: si el nombre del proyecto de compose cambia o se corre con `docker-compose.yml` de otra carpeta, el nombre del contenedor cambia y el objetivo falla con un mensaje de Docker genérico («no such container»), no con un error explicado. No se verificó en esta pasada porque no se dejó el stack completo levantado; se deja anotado como riesgo adicional de reproducibilidad no listado hasta ahora.

---

### 6. `sus` — ⚠️ CONFIRMA OBS-R1-02, con matiz

```bash
sus:
	...
	python docs/mediciones/sus/analisis-sus.py > docs/mediciones/sus/salida-sus.txt
```

**En esta máquina Windows concreta, `python` sí resuelve** (a `python.exe` vía el alias de ejecución de la Microsoft Store), así que aquí el objetivo no habría roto. **Pero en Linux/macOS modernos, donde el resto del Makefile evidentemente asume ejecutarse** (usa `bash`, `docker run`, sintaxis POSIX de `$$()`), **el comando `python` a secas no existe por defecto** desde hace varios años — solo `python3`. El propio Makefile lo confirma indirectamente: usa `python3` en ningún otro lado como referencia, y el resto de scripts Python del repo (`analisis-sus.py`, `graficar-sus.py`) no declaran shebang `#!/usr/bin/env python3` sino que dependen de cómo se invoquen.

**Confirmado el hallazgo, con la aclaración de que el fallo es dependiente del sistema operativo del evaluador** — que es justo el motivo por el que la guía exige usar `python3` explícitamente: es el nombre garantizado en cualquier distribución Linux moderna.

---

### 7. `srs` — ✅ NO ROMPE (tiene fallback Docker)

```bash
$ ls docs/requisitos/SRS.md
-rw-r--r-- ... docs/requisitos/SRS.md
```

El objetivo `srs:` **si tiene** fallback a Docker (`pandoc/latex:3.1`) cuando no hay `pandoc` local — a diferencia de `docs:` (ver siguiente punto). No se ejecutó completo (implica pull de imagen y build), pero la lógica del objetivo es sólida y coherente con «Requiere Docker… (ver README)».

---

### 8. `docs` — ❌ ROMPE EN ESTA MÁQUINA, Y ES UN HALLAZGO NUEVO NO DOCUMENTADO ANTES

```bash
$ command -v pdflatex
(no encontrado)
$ command -v bibtex
(no encontrado)
```

**Hallazgo no listado hasta ahora en las observaciones del docente ni en el plan:** el objetivo `docs:` —el que compila el PDF académico final, precisamente el que el docente marcó como piso incumplido (PISO-02)— **exige `pdflatex` y `bibtex` instalados localmente y no tiene ningún fallback en contenedor**, a diferencia de `srs:` y `lighthouse:`, que sí usan Docker para no depender de instalación local.

Esto es inconsistente con la promesa del propio encabezado del `Makefile`:
> *«Reproduccion end-to-end desde una clonacion limpia -- levanta contenedores… corre pruebas, benchmarks y auditorias, y compila el documento academico final. Requisitos previos: Docker + Docker Compose…»*

El encabezado dice que **Docker** es el requisito previo declarado, pero `make docs` **necesita además una distribución TeX local completa** (TeX Live o MiKTeX), que no es un requisito trivial (varios GB) y no está mencionado como prerrequisito junto a Docker en el README ni en el encabezado del Makefile. En una máquina "solo con Docker instalado" —que es literalmente lo que el Makefile dice que basta— **`make all` se detiene aquí sin remedio**, justo en el paso que compila el entregable que más le costó al equipo.

**Recomendación para el plan (nueva tarea, se añade como T-36b):** contenedorizar `docs:` igual que `srs:`, usando `pandoc/latex:3.1` o una imagen con `texlive-full` + `pdflatex`/`bibtex`/`makeglossaries`, para que `make all` sea reproducible con solo Docker, tal como el propio Makefile promete.

---

## Resumen de rupturas encontradas, en el orden en que `make all` las alcanzaría

| # | Objetivo | Rompe? | Observación que confirma | Es nueva? |
|---|---|---|---|---|
| 1 | `up` | **Sí** | OBS-R1-01 (`.env` no versionado) | No, ya prevista |
| 2 | `test` | No | — | — |
| 3 | `bench` | No probado a fondo | OBS-P2-01 (endpoint público, sin cambios) | No |
| 4 | `audit` | Parcial (script huérfano) | OBS-R1-03 (`audit-sql-dynamic` no encadenado) | No, confirmada por lectura directa |
| 5 | `audit-zap` / `lighthouse` | No probado a fondo | — | **Sí — nombre de contenedor `pfc_frontend` hardcodeado, riesgo de fragilidad adicional** |
| 6 | `sus` | Depende del SO del evaluador | OBS-R1-02 (`python` vs `python3`) | No, con matiz de que en esta máquina Windows no reprodujo |
| 7 | `srs` | No | — | — |
| 8 | `docs` | **Sí, en máquina solo-Docker** | — | **Sí — `make docs` exige TeX local, sin fallback Docker, pese a que el Makefile promete que basta con Docker** |

**Conclusión de la primera pasada (simulada):** el primer bloqueo real y determinista, en cualquier sistema operativo, sigue siendo el `.env` (T-36). El segundo bloqueo determinista, específico de una máquina que solo tiene Docker (que es el escenario que el propio Makefile dice soportar), es el objetivo `docs:` sin fallback de contenedor — hallazgo nuevo, añadido aquí y recomendado para T-36b.

---

## Segunda pasada — con `make` real (GNU Make 4.4.1)

Clon independiente, nuevo directorio vacío. Se corrió `make all` sin tocar nada antes, y después cada objetivo por separado con `make <objetivo>` para no perder tiempo si uno fallaba a mitad del encadenado.

### `make all` — rompe exactamente donde predijo la simulación

```
$ make all
docker compose -f artisync/docker-compose.yml --env-file artisync/.env up -d --build
couldn't find env file: C:\...\artisync-clean2\artisync\.env
make: *** [Makefile:41: up] Error 1
```

**Idéntico, carácter por carácter, al mensaje de la simulación.** Confirma OBS-R1-01 con evidencia de primera mano, no inferida.

### `make test` — ✅ confirma limpio, con cifras reales

```
[INFO] Tests run: 749, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  01:40 min
```

**749 pruebas, 0 fallos**, corridas sobre el commit exacto de este clon, sin Docker levantado (usan base de datos embebida de prueba). Confirma que el objetivo `test:` es reproducible tal cual está — nada que corregir aquí.

### `make sus` — ✅ pasa en esta máquina (con el matiz ya documentado)

```
python docs/mediciones/sus/analisis-sus.py > docs/mediciones/sus/salida-sus.txt
OK: ver docs/mediciones/sus/salida-sus.txt
```

Confirma que en esta máquina Windows concreta `python` resuelve (alias del sistema). **El matiz de OBS-R1-02 sigue en pie**: en un evaluador Linux sin ese alias, este mismo objetivo rompe con `python: command not found`, porque el Makefile llama a `python` en vez de `python3`.

### `make docs` — ❌ rompe, confirmado con evidencia directa

```
$ make docs
ERROR: pdflatex no esta instalado. Ver https://www.tug.org/texlive/ (o MiKTeX en Windows).
make: *** [Makefile:196: docs] Error 1
```

Confirma el hallazgo de la primera pasada (ver T-36b): sin fallback de Docker, a diferencia de `srs:`.

### `make sync-procs-check` — ❌ falla, pero es un falso positivo de esta máquina, NO un defecto del repositorio

```
FALLO: R__procedimientos.sql esta desincronizado respecto de db/procs/.
--- .../R__procedimientos.sql	(checkout local)
+++ /tmp/tmp.xxxx	(regenerado por el script)
@@ ... @@
--- ===...
+++ ===...   [contenido idéntico, solo cambia el final de línea]
make: *** [Makefile:57: sync-procs-check] Error 1
```

**Investigado a fondo antes de anotarlo, porque parecía grave y no lo es.** El diff muestra *todas* las líneas como cambiadas pero el contenido visible es idéntico — la sospecha inmediata es fin de línea. Confirmado:

```bash
$ file R__procedimientos.sql
R__procedimientos.sql: ... with CRLF line terminators
$ git show HEAD:.../R__procedimientos.sql | file -
/dev/stdin: Unicode text, UTF-8 text        # sin CRLF — el blob en git está en LF puro
```

**Causa raíz:** el blob en el repositorio está correctamente en LF (`.gitattributes` tiene `* text=auto`, que normaliza al comitear). Pero esta máquina tiene `core.autocrlf=true` en la configuración global de Git, así que **al hacer `checkout` el archivo se materializa con CRLF**. `scripts/sync-procs.sh` regenera el contenido con LF puro (bash/heredoc, sin pasar por el filtro de checkout de Git). El script de `--check` compara los dos y, como difieren solo en el terminador de línea, reporta "desincronizado" cuando en realidad **el contenido es idéntico**.

**Esto no es un hallazgo del repositorio** — es un artefacto de la configuración local de Git de esta máquina de prueba (`core.autocrlf=true`, común en instalaciones por defecto de Git para Windows). En Linux, o en Windows con `core.autocrlf=input`/`false`, el checkout sale en LF y el check pasa limpio. **No se añade como observación nueva del plan.**

**Nota menor para el equipo, sin urgencia:** el `.gitattributes` fuerza `eol=lf` para `*.sh` pero no para el `.sql` generado. Si algún integrante del equipo tiene `core.autocrlf=true` en su máquina (revisar con `git config --get core.autocrlf`) y corre `make sync-procs-check` localmente, verá este mismo falso positivo y puede perder tiempo pensando que rompió la sincronía. Una línea más en `.gitattributes` lo evita: `artisync/Backend/src/main/resources/db/migration/R__procedimientos.sql text eol=lf`. No se considera necesario para el examen; se dejó anotado por si el equipo quiere aplicarlo junto con T-12.

### Higiene de SQL (parte estática de `audit:`) — ✅ confirmada, sin Docker

```
== Auditoria estatica de SQL dinamico (texto, Backend) ==
OK: sin EXECUTE IMMEDIATE/sp_executesql; sin concatenacion en @Query nativeQuery=true.
```

Se ejecutó a mano la parte grep del objetivo (la parte de SpotBugs necesita el daemon de Docker, que estaba apagado en esta pasada). Confirma la fortaleza n.º 3: higiene de SQL intacta.

### Lo que no se pudo probar en esta pasada (Docker apagado, no por el repositorio)

`up` (más allá del punto de ruptura del `.env`), `bench`, la parte SpotBugs de `audit`, `audit-zap` y `lighthouse` requieren el daemon de Docker corriendo. Quedan pendientes de una pasada con Docker Desktop activo — recomendado antes del T-49 final.

## Conclusión final de T-00

Los dos bloqueos deterministas de `make all` en un clon limpio, **confirmados ahora con `make` real**, son:

1. **`up` rompe siempre**, en cualquier sistema operativo, por el `.env` no versionado → **T-36**.
2. **`docs` rompe en una máquina que solo tiene Docker** (sin TeX Live/MiKTeX local) → **T-36b**.

Y un hallazgo adicional, de severidad baja y **no atribuible al repositorio**: `sync-procs-check` puede dar un falso positivo en máquinas Windows con `core.autocrlf=true` — mitigable con una línea de `.gitattributes`, sin urgencia.

Esta bitácora se repite el 11 de septiembre (T-49) sobre el commit final, con Docker Desktop activo, y debe terminar con `make all` en código 0 sin ningún paso manual.

---

## Tercera pasada — `Makefile` corregido, Docker Desktop activo

Clon nuevo e independiente de los dos anteriores. Se aplicaron las correcciones al `Makefile`/`.gitattributes`/`.env.example` y se reverificó objetivo por objetivo.

### Correcciones aplicadas

1. **`up` genera `artisync/.env` automáticamente** si falta, copiándolo de `.env.example` y sustituyendo `JWT_SECRET` por uno real (`openssl rand -hex 32`) — ver punto 4 abajo, el placeholder original no servía.
2. **`docs` cae a un contenedor con TeX Live completo** si no hay `pdflatex`/`bibtex` locales, igual que ya hacían `srs`/`lighthouse`, e incluye `makeglossaries`.
3. **`sus` usa `python3`** en vez de `python`.
4. **`audit-sql-dynamic` se encadena de verdad**: nuevo objetivo `audit-sql-dynamic:`, del que ahora depende `audit:` (así `make audit` sola también lo corre, sin duplicarlo en `make all`).
5. **`lighthouse` renombra sus reportes automáticamente** al final de cada perfil (antes exigía un paso manual documentado en un comentario).
6. **`.gitattributes`: `*.sql text eol=lf`** para evitar falsos "desincronizado" en `sync-procs-check`.

### Hallazgos nuevos, encontrados solo al ejecutar de verdad con Docker activo

Estos tres no aparecieron en las dos pasadas anteriores porque nunca se había llegado a correr el pipeline completo con el daemon de Docker arriba. Los tres se corrigieron también.

**a) El placeholder `JWT_SECRET` de `.env.example` tiene 31 bytes, no 32.** El plan original asumía que `generar_con_openssl_rand_hex_32` tenía 32 caracteres — es un error de conteo, tiene 31 (`echo -n "..." | wc -c`). El backend exige ≥32 bytes para HS256 y con 31 el contenedor `pfc_backend` entra en reinicio infinito (`IllegalStateException: ... se recibieron 31`). Corregido en dos sitios: `up:` ahora genera un secreto real con `openssl rand -hex 32` en vez de dejar el placeholder, y el placeholder de `.env.example` en sí se alargó a 39 caracteres para que también sea válido si alguien lo copia a mano.

**b) `docker run -v ... -w /data` se rompe en Windows/Git Bash por traducción automática de rutas (MSYS).** Al probar `make docs` con la rama Docker, el mount fallaba con `Error response from daemon: the working directory 'E:/Programs/Git/...' is invalid` — MSYS reescribe cualquier argumento que empiece con `/` como si fuera una ruta POSIX a traducir a Windows, incluyendo el `-w /data` destinado al contenedor. Se corrigió anteponiendo `MSYS_NO_PATHCONV=1` a **las seis** invocaciones `docker run` del Makefile (`audit`, `audit-zap`, `lighthouse` ×2, `docs`, `srs`) — es una variable sin efecto en Linux/macOS, así que no cambia nada fuera de Windows.

**c) El mount de `docs:` solo incluía `docs/informe-final`, pero `main.tex` referencia imágenes fuera de esa carpeta.** `\graphicspath{{../diagramas/}{../mediciones/}{../mediciones/sus/}}` en `main.tex` necesita el resto de `docs/`. Con el mount original (`-v "$(CURDIR)/docs/informe-final:/data"`) pdflatex no encontraba `boxplot-sus.png`, `Entidad_Relacion.png`, `c4-nivel2-contenedores.png` ni `secuencia_login_jwt.png`, terminaba con código de salida 1 pese a escribir un PDF de 69 páginas (el número viejo, con las cuatro imágenes en gris de "draft"), y el `&&` de la receta cortaba la cadena antes de `bibtex`. Corregido montando el repositorio completo (`-v "$(CURDIR):/repo" -w /repo/docs/informe-final`), igual que ya hacía `srs:`.

**d) Bug propio en la primera versión del renombrado automático de `lighthouse`.** El primer intento de la corrección 5 recorría `*.report.json` y `*.report.html` en un solo bucle con un contador compartido, así que el `.json` de una corrida quedaba como `run1` y su `.html` correspondiente como `run2` — deja de estar claro que son la misma corrida. Corregido: el bucle itera solo sobre los `.json`, y por cada uno mueve también su `.html` hermano (mismo nombre base) con el mismo número de corrida. Verificado con archivos simulados: tres corridas quedan como `run1`/`run2`/`run3`, cada una con su `.json` y `.html` emparejados.

### Resultados objetivo por objetivo (tercera pasada)

| Objetivo | Resultado |
|---|---|
| `up` | ✅ **Código 0.** `.env` generado solo, secreto JWT real, los cuatro contenedores (`postgres`, `redis`, `backend`, `frontend`) terminan `Healthy`/`Started`. Frontend responde `200` en `localhost:4200`. (Nota aparte, no atribuible al Makefile: un volumen de Postgres de una prueba manual anterior de esta misma sesión, con otras credenciales, causó un primer fallo por "role does not exist" — se limpió con `docker compose down -v` y no volvió a ocurrir; en un clon verdaderamente nuevo, sin ese volumen previo, no aplica.) |
| `audit-sql-dynamic` | ✅ Código 0, "AUDITORIA SUPERADA: sin SQL dinamico ni concatenacion. Rutinas auditadas: 29". |
| `sync-procs-check` | ✅ Código 0, "R__procedimientos.sql sincronizado con db/procs/ (29 rutinas)" — tras forzar LF en los `.sql` fuente además del destino. |
| `sus` | ✅ Código 0 con `python3`. |
| `docs` (rama Docker) | ✅ Código 0. **76 páginas**, cero `[?]`, cero referencias sin resolver — coincide con lo que el docente obtuvo al compilar correctamente. |
| `srs` (rama Docker) | ✅ Código 0, PDF generado. Quedan avisos preexistentes de caracteres `≤`/`≥` faltantes en la fuente (el `sed` de sustitución no los captura todos) — no bloquea la compilación, se deja anotado para quien quiera revisarlo, fuera del alcance de este arreglo. |
| `lighthouse` (lógica de renombrado) | ✅ Verificada en aislado con archivos simulados; el pipeline completo (instala Chromium, 3 corridas × 2 perfiles) no se ejecutó de punta a punta por tiempo — pendiente para T-16/T-49. |

**Conclusión de la tercera pasada:** los dos bloqueos deterministas de la bitácora original (`.env` y `docs` sin fallback Docker) están resueltos y verificados con `make` real. Aparecieron y se corrigieron tres problemas más, invisibles hasta que se probó con Docker realmente activo: el JWT_SECRET del placeholder, la traducción de rutas de MSYS en Windows, y el mount incompleto de `docs:`. El `Makefile` queda pendiente de una corrida completa de `make all` de principio a fin (con `bench`, `audit-zap` y `lighthouse` reales) antes de T-49.
