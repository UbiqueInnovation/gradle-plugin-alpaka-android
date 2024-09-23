
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
		minSdk = 24
		targetSdk = 34
		versionCode = 1
		versionName = "0.0.1"

		testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
	}

	flavorDimensions.add("default")
	productFlavors {
		create("dev") {
			dimension = "default"
			applicationIdSuffix = ".dev"

			extraProperties.set("launcherIconLabel", "blub")
			extraProperties.set("uploadKey", "linth-example-flavor-dev-upload-key")
		}
		create("prod") {
			dimension = "default"
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}

tasks.withType(KotlinCompile::class.java) {
	compilerOptions.jvmTarget = JvmTarget.JVM_17
	@Suppress("DEPRECATION")
	kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
}

dependencies {
	implementation(libs.appcompatV7)
	implementation(libs.constraintLayout)
	implementation(libs.livedata)
	implementation(libs.viewmodel)
}

linthPlugin {
	uploadKey = "linth-example-upload-key"
	labelAppIcons = true
	changelogCommitCount = 5
}