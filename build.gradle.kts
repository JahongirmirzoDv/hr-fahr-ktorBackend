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


//    implementation("io.ktor:ktor-server-partial-content-jvm:$ktorVersion")
//    implementation("io.ktor:ktor-server-auto-head-response-jvm:$ktorVersion")
//
//    // For multipart data
//    implementation("io.ktor:ktor-server-multipart-jvm:$ktorVersion")
//
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")

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

    implementation("org.bytedeco:javacv-platform:1.5.10")

//    implementation("java.awt:java.awt.image:1.0")


//    implementation("ai.djl:api:0.26.0")
//    implementation("ai.djl.pytorch:pytorch-engine:0.26.0")
//    runtimeOnly("ai.djl.pytorch:pytorch-native-cpu:0.26.0")
//    implementation("ai.djl.onnxruntime:onnxruntime-engine:0.26.0")
//
//    // REMOVE or COMMENT OUT the mxnet-model-zoo if it's not needed
//    // implementation("ai.djl.mxnet:mxnet-model-zoo:0.26.0")
//
//    // ADD the PyTorch model zoo
//    implementation("ai.djl.pytorch:pytorch-model-zoo:0.26.0")



    implementation("at.favre.lib:bcrypt:0.9.0")
}
