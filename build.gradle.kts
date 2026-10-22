plugins {
    java
}

group = "fr.cykalos"
version = "1.4.0"

// Paper 26.1+ tourne sur Java 25
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Version de l'API Paper visée (26.1.x). Pour 26.2, remplacer par une build 26.2.x.
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.61-stable")
    // SQLite n'est pas embarqué : Paper le télécharge au démarrage via "libraries" dans plugin.yml
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") { expand(props) }
    }

    jar {
        archiveFileName.set("Randomize_Chest-${project.version}.jar")
        from("LICENSE")
        // Pack de ressources embarqué, extrait au démarrage dans plugins/Randomize_Chest/resourcepack/
        from("resourcepack") {
            into("resourcepack")
            exclude("LISEZMOI.md")
        }
    }
}
