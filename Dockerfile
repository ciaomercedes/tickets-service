# ---------- Etapa 1: compilar con Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- Etapa 2: imagen liviana solo con el .jar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/tickets-service.jar app.jar

# Carpeta donde se monta el EFS (docker run -v /mnt/efs:/app/efs ...)
ENV EFS_PATH=/app/efs
RUN mkdir -p /app/efs
VOLUME /app/efs

EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=America/Santiago", "-jar", "/app/app.jar"]
