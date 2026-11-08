// The plugin module: the Paper API on the compile classpath, and on the test classpath so tests
// can use plain Paper types such as BoundingBox, and the version stamped into plugin.yml or
// paper-plugin.yml.
plugins {
    id("hyperion.java")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(libs.findLibrary("paper-api").get())
    testImplementation(libs.findLibrary("paper-api").get())
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}
