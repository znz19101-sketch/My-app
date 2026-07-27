
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

rootProject.name = "Guardexa"

include(
    ":app",
    ":core-common",
    ":core-model",
    ":core-domain",
    ":core-data",
    ":core-database",
    ":core-security",
    ":core-policy",
    ":core-time",
    ":core-ui",
    ":core-resilience",
    ":ai-core",
    ":ai-camera-face",
    ":ai-glasses-mediapipe",
    ":ai-liveness-evidence",
    ":device-platform",
    ":feature-apps",
    ":feature-usage-schedules",
    ":feature-ui-core",
    ":feature-reports-notifications"
)
