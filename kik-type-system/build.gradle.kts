plugins {
    id("kik.library")
    alias(libs.plugins.compatibility)
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib"))
}
