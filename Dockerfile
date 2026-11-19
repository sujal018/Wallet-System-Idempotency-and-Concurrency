# Optional: used for deploying to Render / Railway / any Docker host
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV H2_CONSOLE_ENABLED=false
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
