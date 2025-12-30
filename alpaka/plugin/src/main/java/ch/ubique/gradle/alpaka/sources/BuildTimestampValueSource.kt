package ch.ubique.gradle.alpaka.sources

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

abstract class BuildTimestampValueSource : ValueSource<Long, ValueSourceParameters.None> {
	override fun obtain() = System.currentTimeMillis()
}
