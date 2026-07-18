# Stage 1: Build the Maven application
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first to cache this layer
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create execution container
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/rag4tickets-0.0.1-SNAPSHOT.jar app.jar

# Port exposed by Spring Boot
EXPOSE 8080

# Run Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
