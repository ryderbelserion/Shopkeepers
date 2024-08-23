rootProject.name = "Shopkeepers"

pluginManagement {
    repositories {
        maven("https://repo.crazycrew.us/releases")

        gradlePluginPortal()
    }
}

plugins {
    id("com.ryderbelserion.feather-settings") version "0.0.4"
}

include("modules")

listOf(
    "modules/main",
    "modules/api",
).forEach {
    val (folder, name) = it.split("/")

    includeProject(name, folder)
}

fun includeProject(name: String, folder: String) {
    include(name) {
        this.name = "${rootProject.name}-$name"
        this.projectDir = file("$folder/$name")
    }
}

fun include(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}