import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    dependencies {
        // Add our own kik-gradle-plugin on the class path, so modules can take use of it.
        classpath("org.erwinkok.kik:kik-gradle-plugin")
    }
}

plugins {
    alias(libs.plugins.versions)
}

group = "org.erwinkok.kik"
version = "0.1.0"

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
