plugins {
    `java-library`
}

group = "de.sebli"
version = "2.7.1"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.18-R0.1-SNAPSHOT")
    implementation("commons-io:commons-io:2.11.0")
    implementation("commons-net:commons-net:3.8.0")
    compileOnly("org.junit.jupiter:junit-jupiter:5.8.2")
}

val targetJavaVersion = 8
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible)
            options.release.set(targetJavaVersion)
    }
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
    test {
        useJUnitPlatform()
    }
    jar {
        val dependencies = configurations.runtimeClasspath.get().map(::zipTree)
        from(dependencies)
        exclude("META-INF/LICENSE.txt", "META-INF/NOTICE.txt", "META-INF/maven/**")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
