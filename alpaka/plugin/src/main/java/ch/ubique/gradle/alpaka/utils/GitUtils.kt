package ch.ubique.gradle.alpaka.utils

import java.io.BufferedReader
import java.io.File

internal object GitUtils {

	fun obtainBranch(projectDir: File): String {
		val gitCommand = listOf(
			"git",
			"rev-parse",
			"--abbrev-ref",
			"HEAD",
		)
		val process = ProcessBuilder(gitCommand)
			.directory(projectDir)
			.redirectErrorStream(true)
			.start()

		val branchName = process.inputStream.bufferedReader().use(BufferedReader::readText).trim()
		return branchName.takeIf { it.isNotEmpty() } ?: "develop"
	}

	fun obtainLastCommits(projectDir: File, numOfCommits: Int): String {
		// Try to unshallow the repository if it's a shallow clone (common in CI environments like Jenkins)
		tryUnshallowRepository(projectDir, numOfCommits)

		val gitCommand = listOf(
			"git",
			"log",
			"-$numOfCommits",
			"--pretty=format:%s (%cn)",
			"--no-merges",
		)
		val process = ProcessBuilder(gitCommand)
			.directory(projectDir)
			.redirectErrorStream(true)
			.start()

		return process.inputStream.bufferedReader().use(BufferedReader::readText).trim()
	}

	private fun tryUnshallowRepository(projectDir: File, numOfCommits: Int) {
		// Check if this is a shallow clone
		val isShallowCommand = listOf("git", "rev-parse", "--is-shallow-repository")
		val isShallowProcess = ProcessBuilder(isShallowCommand)
			.directory(projectDir)
			.redirectErrorStream(true)
			.start()
		val isShallow = isShallowProcess.inputStream.bufferedReader().use(BufferedReader::readText).trim() == "true"

		if (isShallow) {
			// Fetch additional commits to deepen the shallow clone
			val deepenCommand = listOf("git", "fetch", "--deepen=$numOfCommits")
			ProcessBuilder(deepenCommand)
				.directory(projectDir)
				.redirectErrorStream(true)
				.start()
				.waitFor()
		}
	}

}