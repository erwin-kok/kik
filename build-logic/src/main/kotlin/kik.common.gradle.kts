import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("jvm")
    com.adarshr.`test-logger`
}

group = "org.erwinkok.kik"
version = "0.1.0"

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

repositories {
    mavenCentral()
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://jcenter.bintray.com")
    }
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        url = uri("https://dl.cloudsmith.io/public/consensys/maven/maven/")
    }
    mavenLocal()
}
