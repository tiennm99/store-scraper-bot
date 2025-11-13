FROM gradle:8-jdk21-alpine AS deps
WORKDIR /build
RUN --mount=type=bind,source=build.gradle.kts,target=build.gradle.kts \
    --mount=type=bind,source=settings.gradle.kts,target=settings.gradle.kts \
    --mount=type=cache,target=/root/.gradle \
    gradle dependencies -x check -x test --no-daemon --parallel --build-cache

FROM deps as package
WORKDIR /build
COPY ./src src/
RUN --mount=type=bind,source=build.gradle.kts,target=build.gradle.kts \
    --mount=type=bind,source=settings.gradle.kts,target=settings.gradle.kts \
    --mount=type=cache,target=/root/.gradle \
    gradle build -x check -x test --no-daemon --parallel --build-cache

FROM eclipse-temurin:21-jre-jammy AS final
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
COPY --from=package /build/build/libs/*-all.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
