import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    `java-library`
    id("net.researchgate.release") version "2.8.1"
    `maven-publish`
    id("org.sonarqube") version "3.3"
    id("jacoco")
    id("com.adarshr.test-logger") version "3.1.0"
    id("org.jetbrains.kotlin.plugin.spring") version "1.6.10"
    id("com.github.ben-manes.versions") version "0.39.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
    id("org.owasp.dependencycheck") version "6.5.0.1"
}

group = "no.nav.eessi.pensjon"

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()

    val token = System.getenv("GITHUB_TOKEN")
        ?: project.findProperty("gpr.key")
        ?: throw NullPointerException("Missing token, you have to set GITHUB_TOKEN or gpr.key, see README")

    maven {
        url = uri("https://maven.pkg.github.com/navikt/maven-release")
        credentials {
            username = "token"
            password = token.toString()
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val junitVersion by extra("5.8.2")
val jacksonVersion by extra("2.13.0")
val springBootVersion by extra("2.6.1")


dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.springframework.boot:spring-boot-starter-web:${springBootVersion}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${jacksonVersion}")
    implementation("io.micrometer:micrometer-registry-prometheus:1.8.1")
    implementation("no.nav.eessi.pensjon:ep-metrics:0.4.12")
    implementation("no.nav.eessi.pensjon:ep-logging:1.0.14")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.mockk:mockk:1.12.1")
}

// https://github.com/researchgate/gradle-release
release {
    newVersionCommitMessage = "[Release Plugin] - next version commit: "
    tagTemplate = "release-\${version}"
}

// https://help.github.com/en/actions/language-and-framework-guides/publishing-java-packages-with-gradle#publishing-packages-to-github-packages
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/${rootProject.name}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// https://docs.gradle.org/current/userguide/jacoco_plugin.html
jacoco {
    toolVersion = "0.8.5"
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
}

tasks.named("sonarqube") {
    dependsOn("jacocoTestReport")
}

/* https://github.com/ben-manes/gradle-versions-plugin */
tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    resolutionStrategy {
        componentSelection {
            all {
                val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview", "pr").any { qualifier ->
                    candidate.version.matches("(?i).*[.-]${qualifier}[.\\d-]*".toRegex())
                }
                if (rejected) {
                    reject("Not a real release")
                }
            }
        }
    }
    revision = "release"
}
