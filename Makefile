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

.PHONY: all up down test bench bench-auth bench-auth-cold perf-stats audit audit-sql-dynamic audit-zap clean sus lighthouse lighthouse_wait_backend docs srs sync-procs sync-procs-check

# Imagen con pandoc + LaTeX para generar PDFs sin exigir una instalacion local
# de TeX. Se puede sobreescribir: make srs PANDOC_IMAGE=otra/imagen
PANDOC_IMAGE ?= pandoc/latex:3.1

# Opciones de pandoc compartidas por el target srs.
SRS_PANDOC_OPTS ?= --toc --number-sections --pdf-engine=xelatex \
                   -V lang=es -V geometry:margin=2.5cm -V documentclass=report \
                   --metadata title="SRS - Artisync v1.0.0"

## Reproduccion end-to-end en un solo comando (Bloque D.1): levanta el stack
## completo (Flyway aplica migraciones y postgres aplica db/seed.sql al
## arrancar, sin paso manual aparte), corre pruebas unitarias, benchmark k6,
## auditoria estatica de SQL dinamico y escaneo ZAP, Lighthouse, el analisis
## SUS sobre los datos ya recolectados, y compila el documento academico
## final a PDF. Requiere Docker, k6 y pandoc instalados (ver README).
all: up test bench audit audit-sql-dynamic audit-zap lighthouse sus perf-stats srs docs
	@echo "OK: pipeline de reproduccion completo (make all) termino sin errores."

## Levanta postgres, redis, backend y frontend (con build y live reload) en segundo plano.
## Si artisync/.env no existe, se copia de artisync/.env.example (postgres/redis
## usan los defaults del propio docker-compose.yml). El placeholder JWT_SECRET
## del ejemplo tiene 31 bytes -- por debajo del minimo de 32 que exige JwtService
## (falla el arranque del backend) -- asi que se sustituye por uno real generado
## con openssl.
up:
	@test -f artisync/.env || { \
		cp artisync/.env.example artisync/.env; \
		if command -v openssl >/dev/null 2>&1; then \
			secret=$$(openssl rand -hex 32); \
			sed -i.bak "s/^JWT_SECRET=.*/JWT_SECRET=$$secret/" artisync/.env && rm -f artisync/.env.bak; \
		else \
			echo "AVISO: openssl no disponible, JWT_SECRET quedo con el placeholder de .env.example (31 bytes, el backend no arrancara)."; \
			echo "       Genera uno manualmente: openssl rand -hex 32"; \
		fi; \
		echo "AVISO: artisync/.env no existia, se genero desde .env.example."; \
		echo "       Revisa MAIL_*/PAYPAL_*/IA_PROVIDER si necesitas esas integraciones."; \
	}
	$(COMPOSE) up -d --build

## Detiene los servicios sin borrar los volumenes (datos de postgres persisten).
down:
	$(COMPOSE) down

## Ejecuta la suite de pruebas JUnit del backend (no requiere Docker).
test:
	cd artisync/Backend && ./mvnw -B test

## Regenera R__procedimientos.sql a partir de db/procs/ (fuente canonica, A.2.1).
sync-procs:
	bash scripts/sync-procs.sh

## Verifica que R__procedimientos.sql este sincronizado (lo mismo que corre el CI).
sync-procs-check:
	bash scripts/sync-procs.sh --check

## Prueba de carga k6 (50 VUs, 30s) contra el endpoint de catalogo (publico,
## permitAll), igual configuracion que docs/mediciones/perf/REPORTE-PERF.md.
## Requiere k6 instalado (https://k6.io/docs/get-started/installation/) y el
## sistema levantado con `make up`. Para el escenario PROTEGIDO (autenticado),
## ver los targets `bench-auth` / `bench-auth-cold` (T-14,
## docs/observaciones/PLAN-EXAMEN-FINAL.md).
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

## Imagen oficial de k6 usada por bench-auth/bench-auth-cold (el backend no
## publica el 8080 al host por diseno -- ver docker-compose.yml, servicio
## backend -- asi que k6 corre en un contenedor efimero que comparte el
## namespace de red de pfc_backend, igual patron que el target `lighthouse`
## con pfc_frontend).
K6_IMAGE ?= grafana/k6:0.54.0

