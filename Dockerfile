# --- ЭТАП 1: Сборка (Maven собирает проект внутри Docker) ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# --- ЭТАП 2: Финальный образ (как твой старый на Ubuntu) ---
FROM ubuntu:22.04

# Установка Java 17 и системных библиотек (как в твоем старом конфиге)
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    libgtk-3-0 \
    libdbus-glib-1-2 \
    libgbm1 \
    libasound2 \
    libnss3 \
    libatk-bridge2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Настройка переменных окружения Java
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

WORKDIR /app

# Копируем полученный WAR из этапа сборки
COPY --from=build /app/target/Raging-Horse-Tetris-4.2-0.0.1-SNAPSHOT.war app.war

# Создаем папки для скриншотов
RUN mkdir -p /app/src/main/resources/static/shots \
             /app/src/main/resources/static/mongoPrepareShots

# Пробрасываем порт
EXPOSE 8080

# Вставьте это перед ENTRYPOINT в Dockerfile
RUN zip -d app.war WEB-INF/lib/slf4j-simple-2.0.11.jar || true


# Запуск приложения (без команд установки браузеров)
ENTRYPOINT ["java", "-jar", "app.war", "--shotsPath=/app/src/main/resources/static/shots/", "--mongoPrepareShotsPath=/app/src/main/resources/static/mongoPrepareShots/"]
