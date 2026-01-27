package ch.ubique.gradle.alpaka.extensions

import java.util.Locale

internal fun String.capitalize(): String {
	return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}