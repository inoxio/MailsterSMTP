import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL

plugins {
    id("java")
    id("com.github.ben-manes.versions") version "0.20.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_10
    targetCompatibility = JavaVersion.VERSION_1_10
}

repositories {
    mavenCentral()
    maven("http://repo.spring.io/plugins-release")
}

dependencies {
    // logging
    compile("org.slf4j:slf4j-api:1.7.25")
    // network
    compile("org.apache.mina:mina-core:2.0.19")
    compile("com.sun.mail:javax.mail:1.6.2")
    compile("commons-validator:commons-validator:1.6")
    // test
    testCompile("javax.activation:activation:1.1.1")
    testCompile("junit:junit:4.12")
    testCompile("org.columba:ristretto:1.0-all")
    // logging
    testRuntime("org.apache.logging.log4j:log4j-slf4j-impl:2.11.1")
    testRuntime("com.fasterxml.jackson.core:jackson-databind:2.9.7")
    testRuntime("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.7")
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
