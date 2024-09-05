package ch.ubique.linth

import ch.ubique.linth.model.UploadRequest
import ch.ubique.linth.network.BackendRepository
import ch.ubique.linth.network.OkHttpInstance
import kotlinx.coroutines.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

abstract class UploadToUbDiagTask : DefaultTask() {

	init {
		description = "Uploads Apk to UbDiag"
		group = "publish"
	}

	/*
	@get:Input
	@get:Option(option = "message", description = "A message to be printed in the output file")
	abstract val message: Property<String>

	@get:Input
	@get:Option(option = "tag", description = "A Tag to be used for debug and in the output file")
	@get:Optional
	abstract val tag: Property<String>

	@get:OutputFile
	abstract val outputFile: RegularFileProperty
	 */

	@get:InputFiles
	@get:Optional
	abstract var inputApks: List<File>?

	@get:Input
	@get:Option(option = "uploadKey", description = "A proxy in format url:port")
	abstract var uploadKey: String

	@get:Input
	@get:Option(option = "proxy", description = "A proxy in format url:port")
	@get:Optional
	abstract var proxy: String?

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
					*requireNotNull(inputApks).map { inputApk ->
						async(Dispatchers.IO) {
							val uploadRequest = UploadRequest(
								apk = inputApk,
								appIcon = inputApk,
								appName = "Some fancy name",
								packageName = "some.package.name",
								flavor = "someFancyFlavor",
								branch = "someFancyBranch",
								minSdk = 0,
								targetSdk = 0,
								usesFeature = emptyList(),
								buildNumber = 0L,
								buildTime = 0L,
								buildBatch = "buildBatch",
								changelog = "Some fancy changelog",
								signature = "someFancySignature",
								version = "SomeVersion",
								uploadKey = uploadKey,
							)
							try {
								backendRepository.appsUpload(uploadRequest)
								true
							} catch (e: Exception) {
								logger.error("${e.message?.trim()} while uploading \"${inputApk.name}\" of flavor \"${uploadRequest.flavor}\".")
								false
							}
						}
					}.toTypedArray()
				)
				if(allGood.any { it.not() }){
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
