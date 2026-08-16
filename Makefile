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

.PHONY: up down test bench audit clean sus

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
