import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.Exec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import groovy.json.JsonSlurper
import java.security.MessageDigest

plugins {
    id("net.fabricmc.fabric-loom")
    `maven-publish`
}

version = property("mod_version") as String
group = property("maven_group") as String

base {
    archivesName.set(property("archive_base_name") as String)
}

val embeddedBaritoneVersion = "1.15.0-helikon.26.2.0"
val embeddedBaritoneDirectory = layout.projectDirectory.dir("vendor/baritone")
val embeddedBaritoneJar = embeddedBaritoneDirectory.file(
    "fabric/build/libs/baritone-fabric-$embeddedBaritoneVersion.jar"
)
val java25Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}
val buildEmbeddedBaritone = tasks.register<Exec>("buildEmbeddedBaritone") {
    group = "build"
    description = "Builds the separately licensed Baritone component embedded in Helikon."
    workingDir(embeddedBaritoneDirectory)
    val wrapper = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "gradlew.bat"
    } else {
        "./gradlew"
    }
    commandLine(wrapper, ":fabric:remapJar", "--console=plain")
    environment("JAVA_HOME", java25Launcher.map { it.metadata.installationPath.asFile.absolutePath }.get())
    inputs.files(fileTree(embeddedBaritoneDirectory) {
        exclude("**/.gradle/**", "**/build/**")
    })
    outputs.file(embeddedBaritoneJar)
}

repositories {
    flatDir {
        dirs(embeddedBaritoneDirectory.dir("fabric/build/libs"))
    }
}

sourceSets.create("gametest")

