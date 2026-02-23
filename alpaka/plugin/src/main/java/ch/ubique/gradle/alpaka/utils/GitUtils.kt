package ch.ubique.gradle.alpaka.utils

import java.io.File

internal object GitUtils {

	fun obtainBranch(projectDir: File): String {
		val gitCommand = listOf(
			"git",
			"rev-parse",
			"--abbrev-ref",
			"HEAD",
		)
		val branchName = ProcessUtils.execAndRead(gitCommand, projectDir)
		return branchName.takeIf { it.isNotEmpty() } ?: "develop"
	}

	fun obtainLastCommits(projectDir: File, numOfCommits: Int, allowFetch: Boolean): String {
		// Try to unshallow the repository if it's a shallow clone (common in CI environments like Jenkins)
		if (allowFetch) {
			tryUnshallowRepository(projectDir, numOfCommits)
		}
		val gitCommand = listOf(
			"git",
			"log",
			"-$numOfCommits",
			"--pretty=format:%s (%cn)",
			"--no-merges",
		)
		return ProcessUtils.execAndRead(gitCommand, projectDir)
	}

	private fun tryUnshallowRepository(projectDir: File, numOfCommits: Int) {
		// Check if this is a shallow clone
		val isShallowCommand = listOf("git", "rev-parse", "--is-shallow-repository")
		val isShallow = ProcessUtils.execAndRead(isShallowCommand, projectDir) == "true"

		if (isShallow) {
			// Fetch additional commits to deepen the shallow clone
			val depth = numOfCommits * 2
			val deepenCommand = listOf("git", "fetch", "--deepen=$depth")
			ProcessUtils.execAndWait(deepenCommand, projectDir)
		}
	}

}