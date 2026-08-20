# ==============================================================================
# ЭТАП 1: Сборка gRPC-классов и JAR-артефакта (Heavyweight Builder)
# ==============================================================================
# Используем официальный образ Maven с установленной Java 21 (Eclipse Temurin)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Копируем дескриптор сборки для кэширования слоев зависимостей Maven
COPY pom.xml ./

# Загружаем зависимости в изолированный слой Docker.
# Если pom.xml не менялся, этот шаг мгновенно пропустится при пересборках кода.
RUN mvn dependency:go-offline -B

# 2. Копируем исходный код приложения (включая .proto файлы для gRPC)
COPY src ./src

# Компилируем protobuf-модели, gRPC-клиенты и упаковываем Spring Boot приложение в JAR
# Тесты пропускаем (-DskipTests), так как для них нужен запущенный Hazelcast-кластер
RUN mvn clean package -DskipTests

# ==============================================================================
# ЭТАП 2: Финальный высокопроизводительный образ (Ultra-Low Latency Runtime)
# ==============================================================================
# Берем официальный образ Playwright, в котором уже настроены все нативные Linux-библиотеки
# для работы Chromium/Webkit в headless-режиме (на базе Ubuntu 22.04 LTS)
FROM mcr.microsoft.com/playwright:v1.40.0-focal

# Устанавливаем официальную среду выполнения Temurin OpenJDK 21 JRE в Ubuntu
RUN apt-get update && apt-get install -y wget apt-transport-https gnupg zip && \
    mkdir -p /etc/apt/keyrings && \
    wget -O - https://adoptium.net | gpg --dearmor > /etc/apt/keyrings/adoptium.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://adoptium.net focal main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && apt-get install -y temurin-21-jre && \
    rm -rf /var/lib/apt/lists/*

# Прописываем системные переменные, чтобы система видела установленную Java
ENV JAVA_HOME=/usr/lib/jvm/temurin-21-jre-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# Переменная для Playwright: сообщаем, что браузеры уже вшиты в базовый образ, качать заново не нужно
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

WORKDIR /app

# Копируем готовый JAR-файл из этапа сборки
COPY --from=build /app/target/Raging-Horse-Tetris-4.2-0.0.1-SNAPSHOT.jar app.jar

# СОХРАНЯЕМ КОСТЫЛЬ ОЧИСТКИ ЛОГГЕРА (адаптирован под внутреннюю структуру Spring Boot JAR)
# Удаляет конфликтующий slf4j-simple, если он прилетел транзитивно из сторонних библиотек
RUN zip -d app.jar BOOT-INF/lib/slf4j-simple-*.jar || true

# Создаем физические директории внутри Linux-контейнера для сохранения скриншотов дампов игр
RUN mkdir -p /app/src/main/resources/static/shots \
             /app/src/main/resources/static/mongoPrepareShots

# Открываем порты наружу:
# 8080 - HTTP / WebSockets (STOMP)
# 5701 - Внутренний порт кластеризации Hazelcast
EXPOSE 8080 5701

# Точка входа для запуска игрового движка реального времени
# Настройки JVM:
# 1. -XX:+UseContainerSupport - корректное чтение лимитов памяти/процессора из Docker
# 2. -XX:+UseZGC - Garbage Collector со сверхнизкой задержкой (паузы < 1мс) для бесперебойных веб-сокетов
# 3. --add-opens... - обязательные разрешения на рефлексию памяти для корректной работыHazelcast
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:+UseZGC", \
            "--add-modules", "java.se", \
            "--add-exports", "java.base/jdk.internal.ref=ALL-UNNAMED", \
            "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
            "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", \
            "--add-opens", "java.management/sun.management=ALL-UNNAMED", \
            "--add-opens", "jdk.management/com.sun.management.internal=ALL-UNNAMED", \
            "-jar", "app.jar", \
            "--shotsPath=/app/src/main/resources/static/shots/", \
            "--mongoPrepareShotsPath=/app/src/main/resources/static/mongoPrepareShots/"]
