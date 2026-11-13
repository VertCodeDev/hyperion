// The public api: interfaces and data types other plugins compile against. It depends on the
// Paper API only for types such as Location and Player, and is published to Maven Central as
// dev.vertcode.hyperion:hyperion-api, with its sources and Javadoc. JSpecify is compileOnlyApi so
// plugins compiling against the api see its null annotations.
plugins {
    id("hyperion.java")
    `java-library`
    alias(libs.plugins.maven.publish)
}

val catalog = the<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(catalog.findLibrary("paper-api").get())
    compileOnlyApi(catalog.findLibrary("jspecify").get())
}

// The Javadoc links to the types the api takes and returns, and a missing comment must not fail a
// release: the compiler already checks the code.
tasks.withType<Javadoc>().configureEach {
    val standard = options as StandardJavadocDocletOptions
    standard.encoding = "UTF-8"
    standard.addBooleanOption("Xdoclint:none", true)
}

// Publishing needs a Central Portal token and a GPG key, read from the mavenCentralUsername,
// mavenCentralPassword, signingInMemoryKey and signingInMemoryKeyPassword Gradle properties. The
// publish workflow provides them; a local build never needs them.
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(project.group.toString(), "hyperion-api", project.version.toString())

    pom {
        name.set("Hyperion API")
        description.set("The api of Hyperion, a Paper, Folia and Canvas plugin that brings Blockbench models into Minecraft.")
        url.set("https://github.com/VertCodeDev/hyperion")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("VertCodeDev")
                name.set("VertCode Development")
                url.set("https://github.com/VertCodeDev")
            }
        }

        scm {
            url.set("https://github.com/VertCodeDev/hyperion")
            connection.set("scm:git:https://github.com/VertCodeDev/hyperion.git")
            developerConnection.set("scm:git:ssh://git@github.com/VertCodeDev/hyperion.git")
        }
    }
}
