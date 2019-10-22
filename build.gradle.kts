import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "dev.anli.ftskit"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.41"
}

sourceSets["main"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir("src")
}

sourceSets["test"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir("test")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-client-websockets:1.2.5")
    implementation("io.ktor:ktor-client-cio:1.2.5")
    implementation("io.ktor:ktor-client-js:1.2.5")
    implementation("io.ktor:ktor-client-okhttp:1.2.5")
    implementation("com.beust:klaxon:5.0.1")
    testImplementation("junit:junit:4.12")
}

repositories {
    mavenCentral()
    jcenter()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "12"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "12"
}