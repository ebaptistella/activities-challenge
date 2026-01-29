FROM public.ecr.aws/docker/library/eclipse-temurin:21-jdk AS builder

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* && \
    curl -fsSL https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -o /usr/local/bin/lein && \
    chmod +x /usr/local/bin/lein && \
    lein self-install

COPY project.clj ./
RUN lein deps

COPY src/ ./src/
COPY resources/ ./resources/

RUN lein uberjar

FROM public.ecr.aws/docker/library/eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/target /app/target

EXPOSE 3000

CMD ["sh", "-c", "java -jar /app/target/uberjar/*-standalone.jar"]
