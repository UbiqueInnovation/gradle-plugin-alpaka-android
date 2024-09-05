package ch.ubique.linth.network.json

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UploadDataJson(
	val name: String,
	val packageName: String,
	val flavor: String,
	val branch: String,
	val minSdk: Int,
	val targetSdk: Int,
	val usesFeature: List<String>,
	val buildNumber: Long,
	val buildTime: Long,
	val buildBatch: String,
	val changelog: String,
	val signature: String,
	val version: String,
	val uploadKey: String,
)
