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
# postgresql16-client (pg_dump): version 16 para igualar la version mayor del
# servidor postgres:16 (docker-compose.yml) y evitar desfases entre cliente y
# servidor. Lo invoca PgDumpEjecutor via ProcessBuilder, conectando por red al
# host de Postgres -- nunca via "docker exec" al contenedor sibling, ver
# docs/despliegue/BACKUP.md.
RUN apk add --no-cache wget postgresql16-client
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
