package ch.ubique.gradle.linth.utils

import org.gradle.api.Project
import java.io.BufferedReader

object GitUtils {

	fun obtainBranch(project: Project): String {
		val gitCommand = listOf(
			"git",
			"rev-parse",
			"--abbrev-ref",
			"HEAD",
		)
		val process = ProcessBuilder(gitCommand)
			.directory(project.rootProject.projectDir)
			.redirectErrorStream(true)
			.start()

		val branchName = process.inputStream.bufferedReader().use(BufferedReader::readText).trim()
		return branchName.takeIf { it.isNotEmpty() } ?: "develop"
	}

	fun obtainLastCommits(project: Project, numOfCommits: Int): String {
		val gitCommand = listOf(
			"git",
			"log",
			"-$numOfCommits",
			"--pretty=format:%s (%cn)",
			"--no-merges",
		)
		val process = ProcessBuilder(gitCommand)
			.directory(project.rootProject.projectDir)
			.redirectErrorStream(true)
			.start()

		return process.inputStream.bufferedReader().use(BufferedReader::readText).trim()
	}

}