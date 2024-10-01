import ch.ubique.gradle.alpaka.extensions.applicationvariant.launcherIconLabel
import ch.ubique.gradle.alpaka.extensions.applicationvariant.alpakaUploadKey

plugins {
	alias(libs.plugins.androidApplication)
	alias(libs.plugins.kotlinAndroid)
	id("ch.ubique.gradle.alpaka")
}

android {
	namespace = "com.example.examplekts"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.example.examplekts"
		minSdk = 26
		targetSdk = 34
		versionCode = 1
		versionName = project.version.toString()

		alpakaUploadKey = "defaultConfig upload key"
	}

	flavorDimensions += "default"
	productFlavors {
		create("dev") {
			dimension = "default"
			applicationIdSuffix = ".dev"

			alpakaUploadKey = "dev flavor upload key"
			launcherIconLabel = "develop"
		}
		create("prod") {
			dimension = "default"
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlinOptions {
		jvmTarget = JavaVersion.VERSION_17.toString()
	}
}

dependencies {
	implementation(libs.core.ktx)
	implementation(libs.appcompat)
	implementation(libs.material)
	implementation(libs.activity)
}

alpaka {
	labelAppIcons = true
	changelogCommitCount = 5
}