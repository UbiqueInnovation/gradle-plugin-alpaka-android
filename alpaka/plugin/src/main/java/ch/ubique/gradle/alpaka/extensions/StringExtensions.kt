package ch.ubique.gradle.alpaka.extensions

import java.util.*

fun String.capitalize(): String {
	return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}