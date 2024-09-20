package ch.ubique.gradle.linth.model

import ch.ubique.gradle.linth.network.dto.UploadDataDto
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
	val buildId: String,
	val buildNumber: Long,
	val buildTime: Long,
	val buildBatch: String,
	val changelog: String,
	val signature: String,
	val version: String,
) {

	fun toUploadDataJson(uploadKey: String) = UploadDataDto(
		uploadKey = uploadKey,
		name = appName,
		packageName = packageName,
		flavor = flavor,
		version = version,
		signature = signature,
		minSdk = minSdk,
		targetSdk = targetSdk,
		usesFeature = usesFeature,
		branch = branch,
		changelog = changelog,
		buildId = buildId,
		buildNumber = buildNumber,
		buildTime = buildTime,
		buildBatch = buildBatch,
	)

}