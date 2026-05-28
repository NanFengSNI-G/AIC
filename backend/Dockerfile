# ── Build & Run ──
FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests -B
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/*.jar"]