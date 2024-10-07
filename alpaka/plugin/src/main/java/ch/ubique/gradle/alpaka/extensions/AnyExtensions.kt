package ch.ubique.gradle.alpaka.extensions

import kotlin.reflect.KProperty1

fun Any.prettyPrint(): String {
	return this::class
		.members
		.filterIsInstance<KProperty1<Any, *>>()
		.joinToString(separator = "\n") { property ->
			property.name + ": " + property.get(this).toString().replace("\n", "\\n")
		}
}
