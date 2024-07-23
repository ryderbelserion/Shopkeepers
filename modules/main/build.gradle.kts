plugins {
    alias(libs.plugins.paperweight)
    alias(libs.plugins.shadowJar)

    `paper-plugin`
}

repositories {
    maven("https://repo.glaremasters.me/repository/towny")

    maven("https://maven.citizensnpcs.co/repo")

    maven("https://maven.enginehub.org/repo")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)

    implementation(project(":Shopkeepers-api"))

    compileOnly(libs.bstats.bukkit) {
        exclude("org.bukkit")
    }

    compileOnly(libs.citizens.main) {
        isTransitive = false
    }

    compileOnly(libs.worldguard.bukkit) {
        exclude("org.bukkit")
    }

    compileOnly(libs.towny)

    compileOnly(libs.vault)
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.REOBF_PRODUCTION
}

tasks {
    processResources {
        inputs.properties("pluginVersion" to project.version)
        inputs.properties("website" to project.properties["website"])

        filesMatching("plugin.yml") {
            expand(inputs.properties)
        }
    }

    assemble {
        dependsOn(reobfJar)

        doLast {
            copy {
                from(reobfJar.get())
                into(rootProject.projectDir.resolve("jars"))
            }
        }
    }

    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")

        listOf(
            "org.bstats"
        ).forEach {
            relocate(it, "libs.$it")
        }
    }
}