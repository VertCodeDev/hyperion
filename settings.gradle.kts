pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
    }
}

rootProject.name = "hyperion"

// api: what other plugins compile against to spawn, animate and drive models.
// paper: the plugin itself, the Paper and Folia/Canvas implementation of the api.
include("api", "paper")

project(":api").projectDir = file("modules/api")
project(":paper").projectDir = file("modules/paper")
