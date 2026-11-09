// The Geyser extension: shows Hyperion models to Bedrock players as native Bedrock entities. It
// runs inside Geyser, wherever Geyser runs, and talks to Hyperion on each backend server over the
// Bedrock link: it downloads the Bedrock pack and model list once, then follows a live stream of
// animation state. Gson ships inside it, renamed, so it never clashes with Geyser's own.
plugins {
    id("hyperion.java")
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.geyser.api)
    implementation(libs.gson)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("extension.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    archiveFileName.set("hyperion-geyser-v${project.version}.jar")
    relocate("com.google.gson", "dev.vertcode.hyperion.geyser.libs.gson")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
