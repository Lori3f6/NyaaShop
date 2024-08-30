plugins {
    kotlin("jvm") version "2.0.20"
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
    implementation("land.melon.lab:simplelanguageloader:1.13.5")
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("org.xerial:sqlite-jdbc:3.46.1.0")
    compileOnly("cat.nyaa:ecore:0.3.4")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}