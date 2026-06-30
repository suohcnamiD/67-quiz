FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY build/libs/*.jar server.jar

ENTRYPOINT ["java", "-jar", "server.jar"]