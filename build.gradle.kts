val koinVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.3.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

group = "uz.mobiledv"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven")
        name = "confluence"
    }
}

dependencies {

    val exposedVersion = "0.50.1"
    val ktorVersion = "2.3.4"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-server-config-yaml:$ktorVersion")
    implementation("com.zaxxer:HikariCP:6.3.0")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")


    implementation("io.ktor:ktor-server-status-pages-jvm:${ktorVersion}")

//    implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:$kotlinVersion"))
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") // Or use "1.6.3"
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion") // Or use "1.6.3"


    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
//    implementation("org.jetbrains.exposed:exposed-java-time:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${exposedVersion}")
    implementation("com.h2database:h2:2.2.224")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")



    // Add DJL dependencies for Face Recognition
    implementation("ai.djl:api:0.31.1")
    implementation("ai.djl:model-zoo:0.31.1")
    // Use the ONNX Runtime engine
    implementation("ai.djl.onnxruntime:onnxruntime-engine:0.31.1")
    // Native library for your OS. For production, you might need others.
    // This is for macOS with an ARM processor (like M1/M2/M3).
    // implementation("ai.djl.onnxruntime:onnxruntime-native-mkl:0.27.0-mac-aarch64")
    // For Linux x86_64:
//    implementation("ai.djl.onnxruntime:onnxruntime-native-mkl:0.27.0-linux-x86_64")
    // For Windows x86_64:
    // implementation("ai.djl.onnxruntime:onnxruntime-native-mkl:0.27.0-win-x86_64")

    // For image manipulation
    implementation("org.imgscalr:imgscalr-lib:4.2")
//    implementation("java.xml.bind:jaxb-api:2.3.1")


    implementation("at.favre.lib:bcrypt:0.9.0")
}
