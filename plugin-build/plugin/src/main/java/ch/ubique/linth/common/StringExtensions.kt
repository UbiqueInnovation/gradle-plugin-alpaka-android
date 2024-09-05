package ch.ubique.linth.common

import java.util.Locale

fun String.capitalize(): String {
	return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}