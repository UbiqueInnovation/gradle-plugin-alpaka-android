plugins {
	alias(libs.plugins.androidApplication)
	alias(libs.plugins.kotlinAndroid)
	id("ch.ubique.linth")
}

android {
	namespace = "com.example.appexample"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.example.appexample"
		minSdk = 24
		targetSdk = 34
		versionCode = 1
		versionName = "0.0.1"

		testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
	}
}

dependencies {
	implementation(libs.appcompatV7)
	implementation(libs.constraintLayout)
	implementation(libs.livedata)
	implementation(libs.viewmodel)
}

linthPlugin {
	uploadKey = "f1c8846e-0c3a-44ac-b56a-53feb91d6383"
}