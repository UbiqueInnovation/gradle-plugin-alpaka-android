package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.utils.IconUtils
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File

abstract class WebIconLabelTask : DefaultTask() {

	init {
		group = "alpaka"
		description = "Generate web icon with flavor label overlay"
	}

	@get:Input
	@get:Optional
	abstract var labelValue: String?

	@get:InputFile
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract var sourceWebIconFile: Provider<File>

	@get:OutputFile
	abstract var generatedWebIcon: Provider<File>

	@TaskAction
	fun iconAction() {
		val webIconTarget = generatedWebIcon.get().also {
			it.parentFile.mkdirs()
			it.createNewFile()
		}

		val webIconSource = sourceWebIconFile.get()
		val bannerLabel = labelValue

		if (bannerLabel.isNullOrEmpty()) {
			// copy web icon as-is
			logger.info("No banner label, copy source icon")
			webIconSource.copyTo(webIconTarget, overwrite = true)
		} else {
			logger.info("Apply banner label to web icon: ${webIconSource.absolutePath}")
			IconUtils.drawLabel(webIconSource, webIconTarget, bannerLabel, adaptive = false, monochrome = false)
		}
	}
}
