package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.extensions.fromJsonNotNull
import ch.ubique.gradle.alpaka.extensions.moshi
import ch.ubique.gradle.alpaka.extensions.prettyPrint
import ch.ubique.gradle.alpaka.model.AppMetadata
import ch.ubique.gradle.alpaka.network.BackendRepository
import ch.ubique.gradle.alpaka.network.DryRunBackendRepository
import ch.ubique.gradle.alpaka.network.OkHttpInstance
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import retrofit2.HttpException
import java.io.File

@DisableCachingByDefault
abstract class PublishToAlpakaTask : DefaultTask() {

	init {
		group = "alpaka"
		description = "Upload existing APK to the Alpaka backend"
	}

	@get:Input
	abstract var uploadKey: String

	@get:Input
	@get:Option(option = "baseUrl", description = "The Alpaka backend base URL")
	abstract var baseUrl: String

	@get:Input
	@get:Option(option = "proxy", description = "A proxy in format url:port")
	@get:Optional
	abstract var proxy: String?

	@get:InputFile
	abstract var appMetadataJsonFile: Provider<File>

	@get:InputDirectory
	abstract var apkDir: Provider<Directory>

	@get:InputFile
	abstract var webIcon: Provider<File>

	@get:Input
	abstract var dryrun: Boolean

	private var projectRootDir = project.rootDir

	@TaskAction
	fun uploadAction() {
		val proxy = proxy
		if (proxy != null) {
			val (host, port) = proxy.split(":")
			OkHttpInstance.setProxy(host, port.toInt())
		} else {
			OkHttpInstance.setProxy(null)
		}

		val apkDir = apkDir.get().asFile
		val apkFile = apkDir.listFiles()
			?.singleOrNull { it.extension == "apk" }
			?: throw GradleException("No single APK found in ${apkDir.absolutePath}")

		val webIconFile = webIcon.get()

		val appMetadata = moshi().fromJsonNotNull<AppMetadata>(appMetadataJsonFile.get().readText())

		logger.lifecycle("apk file: ${apkFile.relativeTo(projectRootDir).path} (${apkFile.length() / 1024} kB)")
		logger.lifecycle("icon file: ${webIconFile.relativeTo(projectRootDir).path} (${webIconFile.length() / 1024} kB)")
		logger.lifecycle("metadata:\n${appMetadata.prettyPrint().prependIndent()}")

		logger.lifecycle("Uploading to Alpaka ($baseUrl) ... .. .")

		val backendRepository = if (dryrun) {
			DryRunBackendRepository
		} else {
			BackendRepository(baseUrl)
		}
		try {
			backendRepository.appsUpload(appMetadata, apkFile, webIconFile, uploadKey)
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

		if (dryrun) {
			logger.lifecycle("Dry-run completed.")
		} else {
			logger.lifecycle("Upload to Alpaka successful.")
		}
	}

}
