import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.dokka") version "1.6.20"
    id("org.gradle.maven-publish")
    id("org.gradle.application")
}

group = "com.github.sszuev"
version = "2.0.0-SNAPSHOT"
description = "A simple command-line utility to convert any RDF graph to OWL2-DL ontology"

repositories {
    mavenCentral()
}

dependencies {
    val kotlinCliVersion = "0.3.4"
    val kotlinCoroutinesVersion = "1.6.1"
    val ontapiVersion = "3.0.0"
    val owlapiVersion = "5.1.20"
    val slf4jVersion = "1.7.36"

    implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlinCliVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("com.github.owlcs:ontapi:$ontapiVersion")
    implementation("net.sourceforge.owlapi:owlapi-parsers:$owlapiVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-log4j12:$slf4jVersion")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("com.github.sszuev.ontconverter.MainKt")
}

val fatJar = tasks.register<Jar>("fatJar") {
    dependsOn.addAll(
        listOf(
            "compileJava",
            "compileKotlin",
            "processResources",
            "test"
        )
    )
    archiveFileName.set(rootProject.name + ".jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(mapOf("Main-Class" to application.mainClass))
    }
    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get()
        .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output
    from(contents)
}
val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    dependsOn.add(fatJar)
}
val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)
val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
    dependsOn.add(sourcesJar)
}

tasks {
    build {
        dependsOn(fatJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
}