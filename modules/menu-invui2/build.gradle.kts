// Draws menus with InvUI 2, which needs Java 25 and fits one 26.x line per release. It compiles
// against the newest Paper api and InvUI release; the plugin only loads it on a 26.x server,
// which runs Java 25, and the server downloads the InvUI release for its own line.
plugins {
    id("hyperion.java")
}

val newestJava = libs.versions.java.newest.get().toInt()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(newestJava))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(newestJava)
}

dependencies {
    compileOnly(project(":menu"))
    compileOnly(libs.paper.api.newest)
    compileOnly(libs.invui2)
}
