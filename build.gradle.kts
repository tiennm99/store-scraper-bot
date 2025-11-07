plugins {
    application
    idea
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.miti99"
version = "1.0-SNAPSHOT"

application {
    mainClass = "com.miti99.storescraperbot.Main"
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    testCompileOnly {
        extendsFrom(configurations.testAnnotationProcessor.get())
    }
}

dependencies {
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    implementation("com.couchbase.client:java-client:3.7.6")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.24.3")
    implementation("org.telegram:telegrambots-client:8.0.0")
    implementation("org.telegram:telegrambots-extensions:8.0.0")
    implementation("org.telegram:telegrambots-longpolling:8.0.0")

    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}
