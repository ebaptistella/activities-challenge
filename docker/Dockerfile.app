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
COPY test/ ./test/

RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=cache,target=/root/.lein \
    lein test

# Build: clean, cljsbuild, resource (copy to target/classes). We save public/js from target/classes
# before the uberjar (the prep-task compile can clear target/classes). After the uberjar, we inject it into the JAR.
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=cache,target=/root/.lein \
    lein do clean, cljsbuild once prod, resource && \
    mkdir -p /tmp/jar_add/public && \
    cp -r /app/target/classes/public/js /tmp/jar_add/public/ && \
    lein uberjar && \
    cd /tmp/jar_add && jar uf /app/target/challenge-0.1.0-SNAPSHOT-standalone.jar public/js/

# Verify app.js is in the JAR
RUN jar tf /app/target/challenge-0.1.0-SNAPSHOT-standalone.jar | grep -q 'public/js/app\.js' || \
    (echo "ERROR: public/js/app.js not in JAR" && exit 1)

FROM public.ecr.aws/docker/library/eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/target/challenge-0.1.0-SNAPSHOT-standalone.jar /app/challenge-0.1.0-SNAPSHOT-standalone.jar

EXPOSE 3000

CMD ["sh", "-c", "java -jar /app/challenge-0.1.0-SNAPSHOT-standalone.jar"]