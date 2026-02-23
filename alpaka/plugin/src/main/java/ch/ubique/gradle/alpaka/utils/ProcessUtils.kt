package ch.ubique.gradle.alpaka.utils

import java.io.BufferedReader
import java.io.File

internal object ProcessUtils {

	fun execAndRead(cmd: List<String>, projectDir: File): String {
		return ProcessBuilder(cmd)
			.directory(projectDir)
			.redirectErrorStream(true)
			.start()
			.inputStream.bufferedReader()
			.use(BufferedReader::readText)
			.trim()
	}

	fun execAndWait(cmd: List<String>, projectDir: File): Int {
		return ProcessBuilder(cmd)
			.directory(projectDir)
			.redirectErrorStream(true)
			.start()
			.waitFor()
	}

}
