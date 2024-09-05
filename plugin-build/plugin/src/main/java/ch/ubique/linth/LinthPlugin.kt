package ch.ubique.linth

import ch.ubique.linth.common.capitalize
import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

abstract class LinthPlugin : Plugin<Project> {

	override fun apply(project: Project) {
		val extension = project.extensions.create("linthPlugin", LinthPluginConfig::class.java, project)

		val androidExtension = getAndroidExtension(project)

		project.tasks.register("uploadToUbDiag", UploadToUbDiagTask::class.java) { uploadTask ->
			uploadTask.uploadKey = extension.uploadKey.get()
			uploadTask.proxy = extension.proxy.orNull

			//val apkFilesNotSet = extension.apkFiles.isEmpty.not()

			// setup upload task
			val releaseApks = mutableListOf<File>()
			val uploadFlavors = extension.flavors.orNull?.split(",")?.map { it.trim() }

			androidExtension.applicationVariants.forEach { variant ->
				val flavor = variant.flavorName.capitalize()
				if (uploadFlavors == null || uploadFlavors.contains(variant.flavorName)) {
					uploadTask.dependsOn(project.tasks.named("assemble${flavor}Release"))

					variant.outputs.forEach {
						if (it.outputFile.parentFile.name == "release") {
							releaseApks.add(it.outputFile)
						}
					}
				}
			}

			uploadTask.inputApks = releaseApks
		}

	}

	private fun getAndroidExtension(project: Project): AppExtension {
		val ext = project.extensions.findByType(AppExtension::class.java) ?: error(
			"Android gradle plugin extension has not been applied before"
		)
		return ext
	}
}
