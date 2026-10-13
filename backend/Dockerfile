# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn 2>/dev/null || true
COPY mvnw . 2>/dev/null || true
RUN apk add --no-cache maven \
 && mvn -B -ntp -q -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -B -ntp -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/quiz-backend.jar /app/quiz-backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/quiz-backend.jar"]
