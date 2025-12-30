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
	abstract var dryrun: Boolean

	@TaskAction
	fun noop() = Unit

}
