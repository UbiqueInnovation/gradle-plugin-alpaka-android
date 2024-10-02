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

includeBuild("alpaka")
include(":examplekts")
include(":examplegroovy")
