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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Scenevo"

include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:database")
include(":core:datastore")
include(":domain")
include(":data")
include(":engine:timeline")
include(":engine:render")
include(":engine:tts")
include(":feature:home")
include(":feature:create")
include(":feature:editor")
include(":feature:export")
include(":feature:settings")
