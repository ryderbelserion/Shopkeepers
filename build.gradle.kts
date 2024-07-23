plugins {
    `java-plugin`
}

val buildNumber: String? = System.getenv("BUILD_NUMBER")

rootProject.version = if (buildNumber != null) "${libs.versions.minecraft.get()}-$buildNumber" else "2.22.2"

subprojects.forEach { _ ->
    project.version = rootProject.version
}