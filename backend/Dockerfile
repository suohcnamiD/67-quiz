FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY backend/build/libs/*.jar server.jar

ENTRYPOINT ["java", "-jar", "server.jar"]