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
    compileOnly(files("../../server-snapshot/plugins/WonderfulCoreLibrary-0.1.0.jar"))
    compileOnly(files("../../server-snapshot/plugins/WonderfulGenomeLib-1.0.1.jar"))
    compileOnly(files("../../server-snapshot/plugins/WhatAWonderfulWolf-1.1.2.jar"))
    compileOnly(files("../../build/libs/SimpleCoins-1.0.0.jar"))
}
tasks.jar {
    archiveFileName.set("SimpleCoinsWclServicesProbe.jar")
    from("plugin.yml")
}
