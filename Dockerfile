FROM bellsoft/liberica-openjdk-alpine:21
LABEL authors="antonhorobets"

WORKDIR /app

COPY build/libs/JIkvictBackend-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
