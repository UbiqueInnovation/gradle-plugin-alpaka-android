package ch.ubique.linth

import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class LinthPlugin : Plugin<Project> {

	override fun apply(project: Project) {
		val extension = project.extensions.create("linthPlugin", LinthPluginConfig::class.java, project)

		project.tasks.register("uploadToUbDiag", UploadToUbDiagTask::class.java) {
			it.inputApk = extension.apkFile.orNull?.asFile
			it.uploadKey = extension.uploadKey.get()

			it.proxy = extension.proxy.orNull

			/*
			it.tag.set(extension.tag)
			it.message.set(extension.message)
			it.outputFile.set(extension.outputFile)
			 */
		}
	}
}
