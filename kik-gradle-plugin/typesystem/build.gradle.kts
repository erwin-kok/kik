@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("jvm") version "2.1.0"
    id("java-gradle-plugin")
}

group = "org.erwinkok.kik.gradleplugin"
version = "0.1.0"

gradlePlugin {
    website.set("https://github.com/erwin-kok/kik")
    vcsUrl.set("https://github.com/erwin-kok/kik")
    plugins {
        create("kikCompilerPlugin") {
            id = "org.erwinkok.kik.compiler-plugin"
            implementationClass = "org.erwinkok.kik.compiler.gradleplugin.KikCompilerGradlePlugin"
            displayName = "kik-compiler Gradle Plugin"
            description = displayName
            tags.set(listOf("http", "kotlin", "kotlin-mpp", "k8s"))
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
}

dependencies {
    add("compileOnly", kotlin("gradle-plugin"))
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
