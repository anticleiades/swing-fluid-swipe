plugins {
    idea
    id("java-library")
    id("maven-publish")
}

tasks.compileJava {
    options.compilerArgs.add("--add-exports=java.desktop/sun.awt=fluidswipe.utils");
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":fluidswipe-handler-api"))
    implementation(project(":fluidswipe-macos"))

    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-engine
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}
tasks.test {
    useJUnitPlatform()
}