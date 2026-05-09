# Etapa 1: Construcción
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 1. Creamos el usuario
# 2. Creamos la estructura de carpetas de tu proyecto
# 3. Le damos al usuario 'spring' la propiedad de toda la carpeta /app
RUN addgroup -S spring && adduser -S spring -G spring && \
    mkdir -p /app/uploads/safe /app/uploads/quarantine && \
    chown -R spring:spring /app

# Ahora sí, cambiamos al usuario seguro
USER spring:spring

# Copiamos el .jar (usando --chown para mantener los permisos)
COPY --chown=spring:spring --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseContainerSupport", "-jar", "app.jar"]