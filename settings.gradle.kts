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

rootProject.name = "helikon"
