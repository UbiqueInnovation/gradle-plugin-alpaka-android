@file:Suppress("DEPRECATION")

package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.extensions.getMergedManifestFile
import ch.ubique.gradle.alpaka.extensions.getResDirs
import ch.ubique.gradle.alpaka.model.UploadRequest
import ch.ubique.gradle.alpaka.network.BackendRepository
import ch.ubique.gradle.alpaka.network.OkHttpInstance
import ch.ubique.gradle.alpaka.utils.GitUtils
import ch.ubique.gradle.alpaka.utils.SigningConfigUtils
import ch.ubique.gradle.alpaka.utils.StringUtils
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import retrofit2.HttpException

@DisableCachingByDefault
abstract class UploadToAlpakaBackendTask : DefaultTask() {

	init {
		group = "alpaka"
		description = "Upload APK to the Ubique Alpaka backend"
	}

	@get:Input
	abstract var variant: ApplicationVariant

	@get:Input
	abstract var flavor: String

	@get:Input
	abstract var buildType: String

	@get:Input
	abstract var uploadKey: String

	@get:Input
	@get:Option(option = "proxy", description = "A proxy in format url:port")
	@get:Optional
	abstract var proxy: String?

	@get:Input
	@get:Option(option = "commitCount", description = "The number of commits to include in the changelog")
	@get:Optional
	abstract var commitCount: Int?

	@get:Input
	abstract var uploadRequest: UploadRequest

	@TaskAction
	fun uploadAction() {

		proxy?.let {
			val proxyVal = it.split(":")
			OkHttpInstance.setProxy(proxyVal[0], proxyVal[1].toInt())
		} ?: run {
			OkHttpInstance.setProxy(null)
		}

		val commitHistory = GitUtils.obtainLastCommits(project, numOfCommits = commitCount ?: 10)
		val signature = variant.signingConfig?.let {
			SigningConfigUtils(project.logger).getSignature(it) ?: "invalid"
		} ?: "unsigned"

		val uploadRequest = updateUploadRequestWithManifestInformation().copy(
			changelog = commitHistory,
			signature = signature,
		)

		logger.lifecycle("Starting upload to Alpaka.")
		try {
			val backendRepository = BackendRepository()
			backendRepository.appsUpload(uploadRequest = uploadRequest, uploadKey = uploadKey)
			logger.lifecycle("Upload to Alpaka successful.")
		} catch (e: Exception) {
			val message = if (e is HttpException) {
				e.response()?.run {
					"${errorBody()?.string()} (status ${code()})"
				}
			} else {
				null
			} ?: e.message
			throw GradleException("Upload to Alpaka failed: $message", e)
		}
	}

	private fun updateUploadRequestWithManifestInformation(): UploadRequest {
		val manifestFile = project.getMergedManifestFile(variant.name)
		val resDirs = project.getResDirs(flavor)

		val appName = StringUtils.findAppName(logger, resDirs, manifestFile)
			?: throw GradleException(
				"""
				Failed to find app name in string resources.
				Manifest location: ${manifestFile.absolutePath}
				Resource directories: ${resDirs.joinToString { it.absolutePath }}
				""".trimIndent()
			)

		val usesFeatures = StringUtils.findRequiredFeatures(manifestFile)

		return uploadRequest.copy(
			appName = appName,
			usesFeature = usesFeatures,
		)
	}

}
