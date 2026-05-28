# ── Build & Run ──
FROM bellsoft/liberica-openjdk-alpine:25
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests -B
EXPOSE 8080
CMD java -jar target/*.jar