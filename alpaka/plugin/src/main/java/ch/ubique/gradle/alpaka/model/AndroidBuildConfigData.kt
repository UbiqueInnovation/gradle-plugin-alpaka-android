package ch.ubique.gradle.alpaka.model

import java.io.Serializable

data class AndroidBuildConfigData(
	val applicationId: String,
	val minSdk: Int,
	val targetSdk: Int,
	val versionName: String,
	val versionCode: Long,
) : Serializable
