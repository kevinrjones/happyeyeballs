import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java
    application
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.versionUpdate)
    alias(libs.plugins.catalogUpdate)
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(rootProject.libs.kotlinCoroutines)
    testImplementation(rootProject.libs.junit)
    testImplementation(rootProject.libs.jUnitEngine)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

application {
    mainClass.set("com.knowledgespike.ProgramKt")

    applicationDefaultJvmArgs = listOf("-Dkotlinx.coroutines.debug")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}