package ch.ubique.linth

import ch.ubique.linth.model.UploadRequest
import ch.ubique.linth.network.BackendRepository
import ch.ubique.linth.network.OkHttpInstance
import com.android.build.gradle.AppExtension
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

@OptIn(DelicateCoroutinesApi::class)
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

	@get:InputFile
	@get:Optional
	abstract var inputApk: File?

	@get:Input
	@get:Option(option = "uploadKey", description = "A proxy in format url:port")
	abstract var uploadKey: String

	@get:Input
	@get:Option(option = "proxy", description = "A proxy in format url:port")
	@get:Optional
	abstract var proxy: String?

	@TaskAction
	fun uploadAction() {

		val androidExtension = getAndroidExtension()

		proxy?.let {
			val proxyVal = it.split(":")
			OkHttpInstance.setProxy(proxyVal[0], proxyVal[1].toInt())
		} ?: run {
			OkHttpInstance.setProxy(null)
		}

		val backendRepository = BackendRepository()
		val uploadRequest = UploadRequest(
			apk = requireNotNull(inputApk),
			appIcon = requireNotNull(inputApk),
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

		runBlocking {
			backendRepository.appsUpload(uploadRequest)
			println("Upload to UbDiag successful.")
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


	private fun getAndroidExtension(): AppExtension {
		val ext = project.extensions.findByType(AppExtension::class.java) ?: error(
			"Android gradle plugin extension has not been applied before"
		)
		return ext

	}

}
