FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/product-biz-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
