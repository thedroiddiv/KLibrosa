val libGroupId = "com.thedroiddiv.klibrosa"
val libArtifactId = "Klibrosa"
val libArtifactVersion = "1.1"

plugins {
    kotlin("jvm") version "1.9.23"
    id("maven-publish")
}

group = libGroupId
version = libArtifactVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = libGroupId
            artifactId = libArtifactId
            version =  libArtifactVersion
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}