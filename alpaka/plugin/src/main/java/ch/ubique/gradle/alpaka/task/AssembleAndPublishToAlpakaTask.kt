package ch.ubique.gradle.alpaka.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class AssembleAndPublishToAlpakaTask : DefaultTask() {

	init {
		group = "alpaka"
		description = "Assemble APK and upload to Alpaka"
	}

	@get:Input
	val dryrun: Boolean
		get() = project.findProperty("alpakaDryrun")?.toString()?.toBoolean() ?: false

	@TaskAction
	fun noop() = Unit

}
