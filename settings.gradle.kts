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

include(":appexample")
includeBuild("plugin-build")
