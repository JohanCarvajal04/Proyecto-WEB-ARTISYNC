# Bitácora — Prueba de clon limpio (T-00), repetición del 2026-09-03 tarde

**Fecha:** 2026-09-03, 15:00–15:10 (hora local, GMT-5)
**Ejecutor:** Bryan Figueroa (con Claude Code)
**Objetivo de esta repetición:** la bitácora anterior (`BITACORA-CLON-LIMPIO.md`, misma fecha, mañana) documentó una "tercera pasada" donde el `Makefile` ya aparecía corregido y verificado con Docker activo. Esta repetición nace de una pregunta directa: **¿esas correcciones están realmente resueltas para cualquiera que clone el repo hoy, o solo existen en este disco?** La respuesta es: **solo existen en este disco.**

**Commit real en GitHub en el momento de esta prueba:** `09f6221` (rama `main` y `feat/ia-verificacion-asistida`, ambas apuntando al mismo commit en `origin`).
**Estado del working tree local antes de esta prueba:** `Makefile`, `.gitattributes` y `artisync/.env.example` modificados **sin comitear** — exactamente los archivos que la bitácora de la mañana dice haber corregido. `git log origin/feat/ia-verificacion-asistida..HEAD` no devuelve nada: no hay commits locales por delante del remoto, lo que confirma que estos cambios no están ni siquiera comiteados localmente, solo en el árbol de trabajo.

```bash
git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC.git <tmp>/artisync-clean
cd <tmp>/artisync-clean && make all
```

---

## Hallazgo nuevo y más grave que todo lo documentado hasta ahora: el clon limpio **falla en el paso 1**, antes de tocar el Makefile

```
$ git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC.git artisync-clean
...
error: unable to create file docs/mediciones/jacoco/html/uteq.edu.ec.artisync.service.legal/IEntregableServicio$ArchivoDescargado.html: Filename too long
error: unable to create file docs/mediciones/jacoco/html/uteq.edu.ec.artisync.service.pedido.impl/TicketRevisionServicioImpl.java.html: Filename too long
... (18 errores del mismo tipo)
Updating files: 100% (1405/1405), done.
fatal: unable to checkout working tree
warning: Clone succeeded, but checkout failed.
```

**Causa raíz confirmada:** hay **330 archivos HTML de reportes JaCoCo** (`docs/mediciones/jacoco/html/...`) comiteados en el repositorio. Son artefactos generados por el propio `make test`/`mvn jacoco:report` — no código fuente — y **no deberían estar versionados**. Sus rutas, combinadas con nombres de paquete Java largos (`uteq.edu.ec.artisync.service.shared.almacenamiento/...`), superan el límite de 260 caracteres de ruta que Windows impone por defecto (`MAX_PATH`), incluso antes de sumar la ruta del directorio donde se clona.

```bash
$ git config --get core.longpaths
(vacío — no está activado ni local ni globalmente)
$ git ls-files | grep -c "docs/mediciones/jacoco/html/"
330
$ git check-ignore -v docs/mediciones/jacoco/html/
(sin salida — no está en .gitignore)
```

**Verificación de la causa y de la solución temporal:**

```bash
$ git config --global core.longpaths true
$ git clone https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC.git artisync-clean2
Updating files: 100% (1405/1405), done.
[clona sin errores]
```

Activar `core.longpaths` en la configuración de Git resuelve el síntoma, pero **es un ajuste que ningún paso del `README.md` menciona**, no viene activado por defecto en ninguna instalación estándar de Git for Windows, y es exactamente el tipo de "conocimiento propio" que la prueba T-00 pide no aplicar. Un evaluador que siga el README al pie de la letra, en Windows, con Git recién instalado, **se queda aquí, en el primer comando**, con un mensaje de error de Git genérico que no menciona el repositorio ni sugiere `core.longpaths`.

**Esto es más grave que el bloqueo de `.env` (T-36) porque ocurre un paso antes: ni siquiera se llega a tener el código en disco.** No estaba documentado en ninguna de las tres pasadas de la bitácora anterior — probablemente porque esas pasadas reutilizaron un `git config --global core.longpaths` ya activado de una prueba previa en la misma máquina, o clonaron en una ruta lo bastante corta como para no cruzar el límite por poco.

**Recomendación (nueva tarea, propuesta como T-36c):** dejar de versionar `docs/mediciones/jacoco/html/` — es un artefacto de build regenerable con `make test`, no un entregable. Añadir la carpeta a `.gitignore` y quitarla del índice con `git rm -r --cached docs/mediciones/jacoco/html`. Esto resuelve el problema de raíz para cualquier sistema operativo (no solo Windows) y además reduce el peso del repositorio.

---

## Resto del pipeline, probado sobre el commit real de GitHub (`09f6221`, sin las correcciones locales)

Una vez resuelto el clon (con `core.longpaths=true` como parche temporal, no como solución), se instaló GNU Make 4.4.1 vía winget (`ezwinports.make`, ya estaba instalado de la sesión de la mañana pero no en el `PATH` de esta sesión — se invocó por ruta completa) y se ejecutó `make all` sin tocar nada más.

### `make all` — rompe exactamente donde predijo el plan y la bitácora anterior

```
$ make all
docker compose -f artisync/docker-compose.yml --env-file artisync/.env up -d --build
couldn't find env file: ...\artisync-clean\artisync\.env
make: *** [Makefile:41: up] Error 1
```

