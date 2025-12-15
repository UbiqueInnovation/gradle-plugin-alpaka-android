package ch.ubique.gradle.alpaka.git

import ch.ubique.gradle.alpaka.git.GitUtils
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File

abstract class GitBranchValueSource : ValueSource<String, GitBranchValueSource.Parameters> {

	interface Parameters : ValueSourceParameters {
		var projectDirProvider: Provider<File>
	}

	override fun obtain(): String? {
		val projectDir = parameters.projectDirProvider.get()
		return GitUtils.obtainBranch(projectDir)
	}

}