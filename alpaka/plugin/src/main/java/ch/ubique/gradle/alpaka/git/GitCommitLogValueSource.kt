package ch.ubique.gradle.alpaka.git

import ch.ubique.gradle.alpaka.git.GitUtils
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File

abstract class GitCommitLogValueSource : ValueSource<String, GitCommitLogValueSource.Parameters> {

	interface Parameters : ValueSourceParameters {
		var projectDirProvider: Provider<File>
		var numOfCommits: Provider<Int>
	}

	override fun obtain(): String? {
		val projectDir = parameters.projectDirProvider.get()
		val numOfCommits = parameters.numOfCommits.get()
		return GitUtils.obtainLastCommits(projectDir, numOfCommits)
	}

}
