plugins {
    idea
    kotlin("jvm")
}

group = "org.erwinkok.kik"
version = "0.0.1"

tasks.test {
    useJUnitPlatform()
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
