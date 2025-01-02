import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("jvm")
    alias(libs.plugins.ksp)
    alias(libs.plugins.compatibility)
    alias(libs.plugins.testlogger)
    alias(libs.plugins.buildConfig)
}

group = "org.erwinkok.kik"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.auto.service)
    implementation(libs.auto.service.annotations)
    ksp(libs.auto.service.ksp)
    implementation(projects.kikTypeSystem)

    testImplementation(libs.kotlin.compile.testing)
    testImplementation(libs.kotlin.test)
}

testlogger {
    theme = ThemeType.MOCHA
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}
