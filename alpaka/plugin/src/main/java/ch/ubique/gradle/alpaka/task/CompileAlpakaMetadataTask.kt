package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.extensions.moshi
import ch.ubique.gradle.alpaka.extensions.toPrettyJson
import ch.ubique.gradle.alpaka.model.AndroidBuildConfigData
import ch.ubique.gradle.alpaka.model.AppMetadata
import ch.ubique.gradle.alpaka.utils.ManifestUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault
abstract class CompileAlpakaMetadataTask : DefaultTask() {

	@get:Input
	abstract var applicationId: Provider<String>

	@get:Input
	abstract var flavorName: String

	@get:Input
	abstract var androidConfig: AndroidBuildConfigData

	@get:Input
	abstract var signature: Provider<String>

	@get:Input
	abstract var vcsCommitHistory: Provider<String>

	@get:Input
	abstract var vcsBranch: Provider<String>

	@get:Input
	@get:Optional
	abstract var vcsCommitHash: String?

	@get:Input
	abstract var buildId: String

	@get:Input
	abstract var buildNumber: Long

	@get:Input
	abstract var buildBatch: String

	@get:Input
	abstract var buildTime: Provider<Long>

	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract var resDirs: Provider<List<Directory>>

	@get:InputFile
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val mergedManifestFile: RegularFileProperty

	@get:OutputFile
	abstract var metadataFile: Provider<File>

	@TaskAction
	fun compileAction() {
		val manifestFile = mergedManifestFile.get().asFile

		val resDirs = resDirs.get().map { it.asFile }

		val appName = ManifestUtils.findAppName(logger, resDirs, manifestFile)
			?: throw GradleException(
				"""
				Failed to find app name in string resources.
				Manifest location: ${manifestFile.absolutePath}
				Resource directories: ${resDirs.joinToString { it.absolutePath }}
				""".trimIndent()
			)

		val usesFeatures = ManifestUtils.findRequiredFeatures(manifestFile)

		val appMetadata = AppMetadata(
			appName = appName,
			packageName = applicationId.get(),
			flavor = flavorName,
			branch = vcsBranch.get(),
			minSdk = androidConfig.minSdk,
			targetSdk = androidConfig.targetSdk,
			usesFeature = usesFeatures,
			buildId = buildId,
			buildNumber = buildNumber,
			buildTime = buildTime.get(),
			buildBatch = buildBatch,
			commitHash = vcsCommitHash,
			changelog = vcsCommitHistory.get(),
			signature = signature.get(),
			version = androidConfig.versionName,
			versionCode = androidConfig.versionCode,
		)

		val json = moshi().toPrettyJson(appMetadata)
		metadataFile.get().writeText(json)
	}

}
