package ch.ubique.gradle.linth.config

import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class LinthPluginConfig
@Inject
constructor(project: Project) {
	private val objects = project.objects

	val uploadKey: Property<String> = objects.property(String::class.java)

	val signature: Property<String> = objects.property(String::class.java)

	val changelogCommitCount: Property<Int> = objects.property(Int::class.java)

	val proxy: Property<String> = objects.property(String::class.java)
}