## Genera las 5 corridas "calientes" versionadas del escenario PROTEGIDO
## (GET /api/v1/admin/reportes/finanzas, autenticado) en
## docs/mediciones/perf/k6-auth-run{1..5}.json. Requiere el stack arriba
## (`make up`), el usuario admin del seed base y un usuario con perfil de
## creador sembrado (ver artisync/database/seed-medicion-servicios.sql /
## K6_USER, K6_PASS, CREADOR_CORREO). NO se encadena en `make all`: son 5
## corridas de 30s (~2.5 min) que sobrescribirian evidencia ya versionada
## cada vez que alguien reproduce el pipeline completo; se corre a demanda,
## igual que `bench-auth-cold`.
bench-auth:
	@mkdir -p docs/mediciones/perf
	@for i in 1 2 3 4 5; do \
		echo "== corrida caliente $$i/5 =="; \
		MSYS_NO_PATHCONV=1 docker run --rm --network container:pfc_backend \
			-v "$(CURDIR)/k6:/scripts:ro" -v "$(CURDIR)/docs/mediciones/perf:/output" \
			-e ESCENARIO=caliente -e BASE_URL=http://localhost:8080 \
			-e K6_USER=$${K6_USER:-admin@artisync.com} -e K6_PASS=$${K6_PASS:-ArtisyncAdmin2026!} \
			-e CREADOR_CORREO=$${CREADOR_CORREO:-creador@test.com} \
			$(K6_IMAGE) run --out json=/output/k6-auth-run$$i.json /scripts/comisiones-load.js \
			| tee docs/mediciones/perf/k6-console-auth-run$$i.txt; \
	done
	@echo "OK: 5 corridas calientes en docs/mediciones/perf/k6-auth-run{1..5}.json"

## Igual que `bench-auth` pero para el escenario "frio": reinicia el
## contenedor del backend (pfc_backend) justo antes de CADA corrida, para que
## la JVM arranque sin JIT calentado y con el pool de conexiones vacio (este
## endpoint no tiene @Cacheable, asi que un FLUSHALL de Redis no serviria de
## nada -- ver la nota metodologica en k6/comisiones-load.js). Usa
## `up -d --wait` (no un curl al host, el 8080 no esta publicado) para
## esperar a que el healthcheck del backend pase antes de disparar cada
## corrida.
bench-auth-cold:
	@mkdir -p docs/mediciones/perf
	@for i in 1 2 3 4 5; do \
		echo "== reiniciando pfc_backend antes de la corrida fria $$i/5 =="; \
		$(COMPOSE) restart backend; \
		$(COMPOSE) up -d --wait backend; \
		echo "== corrida fria $$i/5 =="; \
		MSYS_NO_PATHCONV=1 docker run --rm --network container:pfc_backend \
			-v "$(CURDIR)/k6:/scripts:ro" -v "$(CURDIR)/docs/mediciones/perf:/output" \
			-e ESCENARIO=frio -e BASE_URL=http://localhost:8080 \
			-e K6_USER=$${K6_USER:-admin@artisync.com} -e K6_PASS=$${K6_PASS:-ArtisyncAdmin2026!} \
			-e CREADOR_CORREO=$${CREADOR_CORREO:-creador@test.com} \
			$(K6_IMAGE) run --out json=/output/k6-auth-cold-run$$i.json /scripts/comisiones-load.js \
			| tee docs/mediciones/perf/k6-console-auth-cold-run$$i.txt; \
	done
	@echo "OK: 5 corridas frias en docs/mediciones/perf/k6-auth-cold-run{1..5}.json"

## Auditoria estatica de SQL dinamico como script independiente versionado
## (cubre db/procs/*.sql y migraciones Flyway ademas del codigo Java;
## complementa la logica en linea que corre audit: sobre el Backend).
audit-sql-dynamic:
	bash scripts/audit-sql-dynamic.sh

## Auditoria estatica minima de SQL dinamico (regla transversal):
## falla si aparece EXECUTE IMMEDIATE/sp_executesql, o una @Query nativeQuery=true
## que concatene con el operador `+` (indicio de entrada de usuario sin parametrizar).
## Se complementa con SpotBugs+find-sec-bugs (analisis de bytecode, ve lo que el
## grep no puede: concatenacion multi-linea, StringBuilder, JDBC Statement crudo).
## No sustituye la auditoria OWASP completa de docs/mediciones/sec/.
audit: audit-sql-dynamic
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
	MSYS_NO_PATHCONV=1 docker run --rm -v "$(CURDIR)/artisync/Backend:/app" -w /app \
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
	MSYS_NO_PATHCONV=1 docker run --rm -v "$$(pwd)/docs/mediciones/sec/zap:/zap/wrk/:rw" \
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

