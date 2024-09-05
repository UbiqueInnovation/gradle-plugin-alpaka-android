package ch.ubique.linth

import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class LinthPluginConfig
@Inject
constructor(project: Project) {
	private val objects = project.objects

	val flavors: Property<String> = objects.property(String::class.java)
	//val apkFiles: ConfigurableFileCollection = objects.fileCollection()
	val uploadKey: Property<String> = objects.property(String::class.java)
	val proxy: Property<String> = objects.property(String::class.java)
}
