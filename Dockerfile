# --- ЭТАП 1: Сборка ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# --- ЭТАП 2: Финальный образ ---
FROM ubuntu:22.04

# Добавляем zip в список установки, чтобы удалить конфликтный файл
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    zip \
    libgtk-3-0 \
    libdbus-glib-1-2 \
    libgbm1 \
    libasound2 \
    libnss3 \
    libatk-bridge2.0-0 \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

WORKDIR /app

COPY --from=build /app/target/Raging-Horse-Tetris-4.2-0.0.1-SNAPSHOT.war app.war

# КОСТЫЛЬ ДЛЯ УДАЛЕНИЯ КОНФЛИКТНОЙ БИБЛИОТЕКИ
# Эта команда удаляет slf4j-simple прямо из собранного WAR-файла
RUN zip -d app.war WEB-INF/lib/slf4j-simple-2.0.11.jar || true

RUN mkdir -p /app/src/main/resources/static/shots \
             /app/src/main/resources/static/mongoPrepareShots

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.war", "--shotsPath=/app/src/main/resources/static/shots/", "--mongoPrepareShotsPath=/app/src/main/resources/static/mongoPrepareShots/"]
