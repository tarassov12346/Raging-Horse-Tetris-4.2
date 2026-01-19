# --- ЭТАП 1: Сборка ---
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Копируем настройки и зависимости для кэширования
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходники и собираем проект
COPY src ./src
RUN mvn clean package -DskipTests

# --- ЭТАП 2: Запуск ---
# Используем образ Playwright, так как в нем уже есть все нужные библиотеки для браузеров
FROM ://mcr.microsoft.com

# Устанавливаем Java 17 (так как в образе playwright может быть другая версия)
RUN apt-get update && apt-get install -y openjdk-17-jre && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копируем собранный файл из первого этапа
# Имя файла берется из pom.xml: artifactId-version.war
COPY --from=build /app/target/Raging-Horse-Tetris-4.2-0.0.1-SNAPSHOT.war app.war

# Создаем папки для скриншотов, которые указаны в твоем properties
RUN mkdir -p src/main/resources/static/shots src/main/resources/static/mongoPrepareShots

# Настройки окружения
ENV SERVER_PORT=8080
EXPOSE 8080

# Команда запуска
ENTRYPOINT ["java", "-jar", "app.war"]
