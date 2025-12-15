package ch.ubique.gradle.alpaka.git

import java.io.BufferedReader
import java.io.File

object GitUtils {

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

}