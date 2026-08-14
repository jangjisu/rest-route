# syntax=docker/dockerfile:1

# --- Build stage ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# 의존성 캐시 레이어: 빌드 스크립트와 wrapper 를 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 후 실행 가능 jar 빌드 (이미지 빌드 시 테스트는 제외)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# --- Runtime stage ---
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# H2 file DB 저장 위치 (compose 에서 volume 마운트)
RUN mkdir -p /app/data

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
