plugins {
    id("kik.library")
    alias(libs.plugins.compatibility)
    id("org.erwinkok.kik.compiler-plugin")
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib"))
}
