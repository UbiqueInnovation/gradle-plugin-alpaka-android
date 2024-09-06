package ch.ubique.linth.common


import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
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


fun Project.getResDirs(flavor: String): List<File> {
	val androidModules: List<BaseExtension> = configurations
		.asSequence()
		.flatMap { it.dependencies }
		.filterIsInstance<ProjectDependency>()
		.map { it.dependencyProject }
		.distinct()
		.mapNotNull { it.extensions.findByType(BaseExtension::class.java) }
		.toList()

	val resDirs: List<File> = androidModules
		.flatMap {
			listOfNotNull(
				it.sourceSets.findByName(flavor.lowercase()),
				it.sourceSets.findByName("main")
			)
		}
		.flatMap { it.res.srcDirs }
		.filter { it.path.contains("generated").not() }

	return resDirs
}