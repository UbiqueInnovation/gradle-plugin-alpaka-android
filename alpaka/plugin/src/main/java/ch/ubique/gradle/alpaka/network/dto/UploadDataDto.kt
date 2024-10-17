package ch.ubique.gradle.alpaka.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UploadDataDto(
	val uploadKey: String,
	val name: String,
	val packageName: String,
	val flavor: String,
	val version: String,
	val versionCode: Long,
	val signature: String,
	val minSdk: Int,
	val targetSdk: Int,
	val usesFeature: List<String>,
	val branch: String,
	val changelog: String,
	val buildId: String,
	val buildNumber: Long,
	val buildTime: Long,
	val buildBatch: String,
)
