plugins {
    idea
    `java-library`
    id("com.vanniktech.maven.publish") version "0.36.0"
}

val accountName: String by properties
val issueManagementSys: String by properties
val scmUrlRoot: String by properties

val pubGroupID: String by properties
val pubName: String by project.properties
val pubVersion: String by project.properties
val pubMail: String by properties
val pubDescription: String by properties
val pubAuthor: String by properties
val pubLicenseName: String by properties

allprojects {
    group = pubGroupID
    version = pubVersion

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_9
            targetCompatibility = JavaVersion.VERSION_1_9
        }

        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).apply {
                locale = "en"
                docEncoding = "UTF-8"
                charSet = "UTF-8"
                encoding = "UTF-8"
                docTitle = project.name
                windowTitle = project.name
                header = "<b>${pubName}</b>"
                quiet()
                // Mantiene il bypass di Jigsaw per accedere alle classi interne di AWT
                addStringOption("-add-exports", "java.desktop/sun.awt=fluidswipe.utils")
                addStringOption("source", "9")
                addBooleanOption("html5", true)
                links("https://docs.oracle.com/javase/9/docs/api/")
            }
        }
    }
}

subprojects {
    apply(plugin = "com.vanniktech.maven.publish")

    mavenPublishing {
        coordinates(pubGroupID, project.name, pubVersion)

        pom {
            name.set(project.name)
            description.set(pubDescription)
            url.set("https://${scmUrlRoot}/${accountName}/${pubName}")

            licenses {
                license {
                    name.set(pubLicenseName)
                    url.set("https://${scmUrlRoot}/${accountName}/${pubName}/blob/master/LICENSE")
                }
            }
            developers {
                developer {
                    id.set(accountName)
                    name.set(pubAuthor)
                    email.set(pubMail)
                }
            }
            issueManagement {
                system.set(issueManagementSys)
                url.set("https://${scmUrlRoot}/${accountName}/${pubName}/issues")
            }
            scm {
                connection.set("scm:git:git://${scmUrlRoot}/${accountName}/${pubName}.git")
                developerConnection.set("scm:git:ssh://git@github.com:${accountName}/${pubName}.git")
                url.set("https://${scmUrlRoot}/${accountName}/${pubName}")
            }
        }

        publishToMavenCentral(automaticRelease = false)

        signAllPublications()
    }
}