Confirma OBS-R1-01 con evidencia de primera mano, sobre el estado real y actual de GitHub. **Este bloqueo sigue vigente porque la corrección de T-36 nunca se subió al repositorio remoto** — solo existe como cambio sin comitear en `D:\Proyecto\Proyecto-WEB-ARTISYNC\Makefile`.

### `make test` — ✅ pasa, 749 pruebas, 0 fallos

```
[INFO] Tests run: 749, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Idéntico al resultado de la bitácora anterior. Objetivo reproducible sin Docker, sin intervención manual.

### `make sus` — ✅ pasa en esta máquina (con el mismo matiz de siempre)

```
python docs/mediciones/sus/analisis-sus.py > docs/mediciones/sus/salida-sus.txt
OK: ver docs/mediciones/sus/salida-sus.txt
```

`python` resuelve en esta máquina Windows (alias de la Microsoft Store). **OBS-R1-02 sigue sin corregirse en el Makefile real de GitHub** — en Linux/macOS, `python` a secas rompe con "command not found". La corrección (`python3`) también existe solo sin comitear localmente.

### `make docs` — ❌ rompe, confirmado, sin fallback de Docker

```
$ make docs
ERROR: pdflatex no esta instalado. Ver https://www.tug.org/texlive/ (o MiKTeX en Windows).
make: *** [Makefile:196: docs] Error 1
```

Confirma T-36b tal cual. La corrección (fallback a contenedor `texlive`) también existe solo sin comitear.

### `make sync-procs-check` — ❌ falla por falso positivo de CRLF (no es un defecto del repo)

```
FALLO: R__procedimientos.sql esta desincronizado respecto de db/procs/.
--- (diff solo en fin de línea, contenido idéntico)
```

```bash
$ git config --get core.autocrlf
true
```

Mismo diagnóstico que la bitácora anterior: esta máquina tiene `core.autocrlf=true`, así que el checkout materializa el `.sql` con CRLF mientras el script de verificación regenera con LF puro. Confirmado, no es un hallazgo nuevo del repositorio — la corrección propuesta (`*.sql text eol=lf` en `.gitattributes`) también sigue sin comitear.

### `audit-sql-dynamic.sh` — ✅ el script funciona solo, pero sigue sin estar encadenado en el `Makefile` real

```bash
$ grep -n "audit-sql-dynamic" Makefile
(sin resultados)
$ bash scripts/audit-sql-dynamic.sh
AUDITORIA SUPERADA: sin SQL dinamico ni concatenacion.
  - Rutinas auditadas en db/procs/: 29
```

Confirma OBS-R1-03: el objetivo `audit:` del `Makefile` de GitHub no invoca este script (verificado leyendo el cuerpo completo de `audit:`, líneas 82–102 del `Makefile` real). El script en sí es correcto y pasa cuando se corre a mano.

---

## Verificación adicional: ¿las correcciones locales (sin comitear) funcionan de verdad cuando se aplican?

Para no quedarse solo en "esto está roto", se copiaron los tres archivos corregidos (`Makefile`, `.gitattributes`, `artisync/.env.example`) del working tree local sobre el mismo clon limpio de GitHub, y se repitió `make up` con Docker Desktop activo:

```
$ make up
 Container pfc_postgres Healthy
 Container pfc_redis Healthy
 Container pfc_backend Healthy
 Container pfc_frontend Started
$ curl -s -o /dev/null -w "%{http_code}" http://localhost:4200
200
```

**Sí funciona.** `.env` se generó solo con un `JWT_SECRET` real de 64 caracteres hex, los cuatro contenedores llegaron a `Healthy`/`Started`, y el frontend respondió `200`. Esto confirma que el trabajo de corrección de la sesión de la mañana es técnicamente sólido — el problema no es que las correcciones no funcionen, es que **no están publicadas**.

Stack detenido y limpiado al terminar (`docker compose down -v`) para no dejar contenedores ni volúmenes huérfanos en la máquina de prueba.

---

## Conclusión de esta repetición

**Pregunta del usuario: "¿funciona de verdad y está todo resuelto?" Respuesta: no, todavía no, y por una razón distinta a la que documentaba la bitácora de la mañana.**

1. **Hallazgo nuevo y prioritario (T-36c, propuesto):** el clon limpio en Windows falla en el primer comando (`git clone`) por 330 reportes JaCoCo comiteados que exceden `MAX_PATH`. No documentado antes. Se recomienda `git rm -r --cached docs/mediciones/jacoco/html` + `.gitignore`.
2. **Las correcciones de T-36, T-36b, OBS-R1-02 y `.gitattributes` (CRLF) funcionan y fueron re-verificadas hoy mismo con Docker real** — pero siguen **sin commit y sin push**. Mientras no se suban, cualquier clon real desde GitHub (el profesor, un compañero, un evaluador) reproduce exactamente los mismos bloqueos que documentó la primera bitácora de la mañana: `.env` faltante en `up`, `pdflatex` faltante en `docs`, `audit-sql-dynamic` huérfano.
3. **`test`, `sus` (en Windows) y la parte estática de `audit` siguen siendo reproducibles sin cambios**, tanto en el commit real como con las correcciones aplicadas.

**Siguiente paso recomendado, antes de repetir T-49 el 11 de septiembre:** comitear y hacer push de `Makefile`, `.gitattributes`, `artisync/.env.example` (ya modificados en este disco), y resolver el hallazgo del JaCoCo/`MAX_PATH` (T-36c) — sin eso, `make all` sobre un clon real seguirá rompiendo en el primer paso para cualquier evaluador en Windows.
