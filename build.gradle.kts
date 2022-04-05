import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "com.github.sszuev"
version = "2.0.0-SNAPSHOT"
description = "A simple command-line utility to convert any RDF graph to OWL2-DL ontology"

repositories {
    mavenCentral()
}

dependencies {
    val kotlincliVersion = "0.3.4"
    val ontapiVersion = "3.0.0"
    val owlapiVersion = "5.1.20"
    val slf4jVersion = "1.7.36";

    implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlincliVersion")
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

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(
            listOf(
                "compileJava",
                "compileKotlin",
                "processResources"
            )
        ) // We need this for Gradle optimization to work
        archiveFileName.set(rootProject.name + ".jar")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes(mapOf("Main-Class" to application.mainClass))
        } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
}
