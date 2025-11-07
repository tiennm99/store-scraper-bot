FROM amazoncorretto:21.0.5-alpine AS deps
WORKDIR /build
COPY --chmod=0755 gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon

FROM deps AS package
WORKDIR /build
COPY ./src src/
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/build/build \
    ./gradlew shadowJar -x check -x test --no-daemon --parallel --build-cache \
    && cp build/libs/*-all.jar app.jar

FROM amazoncorretto:21.0.5-alpine AS final
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
WORKDIR /app
COPY --from=package --chown=appuser:appgroup /build/app.jar app.jar
ENTRYPOINT [ "sh", "-c", "java ${JAVA_OPTS} -jar app.jar" ]
