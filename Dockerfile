FROM openjdk:8-jre-slim

WORKDIR /app

COPY target/stock-watchlist-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
