# ── Stage 1: Build ─────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build

# Cache dependencies (this layer changes rarely)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Build application (this layer changes with code)
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# Extract layers for better Docker caching
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ── Stage 2: Runtime ───────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

# Security: non-root user
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# Copy layers in order of change frequency (least → most)
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
