FROM bellsoft/liberica-openjdk-debian:21
LABEL authors="antonhorobets"

RUN apt-get update && \
    apt-get install -y tzdata ca-certificates openssl && \
    ln -sf /usr/share/zoneinfo/Europe/Prague /etc/localtime && \
    echo "Europe/Prague" > /etc/timezone && \
    update-ca-certificates && \
    JAVA_CACERTS=$(find /usr/lib/jvm -name cacerts | head -1) && \
    echo "Java cacerts location: $JAVA_CACERTS" && \
    openssl s_client -connect ldap.stuba.sk:636 -showcerts </dev/null 2>/dev/null | \
      awk '/BEGIN CERT/,/END CERT/ {print; if (/END CERT/) {print "---SPLIT---"}}' | \
      csplit -s -z -f /tmp/cert- - '/---SPLIT---/' '{*}' && \
    for cert_file in /tmp/cert-*; do \
      if [ -s "$cert_file" ]; then \
        cert_alias="ldap-cert-$(basename $cert_file)"; \
        echo "Importing $cert_alias"; \
        keytool -import -trustcacerts -keystore "$JAVA_CACERTS" \
          -storepass changeit -noprompt -alias "$cert_alias" -file "$cert_file" 2>&1 || true; \
      fi; \
    done && \
    rm -f /tmp/cert-* && \
    keytool -list -keystore "$JAVA_CACERTS" -storepass changeit | grep -i ldap && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENV TZ=Europe/Prague
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Europe/Prague"

WORKDIR /app
COPY build/libs/JIkvictBackend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
