import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

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

tasks.register("build") {
    dependsOn(gradle.includedBuild("kik-compiler-plugin").task(":build"))
}

tasks.register("clean") {
    dependsOn(gradle.includedBuild("kik-compiler-plugin").task(":clean"))
}
