package ch.ubique.gradle.alpaka.extensions

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import java.io.File

internal fun Project.getResDirs(flavor: String) = getResDirs(setOf(flavor))

internal fun Project.getResDirs(flavors: Set<String>): List<File> {
	val androidModules: List<CommonExtension> = configurations
		.asSequence()
		.flatMap { it.dependencies }
		.filterIsInstance<ProjectDependency>()
		.map { project(it.path) }
		.distinct()
		.mapNotNull { it.extensions.findByType(CommonExtension::class.java) }
		.toList()

	val resDirs: List<File> = androidModules
		.flatMap { module ->
			flavors.map { module.sourceSets.findByName(it) }
				.plus(module.sourceSets.findByName("main"))
				.filterNotNull()
		}
		.flatMap { it.res.directories } // TODO: use variants api
		.map { File(it) }
		.filter { it.path.contains("generated/").not() }

	return resDirs
}