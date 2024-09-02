pluginManagement {
    repositories {
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

include(":example")
includeBuild("plugin-build")
