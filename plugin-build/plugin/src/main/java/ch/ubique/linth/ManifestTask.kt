package ch.ubique.linth

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class ManifestTask : DefaultTask() {

	init {
		description = "Inject Metadata into Manifest"
		group = "linth"
	}

	@get:Input
	abstract var flavorAndBuildType: Set<Pair<String, String>>


	@TaskAction
	fun injectMetadataIntoManifest() {
		flavorAndBuildType.forEach { (flavor, buildType) ->
			println("Injecting metadata into manifest for flavor: $flavor and buildType: $buildType")
		}
	}

}