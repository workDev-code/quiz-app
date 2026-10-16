# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
RUN apk add --no-cache maven bash
COPY pom.xml .
RUN mvn -B -ntp -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/target/quiz-backend.jar /app/quiz-backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/quiz-backend.jar"]
