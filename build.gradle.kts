plugins {
    `java`
}

group = "dev.hopperfilter"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// --- Auto-bump: every build produces a newer version -----------------------
// The version lives in gradle.properties. When a build-producing task is
// requested, the patch version is bumped first (unless -PnextVersion=... is
// given) and written back, so the jar, plugin.yml AND gradle.properties always
// move to a newer version with every rebuild.
val versionFile = file("gradle.properties")

fun readVersion(): String {
    val match = Regex("(?m)^version=(.*)$").find(versionFile.readText())
    return match?.groupValues?.get(1)?.trim()?.ifEmpty { null } ?: "0.0.0"
}

val wantsBuild = gradle.startParameter.taskNames.any {
    it.contains("build", ignoreCase = true) || it == "assemble" || it == "jar"
}

if (wantsBuild) {
    val current = readVersion()
    val requested = providers.gradleProperty("nextVersion").orNull
    val next = if (requested != null) {
        requested
    } else {
        val parts = current.split(".")
        val base = parts.take(2).joinToString(".")
        val patch = (parts.getOrNull(2)?.toIntOrNull() ?: 0) + 1
        "$base.$patch"
    }
    if (next != current) {
        val oldText = versionFile.readText()
        val newText = if (Regex("(?m)^version=.*$").containsMatchIn(oldText)) {
            oldText.replace(Regex("(?m)^version=.*$"), "version=$next")
        } else {
            oldText.trimEnd('\n') + "\nversion=$next\n"
        }
        versionFile.writeText(newText)
    }
    version = next
} else {
    version = readVersion()
}

tasks.processResources {
    expand("version" to project.version.toString())
}

tasks.jar {
    archiveBaseName = "FilteredHoppers"
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
}