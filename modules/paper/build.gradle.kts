// The plugin: loads models, builds the resource pack and shows models to players. Runs on Paper
// and on Folia/Canvas. The api and the menu modules ship inside its jar, and so does Cloud,
// relocated under dev.vertcode.hyperion.libs: a library loader's copy of Cloud is visible to
// every plugin that depends on Hyperion, and a second org.incendo.cloud on such a plugin's path
// breaks its own commands. InvUI does not ship: HyperionLoader has the server download the
// release that fits the server's version. PacketEvents is a separate plugin the server installs,
// so it updates for new Minecraft versions on its own.
plugins {
    id("hyperion.paper-plugin")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

// menu-invui2 is built for Java 25 and only loaded on 26.x servers, which run it; the plugin itself
// stays on Java 21, so Gradle must not refuse the newer module.
java {
    disableAutoTargetJvm()
}

dependencies {
    implementation(project(":api"))
    implementation(project(":menu"))
    runtimeOnly(project(":menu-invui1"))
    runtimeOnly(project(":menu-invui2"))

    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.placeholderapi)
    implementation(libs.cloud.paper)
    implementation(libs.cloud.annotations)
    implementation(libs.cloud.minecraft.extras) {
        exclude(group = "net.kyori")
    }
}

// The loader reads the library versions from this file, so they live only in the catalog.
tasks.processResources {
    val versions = mapOf(
        "invui1" to libs.versions.invui1.get(),
        "invui2" to libs.versions.invui2.asProvider().get(),
        "invui2For261" to libs.versions.invui2.mc261.get(),
        "invui2For262" to libs.versions.invui2.mc262.get()
    )
    inputs.properties(versions)
    filesMatching("hyperion-libraries.properties") {
        expand(versions)
    }
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    archiveFileName.set("hyperion-paper-v${project.version}.jar")
    relocate("org.incendo.cloud", "dev.vertcode.hyperion.libs.cloud")
    relocate("io.leangen.geantyref", "dev.vertcode.hyperion.libs.geantyref")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
