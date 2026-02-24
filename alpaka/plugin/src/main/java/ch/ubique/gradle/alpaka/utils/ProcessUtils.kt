package ch.ubique.gradle.alpaka.utils

import org.gradle.api.logging.Logger
import org.gradle.process.ProcessExecutionException
import java.io.BufferedReader
import java.io.File

internal object ProcessUtils {

	fun exec(cmd: List<String>, projectDir: File, logger: Logger): Result<String> {
		return exec(cmd, projectDir)
			.onSuccess {
				logger.debug(cmd.joinToString(" "))
				logger.debug(it)
			}
			.onFailure {
				logger.warn(cmd.joinToString(" "))
				logger.warn(it.message)
			}
	}

	fun exec(cmd: List<String>, projectDir: File): Result<String> {
		val process = ProcessBuilder(cmd)
			.directory(projectDir)
			.redirectErrorStream(true)
			.runCatching { start() }
			.getOrElse { return@exec Result.failure(it) }
		val output = process.inputStream.bufferedReader().use(BufferedReader::readText).trim()
		val exitCode = process.waitFor()
		return if (exitCode != 0) {
			Result.failure(ProcessExecutionException(output))
		} else {
			Result.success(output)
		}
	}

}
