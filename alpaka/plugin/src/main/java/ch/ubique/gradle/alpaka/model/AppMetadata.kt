package ch.ubique.gradle.alpaka.model

import ch.ubique.gradle.alpaka.network.dto.UploadDataDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppMetadata(
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
	val commitHash: String?,
	val changelog: String,
	val signature: String,
	val version: String,
	val versionCode: Long,
) {

	fun toUploadDataDto(uploadKey: String) = UploadDataDto(
		uploadKey = uploadKey,
		name = appName,
		packageName = packageName,
		flavor = flavor,
		version = version,
		versionCode = versionCode,
		signature = signature,
		minSdk = minSdk,
		targetSdk = targetSdk,
		usesFeature = usesFeature,
		branch = branch,
		commitHash = commitHash,
		changelog = changelog,
		buildId = buildId,
		buildNumber = buildNumber,
		buildTime = buildTime,
		buildBatch = buildBatch,
	)

}
