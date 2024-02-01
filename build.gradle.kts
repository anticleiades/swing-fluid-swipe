plugins {
    idea
    signing
    `java-library`
    `maven-publish`
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

val ossrhUsername: String by properties
val ossrhPassword: String by properties

allprojects {
    group = pubGroupID
    version = pubVersion
    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    configure<SigningExtension> {
        val publishing = extensions["publishing"] as PublishingExtension
        sign(publishing.publications)
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_9
            targetCompatibility = JavaVersion.VERSION_1_9
            withSourcesJar()
            withJavadocJar()
        }

        tasks {
            withType<Javadoc>().configureEach {
                (options as StandardJavadocDocletOptions).apply {
                    locale = "en"
                    docEncoding = "UTF-8"
                    charSet = "UTF-8"
                    encoding = "UTF-8"
                    docTitle = project.name
                    windowTitle = project.name
                    header = "<b>${pubName}</b>"
                    quiet()
                    addStringOption("-add-exports", "java.desktop/sun.awt=fluidswipe.utils")
                    addStringOption("source", "9")
                    addBooleanOption("html5", true)
                    links("https://docs.oracle.com/javase/9/docs/api/")
                }
            }
        }

        publishing {
            if (project.path == ":") {
                // Skip the root project
                return@publishing
            }
            publications {
                create<MavenPublication>(project.name) {
                    groupId = pubGroupID
                    artifactId = project.name
                    version = pubVersion
                    description = project.description
                    from(project.components["java"])
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
                            url.set("https://${scmUrlRoot}/${accountName}/${pubName}")
                            connection.set("scm:git:git://${scmUrlRoot}/${accountName}/${pubName}.git")
                            developerConnection.set("scm:git:ssh://git@github.com:${accountName}/${pubName}.git")
                        }
                    }
                }
            }
            repositories {
                maven {
                    name = "ossrh"

                    val isSnapshot = version.toString().endsWith("SNAPSHOT")

                    url = if (isSnapshot) uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                    else uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

                    credentials {
                        username = ossrhUsername
                        password = ossrhPassword
                    }
                }
            }
        }
    }
}