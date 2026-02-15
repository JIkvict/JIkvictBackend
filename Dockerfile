FROM bellsoft/liberica-openjdk-debian:21
LABEL authors="antonhorobets"

RUN apt-get update && \
    apt-get install -y tzdata ca-certificates openssl ldap-utils && \
    ln -sf /usr/share/zoneinfo/Europe/Prague /etc/localtime && \
    echo "Europe/Prague" > /etc/timezone && \
    update-ca-certificates && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENV TZ=Europe/Prague
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Europe/Prague"

WORKDIR /app
COPY build/libs/JIkvictBackend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
