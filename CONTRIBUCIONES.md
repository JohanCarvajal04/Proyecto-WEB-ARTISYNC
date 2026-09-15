# Declaración de aportes — cierre del examen suspenso

> **Borrador.** Este archivo cruza cada punto cerrado en esta ronda con los archivos que lo
> respaldan. Falta la parte que nadie más que el equipo puede completar: quién de los tres lo
> hizo, y su firma con correo institucional. Reemplacen la columna "Responsable" y añadan la
> firma antes de defender — sin eso, EV-4 sigue incompleto aunque el trabajo esté hecho.

## Puntos cerrados en esta ronda (P1–P14 de la guía del examen suspenso)

| Punto | Qué se cerró | Archivos principales | Responsable (a confirmar) |
|---|---|---|---|
| P4 | Contraseña del keystore ahora sale del entorno | `artisync/docker-compose.medicion.yml` | _____________ |
| P5 | Cobertura de los 9 paquetes `controller.*` ≥70% líneas y ramas | `OfferingControllerTest.java`, `CategoryControllerTest.java` | _____________ |
| P6 | 23 rutinas migradas de `nativeQuery=true` a `NamedParameterJdbcTemplate` | 12 pares `*RepositoryCustom`/`*RepositoryImpl`, `repository/support/PgArrays.java` | _____________ |
| P7 | Javadoc restaurado a 98.8%, `mvn javadoc:javadoc` sin errores | 12 clases `*RepositoryImpl`, `JwtService.java`, `AuditContext.java`, `WithdrawalPayoutReconciliationScheduler.java` | _____________ |
| P8 | 0% de tipos en español (2 tipos renombrados) | `UserRepositoryImpl.java`, `UserSessionRepositoryImpl.java`, `AiCertificateRepositoryImpl.java` | _____________ |
| P11 | 28 DOI verificados contra doi.org/Crossref/DataCite; 1 corrección (`RALPH2021`) | `docs/informe-final/referencias.bib`, `scripts/verificar-doi.py`, `docs/mediciones/verificacion-doi.txt` | _____________ |
| P13 | Cuaderno de reproducción ejecutado de punta a punta | `docs/mediciones/reproduccion.ipynb` | _____________ |
| P14 | Estructura anónima de constancia de consentimiento (pendiente de completar con datos reales) | `docs/etica/consentimientos/registro-consentimientos.csv`, `README.md` | _____________ |
| EV-1 | `VERIFICACION.md` — expediente completo de los 14 puntos | `VERIFICACION.md` | _____________ |
| EV-2 | Target `make verify` (encadena test, cobertura, P6, DOI, sync-procs, javadoc) | `Makefile`, `scripts/verificar-cobertura-controladores.py` | _____________ |

## Pendiente fuera del alcance de esta ronda

- **P9** (figuras en inglés) y **P10** (abstract 200–250 palabras), **P2/P3/P12** ya estaban
  resueltos por el equipo antes de esta sesión — no se tocaron, solo se verificaron de nuevo
  (ver `VERIFICACION.md`).
- **P14**: la constancia real de los 16 participantes del SUS requiere gestión directa con
  personas reales; queda para que el equipo la complete antes del cierre.
- **EV-3** (mover la etiqueta `v1.1.0` al commit final): deliberadamente el último paso, después
  de que el equipo revise y firme este archivo — no tiene sentido etiquetar antes de confirmar
  que todo lo de arriba se comitea correctamente.

## Firma

Al completar la columna "Responsable" y antes de defender, cada integrante firma aquí
confirmando que puede explicar sin ayuda el código y la evidencia de los puntos que declara
suyos (criterio de la guía, sección 4):

- Johan Stalin Carvajal Loor — `_____________` (correo institucional) — fecha: __________
- Bryan Javier Figueroa Morales — `_____________` (correo institucional) — fecha: __________
- Jhon Kevin Ríos Cuyabazo — `_____________` (correo institucional) — fecha: __________
