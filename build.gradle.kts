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
    compileOnly("io.papermc.paper:paper-api:26.2.build.134-stable")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-ea")
}

tasks.register<JavaExec>("domainContractCheck") {
    group = "verification"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("co.surumene.simplecoins.domain.DomainContractCheck")
    jvmArgs("-ea")
}

tasks.check {
    dependsOn("domainContractCheck")
}
