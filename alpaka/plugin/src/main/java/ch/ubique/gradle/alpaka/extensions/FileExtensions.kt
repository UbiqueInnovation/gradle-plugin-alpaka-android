package ch.ubique.gradle.alpaka.extensions

import java.io.File

internal fun File.listFilesOrEmpty(): List<File> {
	return listFiles()?.asList() ?: emptyList()
}
