FROM maven:3.8.7-openjdk-18-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-Djasypt.encryptor.password=${JASYPT_ENCRYPTOR_PASSWORD}", "-jar", "app.jar"]

EXPOSE 8080