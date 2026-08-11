FROM eclipse-temurin:21-jre

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 18112

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
