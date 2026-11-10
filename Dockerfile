# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: imagen liviana para ejecutar ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Punto de montaje del EFS (docker run -v /mnt/efs:/app/efs)
RUN mkdir -p /app/efs
ENV EFS_PATH=/app/efs

COPY --from=build /build/target/tickets-app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
