pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QuickMemo"
include(":app")

val useLocalRichEditor = providers.gradleProperty("useLocalRichEditor").orNull == "true"

if (useLocalRichEditor) {
    includeBuild("external/compose-rich-editor") {
        dependencySubstitution {
            substitute(module("com.mohamedrejeb.richeditor:richeditor-compose"))
                .using(project(":richeditor-compose"))
        }
    }
}
