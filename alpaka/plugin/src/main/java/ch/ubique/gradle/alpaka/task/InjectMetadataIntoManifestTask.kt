package ch.ubique.gradle.alpaka.task

import ch.ubique.gradle.alpaka.extensions.getMergedManifestFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault
abstract class InjectMetadataIntoManifestTask : DefaultTask() {

	companion object {
		const val METADATA_KEY_BUILD_BATCH = "ch.ubique.alpaka.build.batch"
		const val METADATA_KEY_BUILD_ID = "ch.ubique.alpaka.build.id"
		const val METADATA_KEY_BUILD_NUMBER = "ch.ubique.alpaka.build.number"
		const val METADATA_KEY_BUILD_TIMESTAMP = "ch.ubique.alpaka.build.timestamp"
		const val METADATA_KEY_BRANCH = "ch.ubique.alpaka.branch"
		const val METADATA_KEY_FLAVOR = "ch.ubique.alpaka.flavor"
	}

	init {
		group = "alpaka"
		description = "Inject build metadata into Android manifest"
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
		manifestContent = addMetadata(manifestContent, METADATA_KEY_BUILD_BATCH, buildBatch)
		manifestContent = addMetadata(manifestContent, METADATA_KEY_BUILD_ID, buildId)
		manifestContent = addMetadata(manifestContent, METADATA_KEY_BUILD_NUMBER, buildNumber.toString())
		manifestContent = addMetadata(manifestContent, METADATA_KEY_BUILD_TIMESTAMP, buildTimestamp.toString())
		manifestContent = addMetadata(manifestContent, METADATA_KEY_BRANCH, buildBranch)
		manifestContent = addMetadata(manifestContent, METADATA_KEY_FLAVOR, flavor)

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
	private fun addMetadata(manifest: String, metaName: String, metaValue: String): String {
		val xmlAppClosingTag = "</application>"
		val metaTag = "<meta-data android:name=\"$metaName\" android:value=\"$metaValue\" />"
		return manifest.replace(xmlAppClosingTag, "    $metaTag\n    $xmlAppClosingTag")
	}

}