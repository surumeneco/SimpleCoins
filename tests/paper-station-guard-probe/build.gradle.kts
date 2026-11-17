plugins { java }

group = "co.surumene.probes"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.134-stable")
    compileOnly(files("../../build/libs/SimpleCoins-1.0.0.jar"))
}
tasks.jar {
    archiveFileName.set("SimpleCoinsStationGuardProbe.jar")
    from("plugin.yml")
}
