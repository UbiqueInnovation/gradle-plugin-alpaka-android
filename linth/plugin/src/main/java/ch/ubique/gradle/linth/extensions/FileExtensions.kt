package ch.ubique.gradle.linth.extensions

import java.io.File

fun File?.olderThan(other: File, orLastModified: Long): Boolean {
	return this?.lastModified()?.let {
		other.lastModified() > it || orLastModified > it
	} ?: true
}