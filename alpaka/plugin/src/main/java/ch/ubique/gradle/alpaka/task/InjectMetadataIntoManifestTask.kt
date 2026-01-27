package ch.ubique.gradle.alpaka.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
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
	abstract var buildTimestamp: Provider<out Long>

	@get:Input
	abstract var buildBranch: Provider<out String>

	@get:InputFile
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val inputManifest: RegularFileProperty

	@get:OutputFile
	abstract val outputManifest: RegularFileProperty

	@TaskAction
	fun injectMetadataIntoManifest() {
		val manifestFile = inputManifest.get().asFile
		if (manifestFile.exists()) {
			manipulateManifestFile(manifestFile, outputManifest.get().asFile)
		} else {
			throw GradleException("Manifest file not found for $variantName at expected location: ${manifestFile.absolutePath}")
		}
	}

	/**
	 * Add custom meta data to manifest.
	 * @param manifestFile
	 */
	private fun manipulateManifestFile(manifestFile: File, outputFile: File) {
		logger.info("InjectMetadataIntoManifestTask: from manifest ${manifestFile.absolutePath} to ${outputFile.absolutePath}")

		// read manifest file
		var manifestContent = manifestFile.readText(Charsets.UTF_8)

		// inject meta-data tags into the manifest
		manifestContent = addMetadata(manifestContent, METADATA_KEY_BUILD_BATCH, buildBatch)
		manifestContent = addMetadata(manifestContent, METADATA_KEY_BUILD_ID, buildId)
		manifestContent = addMetadata(manifestContent, METADATA_KEY_BUILD_NUMBER, buildNumber.toString())
		manifestContent = addMetadata(manifestContent, METADATA_KEY_BUILD_TIMESTAMP, buildTimestamp.get().toString())
		manifestContent = addMetadata(manifestContent, METADATA_KEY_BRANCH, buildBranch.get())
		manifestContent = addMetadata(manifestContent, METADATA_KEY_FLAVOR, flavor)

		// store modified manifest
		outputFile.writeText(manifestContent, Charsets.UTF_8)
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