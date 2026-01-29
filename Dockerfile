FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

RUN curl -fsSL https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -o /usr/local/bin/lein && \
    chmod +x /usr/local/bin/lein && \
    lein self-install

COPY project.clj ./
RUN lein deps

COPY src/ ./src/
COPY resources/ ./resources/

RUN lein uberjar

FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/target/*-standalone.jar app.jar

EXPOSE 3000

CMD ["java", "-jar", "app.jar"]
