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
        // Solo para libsu (terminal). El repositorio de Guardian Project se retiró:
        // ninguna dependencia lo usaba y era un origen más de código sin firmar.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "NexusChat"
include(":app")
