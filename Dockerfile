# Сборка
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Запуск - используем ARG чтобы обойти ошибку парсинга ://
ARG REGISTRY=mcr.microsoft.com
FROM ${REGISTRY}/playwright/java:v1.40.0-jammy

# Установка Java 17
RUN apt-get update && apt-get install -y openjdk-17-jre && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копируем ваш WAR (название строго по pom.xml)
COPY --from=build /app/target/Raging-Horse-Tetris-4.2-0.0.1-SNAPSHOT.war app.war

# Создаем папки для скриншотов
RUN mkdir -p /app/static/shots /app/static/mongoPrepareShots

EXPOSE 8080

# Запуск с исправлением путей (в Linux нужны прямые слэши /)
ENTRYPOINT ["java", "-jar", "app.war", "--shotsPath=/app/static/shots/", "--mongoPrepareShotsPath=/app/static/mongoPrepareShots/"]
