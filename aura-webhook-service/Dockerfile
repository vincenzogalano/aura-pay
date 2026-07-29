FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN apk add --no-cache maven && mvn package -DskipTests -pl aura-webhook-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/aura-webhook-service/target/*-exec.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
