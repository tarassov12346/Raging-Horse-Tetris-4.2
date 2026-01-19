# Этап 1: Сборка
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск
FROM ://mcr.microsoft.com

# Установка Java 17
RUN apt-get update && apt-get install -y openjdk-17-jre && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копируем собранный WAR (имя из вашего pom.xml)
COPY --from=build /app/target/Raging-Horse-Tetris-4.2-0.0.1-SNAPSHOT.war app.war

# Создаем папки для скриншотов (в Linux путях)
RUN mkdir -p /app/static/shots /app/static/mongoPrepareShots

# Переопределяем пути из application.properties на Linux-формат
ENV SHOTS_PATH=/app/static/shots/
ENV MONGO_PATH=/app/static/mongoPrepareShots/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.war", "--shotsPath=${SHOTS_PATH}", "--mongoPrepareShotsPath=${MONGO_PATH}"]
