package ch.ubique.linth.common

import java.io.BufferedReader

object GitUtils {

	fun obtainBranch(): String {
		val cmdGitBranch = "git rev-parse --abbrev-ref HEAD"
		val process = ProcessBuilder(*cmdGitBranch.split(" ").toTypedArray())
			.redirectErrorStream(true)
			.start()

		var branchName = process.inputStream.bufferedReader().use(BufferedReader::readText).trim()
		if (branchName.isEmpty()) {
			branchName = "develop"
		}
		return branchName
	}

	fun obtainLastCommits(numOfCommits: Int = 10): String {
		/* eventuell das if-else nutzen wenn es sonst auf dem GitHub Runner nicht geht

		val process = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
			val lastCommits = "git -C ./ log -$numOfCommits --pretty=format:\"• %s\" --no-merges"
			ProcessBuilder(*lastCommits.split(" ").toTypedArray())
				.redirectErrorStream(true)
				.start()
		} else {
			val lastCommits = arrayOf("git", "-C", "./", "log", "-$numOfCommits", "--pretty=format:• %s", "--no-merges")
			ProcessBuilder(*lastCommits)
				.redirectErrorStream(true)
				.start()
		}
		 */

		val lastCommits = "git -C ./ log -$numOfCommits --pretty=format:\"• %s\" --no-merges"
		val process = ProcessBuilder(*lastCommits.split(" ").toTypedArray())
			.redirectErrorStream(true)
			.start()

		val last10Commits = process.inputStream.bufferedReader().use(BufferedReader::readText).trim()
		return last10Commits
	}

}