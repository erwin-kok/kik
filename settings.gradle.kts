@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "kik"

includeBuild("kik-gradle-plugin") {
    dependencySubstitution {
        substitute(module("org.erwinkok.kik:kik-gradle-plugin")).using(project(":"))
    }
}

include(":kik-compiler-plugin")
include(":kik-type-system")
include(":kik-example")
