package ch.ubique.gradle.alpaka.extensions

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Provider
import java.io.File

/**
 * returns File with merged manifest
 */
fun Project.getMergedManifestFile(variantName: String): Provider<File> {
	return layout.buildDirectory
		.file("intermediates/merged_manifests/${variantName}/process${variantName.capitalize()}Manifest/AndroidManifest.xml")
		.map { it.asFile }
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
				it.sourceSets.findByName(flavor),
				it.sourceSets.findByName("main")
			)
		}
		.flatMap { it.res.srcDirs }
		.filter { it.path.contains("generated").not() }

	return resDirs
}