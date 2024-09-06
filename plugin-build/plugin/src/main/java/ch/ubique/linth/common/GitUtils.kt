package ch.ubique.linth.common

import java.io.BufferedReader

object GitUtils {

	/**
	 * Get the branch name from version control.
	 * @return
	 */

	fun obtainBranch(): String {
		val cmdGitBranch = "git rev-parse --abbrev-ref HEAD"
		val process = ProcessBuilder(*cmdGitBranch.split(" ").toTypedArray())
			.redirectErrorStream(true)
			.start()

		var branchName: String = process.inputStream.bufferedReader().use(BufferedReader::readText).trim()
		if (branchName.isEmpty()) {
			branchName = "develop"
		}
		println("Branch name: $branchName")
		return branchName
	}

}