package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.utils.IconUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File

abstract class LauncherIconLabelTask : DefaultTask() {

	init {
		group = "alpaka"
		description = "Generate app icon with flavor label overlay"
	}

	@get:Input
	@get:Optional
	abstract var labelValue: String?

	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract var manifestFiles: Provider<out List<RegularFile>>

	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract var resDirs: Provider<List<Directory>>

	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val buildLogicFiles: ConfigurableFileCollection

	@get:OutputDirectory
	abstract val generatedIconDir: DirectoryProperty

	@TaskAction
	fun iconAction() {
		val generatedIconDir = generatedIconDir.get().asFile
		val resDirs = resDirs.get().map { it.asFile }
		val manifestFiles = manifestFiles.get().map { it.asFile }

		val buildSrcLastModified = (buildLogicFiles.files + manifestFiles).maxOfOrNull { it.lastModified() } ?: 0L

		val allIcons = IconUtils.findIcons(resDirs, manifestFiles)

		val bannerLabel = labelValue

		if (bannerLabel.isNullOrEmpty()) {
			// delete any unwanted files
			generatedIconDir.deleteRecursively()
		} else {
			allIcons.forEach { original ->
				val resTypeName = original.parentFile.name
				val originalBaseName = original.name.substringBefore(".")
				val targetDir = File(generatedIconDir, resTypeName)

				val sourcesLastModified = original.lastModified().coerceAtLeast(buildSrcLastModified)
				val targetLastModified = targetDir
					.listFiles { file -> file.name.matches(Regex("$originalBaseName\\.[^.]+")) }
					?.firstOrNull()
					?.lastModified() ?: 0L

				if (targetLastModified < sourcesLastModified) {
					val copy = File(targetDir, original.name)
					targetDir.mkdirs()
					original.copyTo(copy, overwrite = true)

					val isMonochrome = originalBaseName.endsWith("_monochrome")
					val isAdaptive = originalBaseName.endsWith("_foreground") || isMonochrome
					IconUtils.createLayeredLabel(copy, bannerLabel, isAdaptive, isMonochrome)
				}
			}
		}
	}
}
