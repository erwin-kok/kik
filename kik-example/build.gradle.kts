plugins {
    id("kik.library")
    id("org.erwinkok.kik.compiler-plugin")
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib"))

    implementation(projects.kikTypeSystem)
}
