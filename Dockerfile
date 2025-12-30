# Build stage (Maven + JDK 17)
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage (use a reliable Temurin JRE)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install curl used by HEALTHCHECK
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy built jar
COPY --from=build /app/target/pastebin-lite-*.jar app.jar

# Run as non-root user
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/healthz || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
