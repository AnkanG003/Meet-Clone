# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B || true

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Extract Spring Boot Layers
FROM eclipse-temurin:17-jre-alpine AS extractor
WORKDIR /extracted
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: Minimal Runtime
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

COPY --from=extractor /extracted/dependencies/ ./
COPY --from=extractor /extracted/spring-boot-loader/ ./
COPY --from=extractor /extracted/snapshot-dependencies/ ./
COPY --from=extractor /extracted/application/ ./

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8084
ENTRYPOINT ["sh", "-c", "if [ -d org/springframework/boot/loader/launch ]; then LAUNCHER=org.springframework.boot.loader.launch.JarLauncher; else LAUNCHER=org.springframework.boot.loader.JarLauncher; fi; exec java $JAVA_OPTS $LAUNCHER"]
