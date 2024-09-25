import ch.ubique.gradle.linth.extensions.applicationvariant.launcherIconLabel
import ch.ubique.gradle.linth.extensions.applicationvariant.linthUploadKey

plugins {
	alias(libs.plugins.androidApplication)
	alias(libs.plugins.kotlinAndroid)
	id("ch.ubique.gradle.linth")
}

android {
	namespace = "com.example.appexample"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.example.appexample"
		minSdk = 26
		targetSdk = 34
		versionCode = 1
		versionName = "1.0"

		linthUploadKey = "defaultConfig upload key"
	}

	flavorDimensions += "default"
	productFlavors {
		create("dev") {
			dimension = "default"
			applicationIdSuffix = ".dev"

			linthUploadKey = "dev flavor upload key"
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
	implementation(libs.activity)
}

linth {
	labelAppIcons = true
	changelogCommitCount = 5
}