FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew \
    && ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

#docker buildx build --platform linux/amd64,linux/arm64 -t rlarbdud/st-back:0.1.2 --push .
#docker buildx build --platform linux/amd64,linux/arm64 -t rlarbdud/st-front:0.2 --push .
#docker buildx build --platform linux/amd64,linux/arm64 -t rlarbdud/st-forecast:0.1 --push .
#docker buildx build --platform linux/amd64 , linux/arm64 -t rlarbdud/st-forecast-base:0.1 -t rlarbdud/st-forecast-base:latest --push .
