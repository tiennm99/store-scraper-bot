FROM amazoncorretto:21.0.5-alpine AS deps

WORKDIR /build

COPY --chmod=0755 gradlew gradlew
COPY gradle/ gradle/

RUN --mount=type=bind,source=build.gradle.kts,target=build.gradle.kts \
    --mount=type=bind,source=settings.gradle.kts,target=settings.gradle.kts \
    --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon --parallel --build-cache



FROM deps as package

WORKDIR /build

COPY ./src src/
RUN --mount=type=bind,source=build.gradle.kts,target=build.gradle.kts \
    --mount=type=bind,source=settings.gradle.kts,target=settings.gradle.kts \
    --mount=type=cache,target=/root/.gradle \
    ./gradlew clean build -x check -x test --no-daemon --parallel --build-cache && \
    mv build/libs/*-all.jar build/libs/app.jar || \
    mv build/libs/*.jar build/libs/app.jar



FROM amazoncorretto:21.0.5-alpine AS final

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

COPY --from=package /build/build/libs/app.jar app.jar

ENTRYPOINT ["java", "${JAVA_OPTS}", "-jar" "app.jar"]
