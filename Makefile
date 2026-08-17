# Makefile — Artisync (PFC Entrega Final v1.0.0)
#
# Objetivos minimos exigidos por el Bloque B.1 de la guia de la Tercera Entrega:
# up, down, test, bench, audit, clean. `make up` debe levantar el sistema
# completo desde una clonacion limpia sin intervencion humana adicional.
#
# `make all` (Bloque D.1 de la guia de la Entrega Final): reproduce end-to-end
# desde una clonacion limpia -- levanta contenedores (que aplican migraciones
# Flyway y semillas SQL automaticamente al arrancar), corre pruebas, benchmarks
# y auditorias, y compila el documento academico final. Termina con codigo 0
# solo si todo el pipeline fue reproducido sin errores.
#
# Requisitos previos: Docker + Docker Compose, y `cp artisync/.env.example artisync/.env`
# con las variables editadas (ver README).

SHELL := /bin/bash
COMPOSE := docker compose -f artisync/docker-compose.yml --env-file artisync/.env

.PHONY: all up down test bench audit audit-zap clean sus lighthouse docs

## Reproduccion end-to-end en un solo comando (Bloque D.1): levanta el stack
## completo (Flyway aplica migraciones y postgres aplica db/seed.sql al
## arrancar, sin paso manual aparte), corre pruebas unitarias, benchmark k6,
## auditoria estatica de SQL dinamico y escaneo ZAP, Lighthouse, el analisis
## SUS sobre los datos ya recolectados, y compila el documento academico
## final a PDF. Requiere Docker, k6 y pandoc instalados (ver README).
all: up test bench audit audit-zap lighthouse sus docs
	@echo "OK: pipeline de reproduccion completo (make all) termino sin errores."

## Levanta postgres, redis, backend y frontend (con build y live reload) en segundo plano.
up:
	$(COMPOSE) up -d --build

## Detiene los servicios sin borrar los volumenes (datos de postgres persisten).
down:
	$(COMPOSE) down

## Ejecuta la suite de pruebas JUnit del backend (no requiere Docker).
test:
	cd artisync/Backend && ./mvnw -B test

## Prueba de carga k6 (50 VUs, 30s) contra el endpoint de catalogo, igual
## configuracion que docs/mediciones/perf/REPORTE-PERF.md. Requiere k6
## instalado (https://k6.io/docs/get-started/installation/) y el sistema
## levantado con `make up`.
bench:
	@command -v k6 >/dev/null 2>&1 || { \
		echo "ERROR: k6 no esta instalado. Ver https://k6.io/docs/get-started/installation/"; \
		exit 1; \
	}
	@if [ ! -f k6/catalogo-load.js ]; then \
		echo "ERROR: falta k6/catalogo-load.js (script de carga versionado, pendiente)."; \
		echo "       Ver docs/mediciones/perf/REPORTE-PERF.md para la configuracion exacta"; \
		echo "       (50 VUs, 30s, GET /api/v1/catalogo?page=0&size=20) usada en las corridas ya archivadas."; \
		exit 1; \
	fi
	k6 run k6/catalogo-load.js

