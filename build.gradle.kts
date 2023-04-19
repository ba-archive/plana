plugins {
  val kotlinVersion = "1.7.10"
  kotlin("jvm") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion

  id("net.mamoe.mirai-console") version "2.14.0"
}

group = "io.blue-archive"
version = "0.1.0"

repositories {
  maven("https://maven.aliyun.com/repository/public")
  mavenCentral()
}

dependencies {
  implementation("com.tencentcloudapi:tencentcloud-sdk-java:3.1.741")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.10")
  testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
  testImplementation("io.kotest:kotest-runner-junit5:5.3.0")
  testImplementation("io.kotest:kotest-assertions-core:5.3.0")
}
