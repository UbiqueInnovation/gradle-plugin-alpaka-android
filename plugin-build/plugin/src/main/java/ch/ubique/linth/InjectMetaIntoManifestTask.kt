package ch.ubique.linth

import ch.ubique.linth.common.GitUtils
import ch.ubique.linth.common.capitalize
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class InjectMetaIntoManifestTask : DefaultTask() {

	private val METADATA_KEY_BUILDID = "ch.ubique.linth.buildid"
	private val METADATA_KEY_BUILDNUMBER = "ch.ubique.linth.buildnumber"
	private val METADATA_KEY_BRANCH = "ch.ubique.linth.branch"
	private val METADATA_KEY_FLAVOR = "ch.ubique.linth.flavor"


	private var buildId: String = "0"
	private var buildNumber: String = "0"
	private var buildBranch: String = "master"
	private var buildFlavor: String = "default"


	init {
		description = "Inject Metadata into Manifest"
		group = "linth"
		buildId = project.findProperty("buildid")?.toString() ?: project.findProperty("ubappid")?.toString() ?: "localbuild"
		buildNumber = project.findProperty("buildnumber")?.toString() ?: "0"
		buildBranch = project.findProperty("branch")?.toString() ?: GitUtils.obtainBranch()

	}

	@get:Input
	abstract var flavorAndBuildType: Set<Pair<String, String>>


	@TaskAction
	fun injectMetadataIntoManifest() {
		flavorAndBuildType.forEach { (flavor, buildType) ->
			println("Injecting metadata into manifest for flavor: $flavor and buildType: $buildType")
			val variantName = flavor + buildType.capitalize()
			val manifestFile = File(
				project.layout.buildDirectory.asFile.get(),
				"intermediates/merged_manifests/${variantName}/process${variantName}Manifest/AndroidManifest.xml"
			)
			if (manifestFile.exists()) {
				manipulateManifestFile(manifestFile)
			} else {
				println("Manifest file not found for flavor: $flavor and buildType: $buildType")
			}

		}
	}


	/**
	 * Add custom meta data to manifest.
	 * @param manifestFile
	 */

	private fun manipulateManifestFile(manifestFile: File) {
		// read manifest file
		var manifestContent = manifestFile.readText(Charsets.UTF_8)

		// inject meta-data tags into the manifest
		manifestContent = addMetaData(manifestContent, METADATA_KEY_BUILDID, buildId)
		manifestContent = addMetaData(manifestContent, METADATA_KEY_BUILDNUMBER, buildNumber)
		manifestContent = addMetaData(manifestContent, METADATA_KEY_BRANCH, buildBranch)
		manifestContent = addMetaData(manifestContent, METADATA_KEY_FLAVOR, buildFlavor)

		// store modified manifest
		manifestFile.writeText(manifestContent, Charsets.UTF_8)
	}

	/**
	 * Inject a <meta-data> into the manifest XML.
	 * @param manifest content
	 * @param metaName meta-data key
	 * @param metaValue meta-data value
	 * @return
	 */
	private fun addMetaData(manifest: String, metaName: String, metaValue: String): String {
		val xmlAppClosingTag = "</application>"
		val metaTag = "<meta-data android:name=\"$metaName\" android:value=\"$metaValue\" />"
		return manifest.replace("${xmlAppClosingTag}", "    $metaTag\n    $xmlAppClosingTag")
	}

}