## Auditoria de calidad web Lighthouse (Bloque A.1, OBS-P4-01): perfiles
## mobile y desktop, 3 corridas cada uno via @lhci/cli, contra el
## DESPLIEGUE PUBLICO real (LIGHTHOUSE_URL, default
## https://artisync-frontend.onrender.com), sobre 3 rutas publicas
## (/explorar, /explorar/creadores, /auth/login) -- ver collect.url en
## lighthouserc.mobile.json / lighthouserc.desktop.json. Ya no depende de
## `make up` ni de docker-compose.lighthouse.yml: al ser una URL HTTPS
## publica real, Lighthouse detecta "contexto seguro" de forma nativa y no
## hace falta el truco de compartir namespace de red con un contenedor
## frontend local (a diferencia de auditar localhost, que si lo necesitaba).
##
## Corre lhci dentro de un contenedor Linux efimero (node:20 + chromium), en
## vez de invocar npx directo en la maquina del desarrollador: chrome-launcher
## (dependencia de lighthouse) tiene un bug de limpieza de directorio temporal
## en Windows (EPERM al borrar el perfil de Chrome) que aborta la corrida
## antes de escribir el reporte. Los dos perfiles se corren en invocaciones
## SEPARADAS y SECUENCIALES (nunca en paralelo): ambas comparten el mismo
## .lighthouseci/ como scratch dir, y correrlas a la vez contamina el conteo
## de corridas y satura CPU/red, sesgando los resultados. `|| true` porque
## `lhci autorun` sale con codigo != 0 si una assertion no pasa el umbral
## (warn/error) -- eso es una senal a revisar, no una falla del target: los
## reportes igual se escriben a disco antes de salir.
## El backend en Render (plan free) se duerme tras inactividad y responde 502
## en el primer request tras despertar (~30-60s de cold-start). Si Lighthouse
## mide justo en ese momento, las llamadas a /api/v1/categorias|catalogo|
## etiquetas devuelven 502, el frontend muestra un toast de error que se
## convierte en el elemento LCP (~5s de retraso) y ademas arrastra una
## violacion de accesibilidad (boton de cerrar del toast). Este paso hace
## polling al healthcheck del backend ANTES de arrancar lhci, para que el
## cold-start ya haya pasado cuando Lighthouse dispare los requests reales.
BACKEND_HEALTH_URL ?= https://artisync-backend.onrender.com/actuator/health
lighthouse_wait_backend:
	@echo "Esperando a que $(BACKEND_HEALTH_URL) responda antes de correr Lighthouse..."
	@for i in $$(seq 1 24); do \
		if curl -sf -o /dev/null "$(BACKEND_HEALTH_URL)"; then \
			echo "OK: backend listo (intento $$i)."; exit 0; \
		fi; \
		echo "  backend no listo todavia (intento $$i/24), esperando 5s..."; \
		sleep 5; \
	done; \
	echo "AVISO: el backend no respondio tras 2 minutos; se continua igual (podria contaminar la medicion)."

LIGHTHOUSE_TS ?= $(shell date +%Y%m%d-%H%M)
lighthouse: lighthouse_wait_backend
	@rm -rf artisync/Frontend/.lighthouseci
	MSYS_NO_PATHCONV=1 docker run --rm \
		-v "$(CURDIR):/repo" -w /repo/artisync/Frontend \
		node:20-bookworm-slim bash -c ' \
			apt-get update -qq && apt-get install -y -qq chromium >/dev/null; \
			export CHROME_PATH=$$(command -v chromium); \
			npx --yes @lhci/cli@0.15.1 autorun --config=lighthouserc.mobile.json --collect.settings.chromeFlags="--no-sandbox" \
		' || true
	@$(call lighthouse_archive,mobile)
	@rm -rf artisync/Frontend/.lighthouseci
	MSYS_NO_PATHCONV=1 docker run --rm \
		-v "$(CURDIR):/repo" -w /repo/artisync/Frontend \
		node:20-bookworm-slim bash -c ' \
			apt-get update -qq && apt-get install -y -qq chromium >/dev/null; \
			export CHROME_PATH=$$(command -v chromium); \
			npx --yes @lhci/cli@0.15.1 autorun --config=lighthouserc.desktop.json --collect.settings.chromeFlags="--no-sandbox" \
		' || true
	@$(call lighthouse_archive,desktop)
	@echo "OK: reportes archivados en docs/mediciones/lighthouse/ (lhci-<fecha>-<perfil>-prod-<ruta>-runN)."

