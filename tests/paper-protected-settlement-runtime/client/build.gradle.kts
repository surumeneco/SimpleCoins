plugins { application }
repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
dependencies { implementation("org.geysermc.mcprotocollib:protocol:26.2") }
application { mainClass.set("co.surumene.probes.LoopbackClient") }
