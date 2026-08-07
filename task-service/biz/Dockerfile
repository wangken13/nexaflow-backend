FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8108
ENTRYPOINT ["java","-jar","/app.jar"]
