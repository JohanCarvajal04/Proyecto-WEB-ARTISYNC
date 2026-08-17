/**
 * Prueba de carga k6 — GET /api/v1/catalogo (Entrega Final, Bloque A.1 / D.1)
 *
 * Reconstruye la configuración documentada en docs/mediciones/perf/REPORTE-PERF.md
 * (50 VUs, 30s, GET /api/v1/catalogo?page=0&size=20, catálogo sembrado con 202
 * servicios vía artisync/database/seed-medicion-servicios.sql), que hasta la
 * Entrega Final se ejecutó sin que este script quedara versionado en el
 * repositorio (ver docs/observaciones/INFORME-BRECHAS-ENTREGA-FINAL.md, Bloque A.1).
 *
 * Nota de honestidad: este archivo es una reconstrucción fiel a los parámetros
 * documentados, escrita para que `make bench` sea reproducible de aquí en
 * adelante. No se garantiza que sea byte-idéntico al script que generó los
 * números ya archivados en docs/mediciones/perf/k6-*.json — esos datos crudos
 * se conservan como evidencia histórica de sus propias corridas.
 *
 * Uso:
 *   k6 run k6/catalogo-load.js                                   # escenario "caliente"
 *   k6 run -e ESCENARIO=frio k6/catalogo-load.js                 # escenario "frío"
 *          (ejecutar `redis-cli FLUSHALL` inmediatamente antes)
 *   k6 run -e BASE_URL=http://localhost:8080 k6/catalogo-load.js # sobrescribir host
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ESCENARIO = __ENV.ESCENARIO || 'caliente';

// Umbrales según Bloque A.1 de la guía de la Entrega Final:
// p95 <= 200ms con cache caliente, p95 <= 500ms con cache frío, 0% de errores >=500.
const P95_THRESHOLD_MS = ESCENARIO === 'frio' ? 500 : 200;

export const options = {
  vus: 50,
  duration: '30s',
  thresholds: {
    http_req_duration: [`p(95)<${P95_THRESHOLD_MS}`],
    http_req_failed: ['rate<0.001'], // 0 errores HTTP >=500 tolerados
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/catalogo?page=0&size=20`);

  check(res, {
    'status es 200': (r) => r.status === 200,
    'no es error 5xx': (r) => r.status < 500,
  });

  sleep(1);
}
