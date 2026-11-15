plugins { java }

group = "co.surumene.probes"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
dependencies { compileOnly("io.papermc.paper:paper-api:26.2.build.134-stable") }
tasks.jar {
    archiveFileName.set("SimpleCoinsPaperSaveProbe.jar")
    from("plugin.yml")
}
