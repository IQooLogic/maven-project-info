plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
    kotlin("jvm") version "2.0.21"
}

group = "rs.devlabs"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1.7")
        bundledPlugins(
            "com.intellij.java",
            "org.jetbrains.idea.maven",
            "org.jetbrains.idea.maven.model",
            "org.jetbrains.idea.maven.server.api",
            "Git4Idea"
        )
        pluginVerifier()
        instrumentationTools()
    }
}

intellijPlatform {
    instrumentCode = false
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
            untilBuild = "242.*"
        }
//        changeNotes = """
//            <ul>
//            <li>Improve compatibility with the recent Platform versions</li>
//            </ul>
//        """.trimIndent()
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

//    publishing {
//        val intellijPublishToken: String by project
//        token = intellijPublishToken
//    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
