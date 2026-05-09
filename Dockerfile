
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /target/*.jar app.jar

ENV JAVA_OPTS="-Xmx512M -Xms256M"

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]