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

tasks.processResources {
    expand("version" to project.version.toString())
}

tasks.jar {
    archiveBaseName = "PaperFilteredHoppers"
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
}