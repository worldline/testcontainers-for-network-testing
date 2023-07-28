plugins {
    id("java")
}

group = "com.worldline"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-all:4.1.90.Final")
    implementation("org.slf4j:slf4j-api:2.0.6")

    runtimeOnly("ch.qos.logback:logback-classic:1.4.7")

    testImplementation("com.github.docker-java:docker-java-api:3.3.0")
    testImplementation("com.github.docker-java:docker-java-core:3.3.0")
    testImplementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.testcontainers:junit-jupiter:1.18.1")
    testImplementation("org.testcontainers:toxiproxy:1.18.3")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}