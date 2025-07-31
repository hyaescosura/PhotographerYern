pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // ✅ REQUIRED
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://firebase.google.com/download/ai/maven")
        }
    }
}

rootProject.name = "PhotagrapherYern"
include(":app")
