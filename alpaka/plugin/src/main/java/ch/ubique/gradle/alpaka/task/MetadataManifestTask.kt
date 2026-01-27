package ch.ubique.gradle.alpaka.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class MetadataManifestTask : DefaultTask() {

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
		description = "Compile build metadata for Android manifest"
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
	abstract var buildTimestamp: Provider<Long>

	@get:Input
	abstract var buildBranch: Provider<String>

	@get:OutputFile
	abstract val manifestFile: RegularFileProperty

	@TaskAction
	fun createMetadataManifest() {
		val xml = """
			<?xml version="1.0" encoding="utf-8"?>
			<manifest xmlns:android="http://schemas.android.com/apk/res/android">
				<application>
					${createMetadataTag(METADATA_KEY_BUILD_BATCH, buildBatch)}
					${createMetadataTag(METADATA_KEY_BUILD_ID, buildId)}
					${createMetadataTag(METADATA_KEY_BUILD_NUMBER, buildNumber.toString())}
					${createMetadataTag(METADATA_KEY_BUILD_TIMESTAMP, buildTimestamp.get().toString())}
					${createMetadataTag(METADATA_KEY_BRANCH, buildBranch.get())}
					${createMetadataTag(METADATA_KEY_FLAVOR, flavor)}
				</application>
			</manifest>
		""".trimIndent()

		val manifestFile = manifestFile.get().asFile
		manifestFile.writeText(xml, Charsets.UTF_8)
	}

	private fun createMetadataTag(name: String, value: String): String {
		return """<meta-data android:name="$name" android:value="$value" />"""
	}

}