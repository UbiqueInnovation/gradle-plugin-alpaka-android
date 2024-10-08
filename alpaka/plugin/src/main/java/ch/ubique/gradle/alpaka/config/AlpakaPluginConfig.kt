package ch.ubique.gradle.alpaka.config

import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class AlpakaPluginConfig
@Inject
constructor(project: Project) {
	private val objects = project.objects

	val changelogCommitCount: Property<Int> = objects.property(Int::class.java)

	val proxy: Property<String> = objects.property(String::class.java)

	val labelAppIcons: Property<Boolean> = objects.property(Boolean::class.java)
}
