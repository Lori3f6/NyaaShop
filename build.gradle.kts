plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cat.nyaa"
version = "0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") //papermc
    maven("https://ci.nyaacat.com/maven/")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("land.melon.lab:simplelanguageloader:1.13.11")
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("org.xerial:sqlite-jdbc:3.46.1.0")
    compileOnly("cat.nyaa:ecore:0.3.4")
    compileOnly("cat.nyaa:ukit:1.7.2")
}

tasks {
    processResources {
        filesMatching("**/plugin.yml") {
            expand("version" to project.version)
        }
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("all")
        dependencies {
            include(dependency("land.melon.lab:simplelanguageloader:1.13.11"))
        }
    }
}

kotlin {
    jvmToolchain(21)
}