FROM public.ecr.aws/docker/library/eclipse-temurin:21-jdk AS builder

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* && \
    curl -fsSL https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -o /usr/local/bin/lein && \
    chmod +x /usr/local/bin/lein && \
    lein self-install

COPY project.clj ./
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=cache,target=/root/.lein \
    lein deps

COPY src/ ./src/
COPY resources/ ./resources/
COPY config/ ./config/
COPY test/ ./test/

RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=cache,target=/root/.lein \
    lein test

RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=cache,target=/root/.lein \
    lein uberjar

FROM public.ecr.aws/docker/library/eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/target/challenge-0.1.0-SNAPSHOT-standalone.jar /app/challenge-0.1.0-SNAPSHOT-standalone.jar

EXPOSE 3000

CMD ["sh", "-c", "java -jar /app/challenge-0.1.0-SNAPSHOT-standalone.jar"]