# Makefile — Artisync (PFC Tercera Entrega v0.9.0-rc)
#
# Objetivos minimos exigidos por el Bloque B.1 de la guia de la Tercera Entrega:
# up, down, test, bench, audit, clean. `make up` debe levantar el sistema
# completo desde una clonacion limpia sin intervencion humana adicional.
#
# Requisitos previos: Docker + Docker Compose, y `cp artisync/.env.example artisync/.env`
# con las variables editadas (ver README).

SHELL := /bin/bash
COMPOSE := docker compose -f artisync/docker-compose.yml --env-file artisync/.env

.PHONY: up down test bench audit clean sus lighthouse

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
## No sustituye la auditoria OWASP completa de docs/mediciones/sec/.
audit:
	@echo "== Auditoria estatica de SQL dinamico (Backend) =="
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
	@echo "(Heuristica estatica — la verificacion empirica completa esta en docs/mediciones/sec/REPORTE-SEC.md, control A03.)"

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
