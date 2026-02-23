package ch.ubique.gradle.alpaka.sources

import ch.ubique.gradle.alpaka.utils.GitUtils
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File

abstract class GitCommitLogValueSource : ValueSource<String, GitCommitLogValueSource.Parameters> {

	interface Parameters : ValueSourceParameters {
		var projectDirProvider: Provider<File>
		var numOfCommits: Provider<Int>
		var allowFetch: Provider<Boolean>
	}

	override fun obtain(): String? {
		val projectDir = parameters.projectDirProvider.get()
		val numOfCommits = parameters.numOfCommits.get()
		val allowFetch = parameters.allowFetch.get()
		return GitUtils.obtainLastCommits(projectDir, numOfCommits, allowFetch)
	}

}
