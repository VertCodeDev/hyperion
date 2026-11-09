// The plugin: loads models, builds the resource pack and shows models to players. Runs on Paper
// and on Folia/Canvas. The api and the menu modules ship inside its jar. Cloud and InvUI do not:
// HyperionLoader has the server download them on start, InvUI in the release that fits the
// server's version, and Paper keeps them apart from every other plugin's copies. PacketEvents is
// a separate plugin the server installs, so it updates for new Minecraft versions on its own.
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
    compileOnly(libs.cloud.paper)
    compileOnly(libs.cloud.annotations)
    compileOnly(libs.cloud.minecraft.extras) {
        exclude(group = "net.kyori")
    }
}

// The loader reads the library versions from this file, so they live only in the catalog.
tasks.processResources {
    val versions = mapOf(
        "cloud" to libs.versions.cloud.asProvider().get(),
        "cloudMinecraft" to libs.versions.cloud.minecraft.get(),
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
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
