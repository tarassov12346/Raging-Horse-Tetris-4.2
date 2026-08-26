# ==============================================================================
# Этап 1: Сборка игрового ядра (Твой эталонный Builder-слой)
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Копируем дескриптор сборки для кэширования слоев зависимостей Maven
COPY pom.xml ./

# Загружаем зависимости в изолированный слой Docker
RUN mvn dependency:go-offline -B

# 2. Копируем исходный код приложения (включая .proto файлы для gRPC)
COPY src ./src

# Компилируем protobuf-модели, gRPC-клиенты и упаковываем Spring Boot приложение в JAR
RUN mvn clean package -DskipTests

# ==============================================================================
# Этап 2: Финальный рантайм (Твоя база v1.40.0-focal + чистая Java 21)
# ==============================================================================
FROM mcr.microsoft.com/playwright:v1.40.0-focal
WORKDIR /app

# Неинтерактивный режим, чтобы apt-get не запрашивал выбор таймзоны при сборке
ENV DEBIAN_FRONTEND=noninteractive

# Добавляем официальный репозиторий Ubuntu для установки Java 21 (так как в чистой Focal её нет)
# И устанавливаем утилиту zip для твоего костыля логгера
RUN apt-get update && apt-get install -y software-properties-common && \
    add-apt-repository -y ppa:openjdk-r/ppa && \
    apt-get update && apt-get install -y \
    openjdk-21-jre-headless \
    zip \
    wget \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Прописываем системные переменные, чтобы среда безошибочно видела Java
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# Переменная для Playwright: сообщаем, что браузеры уже вшиты в базовый образ
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# Копируем собранный JAR-файл игры из первого этапа сборки
COPY --from=build /app/target/Raging-Horse-Tetris-4.2-0.0.1-SNAPSHOT.jar app.jar

# ТВОЙ КОСТЫЛЬ ОЧИСТКИ ЛОГГЕРА (удаляет конфликтующий slf4j-simple на лету)
RUN zip -d app.jar BOOT-INF/lib/slf4j-simple-*.jar || true

# Создаем физические директории внутри Linux-контейнера для сохранения скриншотов дампов игр
RUN mkdir -p /app/src/main/resources/static/shots \
             /app/src/main/resources/static/mongoPrepareShots && \
    chmod -R 777 /app/src/main/resources/static

# Открываем порты: 8080 для веб-трафика/WebSocket, 5701 для кластера Hazelcast
EXPOSE 8080 5701

# Точка входа для запуска игрового движка реального времени с твоими флагами Hazelcast и путями
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:+UseZGC", \
            "--add-modules", "java.se", \
            "--add-exports", "java.base/jdk.internal.ref=ALL-UNNAMED", \
            "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
            "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", \
            "--add-opens", "java.base/sun.indigo.ch=ALL-UNNAMED", \
            "--add-opens", "java.base/sun.nio.cs=ALL-UNNAMED", \
            "--add-opens", "java.base/sun.security.action=ALL-UNNAMED", \
            "--add-opens", "java.base/sun.util.calendar=ALL-UNNAMED", \
            "--add-opens", "java.management/sun.management=ALL-UNNAMED", \
            "--add-opens", "jdk.management/com.sun.management.internal=ALL-UNNAMED", \
            "-jar", "app.jar", \
            "--shotsPath=/app/src/main/resources/static/shots/", \
            "--mongoPrepareShotsPath=/app/src/main/resources/static/mongoPrepareShots/"]
