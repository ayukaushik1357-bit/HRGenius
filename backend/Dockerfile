# Multi-stage Dockerfile for HRGenius Spring Boot Backend
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy POM and download dependencies for faster layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production package
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy jar from builder stage
COPY --from=builder /app/target/hrgenius-backend-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
