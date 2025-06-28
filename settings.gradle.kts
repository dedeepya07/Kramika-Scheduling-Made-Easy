pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Removed FAIL_ON_PROJECT_REPOS to allow app-level repos
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DailyScheduler"
include(":app")