## Renombra los reportes crudos que escribe lhci (nombrados por lhci a partir
## del host+ruta de la URL, ej. artisync_frontend_onrender_com-explorar-
## <timestamp>.report.json) a la convencion versionada del proyecto:
## lhci-<fecha>-<perfil>-prod-<ruta>-run<N>.report.{json,html}. Recibe el
## perfil ($1: mobile|desktop) como argumento.
define lighthouse_archive
for route in explorar explorar_creadores auth_login; do \
	n=1; \
	for jf in $$(ls docs/mediciones/lighthouse/artisync_frontend_onrender_com-$$route-*.report.json 2>/dev/null | sort); do \
		hf="$${jf%.report.json}.report.html"; \
		mv "$$jf" "docs/mediciones/lighthouse/lhci-$(LIGHTHOUSE_TS)-$(1)-prod-$$route-run$$n.report.json"; \
		[ -e "$$hf" ] && mv "$$hf" "docs/mediciones/lighthouse/lhci-$(LIGHTHOUSE_TS)-$(1)-prod-$$route-run$$n.report.html"; \
		n=$$((n+1)); \
	done; \
done; \
[ -e docs/mediciones/lighthouse/manifest.json ] && mv docs/mediciones/lighthouse/manifest.json "docs/mediciones/lighthouse/lhci-$(LIGHTHOUSE_TS)-$(1)-prod-manifest.json" || true
endef

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
	python3 docs/mediciones/sus/analisis-sus.py > docs/mediciones/sus/salida-sus.txt
	@echo "OK: ver docs/mediciones/sus/salida-sus.txt"

## Test inferencial (Mann-Whitney U + tamano de efecto A12 de Vargha-Delaney,
## mas Welch t / d de Cohen como contraste) sobre los NDJSON crudos de k6 ya
## versionados: catalogo (k6-run*.json vs k6-cold-run*.json) y el endpoint
## protegido (k6-auth-run*.json vs k6-auth-cold-run*.json). Requiere scipy
## (ver docs/mediciones/perf/requirements.txt); NO relanza k6 -- solo
## recalcula sobre archivos ya generados por `bench`/`bench-auth`/
## `bench-auth-cold`, por eso si se encadena en `make all` (T-15,
## docs/observaciones/PLAN-EXAMEN-FINAL.md).
perf-stats:
	pip install -q -r docs/mediciones/perf/requirements.txt
	python3 docs/mediciones/perf/analisis-inferencial.py > docs/mediciones/perf/salida-inferencial.txt
	@echo "OK: ver docs/mediciones/perf/salida-inferencial.txt"

## Compila el documento academico final (Bloque B / D.1) desde la fuente
## LaTeX de docs/informe-final/main.tex (bibliografia IEEE en
## referencias.bib). Usa pdflatex/bibtex/makeglossaries locales si estan
## instalados; si no, cae a un contenedor con TeX Live completo, igual que
## srs/lighthouse, para que "make all" sea reproducible con solo Docker.
##
## Nota de migracion: hasta la v1.0.0-rc este objetivo compilaba los .md de
## docs/informe-final/ con pandoc. Esa carpeta ahora contiene la fuente
## LaTeX (.tex/.bib) directamente, ver docs/informe-final/README.md.
caratula:
	@if command -v pdflatex >/dev/null 2>&1; then \
		echo "== Compilando caratula con TeX local =="; \
		cd docs/informe-final && pdflatex -interaction=nonstopmode caratula.tex; \
	else \
		echo "== Compilando caratula en contenedor texlive =="; \
		MSYS_NO_PATHCONV=1 docker run --rm -v "$(CURDIR):/repo" -w /repo/docs/informe-final texlive/texlive:latest \
			bash -c "pdflatex -interaction=nonstopmode caratula.tex"; \
	fi
	cp docs/informe-final/caratula.pdf docs/informe-final/Caratula-v1.1.0.pdf
	@echo "OK: docs/informe-final/Caratula-v1.1.0.pdf generado."

