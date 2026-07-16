pluginManagement {
    val loomVersion = settings.providers.gradleProperty("loom_version").get()

    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("net.fabricmc.fabric-loom") version loomVersion
    }
}

plugins {
    // Auto-provisions the pinned Java 25 toolchain when it is not installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "helikon"
