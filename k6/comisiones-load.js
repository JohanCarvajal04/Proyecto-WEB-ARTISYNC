// Prueba de carga contra un endpoint PROTEGIDO y representativo del uso real:
// GET /api/v1/admin/reportes/finanzas (reporte de comisiones por creador,
// respaldado por fn_reporte_comisiones_creador — join de 5 tablas + agregación
// sobre la cadena de escrow). Complementa k6/catalogo-load.js (público,
// permitAll) midiendo también el coste de autenticación/autorización
// (OBS-P2-01/02, docs/observaciones/PLAN-EXAMEN-FINAL.md, T-14).
//
// A diferencia de /api/v1/catalogo, este endpoint no tiene @Cacheable, así
// que un FLUSHALL de Redis no le haría nada. Aquí "caliente"/"frío" se
// define a nivel de proceso, no de caché de aplicación: "frío" = el
// contenedor del backend se reinicia justo antes de la corrida (JVM sin JIT
// calentado, pool de conexiones vacío); "caliente" = corridas consecutivas
// sobre un backend que ya lleva un rato arriba. Ver Makefile (targets
// bench-auth / bench-auth-cold) y docs/mediciones/perf/REPORTE-PERF.md para
// la misma advertencia metodológica aplicada al catálogo.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ESCENARIO = __ENV.ESCENARIO || 'caliente';
const K6_USER = __ENV.K6_USER || 'admin@artisync.com';
const K6_PASS = __ENV.K6_PASS || 'ArtisyncAdmin2026!';
// Correo del creador cuyo reporte de comisiones se consulta en cada
// iteración; debe tener perfil de creador (POST /api/v1/perfiles) y,
// idealmente, transacciones sembradas (ver artisync/database/seed-medicion-*.sql)
// para que la consulta agregue datos reales en vez de un JSON vacío.
const CREADOR_CORREO = __ENV.CREADOR_CORREO || 'creador@test.com';

// Mismo perfil de carga que catalogo-load.js: 50 VUs, 30s. El umbral de p95
// se deja más alto que el del catálogo porque esta consulta hace un JOIN de
// 5 tablas con agregación (no hay cache de aplicación de por medio); ajustar
// tras la primera corrida si el backend real lo permite más bajo.
const P95_THRESHOLD_MS = ESCENARIO === 'frio' ? 1500 : 800;

export const options = {
  vus: 50,
  duration: '30s',
  thresholds: {
    http_req_duration: [`p(95)<${P95_THRESHOLD_MS}`],
    http_req_failed: ['rate<0.001'],
  },
};

function loginYObtenerIdPerfil() {
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ correo: K6_USER, contrasena: K6_PASS }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  if (loginRes.status !== 200) {
    throw new Error(`setup: login falló (status ${loginRes.status}): ${loginRes.body}`);
  }
  const token = loginRes.json('accessToken');

  const authHeaders = { headers: { Authorization: `Bearer ${token}` } };

  // 1) Resolver idUsuario del creador por correo (solo un admin puede listar
  //    usuarios; no hay endpoint público de búsqueda por correo).
  const usuariosRes = http.get(
    `${BASE_URL}/api/v1/admin/usuarios?busqueda=${encodeURIComponent(CREADOR_CORREO)}&size=1`,
    authHeaders,
  );
  if (usuariosRes.status !== 200) {
    throw new Error(`setup: GET /admin/usuarios falló (status ${usuariosRes.status}): ${usuariosRes.body}`);
  }
  const contenido = usuariosRes.json('content');
  if (!contenido || contenido.length === 0) {
    throw new Error(`setup: no se encontró ningún usuario con correo "${CREADOR_CORREO}". Sembrar antes de correr k6 (ver artisync/database/seed-medicion-servicios.sql).`);
  }
  const idUsuario = contenido[0].idUsuario;

  // 2) Resolver idPerfil (de creador) a partir del idUsuario.
  const perfilRes = http.get(`${BASE_URL}/api/v1/perfiles/usuario/${idUsuario}`, authHeaders);
  if (perfilRes.status !== 200) {
    throw new Error(`setup: GET /perfiles/usuario/${idUsuario} falló (status ${perfilRes.status}): ${perfilRes.body}. ¿"${CREADOR_CORREO}" tiene perfil de creador (POST /api/v1/perfiles)?`);
  }
  const idPerfil = perfilRes.json('idPerfil');

  return { token, idPerfil };
}

export function setup() {
  return loginYObtenerIdPerfil();
}

export default function (data) {
  const url = `${BASE_URL}/api/v1/admin/reportes/finanzas` +
    `?idPerfil=${data.idPerfil}&desde=2020-01-01T00:00:00&hasta=2026-12-31T23:59:59`;
  const res = http.get(url, { headers: { Authorization: `Bearer ${data.token}` } });
  check(res, {
    'status es 200': (r) => r.status === 200,
    'no es error 5xx': (r) => r.status < 500,
  });
  sleep(1);
}