## Auditoria estatica minima de SQL dinamico (regla transversal):
## falla si aparece EXECUTE IMMEDIATE/sp_executesql, o una @Query nativeQuery=true
## que concatene con el operador `+` (indicio de entrada de usuario sin parametrizar).
## Se complementa con SpotBugs+find-sec-bugs (analisis de bytecode, ve lo que el
## grep no puede: concatenacion multi-linea, StringBuilder, JDBC Statement crudo).
## No sustituye la auditoria OWASP completa de docs/mediciones/sec/.
audit:
	@echo "== Auditoria estatica de SQL dinamico (texto, Backend) =="
	@if grep -rnE "EXECUTE IMMEDIATE|sp_executesql" artisync/Backend/src/main/java artisync/database 2>/dev/null; then \
		echo "FALLO: se encontro SQL dinamico prohibido (EXECUTE IMMEDIATE / sp_executesql)."; \
		exit 1; \
	fi
	@native_concat=$$(grep -rlnE 'nativeQuery\s*=\s*true' artisync/Backend/src/main/java --include='*.java' 2>/dev/null \
		| xargs -r grep -lE '@Query\(.*\+' 2>/dev/null); \
	if [ -n "$$native_concat" ]; then \
		echo "FALLO: @Query nativeQuery=true con concatenacion '+' en: $$native_concat"; \
		exit 1; \
	fi
	@echo "OK: sin EXECUTE IMMEDIATE/sp_executesql; sin concatenacion en @Query nativeQuery=true."
	@echo "== Analisis estatico de bytecode: SpotBugs + find-sec-bugs =="
	@# Se corre dentro de un contenedor maven:3.9-eclipse-temurin-21 (misma imagen que
	@# compila el backend en Dockerfile) en vez de ./mvnw directo: SpotBugs lee tambien
	@# las clases de plataforma del JDK que ejecuta el proceso, y su ASM embebido no
	@# soporta bytecode mas nuevo que Java 21 (falla con class file "major version" >65
	@# si la maquina tiene un JDK mas nuevo por defecto, aunque el proyecto compile
	@# correctamente para release 21).
	docker run --rm -v "$(CURDIR)/artisync/Backend:/app" -w /app \
		maven:3.9-eclipse-temurin-21 mvn -B spotbugs:spotbugs
	@mkdir -p docs/mediciones/sec/static-analysis
	@cp artisync/Backend/target/spotbugsXml.xml \
		docs/mediciones/sec/static-analysis/spotbugs-$$(date +%Y%m%d-%H%M).xml
	@echo "OK: reporte SpotBugs archivado en docs/mediciones/sec/static-analysis/."
	@echo "(Heuristica estatica — la verificacion empirica completa esta en docs/mediciones/sec/REPORTE-SEC.md, control A03.)"

## Escaneo dinamico OWASP ZAP baseline (Bloque A.1/A.2.3) contra el frontend
## (localhost:4200, el backend en 8080 no se publica al host por diseno — ver
## docker-compose.yml). Requiere el stack completo arriba (`make up`) y Docker
## disponible para correr la imagen oficial de ZAP.
audit-zap:
	@mkdir -p docs/mediciones/sec/zap
	docker run --rm -v "$$(pwd)/docs/mediciones/sec/zap:/zap/wrk/:rw" \
		--network host ghcr.io/zaproxy/zaproxy:stable \
		zap-baseline.py -t http://localhost:4200 \
		-r zap-baseline-$$(date +%Y%m%d-%H%M).html \
		-J zap-baseline-$$(date +%Y%m%d-%H%M).json || true
	@echo "OK: reporte ZAP baseline archivado en docs/mediciones/sec/zap/."
	@echo "(zap-baseline.py devuelve codigo != 0 si hay hallazgos WARN/FAIL; revisar el reporte, no solo el exit code.)"

## Baja los servicios y borra volumenes; limpia el build de Maven.
clean:
	$(COMPOSE) down -v
	cd artisync/Backend && ./mvnw -B clean

