import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

// Every module: the Java toolchain from the version catalog, UTF-8, -parameters, Lombok,
// JSpecify and JetBrains annotations, JUnit 5, NullAway checking the JSpecify annotations at
// compile time through Error Prone, and a second compile against the newest supported Paper api.
plugins {
    java
    id("net.ltgt.errorprone")
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

    // Only @NullMarked packages are checked, which is every package in this repository.
    options.errorprone {
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:JSpecifyMode", "true")

        // House style hoists every format string into a named constant.
        disable("InlineFormatString")

        // BytesSource keeps its array component on purpose and compares it by content.
        disable("ArrayRecordComponent")

        // whenComplete is how async work ends here: its callback handles the error itself.
        disable("FutureReturnValueIgnored")
    }
}

// The empty folders hold a .gitkeep until they have sources; it never belongs in a jar.
tasks.withType<AbstractArchiveTask>().configureEach {
    exclude("**/.gitkeep")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    compileOnly(libs.findLibrary("jspecify").get())
    compileOnly(libs.findLibrary("jetbrains-annotations").get())
    compileOnly(libs.findLibrary("lombok").get())
    annotationProcessor(libs.findLibrary("lombok").get())
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
    errorprone(libs.findLibrary("errorprone-core").get())
    errorprone(libs.findLibrary("nullaway").get())
}

// Hyperion compiles against the oldest Paper api it supports, so nothing newer can be used by
// accident. This second compile, against the newest api, catches the opposite: an old method a
// newer server has removed. It is part of `check`, so every build runs it.
val newestPaper = configurations.create("newestPaper")
val mainSources = the<SourceSetContainer>()["main"]

dependencies {
    newestPaper(libs.findLibrary("paper-api-newest").get())
}

val compileAgainstNewestPaper = tasks.register<JavaCompile>("compileAgainstNewestPaper") {
    source = mainSources.java
    classpath = newestPaper + mainSources.compileClasspath.filter { !it.name.startsWith("paper-api-") }
    options.annotationProcessorPath = mainSources.annotationProcessorPath
    destinationDirectory.set(layout.buildDirectory.dir("classes/newest-paper"))
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(libs.findVersion("java-newest").get().requiredVersion))
    })
    options.errorprone.enabled.set(false)
}

tasks.named("check") {
    dependsOn(compileAgainstNewestPaper)
}
