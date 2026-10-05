# ==============================================================================
# Build Stage: Maven + Eclipse Temurin JDK 17
# ==============================================================================
FROM maven:3.9.8-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Pre-fetch dependencies to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build executable jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Runtime Stage: Lightweight Eclipse Temurin JRE 17 Alpine
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Run as non-root user for security best practices
RUN addgroup -S spring && adduser -S spring -G spring

# Copy jar from builder stage
COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar

USER spring:spring

EXPOSE 8080

# Production-grade JVM memory and GC settings
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
