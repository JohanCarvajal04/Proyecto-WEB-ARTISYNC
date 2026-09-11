# --- Etapa de build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Etapa de ejecucion ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# postgresql18-client (pg_dump): pg_dump exige que el cliente sea igual o mas
# nuevo que el servidor ("aborting because of server version mismatch" si no).
# postgres:16 en docker-compose.yml y el Postgres 18 gestionado de Render
# quedan ambos cubiertos por un cliente 18 (mas nuevo que ambos); un cliente
# 16 no puede respaldar el servidor 18 de Render. Lo invoca PgDumpEjecutor via
# ProcessBuilder, conectando por red al host de Postgres -- nunca via
# "docker exec" al contenedor sibling, ver docs/despliegue/BACKUP.md.
RUN apk add --no-cache wget postgresql18-client
# Sin esto la JVM corre en UTC (el default de la imagen base), 5 horas por
# delante de Ecuador. Como el frontend envia LocalDateTime "de pared" sin
# offset (p.ej. datetime-local de Sorteo/Pedido), @Future los comparaba
# contra un reloj adelantado y rechazaba horas de hoy que ya eran futuras en
# hora local -- y el cierre automatico de sorteos (SorteoScheduler) se
# retrasaba el mismo margen.
ENV TZ=America/Guayaquil
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Flags para arranque rapido en CPU compartida/limitada (ej. Render free
# tier): menos compilacion JIT tiered y GC serial (mas liviano con pocos
# nucleos) que la config por defecto orientada a servidores con mas CPU.
ENTRYPOINT ["java", "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
