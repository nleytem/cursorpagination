# ==================== Build Stage ====================
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy Maven files first for better layer caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests --no-transfer-progress

# ==================== Runtime Stage ====================
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create non-root user (good practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy only the built artifact
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]