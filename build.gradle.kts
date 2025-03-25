plugins {
    id 'java'
    id 'com.github.johnrengelman.shadow' version '7.1.2'
}

group = 'com.ubivismedia'
version = '1.0-SNAPSHOT'
description = 'AI Dungeon Generator'

repositories {
    mavenCentral()
    maven { url 'https://hub.spigotmc.org/nexus/content/repositories/snapshots/' }
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT'
    
    // For algorithm implementations
    implementation 'org.apache.commons:commons-math3:3.6.1'
    implementation 'com.google.guava:guava:31.1-jre'
    
    // For asynchronous processing
    implementation 'com.github.ben-manes.caffeine:caffeine:3.1.5'
    
    // For configuration
    implementation 'org.yaml:snakeyaml:2.0'
    
    // For testing
    testImplementation 'org.junit.jupiter:junit-jupiter:5.9.2'
    testImplementation 'org.mockito:mockito-core:5.2.0'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

compileJava {
    options.encoding = 'UTF-8'
    options.compilerArgs += ['-parameters']
}

compileTestJava {
    options.encoding = 'UTF-8'
}

shadowJar {
    archiveClassifier.set('')
    dependencies {
        exclude(dependency('org.spigotmc:spigot-api'))
    }
    relocate 'org.apache.commons', 'com.yourdomain.aidungeon.libs.commons'
    relocate 'com.google.guava', 'com.yourdomain.aidungeon.libs.guava'
    relocate 'com.github.benmanes.caffeine', 'com.yourdomain.aidungeon.libs.caffeine'
    relocate 'org.yaml.snakeyaml', 'com.yourdomain.aidungeon.libs.snakeyaml'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

test {
    useJUnitPlatform()
}

tasks.build.dependsOn(shadowJar)
