import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.RecordingCopyTask
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL

plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.ben-manes.versions") version "0.21.0"
    id("com.jfrog.bintray") version "1.8.4"
}

group = "de.inoxio"
version = "1.0.5"

java {
    sourceCompatibility = JavaVersion.VERSION_1_10
    targetCompatibility = JavaVersion.VERSION_1_10
}

repositories {
    mavenCentral()
}

dependencies {
    // logging
    implementation("org.slf4j:slf4j-api:1.7.26")
    // network
    api("org.apache.mina:mina-core:2.1.3")
    implementation("com.sun.mail:jakarta.mail:1.6.3")
    implementation("commons-validator:commons-validator:1.6")
    // test
    testImplementation("junit:junit:4.12")
    // logging
    testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.11.2")
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn + "classes"
    classifier = "sources"
    from(sourceSets["main"].allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn + "javadoc"
    classifier = "javadoc"
    from(tasks.withType<Javadoc>().first().destinationDir)
}

publishing {
    publications {
        register("publication", MavenPublication::class) {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String

            from(components["java"])
            artifact(sourcesJar.get())
            artifact(javadocJar.get())

            pom.withXml {
                asNode().apply {
                    appendNode("description", "A NIO SMTP server API written in Java")
                    appendNode("name", "$groupId:$artifactId")
                    appendNode("url", "https://github.com/inoxio/MailsterSMTP")

                    val license = appendNode("licenses").appendNode("license")
                    license.appendNode("name", "The Apache Software License, Version 2.0")
                    license.appendNode("url", "http://www.apache.org/licenses/LICENSE-2.0.txt")

                    val developer = appendNode("developers").appendNode("developer")
                    developer.appendNode("name", "Michael Kunze")
                    developer.appendNode("email", "mkunze@inoxio.de")
                    developer.appendNode("organization", "inoxio Quality Services GmbH")
                    developer.appendNode("organizationUrl", "https://www.inoxio.de")

                    val scm = appendNode("scm")
                    scm.appendNode("connection", "scm:git:git://github.com/inoxio/MailsterSMTP.git")
                    scm.appendNode("developerConnection", "scm:git:ssh://github.com:inoxio/MailsterSMTP.git")
                    scm.appendNode("url", "http://github.com/inoxio/MailsterSMTP/tree/master")
                }
            }
        }
    }
}

bintray {
    user = properties["bintray.user"] as String?
            ?: System.getenv("BINTRAY_USER")
    key = properties["bintray.api-key"] as String?
            ?: System.getenv("BINTRAY_API_KEY")

    setPublications("publication")

    publish = true
    override = false

    pkg.apply {
        repo = "maven"
        name = project.name
        userOrg = "inoxio"
        desc = "A NIO SMTP server API written in Java"
        websiteUrl = "https://github.com/inoxio/MailsterSMTP"
        issueTrackerUrl = "https://github.com/inoxio/MailsterSMTP/issues"
        vcsUrl = "https://github.com/inoxio/MailsterSMTP.git"
        setLicenses("Apache-2.0")
        githubRepo = "inoxio/MailsterSMTP"

        version.apply {
            name = project.version as String
            vcsTag = project.version as String
            gpg.apply {
                sign = true
            }
            mavenCentralSync.apply {
                sync = true
                user = properties["oss.user"] as String?
                        ?: System.getenv("OSS_USER")
                password = properties["oss.password"] as String?
                        ?: System.getenv("OSS_PASSWORD")
                close = "1"
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
    withType<DependencyUpdatesTask> {
        resolutionStrategy {
            componentSelection {
                all {
                    if (listOf("alpha", "beta", "b01", "rc", "cr", "m")
                                    .asSequence()
                                    .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*") }
                                    .any { it.matches(candidate.version) }) {
                        reject("Release candidate")
                    }
                }
            }
        }
    }
    withType<Wrapper> {
        distributionType = ALL
        gradleVersion = "5.4.1"
    }
    withType<Javadoc> {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}
