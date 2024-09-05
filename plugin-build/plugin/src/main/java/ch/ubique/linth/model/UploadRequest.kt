package ch.ubique.linth.model

import ch.ubique.linth.network.json.UploadDataJson
import java.io.File

data class UploadRequest(
	val apk: File,
	val appIcon: File,
	val appName: String,
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
) {

	fun toUploadDataJson() = UploadDataJson(
		name = appName,
		packageName = packageName,
		flavor = flavor,
		branch = branch,
		minSdk = minSdk,
		targetSdk = targetSdk,
		usesFeature = usesFeature,
		buildNumber = buildNumber,
		buildTime = buildTime,
		buildBatch = buildBatch,
		changelog = changelog,
		signature = signature,
		version = version,
		uploadKey = uploadKey,
	)

}
