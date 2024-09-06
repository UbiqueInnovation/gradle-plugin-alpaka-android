package ch.ubique.linth

import ch.ubique.linth.common.IconUtils
import ch.ubique.linth.common.getMergedManifestFile
import com.android.build.gradle.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import kotlin.math.max

abstract class IconTask : DefaultTask() {

	init {
		description = "Generates App Icon"
		group = "linth"
	}

	@get:Input
	abstract var flavor: String

	@get:Input
	abstract var buildType: String

	@get:InputFile
	@get:Optional
	abstract var targetWebIcon: File?

	@TaskAction
	fun iconAction() {
		val moduleDir = File(project.rootDir, project.name)
		val buildDir = project.layout.buildDirectory.asFile.get()
		val targetWebIcon = targetWebIcon

		val gradleLastModified = max(
			File(moduleDir, "build.gradle").lastModified(),
			File(project.rootDir, "build.gradle").lastModified()
		)

		val generatedResDir = File("$buildDir/generated/res/launcher-icon/")
		
		// get banner label
		val defaultLabelEnabled = false//android.defaultConfig.launcherIconLabelEnabled
		val flavorLabelEnabled = true//flavor.launcherIconLabelEnabled

		val bannerLabel = if (flavorLabelEnabled) {
			flavor
		} else {
			null
		}

		val manifestFile = project.getMergedManifestFile(flavor, buildType)

		val androidModules: List<BaseExtension> = project.configurations
			.asSequence()
			.flatMap { it.dependencies }
			.filterIsInstance<ProjectDependency>()
			.map { it.dependencyProject }
			.distinct()
			.mapNotNull { it.extensions.findByType(BaseExtension::class.java) }
			.toList()

		val resDirs: List<File> = androidModules
			.flatMap {
				listOfNotNull(
					it.sourceSets.findByName(flavor),
					it.sourceSets.findByName("main")
				)
			}
			.flatMap { it.res.srcDirs }
			.filter { !it.path.contains("generated") }

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
				println("$name: web icon source not found")
			} else if (bannerLabel.isNullOrEmpty()) {
				// No label, so we only copy the sourceIcon
				println("$name: web icon: $webIconSource")
				webIconSource.copyTo(targetWebIcon, overwrite = true)
			} else {
				println("$name: web icon: $webIconSource")
				IconUtils.drawLabel(webIconSource, targetWebIcon, bannerLabel, adaptive = false)
			}
		}

		if (bannerLabel.isNullOrEmpty()) {
			// No label
			println("$name: skipped icon labelling")
			return
		}

		allIcons.forEach iconsForEach@{ original ->
			val resTypeName = original.parentFile.name
			val originalBaseName = original.name.substringBefore(".")
			val targetDir = File("${generatedResDir}/${flavor.lowercase()}/$resTypeName")

			val modified = targetDir.listFiles { file ->
				file.name.matches(Regex("$originalBaseName\\.[^.]+"))
			}?.firstOrNull()

			if (modified != null
				&& original.lastModified() <= modified.lastModified()
				&& gradleLastModified <= modified.lastModified()
			) {
				return@iconsForEach
			} else {
				println("bannerlabel is $bannerLabel")
				val target = File(targetDir, original.name)
				targetDir.mkdirs()
				original.copyTo(target, overwrite = true)
				IconUtils.createLayeredLabel(target, bannerLabel, originalBaseName.endsWith("_foreground"))
			}

		}

	}

}
