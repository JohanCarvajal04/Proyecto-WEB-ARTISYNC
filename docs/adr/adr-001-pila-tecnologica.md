# ADR-001: Selección de la pila tecnológica principal

**Estado:** Aceptado
**Fecha:** Semana 5 (Entrega 1A) — ratificado en Tercera Entrega, 24 de julio de 2026

## Contexto
El equipo debía seleccionar el lenguaje/framework de servidor para Artisync, una plataforma con RBAC, JWT + 2FA, patrón *escrow* financiero y un motor de flujos de trabajo parametrizable, a desarrollar en 17 semanas académicas con un equipo que tiene experiencia previa en Java/Spring Boot.

## Opciones consideradas
- **A — Spring Boot (Java 21):** mayor throughput y mecanismos de seguridad/persistencia integrados (Spring Security, Spring Data JPA); curva de aprendizaje mitigada por experiencia previa del equipo.
- **B — Express (Node.js 4.18.2):** mayor fiabilidad bajo cargas extremas, pero requiere librerías adicionales para RBAC, validación y persistencia estructurada.
- **C — Django (Python 3.11):** desarrollo rápido pero menor eficiencia observada en pruebas comparativas de carga.

### Nota sobre las comparativas de rendimiento (OBS-04)

Las afirmaciones comparativas de las tres opciones anteriores —«mayor throughput», «mayor fiabilidad bajo cargas extremas», «menor eficiencia observada en pruebas comparativas de carga»— son **apreciaciones cualitativas tomadas de comparativas publicadas por terceros y de la experiencia previa del equipo, no mediciones propias de este proyecto**. No deben leerse como un benchmark de Artisync ni citarse como evidencia empírica.

Las únicas cifras de rendimiento medidas sobre este sistema, con su método, número de corridas y estadística descriptiva, están en `docs/mediciones/perf/REPORTE-PERF.md` (k6, 50 VU / 30 s, 3 corridas en caliente y 3 en frío) y se discuten en el capítulo de evaluación empírica del documento académico. Una versión anterior de este ADR citaba cifras absolutas de usuarios concurrentes sin fuente; se retiraron por no ser verificables.

## Decisión
Se adopta **Java 21 + Spring Boot 4.1.0** para el backend, **Angular 22 + TypeScript + Bootstrap 5** para el frontend, **PostgreSQL 16** como motor relacional, **Redis 7** como caché, y **Docker/Docker Compose** para el despliegue.

## Consecuencias positivas
- Spring Security cubre RBAC y JWT sin código de seguridad manual adicional.
- Spring Data JPA/Hibernate reduce errores de sincronización de esquema (complementado con Flyway para control de versiones del esquema).
- Ecosistema maduro con SDKs oficiales para PayPal y almacenamiento S3-compatible.
- Java 21 LTS garantiza soporte a largo plazo; virtual threads mejoran la escalabilidad del módulo de mensajería en tiempo real.

## Consecuencias negativas
- Mayor complejidad y verbosidad inicial frente a frameworks minimalistas — mitigado con Lombok y revisión de código en equipo.
- Curva de aprendizaje de Spring Security — mitigada con incrementos graduales por módulo.

## Referencias
Corpus de requisitos Entrega 1A (`docs/requisitos/historico/entrega-1a.pdf`, sección 4.3); ISO/IEC 25010:2011 (atributos de calidad considerados: eficiencia de desempeño, mantenibilidad).
