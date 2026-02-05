plugins {
	alias(libs.plugins.androidApplication)
	id("ch.ubique.gradle.alpaka")
}

android {
	namespace = "com.example.examplekts"
	compileSdk = 36

	defaultConfig {
		applicationId = "com.example.examplekts"
		minSdk = 26
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

		alpakaUploadKey = "defaultConfig upload key"
	}

	flavorDimensions += "default"

	productFlavors {
		configureEach {
			resValue("string", "app_name", "Alpaka Plugin KTS DSL Test App ($name)")
		}
		create("dev") {
			dimension = "default"
			applicationIdSuffix = ".dev"

			if (launcherIconLabel != "dev") {
				error("Expected launcherIconLabel 'dev' but was $launcherIconLabel")
			}
		}
		create("tescht") {
			dimension = "default"
			applicationIdSuffix = ".test"
			alpakaUploadKey = "test flavor upload key"
			launcherIconLabel = "test"

			if (launcherIconLabel != "test") {
				error("Expected launcherIconLabel 'test' but was $launcherIconLabel")
			}
		}
		create("prod") {
			dimension = "default"

			if (launcherIconLabel != null) {
				error("Expected launcherIconLabel null but was $launcherIconLabel")
			}
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	buildFeatures {
		resValues = true
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
