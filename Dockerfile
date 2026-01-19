# --- ЭТАП 1: Сборка ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# --- ЭТАП 2: Финальный образ (как ваш старый, но на Ubuntu 22.04) ---
FROM ubuntu:22.04

# Установка Java 17 и системных библиотек (аналогично вашему старому списку + доп. для Playwright)
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    libgtk-3-0 \
    libdbus-glib-1-2 \
    libgbm1 \
    libasound2 \
    libnss3 \
    libatk-bridge2.0-0 \
    libx11-xcb1 \
    libxcb-dri3-0 \
    libdrm2 \
    libice6 \
    libsm6 \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

WORKDIR /app

# Копируем WAR из сборщика
COPY --from=build /app/target/Raging-Horse-Tetris-4.2-0.0.1-SNAPSHOT.war app.war

# Создаем папки (как в вашем новом конфиге)
RUN mkdir -p /app/src/main/resources/static/shots \
             /app/src/main/resources/static/mongoPrepareShots

# КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ:
# Используем Maven-плагин прямо в первом этапе или PropertiesLauncher во втором.
# Но проще всего заставить Playwright скачать браузер в стандартную папку ~/.cache/ms-playwright
RUN java -cp app.war -Dloader.main=com.microsoft.playwright.CLI org.springframework.boot.loader.PropertiesLauncher install chromium

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.war", "--shotsPath=/app/src/main/resources/static/shots/", "--mongoPrepareShotsPath=/app/src/main/resources/static/mongoPrepareShots/"]
