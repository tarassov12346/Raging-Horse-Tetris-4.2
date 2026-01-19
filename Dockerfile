# --- ЭТАП 1: Сборка проекта ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# --- ЭТАП 2: Создание финального образа ---
FROM ubuntu:22.04

# Установка Java 17 и необходимых библиотек для Playwright
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

# Настройка переменных окружения
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

WORKDIR /app

# Копируем собранный WAR из первого этапа (имя из твоего нового pom.xml)
COPY --from=build /app/target/Raging-Horse-Tetris-4.2-0.0.1-SNAPSHOT.war app.war

# Создаем папки для скриншотов (согласно твоим проперти)
RUN mkdir -p /app/src/main/resources/static/shots \
             /app/src/main/resources/static/mongoPrepareShots

# Установка браузеров Playwright (обязательно для работы в контейнере)
# Запускаем установку через CLI из уже скопированного архива
RUN java -cp app.war -Dloader.main=com.microsoft.playwright.CLI org.springframework.boot.loader.PropertiesLauncher install --with-deps chromium


# Пробрасываем порт
EXPOSE 8080

# Запуск приложения
# Мы переопределяем пути к папкам на прямые слэши, чтобы Linux их понимал
ENTRYPOINT ["java", "-jar", "app.war", "--shotsPath=/app/src/main/resources/static/shots/", "--mongoPrepareShotsPath=/app/src/main/resources/static/mongoPrepareShots/"]
