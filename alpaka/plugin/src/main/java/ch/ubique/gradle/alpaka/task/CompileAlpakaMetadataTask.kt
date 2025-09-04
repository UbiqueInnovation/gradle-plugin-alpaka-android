@file:Suppress("DEPRECATION")

package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.extensions.getResDirs
import ch.ubique.gradle.alpaka.extensions.moshi
import ch.ubique.gradle.alpaka.extensions.toPrettyJson
import ch.ubique.gradle.alpaka.model.AppMetadata
import ch.ubique.gradle.alpaka.utils.GitUtils
import ch.ubique.gradle.alpaka.utils.ManifestUtils
import ch.ubique.gradle.alpaka.utils.SigningConfigUtils
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.DefaultConfig
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault
abstract class CompileAlpakaMetadataTask : DefaultTask() {

	@get:Internal
	abstract var androidConfig: DefaultConfig

	@get:Internal
	abstract var variant: ApplicationVariant

	@get:Input
	@get:Option(option = "commitCount", description = "The number of commits to include in the changelog")
	@get:Optional
	abstract var vcsCommitCount: Int?

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

	@get:InputFile
	abstract var mergedManifestFile: Provider<File>

	@get:OutputFile
	abstract var metadataFile: Provider<File>

	@TaskAction
	fun uploadAction() {
		val manifestFile = mergedManifestFile.get()
		val resDirs = project.getResDirs(variant.flavorName)

		val appName = ManifestUtils.findAppName(logger, resDirs, manifestFile)
			?: throw GradleException(
				"""
				Failed to find app name in string resources.
				Manifest location: ${manifestFile.absolutePath}
				Resource directories: ${resDirs.joinToString { it.absolutePath }}
				""".trimIndent()
			)

		val usesFeatures = ManifestUtils.findRequiredFeatures(manifestFile)

		val signature = variant.signingConfig?.let {
			SigningConfigUtils(project.logger).getSignature(it) ?: "invalid"
		} ?: "unsigned"

		val commitHistory = GitUtils.obtainLastCommits(project, numOfCommits = vcsCommitCount ?: 10)

		val appMetadata = AppMetadata(
			appName = appName,
			packageName = variant.applicationId,
			flavor = variant.flavorName,
			branch = vcsBranch,
			minSdk = requireNotNull(androidConfig.minSdk),
			targetSdk = requireNotNull(androidConfig.targetSdk),
			usesFeature = usesFeatures,
			buildId = buildId,
			buildNumber = buildNumber,
			buildTime = buildTime,
			buildBatch = buildBatch,
			commitHash = vcsCommitHash,
			changelog = commitHistory,
			signature = signature,
			version = requireNotNull(androidConfig.versionName),
			versionCode = variant.versionCode.toLong()
		)

		val json = moshi().toPrettyJson(appMetadata)
		metadataFile.get().writeText(json)
	}

}
