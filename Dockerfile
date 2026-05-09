# Etapa 1: Construcción
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Creación de usuario no root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiamos el .jar generado desde la etapa de construcción
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

# Inyectamos los límites de memoria directamente en el comando.
# -Xmx256m: Límite máximo de Heap (Deja ~256MB libres para el sistema y la JVM).
# -Xms128m: Memoria inicial asignada.
# -XX:+UseContainerSupport: Optimiza el comportamiento de Java dentro de Docker.
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseContainerSupport", "-jar", "app.jar"]