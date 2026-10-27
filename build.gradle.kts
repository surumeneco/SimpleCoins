plugins {
    `java-library`
}

group = "co.surumene"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-ea")
}

tasks.register<JavaExec>("domainContractCheck") {
    group = "verification"
    description = "Run assert-enabled, dependency-free Domain contract checks."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("co.surumene.simplecoins.domain.DomainContractCheck")
    jvmArgs("-ea")
}

tasks.check {
    dependsOn("domainContractCheck")
}
