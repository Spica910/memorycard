pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:8.2.0") // Or a version compatible with Material 1.10.0
            }
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useVersion("1.9.0") // Or a recent Kotlin version
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MyAndroidApp"
include(":app")
