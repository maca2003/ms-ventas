# ---------- Etapa 1: compilación ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Primero solo el pom.xml: las dependencias quedan en caché mientras no cambie
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: ejecución ----------
# Imagen liviana solo con el JRE (sin Maven ni código fuente)
FROM eclipse-temurin:21-jre-alpine

# Usuario sin privilegios
RUN addgroup -S -g 1001 app && adduser -S -u 1001 -G app app
WORKDIR /app

COPY --from=build --chown=1001:1001 /app/target/ms-ventas-1.0.0.jar app.jar
USER 1001

EXPOSE 8083

# Verifica que el servicio responde (Docker/Swarm reinicia el contenedor si falla)
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD ["wget", "-q", "-O", "/dev/null", "http://localhost:8083/api/ventas"]

# La JVM usa como máximo el 75% de la memoria asignada al contenedor
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
