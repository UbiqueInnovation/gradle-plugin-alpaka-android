package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.extensions.olderThan
import ch.ubique.gradle.alpaka.utils.IconUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
abstract class LauncherIconLabelTask : DefaultTask() {

	init {
		group = "alpaka"
		description = "Generate app icon with flavor label overlay"
	}

	@get:Input
	abstract var variantName: String

	@get:Input
	abstract var buildType: String

	@get:Input
	@get:Optional
	abstract var labelValue: String?

	@get:InputFile
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract var mergedManifestFile: Provider<File>

	@get:InputFile
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract var sourceWebIconFile: Provider<File>

	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val resDirs: ConfigurableFileCollection

	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val buildLogicFiles: ConfigurableFileCollection

	@get:OutputDirectory
	abstract var generatedIconDir: Provider<Directory>

	@get:OutputFile
	abstract var generatedWebIcon: Provider<File>

	@TaskAction
	fun iconAction() {
		val webIconTarget = generatedWebIcon.get().also {
			it.parentFile.mkdirs()
			it.createNewFile()
		}
		val generatedIconDir = generatedIconDir.get().asFile

		val gradleLastModified = buildLogicFiles.files
			.asSequence()
			.map { it.lastModified() }
			.maxOrNull() ?: 0L

		val allIcons = IconUtils.findIcons(resDirs.files.toList(), mergedManifestFile.get())

		val webIconSource = sourceWebIconFile.get()
		val bannerLabel = labelValue

		if (bannerLabel.isNullOrEmpty()) {
			// delete any unwanted files
			generatedIconDir.deleteRecursively()
			// copy web icon as-is
			if (webIconSource.olderThan(webIconTarget, gradleLastModified)) {
				logger.info("No banner label, copy source icon")
				webIconSource.copyTo(webIconTarget, overwrite = true)
			}
		} else {
			if (webIconSource.olderThan(webIconTarget, gradleLastModified)) {
				logger.info("Apply banner label to web icon: ${webIconSource.absolutePath}")
				IconUtils.drawLabel(webIconSource, webIconTarget, bannerLabel, adaptive = false, monochrome = false)
			}

			allIcons.forEach iconsForEach@{ original ->
				val resTypeName = original.parentFile.name
				val originalBaseName = original.name.substringBefore(".")
				val targetDir = File(generatedIconDir, resTypeName)

				val modified = targetDir.listFiles { file ->
					file.name.matches(Regex("$originalBaseName\\.[^.]+"))
				}?.firstOrNull()

				if (modified.olderThan(original, gradleLastModified)) {
					val target = File(targetDir, original.name)
					targetDir.mkdirs()
					original.copyTo(target, overwrite = true)

					val isMonochrome = originalBaseName.endsWith("_monochrome")
					val isAdaptive = originalBaseName.endsWith("_foreground") || isMonochrome
					IconUtils.createLayeredLabel(target, bannerLabel, isAdaptive, isMonochrome)
				}
			}
		}
	}
}
