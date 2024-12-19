plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("java-library")
    id("maven-publish")
    id("signing")
    id("com.rickbusarow.github-release-fork") version "2.5.2"
}

group = "de.inoxio"
version = "1.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    // network
    api("org.apache.mina:mina-core:2.1.9")
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    // test
    testImplementation("junit:junit:4.13.2")
    // logging
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.13")
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
                description = "A NIO SMTP server API written in Java"
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
}

signing {
    sign(publishing.publications["maven"])
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

githubRelease {
    token(project.findProperty("githubToken.inoxio") as String)
    repo = "MailsterSMTP"
    owner = "inoxio"
    tagName = project.version as String
    releaseName = project.version as String
    generateReleaseNotes = true
    targetCommitish = "master"
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
