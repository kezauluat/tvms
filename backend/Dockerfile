# ---- build stage ----
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B -DskipTests package

# ---- run stage: small JRE image, non-root user ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 tvms
COPY --from=build /app/target/*.jar app.jar
USER tvms
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
