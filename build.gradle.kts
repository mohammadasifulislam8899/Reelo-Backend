plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.xentortyx.labs"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.autoHeadResponse)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.requestValidation)
    implementation(ktorLibs.server.statusPages)
    implementation(libs.exposed.core)
    implementation(libs.exposed.r2dbc)
    implementation(libs.exposed.java.time)
    implementation(libs.flaxoos.ktor.server.rateLimiting)
    implementation(libs.h2database.h2)
    implementation(libs.h2database.r2dbc)
    implementation(libs.koin.ktor)
    implementation(libs.koin.loggerSlf4j)
    implementation(libs.logback.classic)
    implementation(libs.postgresql)
    implementation(libs.postgresql.r2dbc)
    implementation(libs.aws.sdk.s3)
    implementation(ktorLibs.server.cors)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
