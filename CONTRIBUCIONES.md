# Declaración de aportes — cierre del examen suspenso

> **Borrador.** Este archivo cruza cada punto cerrado en esta ronda con los archivos que lo
> respaldan. Falta la parte que nadie más que el equipo puede completar: quién de los tres lo
> hizo, y su firma con correo institucional. Reemplacen la columna "Responsable" y añadan la
> firma antes de defender — sin eso, EV-4 sigue incompleto aunque el trabajo esté hecho.
>
> La columna "Responsable" trae una propuesta inicial basada en `git log --format='%an' -- <archivo>`
> sobre los archivos que cierran cada punto (autoría de commits, no titularidad confirmada). Donde
> aparece más de un nombre, ambos tocaron los mismos archivos y el equipo debe decidir quién lo
> firma — y solo puede firmarlo quien pueda explicar el código y la evidencia sin ayuda (criterio
> de la guía, sección 4). Esto no reemplaza la firma: cada integrante confirma o corrige su fila
> y firma personalmente con su correo `@uteq.edu.ec` antes del cierre.

## Puntos cerrados en esta ronda (P1–P14 de la guía del examen suspenso)

| Punto | Qué se cerró                                                                                                                                 | Archivos principales                                                                                                                        | Responsable (a confirmar)       |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------- |
| P1    | Cookie de refresco con `Secure`/`HttpOnly`/`SameSite=Strict`, verificada en vivo contra el despliegue público                                | `AuthController.java`                                                                                                                       | Bryan Figueroa |
| P2    | 0 de 62 etiquetas `.tex` sin referenciar, script sale con error (`sys.exit(1)`) si aparece alguna                                            | `scripts/auditoria-rubrica.py` (`seccion_p12`), `Makefile` (target `verify`)                                                                | Bryan Figueroa |
| P3    | Sin DOI de marcador (`...1234567`) en el árbol                                                                                               | `docs/informe-final/referencias.bib`                                                                                                        | Bryan Figueroa / Johan Carvajal / Jhon Kevin Ríos |
| P4    | Contraseña del keystore ahora sale del entorno                                                                                               | `artisync/docker-compose.medicion.yml`                                                                                                      | Johan Carvajal / Bryan Figueroa |
| P5    | Cobertura de los 9 paquetes `controller.*` ≥70% líneas y ramas                                                                               | `OfferingControllerTest.java`, `CategoryControllerTest.java`                                                                                | Bryan Figueroa y Johan Carvajal |
| P6    | 23 rutinas migradas de `nativeQuery=true` a `NamedParameterJdbcTemplate`                                                                     | 12 pares `*RepositoryCustom`/`*RepositoryImpl`, `repository/support/PgArrays.java`                                                          | Bryan Figueroa y Johan Carvajal |
| P7    | Javadoc completo (`@param`/`@return`/`@throws`) subido de 72.3% a 100.0% (1166/1166), `mvn javadoc:javadoc` sin errores                     | 12 clases `*RepositoryImpl`, `JwtService.java`, `AuditContext.java`, `WithdrawalPayoutReconciliationScheduler.java`                         | Bryan Figueroa y Johan Carvajal |
| P8    | 0% de tipos en español (2 tipos renombrados)                                                                                                 | `UserRepositoryImpl.java`, `UserSessionRepositoryImpl.java`, `AiCertificateRepositoryImpl.java`                                             | Bryan Figueroa y Johan Carvajal |
| P9    | Diagrama C4 re-renderizado sin texto en español (títulos, cajas, aristas)                                                                    | `docs/diagramas/workspace.dsl`, `C4-nivel1-context_diagram.svg`                                                                             | Bryan Figueroa |
| P10   | Resumen 231 palabras / Abstract 213 palabras, dentro del rango 200-250                                                                       | `docs/informe-final/secciones/00-portada-resumen.tex`                                                                                       | Bryan Figueroa / Jhon Kevin Ríos / Johan Carvajal |
| P11   | 35 DOI verificados contra doi.org/Crossref/DataCite (32 bibliográficos + 3 de software/dataset); 1 corrección (`RALPH2021`)                  | `docs/informe-final/referencias.bib`, `scripts/verificar-doi.py`, `docs/mediciones/verificacion-doi.txt`                                    | Bryan Figueroa, Johan Carvajal y Jhon Kevin Ríos |
| P12   | Lighthouse contra la URL pública real: 18 JSON del lote `lhci-20260905-2150-*` (9 escritorio + 9 móvil, 3 rutas × 3 corridas × 2 perfiles) | `docs/mediciones/lighthouse/lhci-20260905-2150-*.json`, `Makefile` (target `lighthouse`)                                                    | Bryan Figueroa y Johan Carvajal |
| P13   | Cuaderno de reproducción ejecutado de punta a punta                                                                                          | `docs/mediciones/reproduccion.ipynb`                                                                                                        | Bryan Figueroa y Johan Carvajal |
| P14   | Constancia de consentimiento cerrada: 16/16 hash SHA-256 verificados contra los PDF físicos en `G:\EPSCAN\` (2026-09-16), código de salida 0 | `docs/etica/consentimientos/registro-consentimientos.csv`, `docs/mediciones/sus/REPORTE-SUS.md`, `scripts/verificar-consentimientos-sus.py` | Bryan Figueroa y Johan Carvajal |
| EV-1  | `VERIFICACION.md` — expediente completo de los 14 puntos                                                                                     | `VERIFICACION.md`                                                                                                                           | Bryan Figueroa y Johan Carvajal |
| EV-2  | Target `make verify` (encadena test, cobertura, P6, DOI, sync-procs, javadoc)                                                                | `Makefile`, `scripts/verificar-cobertura-controladores.py`                                                                                  | Bryan Figueroa y Johan Carvajal |

## Firma

Al completar la columna "Responsable" y antes de defender, cada integrante firma aquí
confirmando que puede explicar sin ayuda el código y la evidencia de los puntos que declara
suyos (criterio de la guía, sección 4):

- Johan Stalin Carvajal Loor — `jcarvajall@uteq.edu.ec` (correo institucional) — fecha: 16/09/2026
- Bryan Javier Figueroa Morales — `bfigueroa@uteq.edu.ec` (correo institucional) — fecha: 16/09/2026
- Jhon Kevin Ríos Cuyabazo — `jriosc@uteq.edu.ec` (correo institucional) — fecha: 16/09/2026
