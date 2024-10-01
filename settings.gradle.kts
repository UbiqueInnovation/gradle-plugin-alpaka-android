pluginManagement {
    repositories {
		google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
}

dependencyResolutionManagement {
    repositories {
		google()
		mavenCentral()
    }
}

rootProject.name = "gradle-plugin-alpaka-android"

includeBuild("alpaka")
include(":examplekts")
include(":examplegroovy")
