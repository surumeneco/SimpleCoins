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

 
// Isolated Paper observation probe: test classes only, never packaged in SimpleCoins.
tasks.register<Jar>("paperProfileBanProbeJar") {
    dependsOn(tasks.named("testClasses"))
    archiveBaseName.set("SimpleCoinsProfileBanProbe")
    from(sourceSets.test.get().output) {
        include("co/surumene/simplecoins/paper/ProfileBanLiveProbe.class")
    }
    from("tests/paper-profile-ban-smoke/plugin.yml") {
        rename { "plugin.yml" }
    }
}

 
// Separate Paper-only registry probe; never package its JavaPlugin in the main JAR.
tasks.register<Jar>("paperCoinRecipesProbeJar") {
    dependsOn(tasks.named("testClasses"))
    archiveBaseName.set("SimpleCoinsCoinRecipesProbe")
    from(sourceSets.test.get().output) {
        include("co/surumene/simplecoins/paper/CoinRecipesLiveProbe.class")
    }
    from("tests/paper-coin-recipes-smoke/plugin.yml") {
        rename { "plugin.yml" }
    }
}

 
// Wallet recipe probe lives in test output only, separate from the shipped JAR.
tasks.register<Jar>("paperWalletRegistryProbeJar") {
    dependsOn(tasks.named("testClasses"))
    archiveBaseName.set("SimpleCoinsWalletRegistryProbe")
    from(sourceSets.test.get().output) {
        include("co/surumene/simplecoins/paper/WalletRegistryLiveProbe.class")
    }
    from("tests/paper-wallet-smoke/plugin.yml") {
        rename { "plugin.yml" }
    }
}

 
// Source-bounded BlockLocker integration probe: separate from shipped main JAR.
tasks.register<Jar>("paperBlockLockerLiveProbeJar") {
    dependsOn(tasks.named("testClasses"))
    archiveBaseName.set("SimpleCoinsBlockLockerLiveProbe")
    from(sourceSets.test.get().output) {
        include("co/surumene/simplecoins/paper/BlockLockerLiveProbe.class")
    }
    from("tests/paper-blocklocker-smoke/plugin.yml") {
        rename { "plugin.yml" }
    }
}

 
// Test-only journal corruption probe; never shipped in the production JAR.
tasks.register<Jar>("paperJournalRecordProbeJar") {
    dependsOn(tasks.named("testClasses"))
    archiveBaseName.set("SimpleCoinsJournalRecordProbe")
    from(sourceSets.test.get().output) {
        include("co/surumene/simplecoins/paper/JournalRecordLiveProbe.class")
    }
    from("tests/paper-journal-record-smoke/plugin.yml") {
        rename { "plugin.yml" }
    }
}
