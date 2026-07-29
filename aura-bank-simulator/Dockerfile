FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN apk add --no-cache maven && mvn package -DskipTests -pl aura-bank-simulator -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/aura-bank-simulator/target/*-exec.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
