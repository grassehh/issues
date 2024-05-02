plugins {
    kotlin("jvm") version "1.9.23"
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:2.3.10")
    testImplementation("org.zalando:logbook-core:3.8.0")
    testImplementation("org.zalando:logbook-ktor:3.8.0")
    testImplementation("org.zalando:logbook-logstash:3.8.0")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    test {
        useJUnitPlatform()
    }
}

repositories {
    mavenCentral()
}