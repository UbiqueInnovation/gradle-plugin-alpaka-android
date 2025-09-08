package ch.ubique.gradle.alpaka.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
@Deprecated("Deprecated, kept for backwards compatibility, use AssembleAndPublishToAlpakaTask instead")
abstract class UploadToAlpakaBackendTask : DefaultTask() {

	@TaskAction
	fun noop() = Unit

}
