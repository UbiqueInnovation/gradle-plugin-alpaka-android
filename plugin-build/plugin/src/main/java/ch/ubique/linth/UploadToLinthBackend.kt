package ch.ubique.linth

import ch.ubique.linth.model.UploadRequest
import ch.ubique.linth.network.BackendRepository
import ch.ubique.linth.network.OkHttpInstance
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class UploadToLinthBackend : DefaultTask() {

	init {
		description = "Uploads Apk to Linth Backend"
		group = "linth"
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

	@TaskAction
	fun uploadAction() {

		proxy?.let {
			val proxyVal = it.split(":")
			OkHttpInstance.setProxy(proxyVal[0], proxyVal[1].toInt())
		} ?: run {
			OkHttpInstance.setProxy(null)
		}

		val backendRepository = BackendRepository()

		runBlocking {
			backendRepository.appsUpload(uploadRequest = uploadRequest, uploadKey = uploadKey)
			logger.lifecycle("Upload to UbDiag successful.")
		}
	}

}
