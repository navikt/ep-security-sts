import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.72"
    `java-library`
    id("net.researchgate.release") version "2.8.1"
    `maven-publish`
    id("org.sonarqube") version "3.0"
    id("jacoco")
    id("com.adarshr.test-logger") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.72"
    id("com.github.ben-manes.versions") version "0.28.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.14"
}

group = "no.nav.eessi.pensjon"

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()

    listOf("ep-metrics").forEach { repo ->
        val token = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key")
        if (token == null) throw NullPointerException("Missing token, you have to set GITHUB_TOKEN or gpr.key, see README")
        maven {
            url = uri("https://maven.pkg.github.com/navikt/$repo")
            credentials {
                username = "token"
                password = token.toString()
            }
        }
    }

}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val springVersion by extra("5.2.5.RELEASE")
val junitVersion by extra("5.6.2")
val cxfVersion by extra("3.3.6")
val mockitoVersion by extra("3.3.3")
val jacksonVersion by extra("2.11.0")


dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("no.nav.security:oidc-spring-support:0.2.18")
    implementation("org.apache.cxf:cxf-spring-boot-starter-jaxws:${cxfVersion}")
    implementation("org.apache.cxf:cxf-rt-ws-security:${cxfVersion}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${jacksonVersion}")
    implementation("io.micrometer:micrometer-registry-prometheus:1.5.1")
    implementation("no.nav.eessi.pensjon:ep-metrics:0.4.0")
    implementation("no.nav.eessi.pensjon:ep-logging:0.0.16")
    implementation("org.hibernate:hibernate-validator:6.1.5.Final")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.springframework:spring-test:$springVersion")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:${mockitoVersion}")
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
