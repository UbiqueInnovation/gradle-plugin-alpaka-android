package ch.ubique.linth.common


import org.gradle.api.Project
import java.io.File

/**
 * returns File with merged manifest
 */
fun Project.getMergedManifestFile(flavor: String, buildType: String): File {
	val variantName = flavor + buildType
	return File(
		layout.buildDirectory.asFile.get(),
		"intermediates/merged_manifests/${variantName}/process${variantName}Manifest/AndroidManifest.xml"
	)
}
