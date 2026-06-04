# ---- Build stage ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -q package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for least-privilege execution
RUN addgroup -S copilot && adduser -S copilot -G copilot
USER copilot

COPY --from=build /build/target/ai-devops-copilot-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
