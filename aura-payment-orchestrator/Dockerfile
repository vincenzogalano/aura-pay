FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests -pl aura-payment-orchestrator -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/aura-payment-orchestrator/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
