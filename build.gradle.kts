plugins {
    id("java-library")
    id("maven-publish")
}

group = "de.inoxio"
version = "1.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
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

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn + "classes"
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn + "javadoc"
    archiveClassifier.set("javadoc")
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
    }
}
