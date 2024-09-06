package ch.ubique.linth

import ch.ubique.linth.model.UploadRequest
import ch.ubique.linth.network.BackendRepository
import ch.ubique.linth.network.OkHttpInstance
import kotlinx.coroutines.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class UploadToUbDiagTask : DefaultTask() {

	init {
		description = "Uploads Apk to UbDiag"
		group = "publish"
	}

	@get:Input
	@get:Option(option = "uploadKey", description = "A proxy in format url:port")
	abstract var uploadKey: String

	@get:Input
	@get:Option(option = "proxy", description = "A proxy in format url:port")
	@get:Optional
	abstract var proxy: String?

	@get:Input
	abstract var uploadRequests: List<UploadRequest>

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
			coroutineScope {
				val allGood = awaitAll(
					*uploadRequests.map { uploadRequest ->
						val apkName = uploadRequest.apk.name
						async(Dispatchers.IO) {
							try {
								backendRepository.appsUpload(uploadRequest = uploadRequest, uploadKey = uploadKey)
								true
							} catch (e: Exception) {
								logger.error("${e.message?.trim()} while uploading \"$apkName\" of flavor \"${uploadRequest.flavor}\".")
								false
							}
						}
					}.toTypedArray()
				)
				if (allGood.any { it.not() }) {
					logger.lifecycle("Upload to UbDiag had errors.")
				} else {
					logger.lifecycle("Upload to UbDiag successful.")
				}
			}
		}


		//val apkFile = inputApk?.get()?.asFile

		//println("apkFile path is: " + apkFile?.path)


		/*
		val prettyTag = tag.orNull?.let { "[$it]" } ?: ""

		logger.lifecycle("$prettyTag message is: ${message.orNull}")
		logger.lifecycle("$prettyTag tag is: ${tag.orNull}")
		logger.lifecycle("$prettyTag outputFile is: ${outputFile.orNull}")

		outputFile.get().asFile.writeText("$prettyTag ${message.get()}")
		 */
	}

}
