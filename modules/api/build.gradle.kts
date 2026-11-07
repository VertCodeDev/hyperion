// The public api: interfaces and data types other plugins compile against. It depends on the
// Paper API only for types such as Location and Player, and is published as its own jar.
plugins {
    id("hyperion.java")
    `java-library`
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(libs.findLibrary("paper-api").get())
}
