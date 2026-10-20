# Multi-stage Dockerfile for HRGenius Spring Boot Backend
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy wrapper and POM for faster layer caching
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build production package
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/uploads \
    && chown -R appuser:appgroup /app
USER appuser

# Copy jar from builder stage
COPY --from=builder /app/target/hrgenius-backend-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

# Sized for small containers (e.g. 512 MB free hosting); override JAVA_OPTS if you have more memory
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
