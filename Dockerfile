FROM eclipse-temurin:21.0.9_10-jdk-alpine AS deps
WORKDIR /build
COPY --chmod=0755 gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies -x check -x test --no-daemon --parallel --build-cache

FROM deps AS package
WORKDIR /build
COPY ./src src/
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/build/build \
    ./gradlew distTar -x check -x test --no-daemon --parallel --build-cache && \
    cp build/distributions/*.tar.gz app.tar.gz

FROM eclipse-temurin:21.0.9_10-jre-alpine AS final
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser
WORKDIR /app
COPY --from=package /build/app.tar.gz app.tar.gz
RUN tar -xzf app.tar.gz --strip-components=1 && rm app.tar.gz
ENTRYPOINT ["sh", "-c", "cd /app && ./bin/store-scraper-bot"]
