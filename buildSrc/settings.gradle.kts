// buildSrc reads the same version catalog as the build, so plugin versions live in one place.
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
