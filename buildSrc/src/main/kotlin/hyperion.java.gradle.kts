import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

// Every module: the Java toolchain from the version catalog, UTF-8, -parameters, Lombok,
// JSpecify and JetBrains annotations, JUnit 5, and NullAway checking the JSpecify annotations
// at compile time through Error Prone.
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
