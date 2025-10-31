FROM bellsoft/liberica-openjdk-alpine:21
LABEL authors="antonhorobets"

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Europe/Prague /etc/localtime && \
    echo "Europe/Prague" > /etc/timezone

ENV TZ=Europe/Prague
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Europe/Prague"

WORKDIR /app
COPY build/libs/JIkvictBackend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
