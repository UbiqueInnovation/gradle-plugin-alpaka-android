package ch.ubique.linth

import ch.ubique.linth.common.GitUtils
import ch.ubique.linth.common.getMergedManifestFile
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class InjectMetaIntoManifestTask : DefaultTask() {

	companion object {
		const val METADATA_KEY_BUILDID = "ch.ubique.linth.buildid"
		const val METADATA_KEY_BUILDNUMBER = "ch.ubique.linth.buildnumber"
		const val METADATA_KEY_BUILDTIME = "ch.ubique.linth.buildtime"
		const val METADATA_KEY_BUILDBATCH = "ch.ubique.linth.buildbatch"
		const val METADATA_KEY_BRANCH = "ch.ubique.linth.branch"
		const val METADATA_KEY_FLAVOR = "ch.ubique.linth.flavor"
	}

	private var buildFlavor: String = "default"

	init {
		description = "Inject Metadata into Manifest"
		group = "linth"
	}

	@get:Input
	abstract var flavor: String

	@get:Input
	abstract var buildType: String

	@get:Input
	abstract var buildId: String

	@get:Input
	abstract var buildNumber: Long

	@get:Input
	abstract var buildBatch: String

	@get:Input
	abstract var buildTimestamp: Long

	@get:Input
	abstract var buildBranch: String

	@TaskAction
	fun injectMetadataIntoManifest() {
		val manifestFile = project.getMergedManifestFile(flavor, buildType)
		if (manifestFile.exists()) {
			manipulateManifestFile(manifestFile)
		} else {
			println("Manifest file not found for flavor: $flavor and buildType: $buildType")
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
		manifestContent = addMetaData(manifestContent, METADATA_KEY_BUILDNUMBER, buildNumber.toString())
		manifestContent = addMetaData(manifestContent, METADATA_KEY_BUILDTIME, buildTimestamp.toString())
		manifestContent = addMetaData(manifestContent, METADATA_KEY_BUILDBATCH, buildBatch)
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
		return manifest.replace(xmlAppClosingTag, "    $metaTag\n    $xmlAppClosingTag")
	}

}