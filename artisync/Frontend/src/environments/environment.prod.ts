export const environment = {
  production: true,
  // Mismo origen a traves del proxy del frontend (nginx en produccion real,
  // ng serve --proxy-config en Docker dev): el backend ya no publica el 8080
  // al host (OBS-AUTO-05 / A07 OWASP), asi que ':8080' directo dejaria de
  // ser alcanzable. Ademas evita que las peticiones con cookies (login,
  // /auth/refresh, /auth/2fa/verify) sean cross-origin.
  apiUrl: '/api'
};
