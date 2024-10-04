@file:Suppress("DEPRECATION")

package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.extensions.getResDirs
import ch.ubique.gradle.alpaka.model.UploadRequest
import ch.ubique.gradle.alpaka.network.BackendRepository
import ch.ubique.gradle.alpaka.network.OkHttpInstance
import ch.ubique.gradle.alpaka.utils.GitUtils
import ch.ubique.gradle.alpaka.utils.ManifestUtils
import ch.ubique.gradle.alpaka.utils.SigningConfigUtils
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import retrofit2.HttpException
import java.io.File

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

	@get:InputFile
	abstract var mergedManifestFile: Provider<File>

	@get:InputFile
	abstract var apk: Provider<File>

	@get:InputFile
	abstract var webIcon: Provider<File>

	@TaskAction
	fun uploadAction() {
		val proxy = proxy
		if (proxy != null) {
			val (host, port) = proxy.split(":")
			OkHttpInstance.setProxy(host, port.toInt())
		} else {
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
			backendRepository.appsUpload(uploadRequest, apk.get(), webIcon.get(), uploadKey)
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
		val manifestFile = mergedManifestFile.get()
		val resDirs = project.getResDirs(flavor)

		val appName = ManifestUtils.findAppName(logger, resDirs, manifestFile)
			?: throw GradleException(
				"""
				Failed to find app name in string resources.
				Manifest location: ${manifestFile.absolutePath}
				Resource directories: ${resDirs.joinToString { it.absolutePath }}
				""".trimIndent()
			)

		val usesFeatures = ManifestUtils.findRequiredFeatures(manifestFile)

		return uploadRequest.copy(
			appName = appName,
			usesFeature = usesFeatures,
		)
	}

}
