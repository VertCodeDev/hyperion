// The plugin: loads models, builds the resource pack and shows models to players. Runs on Paper
// and on Folia/Canvas. The api and Cloud ship inside its jar, Cloud renamed under
// dev.vertcode.hyperion.libs so it never clashes with another plugin's copy. PacketEvents is a
// separate plugin the server installs, so it updates for new Minecraft versions on its own.
plugins {
    id("hyperion.paper-plugin")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":api"))

    compileOnly(libs.packetevents.spigot)

    implementation(libs.cloud.paper)
    implementation(libs.cloud.annotations)
    implementation(libs.cloud.minecraft.extras) {
        exclude(group = "net.kyori")
    }
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    archiveBaseName.set("Hyperion")
    archiveClassifier.set("")

    val libraries = "dev.vertcode.hyperion.libs"
    relocate("org.incendo.cloud", "$libraries.cloud")
    relocate("io.leangen.geantyref", "$libraries.geantyref")

    // Cloud finds its parsers through service files, which must follow the rename.
    mergeServiceFiles()

    // The annotation processor in cloud-annotations is for compiling, never for the server.
    exclude("META-INF/services/javax.annotation.processing.Processor")
    exclude("META-INF/gradle/incremental.annotation.processors")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
