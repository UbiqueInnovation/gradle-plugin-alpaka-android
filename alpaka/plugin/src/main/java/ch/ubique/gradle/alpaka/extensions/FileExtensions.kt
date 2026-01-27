package ch.ubique.gradle.alpaka.extensions

import java.io.File

internal fun File?.olderThan(other: File, orLastModified: Long): Boolean {
	return this?.lastModified()?.let {
		other.lastModified() > it || orLastModified > it
	} ?: true
}

internal fun File.listFilesOrEmpty(): List<File> {
	return listFiles()?.asList() ?: emptyList()
}
