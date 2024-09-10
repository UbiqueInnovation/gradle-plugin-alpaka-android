package ch.ubique.linth.network.json

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UploadDataJson(
	val uploadKey: String,
	val name: String,
	val packageName: String,
	val flavor: String,
	val version: String,
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
