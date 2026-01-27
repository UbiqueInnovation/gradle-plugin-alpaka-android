package ch.ubique.gradle.alpaka.extensions.gradle

import org.gradle.api.provider.Provider

internal fun <T> Provider<out Collection<Collection<T>>>.flattened(): Provider<List<T>> {
    return map { it.flatten() }
}
