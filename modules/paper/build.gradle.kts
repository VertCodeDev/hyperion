// The plugin: loads models, builds the resource pack and shows models to players. Runs on Paper
// and on Folia/Canvas. The api ships inside its jar.
plugins {
    id("hyperion.paper-plugin")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":api"))
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    archiveBaseName.set("Hyperion")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
