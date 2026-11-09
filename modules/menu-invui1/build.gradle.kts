// Draws menus with InvUI 1, which covers Minecraft 1.21 to 1.21.11. InvUI is not in the jar: the
// server downloads it on start, so this module only compiles against it.
plugins {
    id("hyperion.paper-plugin")
}

dependencies {
    compileOnly(project(":menu"))
    compileOnly(libs.invui1.core)
}
