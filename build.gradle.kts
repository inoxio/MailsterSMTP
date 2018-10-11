import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.RecordingCopyTask
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL

plugins {
    id("java")
    id("com.github.ben-manes.versions") version "0.20.0"
    id("com.jfrog.bintray") version "1.8.4"
}

group = "de.inoxio"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_10
    targetCompatibility = JavaVersion.VERSION_1_10
}

repositories {
    mavenCentral()
}

dependencies {
    // logging
    compile("org.slf4j:slf4j-api:1.7.25")
    // network
    compile("org.apache.mina:mina-core:2.0.19")
    compile("com.sun.mail:javax.mail:1.6.2")
    compile("commons-validator:commons-validator:1.6")
    // test
    testCompile("junit:junit:4.12")
    // logging
    testRuntime("org.apache.logging.log4j:log4j-slf4j-impl:2.11.1")
}

bintray {
    user = properties["bintrayUser"] as String?
            ?: System.getenv("BINTRAY_USER")
    key = properties["bintrayApiKey"] as String?
            ?: System.getenv("BINTRAY_API_KEY")

    setConfigurations("archives")

    publish = true

    pkg(closureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        userOrg = "inoxio"
        name = project.name

        setLicenses("Apache-2.0")
        issueTrackerUrl = "https://github.com/inoxio/MailsterSMTP/issues"
        vcsUrl = "https://github.com/inoxio/MailsterSMTP.git"
        githubRepo = "inoxio/MailsterSMTP"

        version(closureOf<BintrayExtension.VersionConfig> {
            name = project.version as String
            vcsTag = project.version as String
            gpg(closureOf<BintrayExtension.GpgConfig> {
                sign = true
            })
        })
    })
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
        gradleVersion = "4.10.2"
    }
}
