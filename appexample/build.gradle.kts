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
		versionName = "1.0"

		testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	flavorDimensions += "version"
	productFlavors {
		create("dev") {
			applicationIdSuffix = ".demo"
			versionNameSuffix = "-demo"
		}
		create("prod") {
			applicationIdSuffix = ".prod"
			versionNameSuffix = "-prod"
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
	implementation(libs.appcompatV7)
	implementation(libs.constraintLayout)
	implementation(libs.livedata)
	implementation(libs.viewmodel)
}

linthPlugin {
	//apkFile = getLayout().buildDirectory.file("outputs/apk/dev/debug/example.apk")
	uploadKey = "712d6c5e-b23a-4354-8c77-a8440d436ede"
	flavors = "dev"

	proxy = "192.168.8.167:8888"
}