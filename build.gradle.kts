plugins {
    `java-library`
}

group = "co.surumene"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    // Independent guard's API is a shared, compile-only contract (not shaded).
    compileOnly(project(":admission-guard"))
    testImplementation(project(":admission-guard"))
    // Paper API is never bundled into the plugin JAR; tests use its event types.
    compileOnly("io.papermc.paper:paper-api:26.2.build.134-stable")
    testImplementation("io.papermc.paper:paper-api:26.2.build.134-stable")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-ea")
}
