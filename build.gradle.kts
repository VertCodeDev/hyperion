// Shared build logic lives in buildSrc: hyperion.java for every module, hyperion.paper-plugin for
// the plugin module. This file only names the group and the release. A release build passes its
// version as -Prelease=1.2.3, which the publish workflow takes from the git tag; every other build
// is a snapshot.
group = "dev.vertcode.hyperion"
version = providers.gradleProperty("release").getOrElse("0.1.0-SNAPSHOT")
