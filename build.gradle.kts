plugins {
    id("java")
}

group = "com.ubivismedia"
version = "1.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("com.github.milkbowl:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("mysql:mysql-connector-java:8.0.33")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.ubivismedia.dungeonlobby.DungeonLobby"
    }
}
