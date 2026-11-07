// Every module: the Java toolchain from the version catalog, UTF-8, -parameters, Lombok,
// JetBrains annotations and JUnit 5.
plugins {
    java
}

val libs = the<VersionCatalogsExtension>().named("libs")

group = rootProject.group
version = rootProject.version

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(libs.findVersion("java").get().requiredVersion.toInt())
    options.compilerArgs.add("-parameters")
}

// The empty folders hold a .gitkeep until they have sources; it never belongs in a jar.
tasks.withType<AbstractArchiveTask>().configureEach {
    exclude("**/.gitkeep")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    compileOnly(libs.findLibrary("jetbrains-annotations").get())
    compileOnly(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("lombok").get())
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}
