package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.extensions.getMergedManifestFile
import ch.ubique.gradle.alpaka.extensions.getResDirs
import ch.ubique.gradle.alpaka.extensions.olderThan
import ch.ubique.gradle.alpaka.utils.IconUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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

	@get:Input
	abstract var targetWebIconFile: Provider<File>

	@get:OutputDirectory
	abstract var generatedIconDir: Provider<Directory>

	@TaskAction
	fun iconAction() {
		val moduleDir = File(project.rootDir, project.name)
		val targetWebIcon = targetWebIconFile.get().also {
			it.parentFile.mkdirs()
			it.createNewFile()
		}

		val gradleLastModified = listOf(
			File(moduleDir, "build.gradle").lastModified(),
			File(moduleDir, "build.gradle.kts").lastModified(),
			File(project.rootDir, "build.gradle").lastModified(),
			File(project.rootDir, "build.gradle.kts").lastModified(),
			File(project.rootDir, "settings.gradle").lastModified(),
			File(project.rootDir, "settings.gradle.kts").lastModified(),
			File(project.rootDir, "gradle/libs.versions.toml").lastModified(),
		).max()

		val manifestFile = project.getMergedManifestFile(variantName)
		val resDirs = project.getResDirs(flavor)

		val allIcons = IconUtils.findIcons(resDirs, manifestFile)

		val webIconSource = (
				(File(moduleDir, "src/$flavor").listFiles() ?: arrayOf()) +
						(File(moduleDir, "src/main").listFiles() ?: arrayOf()) +
						(moduleDir.listFiles() ?: arrayOf())
				).find { it.name.matches(Regex(".*(web|playstore|512)\\.(png|webp)")) }
			?: throw GradleException("Web icon source not found")

		val bannerLabel = labelValue

		if (bannerLabel.isNullOrEmpty()) {
			if (webIconSource.olderThan(targetWebIcon, gradleLastModified)) {
				logger.info("No banner label, copy source icon")
				webIconSource.copyTo(targetWebIcon, overwrite = true)
			}
		} else {
			if (webIconSource.olderThan(targetWebIcon, gradleLastModified)) {
				logger.info("Apply banner label to web icon: ${webIconSource.absolutePath}")
				IconUtils.drawLabel(webIconSource, targetWebIcon, bannerLabel, adaptive = false)
			}

			allIcons.forEach iconsForEach@{ original ->
				val resTypeName = original.parentFile.name
				val originalBaseName = original.name.substringBefore(".")
				val targetDir = File(generatedIconDir.get().asFile, resTypeName)

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
