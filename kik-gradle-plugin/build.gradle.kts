// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildConfig)
}

group = "org.erwinkok.kik"
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
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.gradlePluginApi)
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
