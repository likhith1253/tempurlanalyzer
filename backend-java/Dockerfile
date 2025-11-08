# Build stage
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# First copy only POM to leverage Docker cache
COPY pom.xml .

# Download dependencies (this step is cached unless POM changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests \
    -Dmaven.repo.remote=https://repo1.maven.org/maven2/

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Environment variables (can be overridden in docker-compose or runtime)
ENV SPRING_PROFILES_ACTIVE=prod
ENV SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb
ENV SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver
ENV SPRING_DATASOURCE_USERNAME=sa
ENV SPRING_DATASOURCE_PASSWORD=password
ENV SPRING_H2_CONSOLE_ENABLED=true

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