loom {
    splitEnvironmentSourceSets()

    mods {
        create("helikon") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
        create("helikon_gametest") {
            sourceSet(sourceSets["gametest"])
        }
    }

    runs {
        create("clientGameTest") {
            client()
            configName = "Helikon Client GameTest"
            source(sourceSets["gametest"])
            vmArg("-Dfabric.client.gametest")
            runDir("build/run/clientGameTest")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    add("clientImplementation", "baritone:baritone-fabric:$embeddedBaritoneVersion")
    add("include", "baritone:baritone-fabric:$embeddedBaritoneVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:${property("junit_version")}")
    testImplementation("com.google.code.gson:gson:${property("gson_version")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${property("junit_platform_version")}")
}

sourceSets {
    named("gametest") {
        compileClasspath += sourceSets["client"].compileClasspath + sourceSets["client"].output + sourceSets["main"].output
        runtimeClasspath += sourceSets["client"].runtimeClasspath + sourceSets["client"].output + sourceSets["main"].output
    }
    named("test") {
        compileClasspath += sourceSets["client"].output
        runtimeClasspath += sourceSets["client"].output
    }
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.named("compileClientJava") {
    dependsOn(buildEmbeddedBaritone)
}

tasks.matching { it.name == "processIncludeJars" }.configureEach {
    dependsOn(buildEmbeddedBaritone)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    dependsOn(buildEmbeddedBaritone)
    from("LICENSE") {
        rename { "${it}_${project.name}" }
    }
    from("vendor/baritone/LICENSE") {
        rename { "LICENSE_baritone" }
    }
    from("vendor/baritone/HELIKON_PORT.md")
}

val helikonReports = layout.buildDirectory.dir("reports/helikon")
val checksumFile = helikonReports.map { it.file("checksums.sha256") }
val dependencyReport = helikonReports.map { it.file("dependencies.txt") }
val distributableJars = fileTree(layout.buildDirectory.dir("libs")) {
    include("*.jar")
    exclude("*-dev.jar")
}
val clientRuntimeClasspath = configurations.named("clientRuntimeClasspath")

/** Fails fast on source formatting that obscures diffs or violates the project indentation policy. */
val verifySourceStyle = tasks.register("verifySourceStyle") {
    group = "verification"
    description = "Checks Java sources for tabs and trailing whitespace."
    val sources = fileTree("src") { include("**/*.java") }
    inputs.files(sources)
    doLast {
        val violations = sources.files.sortedBy { it.invariantSeparatorsPath }.flatMap { source ->
            source.readLines().mapIndexedNotNull { index, line ->
                when {
                    '\t' in line -> "${source.invariantSeparatorsPath}:${index + 1}: tab character"
                    line != line.trimEnd() -> "${source.invariantSeparatorsPath}:${index + 1}: trailing whitespace"
                    else -> null
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Source style violations:\n${violations.joinToString("\n")}")
        }
    }
}

/** Confines optional networking to its declared integration package and preserves the client-only descriptor. */
val verifyClientOnlyArchitecture = tasks.register("verifyClientOnlyArchitecture") {
    group = "verification"
    description = "Checks the client-only descriptor and rejects network APIs outside integration.network."
    val sourceFiles = fileTree("src") { include("**/*.java") }
    inputs.files(sourceFiles)
    inputs.file("src/main/resources/fabric.mod.json")
    doLast {
        val forbiddenTokens = listOf("java.net.", "HttpClient", "WebSocket", "URLConnection", "OkHttp")
        val networkIntegrationPath = "/dev/helikon/client/integration/network/"
        val violations = sourceFiles.files.sortedBy { it.invariantSeparatorsPath }.flatMap { source ->
            if (networkIntegrationPath in source.invariantSeparatorsPath) {
                return@flatMap emptyList()
            }
            source.readLines().mapIndexedNotNull { index, line ->
                forbiddenTokens.firstOrNull(line::contains)?.let { token ->
                    "${source.invariantSeparatorsPath}:${index + 1}: forbidden external-network token '$token'"
                }
            }
        }
        val descriptor = JsonSlurper().parseText(file("src/main/resources/fabric.mod.json").readText()) as Map<*, *>
        val environment = descriptor["environment"]
        val entrypoints = descriptor["entrypoints"] as? Map<*, *>
        val entrypointTypes = entrypoints?.keys?.map(Any?::toString)?.toSet()
        if (environment != "client" || entrypointTypes != setOf("client")) {
            violations + "src/main/resources/fabric.mod.json: client-only entrypoint contract violated"
        } else {
            violations
        }.also { results ->
            if (results.isNotEmpty()) {
                throw GradleException("Client-only architecture violations:\n${results.joinToString("\n")}")
            }
        }
    }
}

/** Writes the resolved runtime dependency set alongside the release artifacts for review. */
val generateDependencyReport = tasks.register("generateDependencyReport") {
    group = "reporting"
    description = "Writes the resolved client runtime dependency artifact list."
    dependsOn(buildEmbeddedBaritone)
    inputs.files(clientRuntimeClasspath)
    inputs.property("releaseVersion", provider { project.version.toString() })
    outputs.file(dependencyReport)
    doLast {
        val artifacts = clientRuntimeClasspath.get().resolvedConfiguration.resolvedArtifacts
            .sortedBy { it.moduleVersion.id.toString() }
        val output = dependencyReport.get().asFile
        output.parentFile.mkdirs()
        output.writeText(buildString {
            appendLine("Helikon ${project.version} client runtime dependencies")
            artifacts.forEach { artifact -> appendLine("${artifact.moduleVersion.id}\t${artifact.file.name}") }
        })
    }
}

/** Creates reproducible SHA-256 sidecar entries for distributable, non-development JARs. */
val generateChecksums = tasks.register("generateChecksums") {
    group = "build"
    description = "Generates SHA-256 checksums for distributable JARs."
    dependsOn(tasks.named("build"))
    inputs.files(distributableJars)
    inputs.property("releaseVersion", provider { project.version.toString() })
    outputs.file(checksumFile)
    doLast {
        val jars = distributableJars.files.sortedBy { it.name }
        if (jars.isEmpty()) {
            throw GradleException("No distributable JARs were produced in build/libs")
        }
        val output = checksumFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(jars.joinToString(separator = "\n", postfix = "\n") { jar ->
            val digest = MessageDigest.getInstance("SHA-256").digest(jar.readBytes())
                .joinToString(separator = "") { byte -> "%02x".format(byte) }
            "$digest  ${jar.name}"
        })
    }
}

/** Bundles the remapped JAR, sources, checksums, dependency report, license, and release notes locally. */
tasks.register<Zip>("releaseBundle") {
    group = "build"
    description = "Builds the local version-1.0 release bundle."
    dependsOn(tasks.named("build"), generateChecksums, generateDependencyReport)
    archiveFileName.set("helikon-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("releases"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    from(distributableJars)
    from(checksumFile) { into("reports") }
    from(dependencyReport) { into("reports") }
    from("LICENSE", "README.md", "CHANGELOG.md")
    from("vendor/baritone/LICENSE") {
        into("licenses/baritone")
    }
    from("vendor/baritone/LICENSE-Part-2.jpg") {
        into("licenses/baritone")
    }
    from("vendor/baritone/HELIKON_PORT.md") {
        into("licenses/baritone")
    }
    from("vendor/baritone") {
        into("source/baritone")
        exclude("**/.gradle/**", "**/build/**", ".idea/**")
    }
    from("docs/security-review.md") { into("docs") }
    from("docs/release.md") { into("docs") }
}

tasks.named("check") {
    dependsOn(verifySourceStyle, verifyClientOnlyArchitecture)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
