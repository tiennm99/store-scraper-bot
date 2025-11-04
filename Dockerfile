FROM amazoncorretto:21.0.5 AS deps
WORKDIR /build
COPY --chmod=0755 gradlew gradlew
COPY gradle/ gradle/
RUN --mount=type=bind,source=build.gradle.kts,target=build.gradle.kts \
    --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon

FROM deps AS package
WORKDIR /build
COPY ./src src/
RUN --mount=type=bind,source=build.gradle.kts,target=build.gradle.kts \
    --mount=type=cache,target=/root/.gradle \
    ./gradlew shadowJar -x test --no-daemon
RUN cp build/libs/*-all.jar app.jar

FROM amazoncorretto:21.0.5 AS final
COPY --from=package build/app.jar app.jar
ENTRYPOINT [ "sh", "-c", "java ${JAVA_OPTS} -jar app.jar" ]
