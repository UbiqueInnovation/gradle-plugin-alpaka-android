package ch.ubique.linth

import ch.ubique.linth.common.getMergedManifestFile
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import kotlin.math.max

abstract class IconTask : DefaultTask() {

	init {
		description = "Generates App Icon"
		group = "linth"
	}

	@get:Input
	abstract var flavors: Set<String>

	@TaskAction
	fun iconAction() {
		val moduleDir = File(project.rootDir, project.name)
		val buildDir = project.layout.buildDirectory.asFile.get()

		val gradleLastModified = max(
			File(moduleDir, "build.gradle").lastModified(),
			File(project.rootDir, "build.gradle").lastModified()
		)

		val generatedResDir = File("$buildDir/generated/res/launcher-icon/")

		flavors.forEach { flavor ->
			// get banner label
			val defaultLabelEnabled = false//android.defaultConfig.launcherIconLabelEnabled
			val flavorLabelEnabled = true//flavor.launcherIconLabelEnabled

			val bannerLabel = if (flavorLabelEnabled) {
				flavor
			} else {
				null
			}

			val manifestFile = project.getMergedManifestFile(flavor, "debug")
			println("ManifestFile: ${manifestFile.path}")
		}
	}

}
