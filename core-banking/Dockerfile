# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

# Copy maven wrapper & pom to cache dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build production jar
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# Add unprivileged user for security best practice
RUN useradd -m -s /bin/bash appuser
USER appuser

# Copy built fat JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
