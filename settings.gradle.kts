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
        maven("https://repo.xenondevs.xyz/releases/")
        maven("https://repo.opencollab.dev/main/")
    }
}

rootProject.name = "hyperion"

// api: what other plugins compile against to spawn, animate and drive models.
// paper: the plugin itself, the Paper and Folia/Canvas implementation of the api.
// menu: what a menu shows, without drawing it; menu-invui1 and menu-invui2 draw it with InvUI 1
// on 1.21.x and InvUI 2 on 26.x. The plugin loads the one that fits the server.
// geyser: the Geyser extension that shows models to Bedrock players as native Bedrock entities,
// wherever Geyser runs: standalone, on a proxy or on the server.
include("api", "paper", "menu", "menu-invui1", "menu-invui2", "geyser")

project(":api").projectDir = file("modules/api")
project(":paper").projectDir = file("modules/paper")
project(":menu").projectDir = file("modules/menu")
project(":menu-invui1").projectDir = file("modules/menu-invui1")
project(":menu-invui2").projectDir = file("modules/menu-invui2")
project(":geyser").projectDir = file("modules/geyser")
