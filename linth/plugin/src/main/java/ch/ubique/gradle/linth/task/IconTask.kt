package ch.ubique.gradle.linth.task

import ch.ubique.gradle.linth.extensions.getMergedManifestFile
import ch.ubique.gradle.linth.extensions.getResDirs
import ch.ubique.gradle.linth.utils.IconUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.File
import kotlin.math.max

@CacheableTask
abstract class IconTask : DefaultTask() {

	init {
		group = "linth"
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
	abstract var targetWebIconPath: String?

	@get:OutputDirectory
	abstract var generatedIconDir: File?

	init {
		val buildDir = project.layout.buildDirectory.asFile.get()
		generatedIconDir = File("$buildDir/generated/res/launcher-icon/")
	}

	@TaskAction
	fun iconAction() {
		val moduleDir = File(project.rootDir, project.name)
		val buildDir = project.layout.buildDirectory.asFile.get()
		val targetWebIcon = targetWebIconPath?.let { path ->
			File(path).also {
				it.parentFile.mkdirs()
				it.createNewFile()
			}
		}

		val gradleLastModified = max(
			File(moduleDir, "build.gradle").lastModified(),
			File(project.rootDir, "build.gradle").lastModified()
		)

		// get banner label
		val defaultLabelEnabled = false//android.defaultConfig.launcherIconLabelEnabled
		val flavorLabelEnabled = true//flavor.launcherIconLabelEnabled

		val bannerLabel = if (flavorLabelEnabled && !flavor.startsWith("prod")) {
			flavor
		} else {
			null
		}

		val manifestFile = project.getMergedManifestFile(variantName)
		val resDirs = project.getResDirs(flavor)

		val allIcons = IconUtils.findIcons(resDirs, manifestFile)

		if (targetWebIcon != null) {
			targetWebIcon.delete()

			// Search for web icon source
			val webIconSource = (
					(File(moduleDir, "src/${flavor.lowercase()}").listFiles() ?: arrayOf()) +
							(File(moduleDir, "src/main").listFiles() ?: arrayOf()) +
							(moduleDir.listFiles() ?: arrayOf())
					).find { it.name.matches(Regex(".*(web|playstore|512)\\.(png|webp)")) }
				?: IconUtils.findLargestIcon(allIcons)  // Fallback if not found

			if (webIconSource == null) {
				logger.warn("Web icon source not found")
			} else if (bannerLabel.isNullOrEmpty()) {
				logger.info("No banner label, copy source icon")
				webIconSource.copyTo(targetWebIcon, overwrite = true)
			} else {
				logger.info("Apply banner label to web icon: ${webIconSource.absolutePath}")
				IconUtils.drawLabel(webIconSource, targetWebIcon, bannerLabel, adaptive = false)
			}
		}

		if (bannerLabel.isNullOrEmpty()) {
			// No label
			logger.info("No banner label, skipping icon labelling")
			return
		}

		allIcons.forEach iconsForEach@{ original ->
			val resTypeName = original.parentFile.name
			val originalBaseName = original.name.substringBefore(".")
			val targetDir = File("${generatedIconDir}/${flavor.lowercase()}/$resTypeName")

			val modified = targetDir.listFiles { file ->
				file.name.matches(Regex("$originalBaseName\\.[^.]+"))
			}?.firstOrNull()

			if (modified != null
				&& original.lastModified() <= modified.lastModified()
				&& gradleLastModified <= modified.lastModified()
			) {
				return@iconsForEach
			} else {
				val target = File(targetDir, original.name)
				targetDir.mkdirs()
				original.copyTo(target, overwrite = true)
				IconUtils.createLayeredLabel(target, bannerLabel, originalBaseName.endsWith("_foreground"))
			}

		}

	}

}