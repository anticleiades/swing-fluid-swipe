plugins {
    idea
    id("java-library")
    id("maven-publish")
}

repositories {
    mavenCentral()
}
dependencies {
    implementation(project(":fluidswipe-handler-api"))
}