plugins {
    id("java")
    kotlin("jvm") version "1.9.23"
}

group = "com.thedroiddiv.klibrosa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}