## Auditoria de calidad web Lighthouse (Bloque A.1): perfiles mobile y desktop,
## 3 corridas cada uno via @lhci/cli, contra el build de PRODUCCION del
## frontend (nginx), no el ng serve de desarrollo. Requiere el backend ya
## saludable (`make up`). Reconstruye el frontend con el override de medicion
## y lo deja asi al terminar; volver al frontend de desarrollo con
## `docker compose -f artisync/docker-compose.yml up -d --build frontend`.
##
## Corre lhci dentro de un contenedor Linux efimero (node:20 + chromium), en
## vez de invocar npx directo en la maquina del desarrollador: chrome-launcher
## (dependencia de lighthouse) tiene un bug de limpieza de directorio temporal
## en Windows (EPERM al borrar el perfil de Chrome) que aborta la corrida
## antes de escribir el reporte. El contenedor efimero comparte el namespace
## de red del propio contenedor "frontend" (--network container:pfc_frontend,
## no la red de compose) para que "localhost:4200" en lighthouserc.*.json
## resuelva igual que en la maquina del desarrollador — usar el nombre DNS
## interno de compose ("frontend") en su lugar rompe la deteccion de
## "contexto seguro" de Lighthouse para localhost y produce falsos negativos
## en is-on-https/redirects-http que no reflejan la aplicacion real. Los dos
## perfiles se corren en invocaciones SEPARADAS y SECUENCIALES (nunca en
## paralelo): ambas comparten el mismo .lighthouseci/ como scratch dir, y
## correrlas a la vez contamina el conteo de corridas y satura CPU/red,
## sesgando los resultados.
lighthouse:
	$(COMPOSE) -f artisync/docker-compose.lighthouse.yml up -d --wait --build frontend
	@rm -rf artisync/Frontend/.lighthouseci
	docker run --rm --network container:pfc_frontend \
		-v "$(CURDIR):/repo" -w /repo/artisync/Frontend \
		node:20-bookworm-slim bash -c ' \
			apt-get update -qq && apt-get install -y -qq chromium >/dev/null; \
			export CHROME_PATH=$$(command -v chromium); \
			npx --yes @lhci/cli@0.15.1 autorun --config=lighthouserc.mobile.json --collect.settings.chromeFlags="--no-sandbox" \
		' || true
	@rm -rf artisync/Frontend/.lighthouseci
	docker run --rm --network container:pfc_frontend \
		-v "$(CURDIR):/repo" -w /repo/artisync/Frontend \
		node:20-bookworm-slim bash -c ' \
			apt-get update -qq && apt-get install -y -qq chromium >/dev/null; \
			export CHROME_PATH=$$(command -v chromium); \
			npx --yes @lhci/cli@0.15.1 autorun --config=lighthouserc.desktop.json --collect.settings.chromeFlags="--no-sandbox" \
		' || true
	@echo "OK: reportes en docs/mediciones/lighthouse/ (nombrados localhost--<timestamp>.report.*; renombrar/archivar con la convencion lhci-YYYYMMDD-HHMM-<perfil>-runN antes de comitear)."

## Calcula el puntaje SUS (Bloque C.3) a partir de docs/mediciones/sus/sus-raw.csv
## y escribe docs/mediciones/sus/salida-sus.txt. Falla si el CSV solo tiene
## encabezado (sin sesiones de usabilidad corridas todavia).
sus:
	@datos=$$(tail -n +2 docs/mediciones/sus/sus-raw.csv | grep -c . || true); \
	if [ "$$datos" -eq 0 ]; then \
		echo "ERROR: docs/mediciones/sus/sus-raw.csv solo tiene encabezado."; \
		echo "       Ver docs/mediciones/sus/instrucciones-formulario.md para correr las sesiones."; \
		exit 1; \
	fi
	python docs/mediciones/sus/analisis-sus.py > docs/mediciones/sus/salida-sus.txt
	@echo "OK: ver docs/mediciones/sus/salida-sus.txt"

## Compila el documento academico final (Bloque B / D.1) de Markdown a PDF con
## pandoc, concatenando los capitulos de docs/informe-final/ en orden numerico.
## Requiere pandoc (y una distribucion LaTeX, ej. MiKTeX/TeX Live, como motor
## de PDF). Si docs/informe-final/ todavia no existe o pandoc no esta
## instalado, informa el motivo y termina con error en vez de fallar en
## silencio -- make all debe poder detectar este paso como pendiente.
docs:
	@if [ ! -d docs/informe-final ]; then \
		echo "ERROR: docs/informe-final/ no existe todavia (documento academico en borrador)."; \
		exit 1; \
	fi
	@command -v pandoc >/dev/null 2>&1 || { \
		echo "ERROR: pandoc no esta instalado. Ver https://pandoc.org/installing.html"; \
		exit 1; \
	}
	pandoc docs/informe-final/*.md \
		--from=markdown --pdf-engine=xelatex \
		--toc --number-sections \
		-o docs/informe-final/Informe-Final-v1.0.0.pdf
	@echo "OK: docs/informe-final/Informe-Final-v1.0.0.pdf generado."
