FROM eclipse-temurin:21-alpine AS deps
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
    ./gradlew build -x check -x test --no-daemon --parallel --build-cache && \
    mv build/libs/*-all.jar app.jar

FROM eclipse-temurin:21-jre-alpine AS final
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
COPY --from=package /build/app.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
