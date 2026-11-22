plugins { java }
group = "co.surumene"
version = "1.0.0"
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }

tasks.jar {
    archiveFileName.set("SimpleCoinsWriterFenceAgent.jar")
    manifest.attributes("Premain-Class" to
        "co.surumene.simplecoins.writeragent.PaperNativeWriterAgent")
    exclude("co/surumene/simplecoins/writeragent/WriterEpochHook*.class")
}
tasks.register<Jar>("bootstrapHookJar") {
    archiveFileName.set("SimpleCoinsWriterFenceBootstrap.jar")
    from(sourceSets.main.get().output) {
        include("co/surumene/simplecoins/writeragent/WriterEpochHook*.class")
    }
}
tasks.assemble { dependsOn("bootstrapHookJar") }
