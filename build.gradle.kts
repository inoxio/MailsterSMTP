import org.jreleaser.model.Active

plugins {
    id("java-library")
    id("maven-publish")
    id("org.jreleaser") version "1.26.0"
}

group = "de.inoxio"
version = "1.2.0"
description = "A NIO SMTP server API written in Java"

repositories {
    mavenCentral()
}

dependencies {
    // logging
    implementation("org.slf4j:slf4j-api:2.0.20")
    // network
    api("org.apache.mina:mina-core:2.2.10")
    implementation("com.sun.mail:jakarta.mail:2.0.2")
    // test
    testImplementation("junit:junit:4.13.2")
    // logging
    testRuntimeOnly("ch.qos.logback:logback-classic:1.6.5")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String

            from(components["java"])

            pom {

                name = "$groupId:$artifactId"
                description = project.description
                url = "https://github.com/inoxio/MailsterSMTP"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        name = "Michael Kunze"
                        email = "mkunze@inoxio.de"
                        organization = "inoxio Quality Services GmbH"
                        organizationUrl = "https://www.inoxio.de"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/inoxio/MailsterSMTP.git"
                    developerConnection = "scm:git:ssh://github.com:inoxio/MailsterSMTP.git"
                    url = "http://github.com/inoxio/MailsterSMTP/tree/master"
                }
            }
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    signing {
        active = Active.ALWAYS
        armored = true
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
    release {
        github {
            repoOwner = "inoxio"
            name = "MailsterSMTP"
            tagName = "{{projectVersion}}"
            releaseName = "{{projectVersion}}"
            branch = "master"
            changelog {
                enabled = false
            }
            releaseNotes {
                enabled = true
            }
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.apply {
            isFork = true
            isIncremental = true
            encoding = "UTF-8"
            compilerArgs = mutableListOf("-Xlint")
        }
    }
    withType<Javadoc> {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        (options as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
    }
}
