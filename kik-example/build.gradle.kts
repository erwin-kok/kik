import org.erwinkok.kik.compiler.gradleplugin.ErrorCheckingMode

plugins {
    id("kik.library")
    id("org.erwinkok.kik.compiler-plugin")
}

kik {
    enabled = true
    errorCheckingMode = ErrorCheckingMode.WARNING
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib"))

    implementation(projects.kikTypeSystem)

    testImplementation(libs.kotlin.test)
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.erwinkok.kik:kik-type-system"))
            .using(project(":kik-type-system"))
        substitute(module("org.erwinkok.kik:kik-compiler-plugin"))
            .using(project(":kik-compiler-plugin"))
    }
}
