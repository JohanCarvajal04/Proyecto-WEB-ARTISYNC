# Reporte de Cronometraje de Generación de PDF — REQ-NF-006

- Fecha: 2026-09-10 (zona horaria del entorno de prueba: UTC-05:00)
- Commit base: rama `feat/ia-verificacion-asistida`
- Entorno: local, Postgres real vía perfil `postgres-it` (`@DataJpaTest`)
- Endpoint/servicio medido: `ContratoServicioImpl.generarPdf` (subyacente a `GET /api/v1/contratos/{id}/pdf`)
- Prueba versionada: [`ContratoPdfTimingIT.java`](../../../artisync/Backend/src/test/java/uteq/edu/ec/artisync/service/legal/impl/ContratoPdfTimingIT.java)
- Configuración: 5 corridas secuenciales contra un contrato real (firmado) sembrado directamente en la base de datos

## Comando ejecutado

```
$ docker compose -f artisync/docker-compose.yml up -d --wait postgres
$ ./mvnw test -Dtest=ContratoPdfTimingIT -Dspring.profiles.active=postgres-it \
    -DDB_NAME=artisyncbd -DDB_USER=postgres -DDB_PASSWORD=changeme
```

## Resultado

| Corrida | Tiempo (ms) |
|---|---|
| 1 (incluye JIT warm-up de Hibernate/openhtmltopdf) | 1218 |
| 2 | 29 |
| 3 | 30 |
| 4 | 20 |
| 5 | 20 |
| **Media** | **263.4** |

**Umbral esperado:** ≤5000ms por generación — **Resultado: cumple** (incluso la primera
corrida, con toda la inicialización en frío de Hibernate y del motor de PDF, queda muy
por debajo del umbral; las corridas 2-5, con las clases ya cargadas por la JVM, están en
el rango de 20-30ms).
