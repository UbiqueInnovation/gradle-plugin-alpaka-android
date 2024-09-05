package ch.ubique.linth

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class LinthPluginConfig
@Inject
constructor(project: Project) {
	private val objects = project.objects

	val apkFile: RegularFileProperty = objects.fileProperty()
	val uploadKey: Property<String> = objects.property(String::class.java)

	val proxy: Property<String> = objects.property(String::class.java)
}
