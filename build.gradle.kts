plugins {
    `java-plugin`
}

val buildNumber: String? = System.getenv("BUILD_NUMBER")

rootProject.version = if (buildNumber != null) "${libs.versions.minecraft.get()}-$buildNumber" else "2.22.3"

subprojects.forEach {
    it.project.version = rootProject.version
}