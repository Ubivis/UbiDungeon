plugins {
    id 'java'
}

group = 'com.ubivismedia'
version = '1.0-SNAPSHOT'

repositories {
    mavenCentral()
    maven { url = 'https://papermc.io/repo/repository/maven-public/' }
    maven { url = 'https://jitpack.io' }
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.24-R0.1-SNAPSHOT'
    compileOnly 'net.milkbowl.vault:VaultAPI:1.7'
    compileOnly 'me.clip:placeholderapi:2.11.3'
    implementation 'org.xerial:sqlite-jdbc:3.36.0.3'
    implementation 'mysql:mysql-connector-java:8.0.33'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

jar {
    manifest {
        attributes 'Main-Class': 'com.ubivismedia.dungeonlobby.DungeonLobby'
    }
}
