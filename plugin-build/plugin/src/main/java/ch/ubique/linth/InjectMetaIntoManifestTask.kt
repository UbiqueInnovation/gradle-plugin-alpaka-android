package ch.ubique.linth

import ch.ubique.linth.common.getMergedManifestFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class InjectMetaIntoManifestTask : DefaultTask() {

	companion object {
		const val METADATA_KEY_BUILD_BATCH = "ch.ubique.linth.build.batch"
		const val METADATA_KEY_BUILD_ID = "ch.ubique.linth.build.id"
		const val METADATA_KEY_BUILD_NUMBER = "ch.ubique.linth.build.number"
		const val METADATA_KEY_BUILD_TIMESTAMP = "ch.ubique.linth.build.timestamp"
		const val METADATA_KEY_BRANCH = "ch.ubique.linth.branch"
		const val METADATA_KEY_FLAVOR = "ch.ubique.linth.flavor"
	}

	init {
		description = "Inject Metadata into Manifest"
		group = "linth"
	}

	@get:Input
	abstract var variantName: String

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
		val manifestFile = project.getMergedManifestFile(variantName)
		if (manifestFile.exists()) {
			manipulateManifestFile(manifestFile)
		} else {
			throw GradleException(
				"""
				Manifest file not found for $variantName
				Tried location: ${manifestFile.absolutePath}
				""".trimIndent()
			)
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
		manifestContent = addMetaData(manifestContent, METADATA_KEY_BUILD_BATCH, buildBatch)
		manifestContent = addMetaData(manifestContent, METADATA_KEY_BUILD_ID, buildId)
		manifestContent = addMetaData(manifestContent, METADATA_KEY_BUILD_NUMBER, buildNumber.toString())
		manifestContent = addMetaData(manifestContent, METADATA_KEY_BUILD_TIMESTAMP, buildTimestamp.toString())
		manifestContent = addMetaData(manifestContent, METADATA_KEY_BRANCH, buildBranch)
		manifestContent = addMetaData(manifestContent, METADATA_KEY_FLAVOR, flavor)

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