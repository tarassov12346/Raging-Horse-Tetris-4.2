# --- ЭТАП 1: Сборка ---
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Кэшируем зависимости
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Сборка
COPY src ./src
RUN mvn clean package -DskipTests

# --- ЭТАП 2: Запуск ---
# Используем официальный образ Playwright
FROM ://mcr.microsoft.com

# Устанавливаем Java 17 (в образе Playwright по умолчанию может быть другая версия)
RUN apt-get update && apt-get install -y openjdk-17-jre && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копируем WAR файл (имя точно по твоему pom.xml)
COPY --from=build /app/target/Raging-Horse-Tetris-4.2-0.0.1-SNAPSHOT.war app.war

# Исправляем пути для Linux, переопределяя проперти через ENV
# В Docker используем прямые слэши /
ENV SHOTSPATH=/app/static/shots/
ENV MONGOPREPARESHOTSPATH=/app/static/mongoPrepareShots/
ENV EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:1111/eureka

# Создаем папки
RUN mkdir -p /app/static/shots /app/static/mongoPrepareShots

EXPOSE 8080

# Запуск с переопределением путей, чтобы не менять сам файл application.properties
ENTRYPOINT ["java", "-jar", "app.war", "--shotsPath=${SHOTSPATH}", "--mongoPrepareShotsPath=${MONGOPREPARESHOTSPATH}"]
