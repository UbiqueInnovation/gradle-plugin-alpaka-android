package ch.ubique.linth

import ch.ubique.linth.common.GitUtils
import ch.ubique.linth.common.StringUtils
import ch.ubique.linth.common.getMergedManifestFile
import ch.ubique.linth.common.getResDirs
import ch.ubique.linth.model.UploadRequest
import ch.ubique.linth.network.BackendRepository
import ch.ubique.linth.network.OkHttpInstance
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class UploadToLinthBackend : DefaultTask() {

	private var commitHistory: String = "no commit history"

	init {
		description = "Uploads Apk to Linth Backend"
		group = "linth"

		commitHistory = GitUtils.obtainLastCommits()
	}

	@get:Input
	@get:Option(option = "uploadKey", description = "A proxy in format url:port")
	abstract var uploadKey: String

	@get:Input
	@get:Option(option = "proxy", description = "A proxy in format url:port")
	@get:Optional
	abstract var proxy: String?

	@get:Input
	abstract var uploadRequest: UploadRequest

	@get:Input
	abstract var flavor: String

	@get:Input
	abstract var buildType: String

	@TaskAction
	fun uploadAction() {

		proxy?.let {
			val proxyVal = it.split(":")
			OkHttpInstance.setProxy(proxyVal[0], proxyVal[1].toInt())
		} ?: run {
			OkHttpInstance.setProxy(null)
		}

		val uploadRequest = updateUploadRequestWithManifestInformation().copy(
			changelog = commitHistory,
		)

		runBlocking {
			logger.lifecycle("Starting upload to UBDiag")
			try {
				val backendRepository = BackendRepository()
				backendRepository.appsUpload(uploadRequest = uploadRequest, uploadKey = uploadKey)
				logger.lifecycle("Upload to UBDiag successful.")
			} catch (e: Exception) {
				throw GradleException("Upload to UBDiag failed: ${e.message}", e)
			}
		}
	}

	private fun updateUploadRequestWithManifestInformation(): UploadRequest {
		val manifestFile = project.getMergedManifestFile(flavor, buildType)
		val resDirs = project.getResDirs(flavor)

		val appName = StringUtils.findAppName(logger, resDirs, manifestFile)
			?: throw GradleException(
				"""
				Failed to find app name in string resources.
				Manifest location: ${manifestFile.absolutePath}
				Resource directories: ${resDirs.joinToString { it.absolutePath }}
				""".trimIndent()
			)

		return uploadRequest.copy(
			appName = appName,
		)
	}

}
