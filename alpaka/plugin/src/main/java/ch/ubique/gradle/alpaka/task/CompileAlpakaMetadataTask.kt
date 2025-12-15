@file:Suppress("DEPRECATION")

package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.extensions.moshi
import ch.ubique.gradle.alpaka.extensions.toPrettyJson
import ch.ubique.gradle.alpaka.model.AndroidBuildConfigData
import ch.ubique.gradle.alpaka.model.AppMetadata
import ch.ubique.gradle.alpaka.utils.ManifestUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault
abstract class CompileAlpakaMetadataTask : DefaultTask() {

	@get:Input
	abstract var flavorName: String

	@get:Input
	abstract var androidConfig: AndroidBuildConfigData

	@get:Input
	@get:Optional
	abstract var signature: String?

	@get:Input
	abstract var vcsCommitHistory: String

	@get:Input
	abstract var vcsBranch: String

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
	abstract var buildTime: Long

	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val resDirs: ConfigurableFileCollection

	@get:InputFile
	abstract var mergedManifestFile: Provider<File>

	@get:OutputFile
	abstract var metadataFile: Provider<File>

	@TaskAction
	fun compileAction() {
		val manifestFile = mergedManifestFile.get()

		val appName = ManifestUtils.findAppName(logger, resDirs.files.toList(), manifestFile)
			?: throw GradleException(
				"""
				Failed to find app name in string resources.
				Manifest location: ${manifestFile.absolutePath}
				Resource directories: ${resDirs.files.joinToString { it.absolutePath }}
				""".trimIndent()
			)

		val usesFeatures = ManifestUtils.findRequiredFeatures(manifestFile)

		val appMetadata = AppMetadata(
			appName = appName,
			packageName = androidConfig.applicationId,
			flavor = flavorName,
			branch = vcsBranch,
			minSdk = androidConfig.minSdk,
			targetSdk = androidConfig.targetSdk,
			usesFeature = usesFeatures,
			buildId = buildId,
			buildNumber = buildNumber,
			buildTime = buildTime,
			buildBatch = buildBatch,
			commitHash = vcsCommitHash,
			changelog = vcsCommitHistory,
			signature = signature ?: "unsigned",
			version = androidConfig.versionName,
			versionCode = androidConfig.versionCode,
		)

		val json = moshi().toPrettyJson(appMetadata)
		metadataFile.get().writeText(json)
	}

}
