# --- ЭТАП 1: Сборка ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Копируем только помник для кэширования слоев
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Собираем проект
COPY src ./src
RUN mvn clean package -DskipTests

# --- ЭТАП 2: Запуск ---
# Используем прямой адрес без переменных и скрытых символов
FROM ubuntu:20.04

# Установка Java 17 (в образе Playwright может быть другая версия)
RUN apt-get update && apt-get install -y openjdk-17-jre && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копируем WAR (убедитесь, что имя совпадает с вашим artifactId и version)
COPY --from=build /app/target/Raging-Horse-Tetris-4.2-0.0.1-SNAPSHOT.war app.war

# Создаем папки для скриншотов
RUN mkdir -p /app/static/shots /app/static/mongoPrepareShots

EXPOSE 8080

# Запуск с явным указанием путей для Linux (вместо Windows-путей из properties)
ENTRYPOINT ["java", "-jar", "app.war", "--shotsPath=/app/static/shots/", "--mongoPrepareShotsPath=/app/static/mongoPrepareShots/"]
