plugins {
    `java-library`
}

group = "co.surumene"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
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
