val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.6.10"
}

group = "ru.fbear"
version = "0.0.1"
application {
    mainClass.set("ru.fbear.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("io.ktor:ktor-network-tls-certificates:$ktor_version")
    implementation("com.vk.api:sdk:1.0.14")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("org.jetbrains.exposed:exposed-dao:0.37.3")
    implementation("org.jetbrains.exposed:exposed-core:0.37.3")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.37.3")
}