docs: caratula
	@if [ ! -f docs/informe-final/main.tex ]; then \
		echo "ERROR: docs/informe-final/main.tex no existe todavia (documento academico en borrador)."; \
		exit 1; \
	fi
	@if command -v pdflatex >/dev/null 2>&1 && command -v bibtex >/dev/null 2>&1; then \
		echo "== Compilando con TeX local =="; \
		cd docs/informe-final && \
			pdflatex -interaction=nonstopmode main.tex && \
			bibtex main && \
			(command -v makeglossaries >/dev/null 2>&1 && makeglossaries main || true) && \
			pdflatex -interaction=nonstopmode main.tex && \
			pdflatex -interaction=nonstopmode main.tex; \
	else \
		command -v docker >/dev/null 2>&1 || { \
			echo "ERROR: se necesita Docker, o una instalacion local de pdflatex+bibtex (TeX Live/MiKTeX)."; \
			exit 1; \
		}; \
		echo "== pdflatex/bibtex no estan localmente; compilando en contenedor texlive/texlive =="; \
		echo "   (se monta el repo completo, no solo docs/informe-final: main.tex usa"; \
		echo "   \\graphicspath hacia ../diagramas/ y ../mediciones/, fuera de esta carpeta)"; \
		MSYS_NO_PATHCONV=1 docker run --rm -v "$(CURDIR):/repo" -w /repo/docs/informe-final texlive/texlive:latest \
			bash -c "pdflatex -interaction=nonstopmode main.tex && bibtex main && makeglossaries main && pdflatex -interaction=nonstopmode main.tex && pdflatex -interaction=nonstopmode main.tex"; \
	fi
	cp docs/informe-final/main.pdf docs/informe-final/Informe-Final-v1.0.0.pdf
	@echo "OK: docs/informe-final/Informe-Final-v1.0.0.pdf generado."

## Genera el PDF del SRS (Bloque A.3.1) desde docs/requisitos/SRS.md.
##
## No exige una instalacion local de LaTeX: usa la imagen oficial pandoc/latex
## en un contenedor efimero, igual que el resto del pipeline se apoya en Docker.
## Asi `make srs` funciona desde una clonacion limpia con solo Docker instalado,
## que es lo que exige el Bloque D.1.
##
## Si prefieres una instalacion local, exporta PANDOC_LOCAL=1 y se usara el
## pandoc del PATH en lugar del contenedor.
srs:
	@test -f docs/requisitos/SRS.md || { echo "ERROR: docs/requisitos/SRS.md no existe."; exit 1; }
	@# La fuente por defecto de xelatex (Latin Modern) no trae los glifos U+2264
	@# ni U+2265, que el SRS usa en los umbrales de los requisitos no funcionales.
	@# Sin esta sustitucion xelatex los omite EN SILENCIO: el PDF saldria con los
	@# umbrales incompletos y el fallo solo apareceria como WARNING en el log, no
	@# en el codigo de salida. Por eso se sustituyen por <= y >= en una copia
	@# temporal, nunca en el fuente. No sirve mapearlos a notacion matematica de
	@# pandoc: varios aparecen dentro de spans de codigo, donde no se interpreta.
	@sed -e 's/≤/<=/g' -e 's/≥/>=/g' docs/requisitos/SRS.md > docs/requisitos/.srs-build.md
	@if [ "$${PANDOC_LOCAL:-0}" = "1" ]; then \
		command -v pandoc >/dev/null 2>&1 || { \
			echo "ERROR: PANDOC_LOCAL=1 pero pandoc no esta en el PATH."; \
			rm -f docs/requisitos/.srs-build.md; \
			exit 1; \
		}; \
		pandoc docs/requisitos/.srs-build.md \
			-o docs/requisitos/SRS-v1.0.0.pdf $(SRS_PANDOC_OPTS); \
	else \
		command -v docker >/dev/null 2>&1 || { \
			echo "ERROR: se necesita Docker (o PANDOC_LOCAL=1 con pandoc instalado)."; \
			rm -f docs/requisitos/.srs-build.md; \
			exit 1; \
		}; \
		MSYS_NO_PATHCONV=1 docker run --rm -v "$(CURDIR):/data" -w /data $(PANDOC_IMAGE) \
			docs/requisitos/.srs-build.md \
			-o docs/requisitos/SRS-v1.0.0.pdf $(SRS_PANDOC_OPTS); \
	fi
	@rm -f docs/requisitos/.srs-build.md
	@echo "OK: docs/requisitos/SRS-v1.0.0.pdf generado."
	@echo "    Recuerda: el PDF solo cierra el criterio D0R cuando lleva la firma"
	@echo "    de aprobacion del docente-director (seccion 8 del SRS)."
