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
# Version fijada a 16 (misma major que el servidor postgres:16 de
# docker-compose/Render): el paquete generico "postgresql-client" de Alpine
# instala la ultima major disponible (hoy 18), y pg_dump de una version mas
# nueva agrega al dump sentencias SET de configuracion que no existen en
# versiones de servidor anteriores (ej. "transaction_timeout", agregado en
# PG17) -- pg_restore fallaba con "unrecognized configuration parameter" al
# restaurar contra el servidor 16.
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
