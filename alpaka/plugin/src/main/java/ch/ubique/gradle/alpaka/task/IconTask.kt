package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.extensions.getResDirs
import ch.ubique.gradle.alpaka.extensions.olderThan
import ch.ubique.gradle.alpaka.utils.IconUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
abstract class IconTask : DefaultTask() {

	init {
		group = "alpaka"
		description = "Generate app icon with flavor label overlay"
	}

	@get:Input
	abstract var variantName: String

	@get:Input
	abstract var flavor: String

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

		val gradleLastModified = listOf(
			File(project.projectDir, "build.gradle").lastModified(),
			File(project.projectDir, "build.gradle.kts").lastModified(),
			File(project.rootDir, "build.gradle").lastModified(),
			File(project.rootDir, "build.gradle.kts").lastModified(),
			File(project.rootDir, "settings.gradle").lastModified(),
			File(project.rootDir, "settings.gradle.kts").lastModified(),
			File(project.rootDir, "gradle/libs.versions.toml").lastModified(),
		).max()

		val resDirs = project.getResDirs(flavor)

		val allIcons = IconUtils.findIcons(resDirs, mergedManifestFile.get())

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
				IconUtils.drawLabel(webIconSource, webIconTarget, bannerLabel, adaptive = false)
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
					IconUtils.createLayeredLabel(target, bannerLabel, originalBaseName.endsWith("_foreground"))
				}
			}
		}

	}

}
